package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class MessageResponse {
    public final UUID id;
    public final UUID chat;
    public final UUID sender;
    public final String text;
    public final String time;
    public final String lastUpdate;

    @JsonCreator
    public MessageResponse(
            @JsonProperty("id") UUID id,
            @JsonProperty("chat") UUID chat,
            @JsonProperty("sender") UUID sender,
            @JsonProperty("text") String text,
            @JsonProperty("time") String time,
            @JsonProperty("lastUpdate") String lastUpdate) {
        this.id = id;
        this.chat = chat;
        this.sender = sender;
        this.text = text;
        this.time = time;
        this.lastUpdate = lastUpdate;
    }
}
