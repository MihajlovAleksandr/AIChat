package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegistrationRequest {
    public final String email;
    public final String password;
    public final String localization;

    @JsonCreator
    public RegistrationRequest(
            @JsonProperty("email") String email,
            @JsonProperty("password") String password,
            @JsonProperty("localization") String localization) {
        this.email = email;
        this.password = password;
        this.localization = localization;
    }

    @Override
    public String toString() {
        return "RegistrationRequest { email='" + email + "', localization='" + localization + "' }";
    }
}
