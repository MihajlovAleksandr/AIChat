package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class LoginInResponse {
    public final UUID userId;

    @JsonCreator
    public LoginInResponse(@JsonProperty("userId") UUID userId) {
        this.userId = userId;
    }
}
