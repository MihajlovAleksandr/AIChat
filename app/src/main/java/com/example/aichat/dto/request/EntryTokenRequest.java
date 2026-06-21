package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EntryTokenRequest {
    public final String token;

    @JsonCreator
    public EntryTokenRequest(@JsonProperty("token") String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "EntryTokenRequest { token='***' }";
    }
}
