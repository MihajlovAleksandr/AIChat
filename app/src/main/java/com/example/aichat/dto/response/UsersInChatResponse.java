package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class UsersInChatResponse {
    public final UUID[] ids;
    public final UserDataResponse[] userData;
    public final Boolean[] isOnline;

    @JsonCreator
    public UsersInChatResponse(
            @JsonProperty("ids") UUID[] ids,
            @JsonProperty("userData") UserDataResponse[] userData,
            @JsonProperty("isOnline") Boolean[] isOnline) {
        this.ids = ids;
        this.userData = userData;
        this.isOnline = isOnline;
    }
}
