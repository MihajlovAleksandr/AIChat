package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class AddUserToChatResponse {
    public final UUID chatId;
    public final UUID userId;
    public final UserDataResponse userData;
    public final boolean isOnline;

    @JsonCreator
    public AddUserToChatResponse(
            @JsonProperty("id") UUID chatId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("userData") UserDataResponse userData,
            @JsonProperty("isOnline") boolean isOnline){
        this.chatId = chatId;
        this.userId = userId;
        this.userData = userData;
        this.isOnline = isOnline;
    }
}