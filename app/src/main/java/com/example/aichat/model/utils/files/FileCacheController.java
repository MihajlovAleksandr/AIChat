package com.example.aichat.model.utils.files;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.utils.media.audio.AudioPlayerManager;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.EnumMap;
import java.util.function.Consumer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileCacheController {

    private static final String TAG = "FileCacheController";
    private static final String PREFS_NAME = "app_prefs";
    private static final String CACHE_LIMIT_KEY = "cache_limit";

    private static final long MB = 1024L * 1024L;
    private static final long GB = 1024L * 1024L * 1024L;
    private static final long DEFAULT_CACHE_LIMIT = 1024L * 1024L * 1024L;
    private static final long MIN_CACHE_LIMIT = 100L * MB;
    private static final long MAX_CACHE_LIMIT = 5L * GB;

    private static final long TEMP_OPENED_TTL_MS = 60L * 60L * 1000L;
    private static final long PENDING_UPLOAD_TTL_MS = 24L * 60L * 60L * 1000L;

    private final Context context;
    private final FileDownloadProgressManager progressManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean cacheLimitJobRunning = new AtomicBoolean(false);
    private final AtomicBoolean clearAllJobRunning = new AtomicBoolean(false);
    private final AtomicBoolean statsJobRunning = new AtomicBoolean(false);

    public FileCacheController(
            @NonNull Context context,
            @Nullable FileDownloadProgressManager progressManager
    ) {
        this.context = context.getApplicationContext();
        this.progressManager = progressManager;
    }

    public long getCacheLimit() {
        long raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(CACHE_LIMIT_KEY, DEFAULT_CACHE_LIMIT);

        return clampCacheLimit(raw);
    }

    public void setCacheLimit(long bytes) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(CACHE_LIMIT_KEY, clampCacheLimit(bytes))
                .apply();

        enforceCacheLimit();
    }

    public void getCacheSizeAsync(@Nullable Consumer<Long> callback) {
        new Thread(() -> {
            Long size = DatabaseManager.getDatabase().fileDao().getTotalSize();
            long result = size != null ? Math.max(0L, size) : 0L;

            mainHandler.post(() -> {
                if (callback != null) {
                    callback.accept(result);
                }
            });
        }).start();
    }

    public void getCacheStatsAsync(@Nullable Consumer<FileCacheStats> callback) {
        if (!statsJobRunning.compareAndSet(false, true)) {
            return;
        }

        new Thread(() -> {
            try {
                FileCacheStats stats = buildCacheStats();
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.accept(stats);
                    }
                });
            } finally {
                statsJobRunning.set(false);
            }
        }).start();
    }

    @NonNull
    public FileCacheStats buildCacheStats() {
        EnumMap<CacheCategory, Long> bytesByCategory = new EnumMap<>(CacheCategory.class);
        EnumMap<CacheCategory, Integer> countsByCategory = new EnumMap<>(CacheCategory.class);

        for (CacheCategory category : CacheCategory.values()) {
            bytesByCategory.put(category, 0L);
            countsByCategory.put(category, 0);
        }

        long total = 0L;
        int count = 0;

        List<com.example.aichat.model.entities.File> files =
                DatabaseManager.getDatabase().fileDao().getAll();

        if (files != null) {
            for (com.example.aichat.model.entities.File entity : files) {
                if (!isValidExistingCacheEntry(entity)) {
                    if (entity != null && entity.fileId != null) {
                        DatabaseManager.getDatabase().fileDao().delete(entity.fileId);
                    }
                    continue;
                }

                long size = resolveRealSize(entity);
                CacheCategory category = CacheCategory.fromFile(entity);

                total += size;
                count++;

                Long oldBytes = bytesByCategory.get(category);
                Integer oldCount = countsByCategory.get(category);

                bytesByCategory.put(category, (oldBytes != null ? oldBytes : 0L) + size);
                countsByCategory.put(category, (oldCount != null ? oldCount : 0) + 1);
            }
        }

        return new FileCacheStats(total, count, bytesByCategory, countsByCategory);
    }

    public void clearAllDownloadedFiles(@Nullable Runnable onComplete) {
        clearCategories(null, onComplete);
    }

    public void clearCategory(@NonNull CacheCategory category, @Nullable Runnable onComplete) {
        Set<CacheCategory> categories = new HashSet<>();
        categories.add(category);
        clearCategories(categories, onComplete);
    }

    public void clearCategories(@Nullable Set<CacheCategory> categories, @Nullable Runnable onComplete) {
        if (!clearAllJobRunning.compareAndSet(false, true)) {
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
            return;
        }

        new Thread(() -> {
            try {
                List<com.example.aichat.model.entities.File> files =
                        DatabaseManager.getDatabase().fileDao().getAll();

                if (files != null) {
                    for (com.example.aichat.model.entities.File entity : files) {
                        if (entity == null) {
                            continue;
                        }

                        CacheCategory category = CacheCategory.fromFile(entity);
                        boolean mustClear = categories == null
                                || categories.isEmpty()
                                || categories.contains(category);

                        if (!mustClear || isProtectedByAudioPlayer(entity)) {
                            continue;
                        }

                        deleteCacheEntry(entity);
                    }
                }

                if ((categories == null || categories.isEmpty()) && progressManager != null) {
                    progressManager.clearAll();
                }

                if (onComplete != null) {
                    mainHandler.post(onComplete);
                }
            } finally {
                clearAllJobRunning.set(false);
            }
        }).start();
    }

    public void enforceCacheLimit() {
        if (!cacheLimitJobRunning.compareAndSet(false, true)) {
            return;
        }

        new Thread(() -> {
            try {
                Long totalSizeObj = DatabaseManager.getDatabase().fileDao().getTotalSize();
                long totalSize = totalSizeObj != null ? Math.max(0L, totalSizeObj) : 0L;
                long maxSize = getCacheLimit();

                if (totalSize <= maxSize) {
                    return;
                }

                List<com.example.aichat.model.entities.File> files =
                        DatabaseManager.getDatabase().fileDao().getOldestFirst();

                if (files == null) {
                    return;
                }

                for (com.example.aichat.model.entities.File entity : files) {
                    if (entity == null || totalSize <= maxSize) {
                        continue;
                    }

                    if (isProtectedByAudioPlayer(entity)) {
                        continue;
                    }

                    long size = resolveRealSize(entity);
                    deleteCacheEntry(entity);
                    totalSize -= Math.max(0L, size);
                }
            } finally {
                cacheLimitJobRunning.set(false);
            }
        }).start();
    }

    public void cleanupTempFiles() {
        cleanupTempFiles(false);
    }

    public void cleanupTempFiles(boolean force) {
        try {
            int deletedCount = 0;
            deletedCount += deleteNamedTempFiles(context.getCacheDir(), "temp_opened_", force ? 0L : TEMP_OPENED_TTL_MS);
            deletedCount += deleteOldPendingUploads(force ? 0L : PENDING_UPLOAD_TTL_MS);
            Log.d(TAG, "Cleaned up " + deletedCount + " temporary cache files");
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning temp files", e);
        }
    }

    public long getPendingUploadSize() {
        File uploadDir = new File(context.getCacheDir(), "pending_uploads");
        return directorySize(uploadDir);
    }

    private int deleteOldPendingUploads(long minAgeMs) {
        File uploadDir = new File(context.getCacheDir(), "pending_uploads");
        return deleteFilesOlderThan(uploadDir, minAgeMs);
    }

    private int deleteNamedTempFiles(@Nullable File root, @NonNull String prefix, long minAgeMs) {
        if (root == null) {
            return 0;
        }

        File[] files = root.listFiles();
        if (files == null) {
            return 0;
        }

        int deleted = 0;
        long now = System.currentTimeMillis();

        for (File file : files) {
            if (file == null || !file.isFile() || !file.getName().startsWith(prefix)) {
                continue;
            }

            if (minAgeMs > 0 && now - file.lastModified() < minAgeMs) {
                continue;
            }

            if (AudioPlayerManager.isProtectedAudioLocalPath(file.getAbsolutePath())) {
                continue;
            }

            if (file.delete()) {
                deleted++;
            }
        }

        return deleted;
    }

    private int deleteFilesOlderThan(@Nullable File dir, long minAgeMs) {
        if (dir == null || !dir.exists()) {
            return 0;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }

        int deleted = 0;
        long now = System.currentTimeMillis();

        for (File file : files) {
            if (file == null) {
                continue;
            }

            if (minAgeMs > 0 && now - file.lastModified() < minAgeMs) {
                continue;
            }

            if (AudioPlayerManager.isProtectedAudioLocalPath(file.getAbsolutePath())) {
                continue;
            }

            if (deleteFileOrDirectory(file)) {
                deleted++;
            }
        }

        return deleted;
    }

    private boolean isValidExistingCacheEntry(@Nullable com.example.aichat.model.entities.File entity) {
        if (entity == null || entity.localPath == null || entity.localPath.trim().isEmpty()) {
            return false;
        }

        File file = new File(entity.localPath);
        return file.exists() && file.isFile();
    }

    private long resolveRealSize(@NonNull com.example.aichat.model.entities.File entity) {
        if (entity.localPath != null) {
            File file = new File(entity.localPath);
            if (file.exists()) {
                return Math.max(0L, file.length());
            }
        }

        return Math.max(0L, entity.size);
    }

    private boolean isProtectedByAudioPlayer(@Nullable com.example.aichat.model.entities.File entity) {
        return entity != null
                && entity.localPath != null
                && AudioPlayerManager.isProtectedAudioLocalPath(entity.localPath);
    }

    private void deleteCacheEntry(@NonNull com.example.aichat.model.entities.File entity) {
        if (entity.localPath != null) {
            File file = new File(entity.localPath);

            if (file.exists() && !AudioPlayerManager.isProtectedAudioLocalPath(file.getAbsolutePath())) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }

        if (entity.fileId != null) {
            DatabaseManager.getDatabase().fileDao().delete(entity.fileId);
        }
    }

    private long directorySize(@Nullable File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }

        if (file.isFile()) {
            return Math.max(0L, file.length());
        }

        long result = 0L;
        File[] children = file.listFiles();

        if (children != null) {
            for (File child : children) {
                result += directorySize(child);
            }
        }

        return result;
    }

    private boolean deleteFileOrDirectory(@Nullable File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteFileOrDirectory(child);
                }
            }
        }

        return file.delete();
    }

    private static long clampCacheLimit(long bytes) {
        if (bytes <= 0) {
            return DEFAULT_CACHE_LIMIT;
        }

        return Math.max(MIN_CACHE_LIMIT, Math.min(MAX_CACHE_LIMIT, bytes));
    }
}
