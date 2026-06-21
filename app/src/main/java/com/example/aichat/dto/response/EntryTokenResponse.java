package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EntryTokenResponse {
    public final String token;

    @JsonCreator
    public EntryTokenResponse(@JsonProperty("token") String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "EntryTokenResponse { token='***' }";
    }
}
