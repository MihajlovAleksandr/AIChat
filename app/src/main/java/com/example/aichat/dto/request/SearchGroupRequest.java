package com.example.aichat.dto.request;

import com.example.aichat.model.entities.ChatType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchGroupRequest {
    public final String chatName;
    public final String chatMatchPredicate = "AllMatch";

    @JsonCreator
    public SearchGroupRequest(@JsonProperty("chatType") String chatName) {
        this.chatName = chatName;
    }

    @Override
    public String toString() {
        return "SearchChatRequest { chatName=" + chatName + " }";
    }
}
