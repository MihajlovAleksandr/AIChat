package com.example.aichat.dto.request;

import com.example.aichat.model.entities.MessageStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class UpdateMessageStatusRequest {
    public final List<UUID> messageIds;
    public final MessageStatus status;

    @JsonCreator
    public UpdateMessageStatusRequest(@JsonProperty("messageIds") List<UUID> messageIds,
                                      @JsonProperty("status") MessageStatus status){
        this.messageIds = messageIds;
        this.status = status;
    }
}
