package com.example.aichat.dto.response;

import java.util.UUID;

public class TypingResponse {

    public UUID chatId;
    public UUID userId;
    public boolean isTyping;

    public TypingResponse() {
    }
}
