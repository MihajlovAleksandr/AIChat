package com.example.aichat.dto.request;

import com.example.aichat.model.ai.AIModel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthRequest {
    public final String email;
    public final String password;

    @JsonCreator
    public AuthRequest(
            @JsonProperty("email") String email,
            @JsonProperty("password") String password) {
        this.email = email;
        this.password = password;
    }

    @Override
    public String toString() {
        return "AuthRequest { email='" + email + "' }";
    }
}
