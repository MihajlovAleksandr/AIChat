package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class ChatEndedResponse {

    public final UUID chatId;
    public final String endedTime;

    @JsonCreator
    public ChatEndedResponse(
            @JsonProperty("chatId") UUID chatId,
            @JsonProperty("endedTime") String endedTime
    ) {
        this.chatId = chatId;
        this.endedTime = endedTime;
    }
}