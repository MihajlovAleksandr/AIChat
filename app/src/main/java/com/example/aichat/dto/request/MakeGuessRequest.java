package com.example.aichat.dto.request;

import com.example.aichat.model.entities.AiRole;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class MakeGuessRequest {
    public final UUID chatId;
    public final AiRole aiRole;

    @JsonCreator
    public MakeGuessRequest(
            @JsonProperty("chatId") UUID chatId,
            @JsonProperty("aiRole") AiRole aiRole
    ) {
        this.chatId = chatId;
        this.aiRole = aiRole;
    }
}
