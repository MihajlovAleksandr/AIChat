package com.example.aichat.dto.request;

import com.example.aichat.model.entities.ChatType;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MatchmakingRequest {
    public final ChatType chatType;
    public final String chatName;
    public final String chatMatchPredicate;
    public MatchmakingRequest(
            @JsonProperty("chatType") ChatType chatType,
            @JsonProperty("chatName") String chatName,
            @JsonProperty("chatMatchPredicate") String chatMatchPredicate){
        this.chatType = chatType;
        this.chatName = chatName;
        this.chatMatchPredicate = chatMatchPredicate;
    }
}
