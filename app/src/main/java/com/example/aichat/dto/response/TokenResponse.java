package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenResponse {
    public final String token;

    @JsonCreator
    public TokenResponse(@JsonProperty("token") String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "TokenResponse { token='***' }";
    }
}
