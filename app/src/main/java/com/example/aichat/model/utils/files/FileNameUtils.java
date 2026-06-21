package com.example.aichat.model.utils.files;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.net.URLDecoder;
import java.util.Locale;

public class FileNameUtils {

    private static final String TAG = "FileNameUtils";

    private final Context context;

    public FileNameUtils(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public String getFileNameFromUri(@Nullable Uri uri) {
        if (uri == null) {
            return "file_" + System.currentTimeMillis();
        }

        String fileName = null;

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null,
                    null,
                    null
            )) {
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

        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = uri.getLastPathSegment();
        }

        return fileName != null && !fileName.trim().isEmpty()
                ? fileName
                : "file_" + System.currentTimeMillis();
    }

    public String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "file_" + System.currentTimeMillis();
        }

        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public String extractFileNameFromUrl(String url, String mimeType) {
        String fileName = null;

        try {
            if (url != null && !url.trim().isEmpty()) {
                String cleanUrl = url;

                int queryIndex = cleanUrl.indexOf("?");

                if (queryIndex >= 0) {
                    cleanUrl = cleanUrl.substring(0, queryIndex);
                }

                int hashIndex = cleanUrl.indexOf("#");

                if (hashIndex >= 0) {
                    cleanUrl = cleanUrl.substring(0, hashIndex);
                }

                int slashIndex = cleanUrl.lastIndexOf("/");

                if (slashIndex >= 0 && slashIndex < cleanUrl.length() - 1) {
                    fileName = cleanUrl.substring(slashIndex + 1);
                }

                if (fileName != null) {
                    fileName = URLDecoder.decode(fileName, "UTF-8");
                }
            }
        } catch (Exception ignored) {
        }

        if (fileName == null || fileName.trim().isEmpty() || !fileName.contains(".")) {
            fileName = getFallbackFileNameByMime(mimeType);
        }

        return fileName != null && !fileName.trim().isEmpty()
                ? sanitizeFileName(fileName)
                : "file_" + System.currentTimeMillis();
    }

    private String getFallbackFileNameByMime(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            return "file.bin";
        }

        String lower = mimeType.toLowerCase(Locale.US);

        if (lower.contains("image")) {
            return "image.jpg";
        }

        if (lower.contains("pdf")) {
            return "document.pdf";
        }

        if (lower.contains("video")) {
            return "video.mp4";
        }

        if (lower.contains("audio")) {
            return "audio.mp3";
        }

        if (lower.contains("text")) {
            return "document.txt";
        }

        return "file.bin";
    }

    public String getMimeTypeFromFile(@Nullable File file) {
        if (file == null) {
            return "application/octet-stream";
        }

        String fileName = file.getName();
        String extension = "";

        int lastDot = fileName.lastIndexOf('.');

        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            extension = fileName.substring(lastDot + 1).toLowerCase(Locale.US);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

            if (mimeType != null && !mimeType.trim().isEmpty()) {
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

            case "mkv":
                return "video/x-matroska";

            case "webm":
                return "video/webm";

            case "mp3":
                return "audio/mpeg";

            case "wav":
                return "audio/wav";

            case "ogg":
                return "audio/ogg";

            case "m4a":
                return "audio/mp4";

            case "zip":
                return "application/zip";

            case "rar":
                return "application/vnd.rar";

            default:
                return "application/octet-stream";
        }
    }
}
