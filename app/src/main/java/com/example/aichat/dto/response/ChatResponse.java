package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class ChatResponse {
    public final UUID id;
    public final String name;
    public final String creationTime;
    public final String endTime;

    @JsonCreator
    public ChatResponse(
            @JsonProperty("id") UUID id,
            @JsonProperty("name") String name,
            @JsonProperty("creationTime") String creationTime,
            @JsonProperty("endTime") String endTime) {
        this.id = id;
        this.name = name;
        this.creationTime = creationTime;
        this.endTime = endTime;
    }
}
