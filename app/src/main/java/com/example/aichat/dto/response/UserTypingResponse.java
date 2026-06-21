package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class UserTypingResponse {

    public final UUID userId;
    public final UUID chatId;
    public final boolean isTyping;

    @JsonCreator
    public UserTypingResponse(
            @JsonProperty("userId") UUID userId,
            @JsonProperty("chatId") UUID chatId,
            @JsonProperty("isTyping") boolean isTyping) {
        this.chatId = chatId;
        this.userId = userId;
        this.isTyping = isTyping;
    }
}
