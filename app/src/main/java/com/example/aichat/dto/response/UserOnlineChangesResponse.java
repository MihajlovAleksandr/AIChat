package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class UserOnlineChangesResponse {
    public final UUID userId;
    public final boolean isOnline;

    @JsonCreator
    public UserOnlineChangesResponse(
            @JsonProperty("userId") UUID userId,
            @JsonProperty("isOnline") boolean isOnline) {
        this.userId = userId;
        this.isOnline = isOnline;
    }
}
