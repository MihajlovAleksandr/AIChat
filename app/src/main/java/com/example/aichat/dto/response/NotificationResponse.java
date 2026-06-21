package com.example.aichat.dto.response;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationResponse {
    public final boolean emailNotificationsEnabled;

    @JsonCreator
    public NotificationResponse(@JsonProperty("emailNotificationsEnabled") boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    @NonNull
    @Override
    public String toString() {
        return "NotificationResponse: emailNotificationsEnabled = " + emailNotificationsEnabled;
    }
}
