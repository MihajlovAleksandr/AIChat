package com.example.aichat.dto.request;

import com.example.aichat.model.entities.ChatType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchChatRequest {
    public final ChatType chatType;
    public final String chatMatchPredicate = "AllMatch";

    @JsonCreator
    public SearchChatRequest(@JsonProperty("chatType") ChatType chatType) {
        this.chatType = chatType;
    }

    @Override
    public String toString() {
        return "SearchChatRequest { chatType=" + chatType + " }";
    }
}
