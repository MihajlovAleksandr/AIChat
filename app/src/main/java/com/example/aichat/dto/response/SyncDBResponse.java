package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SyncDBResponse {
    public final MessageResponse[] newMessages;
    public final MessageResponse[] oldMessages;
    public final ChatResponse[] newChats;
    public final ChatResponse[] oldChats;
    public final boolean isChatSearching;

    @JsonCreator
    public SyncDBResponse(
            @JsonProperty("newMessages") MessageResponse[] newMessages,
            @JsonProperty("oldMessages") MessageResponse[] oldMessages,
            @JsonProperty("newChats") ChatResponse[] newChats,
            @JsonProperty("oldChats") ChatResponse[] oldChats,
            @JsonProperty("isChatSearching") boolean isChatSearching) {
        this.newMessages = newMessages;
        this.oldMessages = oldMessages;
        this.newChats = newChats;
        this.oldChats = oldChats;
        this.isChatSearching = isChatSearching;
    }
}
