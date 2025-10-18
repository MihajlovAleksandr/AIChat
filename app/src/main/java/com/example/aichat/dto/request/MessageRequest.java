package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class MessageRequest {
    public final UUID id;
    public final UUID chat;
    public final UUID sender;
    public final String text;

    @JsonCreator
    public MessageRequest(
            @JsonProperty("id") UUID id,
            @JsonProperty("chat") UUID chat,
            @JsonProperty("sender") UUID sender,
            @JsonProperty("text") String text) {
        this.id = id;
        this.chat = chat;
        this.sender = sender;
        this.text = text;
    }

    @Override
    public String toString() {
        return "MessageRequest { chat=" + chat + ", sender=" + sender + ", text='" + text + "' }";
    }
}
