package com.example.aichat.view.theme;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

public class ThemeEditorBackgroundImageManager {

    private final Context context;

    public ThemeEditorBackgroundImageManager(
            Context context
    ) {

        this.context =
                context.getApplicationContext();
    }

    public boolean isImage(
            Uri uri
    ) {

        String mimeType =
                context.getContentResolver()
                        .getType(
                                uri
                        );

        return mimeType == null
                || mimeType.startsWith(
                "image/"
        );
    }

    public SelectedImage copyImageToInternalStorage(
            Uri uri
    ) throws Exception {

        String originalName =
                getFileNameFromUri(
                        uri
                );

        File directory =
                new File(
                        context.getFilesDir(),
                        "theme_backgrounds"
                );

        if (!directory.exists()) {

            boolean created =
                    directory.mkdirs();

            if (!created
                    && !directory.exists()) {

                throw new IllegalStateException();
            }
        }

        String safeName =
                createSafeImageFileName(
                        uri,
                        originalName
                );

        File target =
                new File(
                        directory,
                        safeName
                );

        InputStream inputStream =
                context.getContentResolver()
                        .openInputStream(
                                uri
                        );

        if (inputStream == null) {
            throw new IllegalStateException();
        }

        FileOutputStream outputStream =
                new FileOutputStream(
                        target
                );

        byte[] buffer =
                new byte[8192];

        int length;

        while ((length = inputStream.read(
                buffer
        )) != -1) {

            outputStream.write(
                    buffer,
                    0,
                    length
            );
        }

        outputStream.flush();

        outputStream.close();

        inputStream.close();

        return new SelectedImage(
                target.getAbsolutePath(),
                target.getName()
        );
    }

    private String createSafeImageFileName(
            Uri uri,
            String originalName
    ) {

        String extension =
                getExtensionFromName(
                        originalName
                );

        if (extension == null) {

            String mime =
                    context.getContentResolver()
                            .getType(
                                    uri
                            );

            extension =
                    MimeTypeMap.getSingleton()
                            .getExtensionFromMimeType(
                                    mime
                            );
        }

        if (extension == null
                || extension.trim().isEmpty()) {

            extension =
                    "jpg";
        }

        return "theme_bg_"
                + System.currentTimeMillis()
                + "_"
                + UUID.randomUUID()
                + "."
                + extension.toLowerCase(
                Locale.US
        );
    }

    private String getFileNameFromUri(
            Uri uri
    ) {

        String result =
                null;

        if ("content".equals(
                uri.getScheme()
        )) {

            Cursor cursor =
                    context.getContentResolver()
                            .query(
                                    uri,
                                    null,
                                    null,
                                    null,
                                    null
                            );

            if (cursor != null) {

                try {

                    if (cursor.moveToFirst()) {

                        int index =
                                cursor.getColumnIndex(
                                        OpenableColumns.DISPLAY_NAME
                                );

                        if (index >= 0) {

                            result =
                                    cursor.getString(
                                            index
                                    );
                        }
                    }

                } finally {

                    cursor.close();
                }
            }
        }

        if (result == null) {

            result =
                    uri.getLastPathSegment();
        }

        return result;
    }

    private String getExtensionFromName(
            String name
    ) {

        if (name == null
                || !name.contains(".")) {

            return null;
        }

        String extension =
                name.substring(
                        name.lastIndexOf(".") + 1
                );

        if (extension.trim().isEmpty()) {
            return null;
        }

        return extension;
    }

    public static class SelectedImage {

        public final String path;

        public final String name;

        public SelectedImage(
                String path,
                String name
        ) {

            this.path =
                    path;

            this.name =
                    name;
        }
    }
}
