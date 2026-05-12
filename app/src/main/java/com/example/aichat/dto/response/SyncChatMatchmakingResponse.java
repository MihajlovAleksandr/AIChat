package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SyncChatMatchmakingResponse {

    public final boolean isSearching;

    @JsonCreator
    public SyncChatMatchmakingResponse(
            @JsonProperty("isSearching") boolean isSearching) {
        this.isSearching = isSearching;
    }
}
