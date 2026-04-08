package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

import javax.annotation.Nullable;

public class SyncDBResponse {
    public final MessageResponse[] newMessages;
    public final MessageResponse[] oldMessages;
    public final MessageResponse[] deletedMessages;
    public final ChatResponse[] newChats;
    public final ChatResponse[] oldChats;
    public final ChatResponse[] deletedChats;

    public final boolean isChatSearching;
    public final @Nullable UUID userAddingToChat;

    @JsonCreator
    public SyncDBResponse(
            @JsonProperty("newMessages") MessageResponse[] newMessages,
            @JsonProperty("oldMessages") MessageResponse[] oldMessages,
            @JsonProperty("deletedMessages") MessageResponse[] deletedMessages,
            @JsonProperty("newChats") ChatResponse[] newChats,
            @JsonProperty("oldChats") ChatResponse[] oldChats,
            @JsonProperty("deletedChats") ChatResponse[] deletedChats,
            @JsonProperty("isChatSearching") boolean isChatSearching,
            @JsonProperty("userAddingToChat") UUID userAddingToChat) {
        this.newMessages = newMessages;
        this.oldMessages = oldMessages;
        this.deletedMessages = deletedMessages;
        this.newChats = newChats;
        this.oldChats = oldChats;
        this.deletedChats = deletedChats;
        this.isChatSearching = isChatSearching;
        this.userAddingToChat = userAddingToChat;
    }
}
