package com.example.aichat.model.utils.files;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.entities.FileType;
import java.util.Locale;

public final class MediaAutoDownloadSettingsManager {

    private static final String PREFS_NAME = "media_auto_download_settings";

    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_TRAFFIC_PRESET = "traffic_preset";
    private static final String KEY_PHOTO = "photo";
    private static final String KEY_VIDEO = "video";
    private static final String KEY_FILES = "files";
    private static final String KEY_STORIES = "stories";

    private static final int TRAFFIC_LOW = 0;
    private static final int TRAFFIC_MEDIUM = 1;
    private static final int TRAFFIC_HIGH = 2;

    public enum NetworkMode {
        MOBILE("mobile", "Через мобильную сеть"),
        WIFI("wifi", "Через сети Wi‑Fi"),
        ROAMING("roaming", "В роуминге");

        public final String key;
        public final String title;

        NetworkMode(@NonNull String key, @NonNull String title) {
            this.key = key;
            this.title = title;
        }

        @NonNull
        public static NetworkMode fromKey(@Nullable String key) {
            if (key == null) {
                return MOBILE;
            }

            for (NetworkMode mode : values()) {
                if (mode.key.equals(key)) {
                    return mode;
                }
            }

            return MOBILE;
        }
    }

    public static final class NetworkPolicy {
        public final NetworkMode mode;
        public final boolean enabled;
        public final int trafficPreset;
        public final boolean photoEnabled;
        public final boolean videoEnabled;
        public final boolean filesEnabled;
        public final boolean storiesEnabled;
        public final long maxVideoBytes;
        public final long maxFileBytes;

        NetworkPolicy(
                @NonNull NetworkMode mode,
                boolean enabled,
                int trafficPreset,
                boolean photoEnabled,
                boolean videoEnabled,
                boolean filesEnabled,
                boolean storiesEnabled
        ) {
            this.mode = mode;
            this.enabled = enabled;
            this.trafficPreset = Math.max(TRAFFIC_LOW, Math.min(TRAFFIC_HIGH, trafficPreset));
            this.photoEnabled = photoEnabled;
            this.videoEnabled = videoEnabled;
            this.filesEnabled = filesEnabled;
            this.storiesEnabled = storiesEnabled;
            this.maxVideoBytes = resolveVideoLimit(mode, this.trafficPreset);
            this.maxFileBytes = resolveFileLimit(mode, this.trafficPreset);
        }

        @NonNull
        public String getSummary() {
            if (!enabled) {
                return "Выключено";
            }

            StringBuilder builder = new StringBuilder();

            if (photoEnabled) {
                builder.append("Фото");
            }

            if (videoEnabled) {
                appendComma(builder);
                builder.append("Видео (").append(FileCacheStats.formatBytes(maxVideoBytes)).append(")");
            }

            if (filesEnabled) {
                appendComma(builder);
                builder.append("Файлы (").append(FileCacheStats.formatBytes(maxFileBytes)).append(")");
            }

            if (storiesEnabled) {
                appendComma(builder);
                builder.append("Истории");
            }

            return builder.length() > 0 ? builder.toString() : "Ничего не выбрано";
        }

        private static void appendComma(@NonNull StringBuilder builder) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
        }
    }

    private final Context context;
    private final SharedPreferences prefs;

    public MediaAutoDownloadSettingsManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public NetworkPolicy getPolicy(@NonNull NetworkMode mode) {
        return new NetworkPolicy(
                mode,
                prefs.getBoolean(key(mode, KEY_ENABLED), defaultEnabled(mode)),
                prefs.getInt(key(mode, KEY_TRAFFIC_PRESET), defaultTrafficPreset(mode)),
                prefs.getBoolean(key(mode, KEY_PHOTO), true),
                prefs.getBoolean(key(mode, KEY_VIDEO), mode != NetworkMode.ROAMING),
                prefs.getBoolean(key(mode, KEY_FILES), mode != NetworkMode.ROAMING),
                prefs.getBoolean(key(mode, KEY_STORIES), mode != NetworkMode.ROAMING)
        );
    }

    public void savePolicy(@NonNull NetworkPolicy policy) {
        prefs.edit()
                .putBoolean(key(policy.mode, KEY_ENABLED), policy.enabled)
                .putInt(key(policy.mode, KEY_TRAFFIC_PRESET), policy.trafficPreset)
                .putBoolean(key(policy.mode, KEY_PHOTO), policy.photoEnabled)
                .putBoolean(key(policy.mode, KEY_VIDEO), policy.videoEnabled)
                .putBoolean(key(policy.mode, KEY_FILES), policy.filesEnabled)
                .putBoolean(key(policy.mode, KEY_STORIES), policy.storiesEnabled)
                .apply();
    }

    public void setEnabled(@NonNull NetworkMode mode, boolean enabled) {
        prefs.edit().putBoolean(key(mode, KEY_ENABLED), enabled).apply();
    }

    public void setTrafficPreset(@NonNull NetworkMode mode, int preset) {
        prefs.edit().putInt(key(mode, KEY_TRAFFIC_PRESET), Math.max(TRAFFIC_LOW, Math.min(TRAFFIC_HIGH, preset))).apply();
    }

    public void setPhotoEnabled(@NonNull NetworkMode mode, boolean enabled) {
        prefs.edit().putBoolean(key(mode, KEY_PHOTO), enabled).apply();
    }

    public void setVideoEnabled(@NonNull NetworkMode mode, boolean enabled) {
        prefs.edit().putBoolean(key(mode, KEY_VIDEO), enabled).apply();
    }

    public void setFilesEnabled(@NonNull NetworkMode mode, boolean enabled) {
        prefs.edit().putBoolean(key(mode, KEY_FILES), enabled).apply();
    }

    public void setStoriesEnabled(@NonNull NetworkMode mode, boolean enabled) {
        prefs.edit().putBoolean(key(mode, KEY_STORIES), enabled).apply();
    }

    public void resetToDefaults() {
        prefs.edit().clear().apply();
    }

    public boolean shouldAutoDownload(
            @NonNull NetworkMode mode,
            @Nullable String mimeType,
            @Nullable FileType fileType,
            @Nullable String fileName,
            long fileSize
    ) {
        NetworkPolicy policy = getPolicy(mode);

        if (!policy.enabled) {
            return false;
        }

        CacheCategory category = categoryFromMetadata(mimeType, fileType, fileName);
        long safeSize = Math.max(0L, fileSize);

        switch (category) {
            case IMAGES:
                return policy.photoEnabled;

            case VIDEO:
                return policy.videoEnabled && (safeSize <= 0 || safeSize <= policy.maxVideoBytes);

            case FILES:
            case OTHER:
                return policy.filesEnabled && (safeSize <= 0 || safeSize <= policy.maxFileBytes);

            case MUSIC:
            case VOICE:
                return true;

            default:
                return false;
        }
    }

    @NonNull
    private static CacheCategory categoryFromMetadata(
            @Nullable String mimeType,
            @Nullable FileType fileType,
            @Nullable String fileName
    ) {
        String mime = mimeType != null ? mimeType.toLowerCase(Locale.US) : "";
        String name = fileName != null ? fileName.toLowerCase(Locale.US) : "";

        if (fileType == FileType.MessageImage || mime.startsWith("image") || hasExtension(name, "jpg", "jpeg", "png", "webp", "gif")) {
            return CacheCategory.IMAGES;
        }

        if (fileType == FileType.VideoMessage || mime.startsWith("video") || hasExtension(name, "mp4", "mkv", "webm", "mov")) {
            return CacheCategory.VIDEO;
        }

        if (fileType == FileType.VoiceMessage) {
            return CacheCategory.VOICE;
        }

        if (mime.startsWith("audio") || hasExtension(name, "mp3", "wav", "m4a", "aac", "flac", "ogg")) {
            return CacheCategory.MUSIC;
        }

        return CacheCategory.FILES;
    }

    private static boolean hasExtension(@NonNull String fileName, @NonNull String... extensions) {
        for (String extension : extensions) {
            if (fileName.endsWith("." + extension)) {
                return true;
            }
        }

        return false;
    }

    private static String key(@NonNull NetworkMode mode, @NonNull String suffix) {
        return mode.key + "_" + suffix;
    }

    private static boolean defaultEnabled(@NonNull NetworkMode mode) {
        return mode != NetworkMode.ROAMING;
    }

    private static int defaultTrafficPreset(@NonNull NetworkMode mode) {
        if (mode == NetworkMode.WIFI) {
            return TRAFFIC_HIGH;
        }

        if (mode == NetworkMode.ROAMING) {
            return TRAFFIC_LOW;
        }

        return TRAFFIC_MEDIUM;
    }

    private static long resolveVideoLimit(@NonNull NetworkMode mode, int preset) {
        if (mode == NetworkMode.WIFI) {
            switch (preset) {
                case TRAFFIC_LOW:
                    return 10L * 1024L * 1024L;
                case TRAFFIC_HIGH:
                    return 50L * 1024L * 1024L;
                case TRAFFIC_MEDIUM:
                default:
                    return 15L * 1024L * 1024L;
            }
        }

        if (mode == NetworkMode.ROAMING) {
            switch (preset) {
                case TRAFFIC_MEDIUM:
                    return 2L * 1024L * 1024L;
                case TRAFFIC_HIGH:
                    return 5L * 1024L * 1024L;
                case TRAFFIC_LOW:
                default:
                    return 0L;
            }
        }

        switch (preset) {
            case TRAFFIC_LOW:
                return 3L * 1024L * 1024L;
            case TRAFFIC_HIGH:
                return 20L * 1024L * 1024L;
            case TRAFFIC_MEDIUM:
            default:
                return 10L * 1024L * 1024L;
        }
    }

    private static long resolveFileLimit(@NonNull NetworkMode mode, int preset) {
        if (mode == NetworkMode.WIFI) {
            switch (preset) {
                case TRAFFIC_LOW:
                    return 1L * 1024L * 1024L;
                case TRAFFIC_HIGH:
                    return 10L * 1024L * 1024L;
                case TRAFFIC_MEDIUM:
                default:
                    return 3L * 1024L * 1024L;
            }
        }

        if (mode == NetworkMode.ROAMING) {
            switch (preset) {
                case TRAFFIC_MEDIUM:
                    return 512L * 1024L;
                case TRAFFIC_HIGH:
                    return 1L * 1024L * 1024L;
                case TRAFFIC_LOW:
                default:
                    return 0L;
            }
        }

        switch (preset) {
            case TRAFFIC_LOW:
                return 512L * 1024L;
            case TRAFFIC_HIGH:
                return 5L * 1024L * 1024L;
            case TRAFFIC_MEDIUM:
            default:
                return 1L * 1024L * 1024L;
        }
    }
}
