package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SetNotificationRequest {
    public final boolean emailNotificationsEnabled;

    @JsonCreator
    public SetNotificationRequest(@JsonProperty("emailNotificationsEnabled") boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    @Override
    public String toString() {
        return "SetNotificationRequest { emailNotificationsEnabled=" + emailNotificationsEnabled + " }";
    }
}
