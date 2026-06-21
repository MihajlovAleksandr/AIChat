package com.example.aichat.model.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AIModel {
    Default("Default", "По умолчанию"),
    DeepSeekChat("DeepSeekChat", "DeepSeek Chat"),
    OllamaQwen3_4B("OllamaQwen3_4B", "Ollama Qwen 3 4B"),
    OllamaLlama3("OllamaLlama3", "Ollama Llama 3"),
    OllamaMistral("OllamaMistral", "Ollama Mistral"),

    /*
     * На сервере enum сейчас называется OllamaGemma4е: последняя буква выглядит как
     * латинская e, но фактически это кириллическая "е". В Android-коде оставляем
     * нормальное имя enum с латинской e, а на сервер отправляем его текущее значение.
     * fromServerValue принимает оба варианта, чтобы окно кастомного промпта не ломалось.
     */
    OllamaGemma4e("OllamaGemma4е", "Ollama Gemma 4e");

    private static final String LATIN_GEMMA_VALUE = "OllamaGemma4e";
    private static final String CYRILLIC_GEMMA_VALUE = "OllamaGemma4е";

    private final String serverValue;
    private final String displayName;

    AIModel(@NonNull String serverValue, @NonNull String displayName) {
        this.serverValue = serverValue;
        this.displayName = displayName;
    }

    @NonNull
    @JsonValue
    public String getServerValue() {
        return serverValue;
    }

    @NonNull
    public String getAiSettingsServerValue(boolean hasCustomPrompt) {
        if (this == Default && hasCustomPrompt) {
            return DeepSeekChat.serverValue;
        }
        return serverValue;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    @Override
    public String toString() {
        return displayName;
    }

    @NonNull
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AIModel fromServerValue(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return Default;
        }

        String raw = value.trim();
        String normalized = normalizeModelValue(raw);

        for (AIModel model : values()) {
            if (model.serverValue.equalsIgnoreCase(raw)
                    || model.name().equalsIgnoreCase(raw)
                    || model.displayName.equalsIgnoreCase(raw)
                    || normalizeModelValue(model.serverValue).equalsIgnoreCase(normalized)
                    || normalizeModelValue(model.name()).equalsIgnoreCase(normalized)
                    || normalizeModelValue(model.displayName).equalsIgnoreCase(normalized)) {
                return model;
            }
        }

        return Default;
    }

    @NonNull
    private static String normalizeModelValue(@NonNull String value) {
        return value
                .replace('\u0435', 'e')
                .replace('\u0415', 'E');
    }

    @NonNull
    public static String getLatinGemmaValue() {
        return LATIN_GEMMA_VALUE;
    }

    @NonNull
    public static String getCyrillicGemmaValue() {
        return CYRILLIC_GEMMA_VALUE;
    }

    @NonNull
    public static AIModel[] chatSelectableModels() {
        return new AIModel[]{
                Default,
                DeepSeekChat,
                OllamaQwen3_4B,
                OllamaLlama3,
                OllamaMistral,
                OllamaGemma4e
        };
    }
}
