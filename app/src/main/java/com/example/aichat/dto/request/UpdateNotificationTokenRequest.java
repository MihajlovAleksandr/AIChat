package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateNotificationTokenRequest {
    public final String token;

    @JsonCreator
    public UpdateNotificationTokenRequest(@JsonProperty("token") String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "UpdateNotificationTokenRequest { notificationToken='***' }";
    }
}
