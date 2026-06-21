package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class DeleteChatRequest {
    public final UUID chatId;
    public DeleteChatRequest(@JsonProperty("chatId") UUID chatId){
        this.chatId = chatId;
    }
}
