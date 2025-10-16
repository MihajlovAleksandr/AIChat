package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class UsersInChatRequest {
    public final UUID chatId;

    @JsonCreator
    public UsersInChatRequest(@JsonProperty("chatId") UUID chatId) {
        this.chatId = chatId;
    }

    @Override
    public String toString() {
        return "UsersInChatRequest { chatId=" + chatId + " }";
    }
}
