package com.example.aichat.dto.response;

import com.example.aichat.model.entities.RegistrationState;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterResponse {

    public final RegistrationState state;
    public final String token;

    @JsonCreator
    public RegisterResponse(
            @JsonProperty("state") RegistrationState state,
            @JsonProperty("token") String token
    ) {
        this.state = state;
        this.token = token;
    }
}
