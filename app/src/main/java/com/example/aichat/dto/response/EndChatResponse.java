package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class EndChatResponse {

    public final String endedTime;  // ← changed from endTime to endedTime

    @JsonCreator
    public EndChatResponse(
            @JsonProperty("endedTime") String endedTime) {  // ← changed from endTime to endedTime
        this.endedTime = endedTime;
    }
}