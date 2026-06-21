package com.example.aichat.dto.response;

import androidx.annotation.Nullable;
import com.example.aichat.model.entities.MessageReply;
import com.example.aichat.model.entities.MessageStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MessageResponse {

    public final UUID id;
    public final UUID chatId;
    @Nullable
    public final UUID userId;
    public final String text;
    public final String time;
    public final String lastUpdate;
    public final List<MessageReply> replies;
    public final HashMap<UUID, MessageStatus> statuses;
    public final List<UUID> files;

    @JsonCreator
    public MessageResponse(
            @JsonProperty("id") UUID id,
            @JsonProperty("chatId") UUID chatId,
            @Nullable
            @JsonProperty("userId") UUID userId,
            @JsonProperty("text") String text,
            @JsonProperty("time") String time,
            @JsonProperty("lastUpdate") String lastUpdate,
            @JsonProperty("replies") List<MessageReply> replies,
            @JsonProperty("statuses") HashMap<UUID, MessageStatus> statuses,
            @JsonProperty("files") List<UUID> files) {
        this.id = id;
        this.chatId = chatId;
        this.userId = userId;
        this.text = text;
        this.time = time;
        this.lastUpdate = lastUpdate;
        this.replies = replies;
        this.statuses = statuses;
        this.files = files;
    }
}
