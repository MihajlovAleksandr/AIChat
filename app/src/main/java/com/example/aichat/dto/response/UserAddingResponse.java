package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

import javax.annotation.Nullable;

public class UserAddingResponse {
    public final @Nullable UUID chatId;

    @JsonCreator
    public UserAddingResponse(
            @JsonProperty("chatId")
            @Nullable UUID chatId
    ){
        this.chatId = chatId;
    }
}
