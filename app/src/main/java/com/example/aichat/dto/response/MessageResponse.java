package com.example.aichat.dto.response;

import androidx.annotation.Nullable;

import com.example.aichat.model.entities.MessageReply;
import com.example.aichat.model.entities.MessageStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MessageResponse {
    public final UUID id;
    public final UUID chat;
    public final UUID sender;
    public final String text;
    public final String time;
    public final String lastUpdate;
    public final List<MessageReply> replyMessages;
    public final HashMap<UUID, MessageStatus> statuses;

    @JsonCreator
    public MessageResponse(
            @JsonProperty("id") UUID id,
            @JsonProperty("chat") UUID chat,
            @JsonProperty("sender") UUID sender,
            @JsonProperty("text") String text,
            @JsonProperty("time") String time,
            @JsonProperty("lastUpdate") String lastUpdate,
            @JsonProperty("replyMessages") List<MessageReply> replyMessages,
            @JsonProperty("statuses") HashMap<UUID, MessageStatus> statuses) {
        this.id = id;
        this.chat = chat;
        this.sender = sender;
        this.text = text;
        this.time = time;
        this.lastUpdate = lastUpdate;
        this.replyMessages = replyMessages;
        this.statuses = statuses;
    }
}
