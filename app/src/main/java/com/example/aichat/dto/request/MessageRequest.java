package com.example.aichat.dto.request;

import com.example.aichat.model.entities.MessageReply;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class MessageRequest {
    public final UUID id;
    public final UUID chat;
    public final UUID sender;
    public final String text;
    public final List<MessageReply> replyMessages;

    @JsonCreator
    public MessageRequest(
            @JsonProperty("id") UUID id,
            @JsonProperty("chat") UUID chat,
            @JsonProperty("sender") UUID sender,
            @JsonProperty("text") String text,
            @JsonProperty("replyMessages") List<MessageReply> replyMessages) {
        this.id = id;
        this.chat = chat;
        this.sender = sender;
        this.text = text;
        this.replyMessages = replyMessages;
    }

    @Override
    public String toString() {
        return "MessageRequest { chat=" + chat + ", sender=" + sender + ", text='" + text + "' }";
    }
}
