package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GoogleTokenRequest {
    public final String token;

    @JsonCreator
    public GoogleTokenRequest(@JsonProperty("token") String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "GoogleTokenRequest { token='***' }";
    }
}
