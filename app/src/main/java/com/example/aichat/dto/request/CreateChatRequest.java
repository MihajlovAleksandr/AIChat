package com.example.aichat.dto.request;

import com.example.aichat.model.entities.ChatType;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateChatRequest {
    public final ChatType chatType;
    public final String chatName;
    public CreateChatRequest(
            @JsonProperty("chatType") ChatType chatType,
            @JsonProperty("chatName") String chatName){
        this.chatType = chatType;
        this.chatName = chatName;
    }

}
