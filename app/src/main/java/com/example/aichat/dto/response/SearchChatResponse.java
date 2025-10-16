package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchChatResponse {
    public final boolean isChatSearching;

    @JsonCreator
    public SearchChatResponse(@JsonProperty("isChatSearching") boolean isChatSearching) {
        this.isChatSearching = isChatSearching;
    }
}
