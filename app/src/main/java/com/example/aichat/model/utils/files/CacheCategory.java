package com.example.aichat.model.utils.files;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.entities.FileType;
import com.example.aichat.R;
import java.io.File;
import java.util.Locale;

public enum CacheCategory {
    VIDEO("Видео", R.drawable.ic_video),
    FILES("Файлы", R.drawable.ic_file),
    IMAGES("Фото", R.drawable.ic_image),
    VOICE("Голосовые сообщения", R.drawable.ic_audio),
    MUSIC("Музыка", R.drawable.ic_audio),
    OTHER("Другое", R.drawable.ic_file);

    private final String title;
    private final int iconRes;

    CacheCategory(@NonNull String title, @DrawableRes int iconRes) {
        this.title = title;
        this.iconRes = iconRes;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }

    @NonNull
    public static CacheCategory fromFile(@Nullable com.example.aichat.model.entities.File entity) {
        if (entity == null) {
            return OTHER;
        }

        FileType type = parseFileType(entity.fileType);
        String mimeType = entity.mimeType != null ? entity.mimeType.toLowerCase(Locale.US) : "";
        String fileName = entity.fileName;

        if ((fileName == null || fileName.trim().isEmpty()) && entity.localPath != null) {
            fileName = new File(entity.localPath).getName();
        }

        String name = fileName != null ? fileName.toLowerCase(Locale.US) : "";

        if (type == FileType.MessageImage || mimeType.startsWith("image") || hasAnyExtension(name, "jpg", "jpeg", "png", "webp", "gif", "bmp")) {
            return IMAGES;
        }

        if (type == FileType.VideoMessage || mimeType.startsWith("video") || hasAnyExtension(name, "mp4", "m4v", "mov", "mkv", "webm", "3gp", "3gpp")) {
            return VIDEO;
        }

        if (type == FileType.VoiceMessage) {
            return VOICE;
        }

        if (mimeType.startsWith("audio") || hasAnyExtension(name, "mp3", "m4a", "aac", "wav", "ogg", "flac")) {
            return MUSIC;
        }

        if (looksLikeRegularDocument(mimeType, name)) {
            return FILES;
        }

        return OTHER;
    }

    @Nullable
    private static FileType parseFileType(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        try {
            return FileType.valueOf(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean looksLikeRegularDocument(@NonNull String mimeType, @NonNull String fileName) {
        return mimeType.contains("pdf")
                || mimeType.contains("word")
                || mimeType.contains("document")
                || mimeType.contains("excel")
                || mimeType.contains("sheet")
                || mimeType.contains("zip")
                || mimeType.contains("rar")
                || mimeType.contains("text")
                || hasAnyExtension(fileName,
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                "zip", "rar", "7z", "txt", "csv", "json", "xml");
    }

    private static boolean hasAnyExtension(@NonNull String fileName, @NonNull String... extensions) {
        if (fileName.trim().isEmpty()) {
            return false;
        }

        for (String extension : extensions) {
            if (fileName.endsWith("." + extension)) {
                return true;
            }
        }

        return false;
    }
}
