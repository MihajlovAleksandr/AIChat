package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SyncResponse {

    public final SyncMatchmakingResponse matchmaking;
    public final SyncMessagesResponse messages;
    public final SyncChatsResponse chats;

    @JsonCreator
    public SyncResponse(
            @JsonProperty("matchmaking") SyncMatchmakingResponse matchmaking,
            @JsonProperty("messages") SyncMessagesResponse messages,
            @JsonProperty("chats") SyncChatsResponse chats) {
        this.matchmaking = matchmaking;
        this.messages = messages;
        this.chats = chats;
    }
}
