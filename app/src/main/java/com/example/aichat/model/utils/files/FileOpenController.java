package com.example.aichat.model.utils.files;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.view.main.chat.VideoPlayerActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@androidx.media3.common.util.UnstableApi
public class FileOpenController {

    private static final String TAG = "FileOpenController";

    private final Context context;
    private final FileNameUtils fileNameUtils;

    public FileOpenController(
            @NonNull Context context,
            @NonNull FileNameUtils fileNameUtils
    ) {
        this.context = context.getApplicationContext();
        this.fileNameUtils = fileNameUtils;
    }

    public void openLocalFile(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }

        try {
            File tempFile = prepareLocalFileForOpening(uri);

            if (tempFile == null || !tempFile.exists()) {
                showToast("Файл не найден");
                return;
            }

            String mimeType = fileNameUtils.getMimeTypeFromFile(tempFile);

            if (isVideo(tempFile.getName(), mimeType)) {
                openVideoInApp(tempFile, mimeType, tempFile.getName());
                return;
            }

            openFileWithIntent(tempFile, mimeType);

        } catch (Exception e) {
            Log.e(TAG, "Error opening local file", e);
            showToast("Не удалось открыть файл: " + e.getMessage());
        }
    }

    public void openDownloadedFile(
            @NonNull File file,
            @Nullable String mimeType,
            @Nullable UUID fileId
    ) {
        try {
            String finalMimeType = mimeType;

            if (finalMimeType == null || finalMimeType.equals("*/*")) {
                finalMimeType = fileNameUtils.getMimeTypeFromFile(file);
            }

            if (finalMimeType == null || finalMimeType.equals("*/*")) {
                finalMimeType = "application/octet-stream";
            }

            if (fileId != null) {
                updateLastOpened(fileId);
            }

            if (isVideo(file.getName(), finalMimeType)) {
                openVideoInApp(file, finalMimeType, file.getName());
                return;
            }

            openFileWithIntent(file, finalMimeType);

        } catch (Exception e) {
            Log.e(TAG, "Error opening downloaded file", e);
            showToast("Не удалось открыть файл: " + e.getMessage());
        }
    }

    @Nullable
    private File prepareLocalFileForOpening(@NonNull Uri uri) {
        try {
            if ("content".equals(uri.getScheme())) {
                String fileName = fileNameUtils.getFileNameFromUri(uri);

                File tempFile = new File(
                        context.getCacheDir(),
                        "temp_opened_"
                                + System.currentTimeMillis()
                                + "_"
                                + fileNameUtils.sanitizeFileName(fileName)
                );

                try (InputStream input = context.getContentResolver().openInputStream(uri);
                     OutputStream output = new FileOutputStream(tempFile)) {

                    if (input == null) {
                        return null;
                    }

                    byte[] buffer = new byte[8192];
                    int length;

                    while ((length = input.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }
                }

                return tempFile;
            }

            if ("file".equals(uri.getScheme())) {
                return new File(uri.getPath());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error preparing local file", e);
        }

        return null;
    }

    public void openFileWithIntent(
            @NonNull File file,
            @Nullable String mimeType
    ) {
        try {
            if (!file.exists()) {
                showToast("Файл не найден");
                return;
            }

            Uri fileUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file
            );

            String finalMime = mimeType;

            if (finalMime == null || finalMime.equals("*/*")) {
                finalMime = fileNameUtils.getMimeTypeFromFile(file);
            }

            if (finalMime == null || finalMime.trim().isEmpty()) {
                finalMime = "*/*";
            }

            Intent intent = buildViewIntent(fileUri, finalMime);

            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> apps = packageManager.queryIntentActivities(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
            );

            if (!apps.isEmpty()) {
                Intent chooser = Intent.createChooser(intent, "Открыть с помощью");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooser);
                return;
            }

            Intent fallback = buildViewIntent(fileUri, "*/*");

            List<ResolveInfo> fallbackApps = packageManager.queryIntentActivities(
                    fallback,
                    PackageManager.MATCH_DEFAULT_ONLY
            );

            if (!fallbackApps.isEmpty()) {
                Intent chooser = Intent.createChooser(fallback, "Открыть с помощью");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooser);
                return;
            }

            showToast("Нет приложений для открытия файла");

        } catch (Exception e) {
            Log.e(TAG, "openFile error", e);
            showToast("Ошибка открытия файла");
        }
    }

    private Intent buildViewIntent(Uri fileUri, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setDataAndType(fileUri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClipData(ClipData.newRawUri("", fileUri));

        return intent;
    }

    private void openVideoInApp(
            @NonNull File file,
            @Nullable String mimeType,
            @Nullable String title
    ) {
        Intent intent = VideoPlayerActivity.createLocalIntent(
                context,
                file.getAbsolutePath(),
                mimeType,
                title
        );

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }

    private boolean isVideo(String fileName, String mimeType) {
        if (mimeType != null && mimeType.toLowerCase(Locale.US).startsWith("video")) {
            return true;
        }

        if (fileName == null) {
            return false;
        }

        String lower = fileName.toLowerCase(Locale.US);

        return lower.endsWith(".mp4")
                || lower.endsWith(".mkv")
                || lower.endsWith(".webm")
                || lower.endsWith(".3gp")
                || lower.endsWith(".mov")
                || lower.endsWith(".avi");
    }

    private void updateLastOpened(UUID fileId) {
        new Thread(() ->
                DatabaseManager.getDatabase()
                        .fileDao()
                        .updateLastOpened(fileId, System.currentTimeMillis())
        ).start();
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
