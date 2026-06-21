package com.example.aichat.model.utils.files;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.connection.files.UploadProgressListener;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.FileType;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;
import java.util.UUID;

public class FileDownloader {

    private static final String TAG = "FileDownloader";

    private final Context context;
    private final FileDownloadProgressManager progressManager;
    private final FileNameUtils fileNameUtils;
    private final FileOpenController fileOpenController;
    private final FileCacheController fileCacheController;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ConcurrentHashMap<UUID, CompletableFuture<File>> activeDownloads = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> lastPostedProgress = new ConcurrentHashMap<>();

    private volatile AudioDownloadListener audioDownloadListener;

    public FileDownloader(
            @NonNull Context context,
            @Nullable FileDownloadProgressManager progressManager,
            @NonNull FileNameUtils fileNameUtils,
            @NonNull FileOpenController fileOpenController,
            @NonNull FileCacheController fileCacheController
    ) {
        this.context = context.getApplicationContext();
        this.progressManager = progressManager;
        this.fileNameUtils = fileNameUtils;
        this.fileOpenController = fileOpenController;
        this.fileCacheController = fileCacheController;
    }

    public void setAudioDownloadListener(@Nullable AudioDownloadListener listener) {
        this.audioDownloadListener = listener;
    }

    public CompletableFuture<File> downloadFile(
            String url,
            String mimeType,
            UUID fileId,
            FileType fileType,
            @Nullable String token
    ) {
        if (fileId == null) {
            CompletableFuture<File> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalArgumentException("fileId is null"));
            return failed;
        }

        CompletableFuture<File> existingActiveDownload = activeDownloads.get(fileId);
        if (existingActiveDownload != null) {
            return existingActiveDownload;
        }

        CompletableFuture<File> newDownload = createDownloadFuture(url, mimeType, fileId, fileType, token);
        CompletableFuture<File> active = activeDownloads.putIfAbsent(fileId, newDownload);

        if (active != null) {
            return active;
        }

        newDownload.whenComplete((file, throwable) -> activeDownloads.remove(fileId));
        return newDownload;
    }

    private CompletableFuture<File> createDownloadFuture(
            String url,
            String mimeType,
            UUID fileId,
            FileType fileType,
            @Nullable String token
    ) {
        return CompletableFuture
                .supplyAsync(() -> findExistingLocalFile(fileId))
                .thenCompose(existingFile -> {
                    if (existingFile != null) {
                        postComplete(fileId);
                        return CompletableFuture.completedFuture(existingFile);
                    }

                    postProgress(fileId, 0);

                    HttpClient httpClient = new HttpClient(token);
                    AtomicBoolean finished = new AtomicBoolean(false);
                    startDownloadProgressHeartbeat(fileId, finished);

                    UploadProgressListener listener = progress ->
                            postProgress(fileId, Math.max(0, Math.min(100, progress.getPercent())));

                    return httpClient.downloadFileAsync(url, fileId, listener)
                            .whenComplete((file, throwable) -> {
                                finished.set(true);
                                lastPostedProgress.remove(fileId);
                            });
                })
                .thenApply(file -> {
                    if (file == null || !file.exists()) {
                        throw new IllegalStateException("Downloaded file does not exist");
                    }

                    String finalMime = normalizeMimeType(mimeType, file);
                    String fileName = resolveDownloadedFileName(url, finalMime, file);
                    long now = System.currentTimeMillis();

                    DatabaseManager.getDatabase()
                            .fileDao()
                            .insert(new com.example.aichat.model.entities.File(
                                    fileId,
                                    file.getAbsolutePath(),
                                    finalMime,
                                    fileType != null ? fileType.name() : null,
                                    file.length(),
                                    now,
                                    now,
                                    fileName
                            ));

                    lastPostedProgress.remove(fileId);
                    postComplete(fileId);
                    notifyAudioDownloadedIfNeeded(fileId, file, finalMime, fileType);
                    return file;
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "downloadFile error", throwable);
                    lastPostedProgress.remove(fileId);
                    postError(fileId);
                    return null;
                });
    }

    @Nullable
    private File findExistingLocalFile(@NonNull UUID fileId) {
        try {
            com.example.aichat.model.entities.File entity =
                    DatabaseManager.getDatabase().fileDao().getById(fileId);

            if (entity == null || entity.localPath == null || entity.localPath.trim().isEmpty()) {
                return null;
            }

            File file = new File(entity.localPath);
            if (file.exists()) {
                return file;
            }

            DatabaseManager.getDatabase().fileDao().delete(fileId);
        } catch (Exception exception) {
            Log.e(TAG, "Cannot check local file cache", exception);
        }

        return null;
    }

    public void downloadAndOpenFile(
            String url,
            String mimeType,
            UUID fileId,
            FileType fileType,
            @Nullable String token
    ) {
        downloadFile(url, mimeType, fileId, fileType, token)
                .thenAccept(file -> {
                    if (file == null || !file.exists()) {
                        return;
                    }

                    String finalMime = normalizeMimeType(mimeType, file);
                    mainHandler.post(() -> fileOpenController.openDownloadedFile(file, finalMime, fileId));
                });
    }

    public void clearState() {
        activeDownloads.clear();
        lastPostedProgress.clear();
    }

    private void startDownloadProgressHeartbeat(
            @NonNull UUID fileId,
            @NonNull AtomicBoolean finished
    ) {
        Thread thread = new Thread(() -> {
            int syntheticProgress = Math.max(1, getLastPostedProgress(fileId));

            while (!finished.get()) {
                try {
                    Thread.sleep(700L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }

                if (finished.get()) {
                    return;
                }

                int current = getLastPostedProgress(fileId);

                if (current > syntheticProgress) {
                    syntheticProgress = current;
                    continue;
                }

                if (current >= 100) {
                    return;
                }

                if (syntheticProgress < 92) {
                    syntheticProgress += syntheticProgress < 20 ? 3 : 1;
                    postProgress(fileId, Math.min(92, syntheticProgress));
                }
            }
        }, "file-download-progress-" + fileId);

        thread.setDaemon(true);
        thread.start();
    }

    private int getLastPostedProgress(@NonNull UUID fileId) {
        Integer value = lastPostedProgress.get(fileId);
        return value != null ? value : 0;
    }

    private void postProgress(@NonNull UUID fileId, int progress) {
        int safeProgress = Math.max(0, Math.min(100, progress));
        Integer previous = lastPostedProgress.get(fileId);

        if (previous != null && safeProgress < previous && previous < 100) {
            safeProgress = previous;
        }

        lastPostedProgress.put(fileId, safeProgress);

        if (progressManager == null) {
            return;
        }

        final int finalProgress = safeProgress;
        mainHandler.post(() -> progressManager.updateProgress(fileId, finalProgress));
    }

    private void postComplete(@NonNull UUID fileId) {
        lastPostedProgress.put(fileId, 100);

        if (progressManager == null) {
            return;
        }

        mainHandler.post(() -> progressManager.complete(fileId));
    }

    private void postError(@NonNull UUID fileId) {
        lastPostedProgress.remove(fileId);

        if (progressManager == null) {
            return;
        }

        mainHandler.post(() -> progressManager.error(fileId));
    }

    @NonNull
    private String normalizeMimeType(@Nullable String mimeType, @NonNull File file) {
        if (mimeType != null && !mimeType.trim().isEmpty() && !"*/*".equals(mimeType)) {
            return mimeType;
        }

        String detected = fileNameUtils.getMimeTypeFromFile(file);
        return detected != null && !detected.trim().isEmpty()
                ? detected
                : "application/octet-stream";
    }

    @NonNull
    private String resolveDownloadedFileName(
            @Nullable String url,
            @NonNull String mimeType,
            @NonNull File file
    ) {
        String fromUrl = extractFileNameFromUrl(url);

        if (fromUrl != null && !fromUrl.trim().isEmpty() && fromUrl.contains(".")) {
            return fromUrl;
        }

        String currentName = file.getName();
        if (currentName != null && !currentName.trim().isEmpty() && currentName.contains(".")) {
            return currentName;
        }

        if (mimeType.startsWith("image/")) return "image.jpg";
        if (mimeType.startsWith("video/")) return "video.mp4";
        if (mimeType.startsWith("audio/")) return "audio.mp3";
        if (mimeType.equals("application/pdf")) return "document.pdf";
        if (mimeType.startsWith("text/")) return "document.txt";

        return "file.bin";
    }

    @Nullable
    private String extractFileNameFromUrl(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        try {
            String clean = url;
            int query = clean.indexOf('?');
            if (query >= 0) clean = clean.substring(0, query);

            int hash = clean.indexOf('#');
            if (hash >= 0) clean = clean.substring(0, hash);

            int slash = clean.lastIndexOf('/');
            String value = slash >= 0 ? clean.substring(slash + 1) : clean;

            if (value.trim().isEmpty()) {
                return null;
            }

            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
            return null;
        }
    }

    private void notifyAudioDownloadedIfNeeded(
            @NonNull UUID fileId,
            @NonNull File file,
            @NonNull String mimeType,
            @Nullable FileType fileType
    ) {
        AudioDownloadListener listener = audioDownloadListener;
        if (listener == null) {
            return;
        }

        boolean audio = mimeType.toLowerCase(Locale.US).startsWith("audio/")
                || fileType == FileType.VoiceMessage;

        if (audio) {
            mainHandler.post(() -> listener.onAudioDownloaded(fileId, file.getAbsolutePath()));
        }
    }

    public interface AudioDownloadListener {
        void onAudioDownloaded(UUID fileId, String localPath);
    }
}
