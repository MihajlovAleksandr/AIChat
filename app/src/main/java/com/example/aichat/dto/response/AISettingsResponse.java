package com.example.aichat.dto.response;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.aichat.model.ai.AIModel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AISettingsResponse {
    @NonNull
    public final AIModel aiModel;

    @Nullable
    public final String prompt;

    @NonNull
    public final List<AIModel> models;

    @JsonCreator
    public AISettingsResponse(
            @Nullable @JsonProperty("aiModel") String aiModel,
            @Nullable @JsonProperty("prompt") String prompt,
            @Nullable @JsonProperty("availableModels") List<String> models
    ) {
        this.aiModel = AIModel.fromServerValue(aiModel);
        this.prompt = prompt;
        this.models = parseModels(models);
    }

    private AISettingsResponse(
            @Nullable AIModel aiModel,
            @Nullable String prompt,
            @Nullable List<AIModel> models,
            boolean local
    ) {
        this.aiModel = aiModel != null ? aiModel : AIModel.Default;
        this.prompt = prompt;
        this.models = models != null ? new ArrayList<>(models) : new ArrayList<>();
    }

    @NonNull
    public static AISettingsResponse local(
            @Nullable AIModel aiModel,
            @Nullable String prompt
    ) {
        return new AISettingsResponse(
                aiModel,
                prompt,
                Arrays.asList(AIModel.chatSelectableModels()),
                true
        );
    }

    @NonNull
    private static List<AIModel> parseModels(
            @Nullable List<String> rawModels
    ) {
        List<AIModel> result = new ArrayList<>();

        if (rawModels == null) {
            return result;
        }

        for (String rawModel : rawModels) {
            AIModel model = AIModel.fromServerValue(rawModel);

            if (!result.contains(model)) {
                result.add(model);
            }
        }

        return result;
    }
}
