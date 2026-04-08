package com.example.aichat.dto.request;

import com.example.aichat.model.entities.ChatType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class AddOtherUserToChatRequest {

    public final UUID chatId;
    public final String chatMatchPredicate;

    @JsonCreator
    public AddOtherUserToChatRequest(@JsonProperty("chatId") UUID chatId,
        @JsonProperty("chatMatchPredicate") String chatMatchPredicate) {
        this.chatId = chatId;
        this.chatMatchPredicate = chatMatchPredicate;
    }

    @Override
    public String toString() {
        return "AddOtherUserToChatRequest  { chatId: " + chatId + ", chatMatchPredicate: "+chatMatchPredicate+" }";
    }
}
