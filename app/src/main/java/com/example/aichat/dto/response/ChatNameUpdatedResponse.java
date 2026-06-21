package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class ChatNameUpdatedResponse {

    public final UUID chatId;
    public final String name;

    @JsonCreator
    public ChatNameUpdatedResponse(
            @JsonProperty("chatId") UUID chatId,
            @JsonProperty("name") String name
    ) {
        this.chatId = chatId;
        this.name = name;
    }
}
