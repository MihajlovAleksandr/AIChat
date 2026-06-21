package com.example.aichat.dto.request;

import java.util.UUID;

public class TypingRequest {

    public UUID chatId;
    public boolean isTyping;

    public TypingRequest(UUID chatId, boolean isTyping) {
        this.chatId = chatId;
        this.isTyping = isTyping;
    }
}
