package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class ChatResponse {
    public final UUID id;
    public final String name;
    public final String creationTime;
    public final String endTime;
    public final List<UUID> users;

    @JsonCreator
    public ChatResponse(
            @JsonProperty("id") UUID id,
            @JsonProperty("name") String name,
            @JsonProperty("creationTime") String creationTime,
            @JsonProperty("endTime") String endTime,
            @JsonProperty("users") List<UUID> users) {
        this.id = id;
        this.name = name;
        this.creationTime = creationTime;
        this.endTime = endTime;
        this.users = users;
    }
}
