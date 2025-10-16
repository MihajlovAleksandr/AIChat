package com.example.aichat.dto.request;

import com.example.aichat.model.entities.PreferenceGender;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PreferenceRequest {
    public final int minAge;
    public final int maxAge;
    public final PreferenceGender gender;

    @JsonCreator
    public PreferenceRequest(
            @JsonProperty("minAge") int minAge,
            @JsonProperty("maxAge") int maxAge,
            @JsonProperty("gender") PreferenceGender gender) {
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.gender = gender;
    }

    @Override
    public String toString() {
        return "PreferenceRequest { minAge=" + minAge + ", maxAge=" + maxAge + ", gender=" + gender + " }";
    }
}
