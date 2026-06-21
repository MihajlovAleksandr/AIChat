package com.example.aichat.model.utils.files;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.utils.media.audio.AudioPlayerManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class FileUploadCache {

    private static final String TAG = "FileUploadCache";
    private static final String UPLOAD_DIR_NAME = "pending_uploads";
    private static final int BUFFER_SIZE = 64 * 1024;

    private final Context context;
    private final FileNameUtils fileNameUtils;

    public FileUploadCache(
            @NonNull Context context,
            @NonNull FileNameUtils fileNameUtils
    ) {
        this.context = context.getApplicationContext();
        this.fileNameUtils = fileNameUtils;
    }

    @Nullable
    public File copyUriToUploadCache(
            @NonNull Uri uri,
            @NonNull UUID fileId
    ) {
        File targetFile = null;

        try {
            String originalName = fileNameUtils.getFileNameFromUri(uri);

            if (originalName == null || originalName.trim().isEmpty()) {
                originalName = "file_" + System.currentTimeMillis();
            }

            File uploadDir = getUploadDir();

            if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                Log.e(TAG, "Cannot create upload cache directory: " + uploadDir.getAbsolutePath());
                return null;
            }

            targetFile = new File(
                    uploadDir,
                    fileId + "_" + fileNameUtils.sanitizeFileName(originalName)
            );

            if (targetFile.exists() && targetFile.length() > 0) {
                return targetFile;
            }

            try (InputStream input = context.getContentResolver().openInputStream(uri);
                 OutputStream output = new FileOutputStream(targetFile, false)) {

                if (input == null) {
                    deleteQuietly(targetFile);
                    return null;
                }

                byte[] buffer = new byte[BUFFER_SIZE];
                int length;

                while ((length = input.read(buffer)) != -1) {
                    if (length > 0) {
                        output.write(buffer, 0, length);
                    }
                }

                output.flush();
            }

            if (!targetFile.exists() || targetFile.length() <= 0) {
                deleteQuietly(targetFile);
                return null;
            }

            return targetFile;

        } catch (Exception e) {
            Log.e(TAG, "copyUriToUploadCache error", e);
            deleteQuietly(targetFile);
            return null;
        }
    }

    public long getUploadCacheSize() {
        return directorySize(getUploadDir());
    }

    public int cleanupOldPendingUploads(long minAgeMs) {
        File[] files = getUploadDir().listFiles();

        if (files == null) {
            return 0;
        }

        int deleted = 0;
        long now = System.currentTimeMillis();

        for (File file : files) {
            if (file == null || !file.isFile()) {
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

    @NonNull
    private File getUploadDir() {
        return new File(context.getCacheDir(), UPLOAD_DIR_NAME);
    }

    private static void deleteQuietly(@Nullable File file) {
        if (file != null && file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private static long directorySize(@Nullable File file) {
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
}
