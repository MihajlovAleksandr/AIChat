package com.example.aichat.model.utils.media.subtitles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Locale;

public final class VideoSubtitleConfig {

    public static final String STT_LANGUAGE_RU = "ru";
    public static final String STT_LANGUAGE_EN_ZH = "en";

    public static final String SUBTITLE_MODE_ORIGINAL = "original";
    public static final String SUBTITLE_MODE_TRANSLATED = "translated";
    public static final String SUBTITLE_MODE_BILINGUAL = "bilingual";

    private VideoSubtitleConfig() {
    }

    @NonNull
    public static String normalizeLanguageCode(@Nullable String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return STT_LANGUAGE_RU;
        }

        String normalized = languageCode.toLowerCase(Locale.US).trim();

        if (normalized.startsWith("en")
                || normalized.startsWith("zh")
                || normalized.startsWith("cn")
                || normalized.startsWith("cmn")
                || normalized.startsWith("yue")) {
            return STT_LANGUAGE_EN_ZH;
        }

        return STT_LANGUAGE_RU;
    }

    @NonNull
    public static String normalizeSubtitleMode(@Nullable String mode) {
        if (SUBTITLE_MODE_TRANSLATED.equals(mode)) {
            return SUBTITLE_MODE_TRANSLATED;
        }

        if (SUBTITLE_MODE_BILINGUAL.equals(mode)) {
            return SUBTITLE_MODE_BILINGUAL;
        }

        return SUBTITLE_MODE_ORIGINAL;
    }

    public static boolean isEnglishChineseLanguage(@Nullable String languageCode) {
        return STT_LANGUAGE_EN_ZH.equals(normalizeLanguageCode(languageCode));
    }

    public static boolean isSameVisibleLanguageOption(@Nullable String optionLanguage, @Nullable String currentLanguage) {
        return normalizeLanguageCode(optionLanguage).equals(normalizeLanguageCode(currentLanguage));
    }
}
