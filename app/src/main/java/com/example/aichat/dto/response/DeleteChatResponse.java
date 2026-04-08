package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class DeleteChatResponse {
    public final UUID chatId;
    public DeleteChatResponse(@JsonProperty("chatId") UUID chatId){
        this.chatId = chatId;
    }
}
