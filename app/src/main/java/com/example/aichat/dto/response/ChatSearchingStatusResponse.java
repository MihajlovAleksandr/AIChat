package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatSearchingStatusResponse {

    public final boolean isSearching;

    @JsonCreator
    public ChatSearchingStatusResponse(
            @JsonProperty("isSearching") boolean isSearching
    ) {
        this.isSearching = isSearching;
    }
}