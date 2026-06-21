package com.example.aichat.model.utils.files;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.entities.FileType;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.Set;
import java.util.UUID;

public class FileManager {

    private final Context context;
    private final FileDownloadProgressManager progressManager;

    private final FileNameUtils fileNameUtils;
    private final FileUploadCache fileUploadCache;
    private final FileOpenController fileOpenController;
    private final FileCacheController fileCacheController;
    private final FileDownloader fileDownloader;
    private final MediaAutoDownloadSettingsManager autoDownloadSettingsManager;

    public FileManager(
            @NonNull Context context,
            @Nullable FileDownloadProgressManager progressManager
    ) {
        this.context = context.getApplicationContext();
        this.progressManager = progressManager;

        this.fileNameUtils = new FileNameUtils(this.context);
        this.fileUploadCache = new FileUploadCache(this.context, fileNameUtils);
        this.fileOpenController = new FileOpenController(this.context, fileNameUtils);
        this.fileCacheController = new FileCacheController(this.context, progressManager);
        this.autoDownloadSettingsManager = new MediaAutoDownloadSettingsManager(this.context);
        this.fileDownloader = new FileDownloader(
                this.context,
                progressManager,
                fileNameUtils,
                fileOpenController,
                fileCacheController
        );
    }

    public void openLocalFile(Uri uri) {
        fileOpenController.openLocalFile(uri);
    }

    @Nullable
    public File copyUriToUploadCache(@NonNull Uri uri, @NonNull UUID fileId) {
        return fileUploadCache.copyUriToUploadCache(uri, fileId);
    }

    public CompletableFuture<File> downloadFile(
            String url,
            String mimeType,
            UUID fileId,
            FileType fileType,
            @Nullable String token
    ) {
        return fileDownloader.downloadFile(url, mimeType, fileId, fileType, token);
    }

    public void downloadAndOpenFile(
            String url,
            String mimeType,
            UUID fileId,
            FileType fileType,
            @Nullable String token
    ) {
        fileDownloader.downloadAndOpenFile(url, mimeType, fileId, fileType, token);
    }

    public void clearAllDownloadedFiles(@Nullable Runnable onComplete) {
        fileCacheController.clearAllDownloadedFiles(onComplete);
    }

    public void clearCacheCategory(@NonNull CacheCategory category, @Nullable Runnable onComplete) {
        fileCacheController.clearCategory(category, onComplete);
    }

    public void clearCacheCategories(@Nullable Set<CacheCategory> categories, @Nullable Runnable onComplete) {
        fileCacheController.clearCategories(categories, onComplete);
    }

    public void cleanupTempFiles() {
        fileCacheController.cleanupTempFiles();
        fileDownloader.clearState();
    }

    public long getCacheLimit() {
        return fileCacheController.getCacheLimit();
    }

    public void setCacheLimit(long bytes) {
        fileCacheController.setCacheLimit(bytes);
    }

    public void getCacheSizeAsync(@Nullable Consumer<Long> callback) {
        fileCacheController.getCacheSizeAsync(callback);
    }

    public void getCacheStatsAsync(@Nullable Consumer<FileCacheStats> callback) {
        fileCacheController.getCacheStatsAsync(callback);
    }

    @NonNull
    public FileCacheStats buildCacheStatsBlocking() {
        return fileCacheController.buildCacheStats();
    }

    @NonNull
    public FileCacheController getFileCacheController() {
        return fileCacheController;
    }

    @NonNull
    public MediaAutoDownloadSettingsManager getAutoDownloadSettingsManager() {
        return autoDownloadSettingsManager;
    }

    public String getFileNameFromUri(Uri uri) {
        return fileNameUtils.getFileNameFromUri(uri);
    }

    public String getMimeTypeFromFile(File file) {
        return fileNameUtils.getMimeTypeFromFile(file);
    }

    public FileDownloadProgressManager getProgressManager() {
        return progressManager;
    }

    public void setAudioDownloadListener(AudioDownloadListener listener) {
        fileDownloader.setAudioDownloadListener((fileId, localPath) -> {
            if (listener != null) {
                listener.onAudioDownloaded(fileId, localPath);
            }
        });
    }

    public interface AudioDownloadListener {
        void onAudioDownloaded(UUID fileId, String localPath);
    }
}
