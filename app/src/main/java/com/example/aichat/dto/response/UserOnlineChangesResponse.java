package com.example.aichat.dto.response;

import androidx.annotation.NonUiContext;
import androidx.annotation.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class UserOnlineChangesResponse {
    public final UUID userId;
    public final String lastOnline;

    @JsonCreator
    public UserOnlineChangesResponse(
            @JsonProperty("userId") UUID userId,
            @JsonProperty("lastOnline") @Nullable String lastOnline) {
        this.userId = userId;
        this.lastOnline = lastOnline;
    }
}
