package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateNotificationTokenRequest {
    public final String notificationToken;

    @JsonCreator
    public UpdateNotificationTokenRequest(@JsonProperty("notificationToken") String notificationToken) {
        this.notificationToken = notificationToken;
    }

    @Override
    public String toString() {
        return "UpdateNotificationTokenRequest { notificationToken='***' }";
    }
}
