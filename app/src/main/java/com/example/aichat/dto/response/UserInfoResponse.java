package com.example.aichat.dto.response;

import com.example.aichat.model.entities.Gender;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserInfoResponse {
    public final UserDataResponse userData;
    public final String lastOnline;
    public final String region;

    @JsonCreator
    public UserInfoResponse(
            @JsonProperty("userData") UserDataResponse userData,
            @JsonProperty("lastOnline") String lastOnline,
            @JsonProperty("region") String region) {
        this.userData = userData;
        this.lastOnline = lastOnline;
        this.region = region;
    }
}
