package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SyncMatchmakingResponse {

    public final SyncChatMatchmakingResponse chatMatchmaking;
    public final SyncGroupMatchmakingResponse groupMatchmaking;

    @JsonCreator
    public SyncMatchmakingResponse(
            @JsonProperty("chatMatchmaking") SyncChatMatchmakingResponse chatMatchmaking,
            @JsonProperty("groupMatchmaking") SyncGroupMatchmakingResponse groupMatchmaking) {
        this.chatMatchmaking = chatMatchmaking;
        this.groupMatchmaking = groupMatchmaking;
    }
}
