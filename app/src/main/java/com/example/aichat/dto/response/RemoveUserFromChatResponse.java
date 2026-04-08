package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class RemoveUserFromChatResponse {
    public final UUID userId;
    public final UUID chatId;

    public RemoveUserFromChatResponse(
            @JsonProperty("userId") UUID userId,
            @JsonProperty("chatId") UUID chatId)
    {
        this.chatId = chatId;
        this.userId = userId;
    }

}