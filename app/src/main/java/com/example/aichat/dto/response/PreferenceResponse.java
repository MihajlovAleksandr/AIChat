package com.example.aichat.dto.response;

import com.example.aichat.model.entities.PreferenceGender;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PreferenceResponse {
    public final int minAge;
    public final int maxAge;
    public final PreferenceGender gender;

    @JsonCreator
    public PreferenceResponse(
            @JsonProperty("minAge") int minAge,
            @JsonProperty("maxAge") int maxAge,
            @JsonProperty("gender") PreferenceGender gender) {
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.gender = gender;
    }
}
