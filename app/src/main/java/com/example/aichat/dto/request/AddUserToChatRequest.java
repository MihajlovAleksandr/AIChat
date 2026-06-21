package com.example.aichat.dto.request;

import com.example.aichat.model.entities.ChatType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AddUserToChatRequest {
    public final ChatType chatType;
    public final String chatMatchPredicate;

    @JsonCreator
    public AddUserToChatRequest(@JsonProperty("chatType") ChatType chatType,
                                @JsonProperty("chatMatchPredicate") String chatMatchPredicate) {
        this.chatType = chatType;
        this.chatMatchPredicate = chatMatchPredicate;
    }

    @Override
    public String toString() {
        return "SearchChatRequest { chatType=" + chatType + " }";
    }
}
