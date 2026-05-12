package com.example.aichat.model.utils;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.files.UploadProgressListener;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.FileType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FileManager {

    private static final String TAG = "FileManager";
    private static final String FILE_PROVIDER_AUTHORITY_SUFFIX = ".provider";

    private AudioDownloadListener audioDownloadListener;
    private static final long MAX_CACHE_SIZE = 1024L * 1024L * 1024L;
    private final Context context;
    private final Fragment fragment;
    private final Map<UUID, File> downloadingFiles = new HashMap<>();
    private final Map<UUID, String> downloadingMimeTypes = new HashMap<>();

    private final FileDownloadProgressManager progressManager;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    public FileManager(@NonNull Context context,
                       FileDownloadProgressManager progressManager) {
        this.context = context.getApplicationContext();
        this.fragment = null;
        this.progressManager = progressManager;
    }

    public void openLocalFile(Uri uri) {
        if (uri == null) return;

        try {
            File tempFile = prepareLocalFileForOpening(uri);
            if (tempFile == null || !tempFile.exists()) {
                showToast("Файл не найден");
                return;
            }

            openFileWithIntent(tempFile, getMimeTypeFromFile(tempFile));

        } catch (Exception e) {
            Log.e(TAG, "Error opening local file", e);
            showToast("Не удалось открыть файл: " + e.getMessage());
        }
    }

    public void setAudioDownloadListener(
            AudioDownloadListener listener
    ) {

        this.audioDownloadListener = listener;
    }
    @Nullable
    private File prepareLocalFileForOpening(Uri uri) {
        try {
            if ("content".equals(uri.getScheme())) {
                String fileName = getFileNameFromUri(uri);
                File tempFile = new File(context.getCacheDir(), "temp_opened_" + System.currentTimeMillis() + "_" + fileName);

                try (InputStream input = context.getContentResolver().openInputStream(uri);
                     OutputStream output = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = input.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }
                }
                return tempFile;

            } else if ("file".equals(uri.getScheme())) {
                return new File(uri.getPath());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error preparing local file", e);
        }
        return null;
    }

    public CompletableFuture<File> downloadFile(String url,
                                                String mimeType,
                                                UUID fileId,
                                                FileType fileType,
                                                @Nullable String token) {

        if (downloadingFiles.containsKey(fileId)) {

            CompletableFuture<File> future =
                    new CompletableFuture<>();

            future.completeExceptionally(
                    new Exception("File already downloading")
            );

            return future;
        }

        return CompletableFuture
                .supplyAsync(() -> {

                    com.example.aichat.model.entities.File existing =
                            DatabaseManager.getDatabase()
                                    .fileDao()
                                    .getById(fileId);

                    if (existing != null &&
                            existing.localPath != null) {

                        File localFile =
                                new File(existing.localPath);

                        if (localFile.exists()) {
                            return localFile;
                        }

                        DatabaseManager.getDatabase()
                                .fileDao()
                                .delete(fileId);
                    }

                    return null;
                })

                .thenCompose(existingFile -> {

                    if (existingFile != null) {

                        mainHandler.post(() -> {

                            if (progressManager != null) {

                                progressManager.complete(fileId);
                            }
                        });

                        return CompletableFuture.completedFuture(
                                existingFile
                        );
                    }

                    HttpClient httpClient =
                            new HttpClient(token);

                    UploadProgressListener progressListener =
                            progress -> {

                                int percent =
                                        progress.getPercent();

                                mainHandler.post(() -> {

                                    if (progressManager != null) {

                                        progressManager.updateProgress(
                                                fileId,
                                                percent
                                        );
                                    }
                                });
                            };

                    return httpClient.downloadFileAsync(
                            url,
                            fileId,
                            progressListener
                    );
                })

                .thenApply(file -> {

                    if (file == null ||
                            !file.exists()) {

                        return null;
                    }

                    if (progressManager != null) {

                        progressManager.complete(fileId);
                    }

                    String finalMime = mimeType;

                    if (finalMime == null ||
                            finalMime.equals("*/*")) {

                        finalMime =
                                getMimeTypeFromFile(file);
                    }

                    long now =
                            System.currentTimeMillis();

                    String originalName =
                            extractFileNameFromUrl(
                                    url,
                                    finalMime
                            );

                    DatabaseManager.getDatabase()
                            .fileDao()
                            .insert(
                                    new com.example.aichat.model.entities.File(
                                            fileId,
                                            file.getAbsolutePath(),
                                            finalMime,
                                            fileType != null
                                                    ? fileType.name()
                                                    : null,
                                            file.length(),
                                            now,
                                            now,
                                            originalName
                                    )
                            );

                    downloadingFiles.put(
                            fileId,
                            file
                    );

                    mainHandler.post(
                            this::enforceCacheLimit
                    );

                    return file;
                })

                .exceptionally(throwable -> {

                    if (progressManager != null) {

                        progressManager.error(fileId);
                    }

                    downloadingFiles.remove(fileId);

                    return null;
                });
    }

    public FileDownloadProgressManager getProgressManager() {
        return progressManager;
    }

    public void clearAllDownloadedFiles(Runnable onComplete) {
        new Thread(() -> {

            List<com.example.aichat.model.entities.File> files =
                    DatabaseManager.getDatabase().fileDao().getAll();

            if (files != null) {
                for (com.example.aichat.model.entities.File entity : files) {
                    File f = new File(entity.localPath);
                    if (f.exists()) {
                        f.delete();
                    }
                }
            }

            DatabaseManager.getDatabase().fileDao().clearAll();

            downloadingFiles.clear();
            downloadingMimeTypes.clear();

            if (progressManager != null) {
                progressManager.clearAll();
            }

            mainHandler.post(onComplete);

        }).start();
    }
    public void downloadAndOpenFile(String url,
                                    String mimeType,
                                    UUID fileId,
                                    FileType fileType,
                                    @Nullable String token) {

        new Thread(() -> {

            com.example.aichat.model.entities.File entity =
                    DatabaseManager.getDatabase().fileDao().getById(fileId);

            if (entity != null) {
                File file = new File(entity.localPath);

                if (file.exists()) {
                    mainHandler.post(() ->
                            openDownloadedFile(file, entity.mimeType, fileId)
                    );
                    return;
                } else {
                    DatabaseManager.getDatabase().fileDao().delete(fileId);
                }
            }

            downloadFile(url, mimeType, fileId, fileType, token)
                    .thenAccept(file -> {
                        if (file == null) return;

                        mainHandler.post(() -> {
                            String finalMime = getMimeTypeFromFile(file);

                            if (progressManager != null) {
                                progressManager.complete(fileId);
                            }

                            openDownloadedFile(file, finalMime, fileId);
                        });
                    });

        }).start();
    }
    private void openDownloadedFile(File file, String mimeType, UUID fileId) {
        if (context == null) return;

        try {
            String finalMimeType = mimeType;

            if (finalMimeType == null || finalMimeType.equals("*/*")) {
                finalMimeType = getMimeTypeFromFile(file);
            }

            if (finalMimeType == null || finalMimeType.equals("*/*")) {
                finalMimeType = "application/octet-stream";
            }

            updateLastOpened(fileId);

            openFileWithIntent(file, finalMimeType);

        } catch (Exception e) {
            Log.e(TAG, "Error opening downloaded file", e);
            showToast("Не удалось открыть файл: " + e.getMessage());
        } finally {
            downloadingFiles.remove(fileId);
            downloadingMimeTypes.remove(fileId);
        }
    }
    private void openFileWithIntent(File file, String mimeType) {
        if (context == null) return;

        try {
            Uri fileUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file
            );

            String finalMime = mimeType;

            if (finalMime == null || finalMime.equals("*/*")) {
                finalMime = getMimeTypeFromFile(file);
            }

            if (finalMime == null) {
                finalMime = "*/*";
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, finalMime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setClipData(ClipData.newRawUri("", fileUri));

            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> apps = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

            if (!apps.isEmpty()) {
                Intent chooser = Intent.createChooser(intent, "Открыть с помощью");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooser);
            } else {
                Intent fallback = new Intent(Intent.ACTION_VIEW);
                fallback.setDataAndType(fileUri, "*/*");
                fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                fallback.setClipData(ClipData.newRawUri("", fileUri));

                List<ResolveInfo> fallbackApps = pm.queryIntentActivities(fallback, PackageManager.MATCH_DEFAULT_ONLY);

                if (!fallbackApps.isEmpty()) {
                    Intent chooser = Intent.createChooser(fallback, "Открыть с помощью");
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(chooser);
                } else {
                    showToast("Нет приложений для открытия файла");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "openFile error", e);
            showToast("Ошибка открытия файла");
        }
    }

    public long getCacheLimit() {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getLong("cache_limit", 1024L * 1024 * 1024);
    }

    private void updateLastOpened(UUID fileId) {
        new Thread(() ->
                DatabaseManager.getDatabase()
                        .fileDao()
                        .updateLastOpened(fileId, System.currentTimeMillis())
        ).start();
    }

    private long getMaxCacheSize() {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getLong("cache_limit", 1024L * 1024L * 1024L);
    }


    public String getFileNameFromUri(Uri uri) {
        String fileName = null;

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name from URI", e);
            }
        }

        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }

        return fileName != null ? fileName : "file_" + System.currentTimeMillis();
    }

    private String extractFileNameFromUrl(String url, String mimeType) {
        String fileName = null;

        if (url != null && url.contains("/")) {
            String[] parts = url.split("/");
            String lastPart = parts[parts.length - 1];
            if (lastPart.contains(".")) {
                fileName = lastPart;
            }
        }
        if (fileName == null && mimeType != null) {
            if (mimeType.contains("image")) fileName = "image.jpg";
            else if (mimeType.contains("pdf")) fileName = "document.pdf";
            else if (mimeType.contains("video")) fileName = "video.mp4";
            else fileName = "file.bin";
        }

        return fileName != null ? fileName : "file_" + System.currentTimeMillis();
    }

    public String getMimeTypeFromFile(File file) {
        String fileName = file.getName();
        String extension = "";
        int lastDot = fileName.lastIndexOf('.');

        if (lastDot > 0) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType != null) {
                return mimeType;
            }
        }
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "mp4":
                return "video/mp4";
            case "mp3":
                return "audio/mpeg";
            case "zip":
                return "application/zip";
            default:
                return "application/octet-stream";
        }
    }

    private void enforceCacheLimit() {

        new Thread(() -> {

            Long totalSizeObj = DatabaseManager.getDatabase().fileDao().getTotalSize();
            long totalSize = totalSizeObj != null ? totalSizeObj : 0;

            long maxSize = getMaxCacheSize();

            if (totalSize <= maxSize) return;

            List<com.example.aichat.model.entities.File> files =
                    DatabaseManager.getDatabase().fileDao().getOldestFirst();

            if (files == null) return;

            for (com.example.aichat.model.entities.File entity : files) {

                if (totalSize <= maxSize) break;

                File file = new File(entity.localPath);

                if (file.exists() && file.delete()) {
                    totalSize -= entity.size;
                }

                DatabaseManager.getDatabase().fileDao().delete(entity.fileId);
            }

        }).start();
    }

    public void cleanupTempFiles() {
        try {
            File cacheDir = context.getCacheDir();
            File[] files = cacheDir.listFiles();

            if (files != null) {
                int deletedCount = 0;

                for (File file : files) {

                    if (file.getName().startsWith("temp_opened_")) {
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                }

                Log.d(TAG, "Cleaned up " + deletedCount + " temp files");
            }

            downloadingFiles.clear();
            downloadingMimeTypes.clear();

        } catch (Exception e) {
            Log.e(TAG, "Error cleaning temp files", e);
        }
    }

    public void setCacheLimit(long bytes) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putLong("cache_limit", bytes)
                .apply();
    }

    public void getCacheSizeAsync(java.util.function.Consumer<Long> callback) {
        new Thread(() -> {
            Long size = DatabaseManager.getDatabase().fileDao().getTotalSize();
            long result = size != null ? size : 0;

            new Handler(Looper.getMainLooper()).post(() -> callback.accept(result));
        }).start();
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public interface AudioDownloadListener {
        void onAudioDownloaded(
                UUID fileId,
                String localPath
        );
    }

}