package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class SyncGroupMatchmakingResponse {

    public final boolean isSearching;
    public final UUID chatId;

    @JsonCreator
    public SyncGroupMatchmakingResponse(
            @JsonProperty("isSearching") boolean isSearching,
            @JsonProperty("chatId") UUID chatId) {
        this.isSearching = isSearching;
        this.chatId = chatId;
    }
}
