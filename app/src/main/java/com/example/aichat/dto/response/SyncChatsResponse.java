package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.UUID;

public class SyncChatsResponse {

    public final ChatResponse[] newChats;
    public final ChatResponse[] updatedChats;
    public final UUID[] deletedChats;

    @JsonCreator
    public SyncChatsResponse(
            @JsonProperty("newChats") ChatResponse[] newChats,
            @JsonProperty("updatedChats") ChatResponse[] updatedChats,
            @JsonProperty("deletedChats") UUID[] deletedChats) {
        this.newChats = newChats;
        this.updatedChats = updatedChats;
        this.deletedChats = deletedChats;
    }
}
