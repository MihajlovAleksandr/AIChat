package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class ChatUserActionResponse {

    public final UUID chatId;
    public final UUID userId;
    public final String action;

    @JsonCreator
    public ChatUserActionResponse(
            @JsonProperty("chatId") UUID chatId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("action") String action
    ) {
        this.chatId = chatId;
        this.userId = userId;
        this.action = action;
    }

    @Override
    public String toString() {
        return "ChatUserActionResponse{" +
                "chatId=" + chatId +
                ", userId=" + userId +
                ", action='" + action + '\'' +
                '}';
    }
}