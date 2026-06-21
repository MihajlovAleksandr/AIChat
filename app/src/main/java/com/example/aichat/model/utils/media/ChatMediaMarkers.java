package com.example.aichat.model.utils.media;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ChatMediaMarkers {

    private static final SimpleDateFormat FILE_DATE_FORMAT =
            new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US);

    private static final String DEFAULT_FILE_NAME = "Файл";
    private static final String VOICE_PREFIX = "voice_message_";
    private static final String VIDEO_CIRCLE_PREFIX = "video_circle_";
    private static final String PLAIN_VIDEO_PREFIX = "video_message_";

    private ChatMediaMarkers() {
    }

    @NonNull
    public static String buildVoiceFileName() {
        return VOICE_PREFIX + timestamp() + ".m4a";
    }

    @NonNull
    public static String buildCircleVideoFileName() {
        return VIDEO_CIRCLE_PREFIX + timestamp() + ".mp4";
    }

    @NonNull
    public static String buildPlainVideoFileName() {
        return PLAIN_VIDEO_PREFIX + timestamp() + ".mp4";
    }

    @NonNull
    public static String buildVoiceDisplayName() {
        return buildVoiceFileName();
    }

    @NonNull
    public static String buildCircleVideoDisplayName() {
        return buildCircleVideoFileName();
    }

    @NonNull
    public static String buildPlainVideoDisplayName() {
        return buildPlainVideoFileName();
    }

    public static boolean isVoiceFileName(@Nullable String fileName) {
        String normalized = normalize(fileName);
        return normalized.startsWith(VOICE_PREFIX)
                || normalized.contains("/" + VOICE_PREFIX);
    }

    public static boolean isCircleVideoFileName(@Nullable String fileName) {
        String normalized = normalize(fileName);
        return normalized.startsWith(VIDEO_CIRCLE_PREFIX)
                || normalized.contains("/" + VIDEO_CIRCLE_PREFIX);
    }

    public static boolean isPlainVideoFileName(@Nullable String fileName) {
        String normalized = normalize(fileName);
        return normalized.startsWith(PLAIN_VIDEO_PREFIX)
                || normalized.contains("/" + PLAIN_VIDEO_PREFIX);
    }

    @NonNull
    public static String buildFileMarker(@Nullable String fileName) {
        return buildFileMarker(null, fileName);
    }

    @NonNull
    public static String buildFileMarker(@Nullable UUID fileId, @Nullable String fileName) {
        String safeName = sanitizeFileNameForMarker(fileName);

        if (fileId == null) {
            return "[file:" + safeName + "]";
        }

        return "[file:" + fileId + ":" + safeName + "]";
    }


    @NonNull
    public static String replaceFileMarkerIds(
            @Nullable String text,
            @Nullable Map<UUID, UUID> idMap
    ) {
        if (text == null || text.isEmpty() || idMap == null || idMap.isEmpty()) {
            return text == null ? "" : text;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[file:([^\\]]*)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String payload = matcher.group(1);
            String replacementMarker = matcher.group(0);

            if (payload != null) {
                String trimmedPayload = payload.trim();

                if (trimmedPayload.length() > 36 && trimmedPayload.charAt(36) == ':') {
                    try {
                        UUID oldId = UUID.fromString(trimmedPayload.substring(0, 36));
                        UUID newId = idMap.get(oldId);

                        if (newId != null) {
                            String name = trimmedPayload.substring(37).trim();
                            replacementMarker = buildFileMarker(newId, name);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            matcher.appendReplacement(
                    buffer,
                    java.util.regex.Matcher.quoteReplacement(replacementMarker)
            );
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    @NonNull
    public static String extractDisplayNameFromMarkerPayload(@Nullable String payload) {
        String value = payload == null ? "" : payload.trim();

        if (value.length() > 36 && value.charAt(36) == ':') {
            try {
                UUID.fromString(value.substring(0, 36));
                value = value.substring(37).trim();
            } catch (Exception ignored) {
            }
        }

        return sanitizeFileNameForMarker(value);
    }

    @NonNull
    public static String sanitizeFileNameForMarker(@Nullable String fileName) {
        String safeName = fileName == null ? "" : fileName.trim();

        if (safeName.isEmpty()) {
            safeName = DEFAULT_FILE_NAME;
        }

        return safeName
                .replace("[", "(")
                .replace("]", ")")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    @NonNull
    private static String normalize(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    @NonNull
    private static String timestamp() {
        synchronized (FILE_DATE_FORMAT) {
            return FILE_DATE_FORMAT.format(new Date());
        }
    }
}
