package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SettingsInfoResponse {
    public final String email;
    public final UserDataResponse userData;
    public final PreferenceResponse preference;
    public final int[] connectionCount;
    public final NotificationResponse notifications;

    @JsonCreator
    public SettingsInfoResponse(
            @JsonProperty("email") String email,
            @JsonProperty("userData") UserDataResponse userData,
            @JsonProperty("preference") PreferenceResponse preference,
            @JsonProperty("connectionCount") int[] connectionCount,
            @JsonProperty("notifications") NotificationResponse notifications) {
        this.email = email;
        this.userData = userData;
        this.preference = preference;
        this.connectionCount = connectionCount;
        this.notifications = notifications;
    }
}
