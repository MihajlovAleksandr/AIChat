package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class EndChatRequest {
    public final UUID chatId;

    @JsonCreator
    public EndChatRequest(@JsonProperty("chatId") UUID chatId) {
        this.chatId = chatId;
    }

    @Override
    public String toString() {
        return "EndChatRequest { chatId=" + chatId + " }";
    }
}
