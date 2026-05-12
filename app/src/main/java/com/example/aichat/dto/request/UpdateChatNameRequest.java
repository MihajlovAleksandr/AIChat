package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class UpdateChatNameRequest {
    public final String name;

    @JsonCreator
    public UpdateChatNameRequest(
            @JsonProperty("name") String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "UpdateChatNameRequest { name='" + name + "' }";
    }
}
