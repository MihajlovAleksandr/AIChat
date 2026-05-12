package com.example.aichat.dto.request;

import com.example.aichat.model.entities.MessageReply;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class MessageRequest {
    public final UUID id;
    public final UUID chatId;
    public final String text;
    public final List<MessageReply> replies;
    public final UUID uploadSessionId;

    @JsonCreator
    public MessageRequest(
            @JsonProperty("id") UUID id,
            @JsonProperty("chatId") UUID chatId,
            @JsonProperty("text") String text,
            @JsonProperty("replies") List<MessageReply> replies,
            @JsonProperty("uploadSessionId") UUID uploadSessionId) {
        this.id = id;
        this.chatId = chatId;
        this.text = text;
        this.replies = replies;
        this.uploadSessionId = uploadSessionId;
    }

    @Override
    public String toString() {
        return "MessageRequest { chat=" + chatId + ", text='" + text + "' }";
    }
}
