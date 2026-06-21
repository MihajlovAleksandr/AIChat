package com.example.aichat.model.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum TranslateStyle {
    Formal("Formal", "Формально"),
    Neutral("Neutral", "Нейтрально"),
    Casual("Casual", "Разговорно"),
    Expressive("Expressive", "Выразительно"),
    Polite("Polite", "Вежливо"),
    Minimalist("Minimalist", "Кратко");

    private final String serverValue;
    private final String displayName;

    TranslateStyle(@NonNull String serverValue, @NonNull String displayName) {
        this.serverValue = serverValue;
        this.displayName = displayName;
    }

    @NonNull
    public String getServerValue() {
        return serverValue;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public static TranslateStyle fromServerValue(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return Neutral;
        }

        String normalized = value.trim();

        for (TranslateStyle style : values()) {
            if (style.serverValue.equalsIgnoreCase(normalized)
                    || style.name().equalsIgnoreCase(normalized)
                    || style.displayName.equalsIgnoreCase(normalized)) {
                return style;
            }
        }

        return Neutral;
    }
}
