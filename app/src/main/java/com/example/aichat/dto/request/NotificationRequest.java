package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationRequest {
    public final boolean emailNotificationsEnabled;

    @JsonCreator
    public NotificationRequest(@JsonProperty("emailNotificationsEnabled") boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    @Override
    public String toString() {
        return "NotificationRequest { emailNotificationsEnabled=" + emailNotificationsEnabled + " }";
    }
}
