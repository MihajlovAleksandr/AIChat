package com.example.aichat.model.utils.media.video;

public final class VideoQualityOption {

    public static final String KEY_AUTO = "AUTO";

    public final String key;
    public final String label;
    public final String details;
    public final int width;
    public final int height;
    public final int bitrate;
    public final boolean auto;
    public final boolean selectable;

    public VideoQualityOption(
            String key,
            String label,
            String details,
            int width,
            int height,
            int bitrate,
            boolean auto,
            boolean selectable
    ) {
        this.key = key;
        this.label = label;
        this.details = details;
        this.width = width;
        this.height = height;
        this.bitrate = bitrate;
        this.auto = auto;
        this.selectable = selectable;
    }
}
