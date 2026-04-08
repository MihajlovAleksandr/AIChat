package com.example.aichat.dto.response;

import com.example.aichat.model.entities.MessageStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class UpdateMessageStatusResponse {
    public final List<UUID> messageIds;
    public final UUID userId;
    public final UUID chatId;
    public final MessageStatus status;

    @JsonCreator
    public UpdateMessageStatusResponse(@JsonProperty("messageId") List<UUID> messageIds,
                                       @JsonProperty("chatId") UUID chatId,
                                       @JsonProperty("userId") UUID userId,
                                       @JsonProperty("status") MessageStatus status)
    {
        this.messageIds = messageIds;
        this.userId = userId;
        this.chatId = chatId;
        this.status = status;
    }
}
