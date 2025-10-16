package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationResponse {
    public final boolean emailNotificationsEnabled;

    @JsonCreator
    public NotificationResponse(@JsonProperty("emailNotificationsEnabled") boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }
}
