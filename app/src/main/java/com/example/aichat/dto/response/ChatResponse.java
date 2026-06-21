package com.example.aichat.dto.response;

import com.example.aichat.model.entities.ChatType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ChatResponse {

    public final UUID id;
    public final ChatType type;
    public final String joinTime;
    public final String endTime;
    public final List<UUID> users;
    public final String name;

    @JsonCreator
    public ChatResponse(
            @JsonProperty("id") UUID id,
            @JsonProperty("type") ChatType type,
            @JsonProperty("chatType") ChatType chatType,
            @JsonProperty("joinTime") String joinTime,
            @JsonProperty("endTime") String endTime,
            @JsonProperty("users") List<UUID> users,
            @JsonProperty("name") String name) {
        this.id = id;
        this.type = type != null ? type : chatType;
        this.joinTime = joinTime;
        this.endTime = endTime;
        this.users = users;
        this.name = name;
    }
}
