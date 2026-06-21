package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateThemeRequest {
    public final String name;
    public final String content;

    @JsonCreator
    public CreateThemeRequest(
            @JsonProperty("name") String name,
            @JsonProperty("content") String content) {
        this.name = name;
        this.content = content;
    }
}
