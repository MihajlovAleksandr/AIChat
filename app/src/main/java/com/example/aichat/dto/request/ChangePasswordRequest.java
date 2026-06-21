package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChangePasswordRequest {
    public final String currentPassword;
    public final String newPassword;

    @JsonCreator
    public ChangePasswordRequest(
            @JsonProperty("currentPassword") String currentPassword,
            @JsonProperty("newPassword") String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "ChangePasswordRequest { currentPassword='***', newPassword='***' }";
    }
}
