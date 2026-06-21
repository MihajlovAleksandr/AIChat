package com.example.aichat.model.utils.media.recording;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.entities.FileType;
import com.example.aichat.model.utils.media.ChatMediaMarkers;
import java.io.File;

public final class RecordedChatMedia {

    public static final String MIME_VOICE_M4A = "audio/mp4";
    public static final String MIME_VIDEO_MP4 = "video/mp4";

    private final File file;
    private final String mimeType;
    private final FileType fileType;
    private final String fileName;
    private final long durationMs;

    private RecordedChatMedia(
            @NonNull File file,
            @NonNull String mimeType,
            @NonNull FileType fileType,
            @NonNull String fileName,
            long durationMs
    ) {
        this.file = file;
        this.mimeType = mimeType;
        this.fileType = fileType;
        this.fileName = fileName;
        this.durationMs = Math.max(0L, durationMs);
    }

    public static RecordedChatMedia voice(@NonNull File file, long durationMs) {
        return new RecordedChatMedia(
                file,
                MIME_VOICE_M4A,
                FileType.VoiceMessage,
                safeName(file, ChatMediaMarkers.buildVoiceDisplayName()),
                durationMs
        );
    }

    public static RecordedChatMedia videoCircle(@NonNull File file, long durationMs) {
        return new RecordedChatMedia(
                file,
                MIME_VIDEO_MP4,
                FileType.VideoMessage,
                safeName(file, ChatMediaMarkers.buildCircleVideoDisplayName()),
                durationMs
        );
    }

    @NonNull
    public File getFile() {
        return file;
    }

    @NonNull
    public String getMimeType() {
        return mimeType;
    }

    @NonNull
    public FileType getFileType() {
        return fileType;
    }

    @NonNull
    public String getFileName() {
        return fileName;
    }

    public long getDurationMs() {
        return durationMs;
    }

    private static String safeName(@Nullable File file, @NonNull String fallback) {
        if (file == null || file.getName() == null || file.getName().trim().isEmpty()) {
            return fallback;
        }

        return file.getName();
    }
}
