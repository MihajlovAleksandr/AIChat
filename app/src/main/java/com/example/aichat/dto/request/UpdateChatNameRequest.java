package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class UpdateChatNameRequest {
    public final UUID chatId;
    public final String name;

    @JsonCreator
    public UpdateChatNameRequest(
            @JsonProperty("chatId") UUID chatId,
            @JsonProperty("name") String name) {
        this.chatId = chatId;
        this.name = name;
    }

    @Override
    public String toString() {
        return "UpdateChatNameRequest { chatId=" + chatId + ", name='" + name + "' }";
    }
}
