package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class SyncMessagesResponse {

    public final MessageResponse[] newMessages;
    public final MessageResponse[] updatedMessages;
    public final UUID[] deletedMessages;

    @JsonCreator
    public SyncMessagesResponse(
            @JsonProperty("newMessages") MessageResponse[] newMessages,
            @JsonProperty("updatedMessages") MessageResponse[] updatedMessages,
            @JsonProperty("deletedMessages") UUID[] deletedMessages) {
        this.newMessages = newMessages;
        this.updatedMessages = updatedMessages;
        this.deletedMessages = deletedMessages;
    }
}
