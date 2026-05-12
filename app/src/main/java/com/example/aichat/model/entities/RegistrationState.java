package com.example.aichat.model.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RegistrationState {
    CREATED("Created"),
    EMAIL_VERIFIED("EmailVerified"),

    USER_DATA_COMPLETED ("UserDataCompleted"),

    PREFERENCE_COMPLETED ("PreferenceCompleted");


    RegistrationState(String value) {
        this.value = value;
    }
    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RegistrationState fromValue(String value) {
        for (RegistrationState state : values()) {
            if (state.value.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown gender: " + value);
    }
}
