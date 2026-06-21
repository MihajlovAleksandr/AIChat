package com.example.aichat.dto.request;

import androidx.annotation.Nullable;
import com.example.aichat.model.ai.AIModel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AISettingsRequest {
    public final String aiModel;
    public final String prompt;

    @JsonCreator
    public AISettingsRequest(
            @JsonProperty("aiModel") AIModel aiModel,
            @Nullable @JsonProperty("prompt") String prompt
    ) {
        this.prompt = normalizePrompt(prompt);

        AIModel safeModel = aiModel != null ? aiModel : AIModel.Default;
        this.aiModel = safeModel.getAiSettingsServerValue(this.prompt != null);
    }

    @Nullable
    private static String normalizePrompt(@Nullable String prompt) {
        if (prompt == null) {
            return null;
        }

        String trimmed = prompt.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
