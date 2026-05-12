package com.example.aichat.model.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IntegrationTypes {
    GOOGLE("GOOGLE"),
    EMAIL_PASSWORD("EMAIL_PASSWORD"),
    TELEGRAM("TELEGRAM"),
    INSTAGRAM("INSTAGRAM"),
    FACEBOOK("FACEBOOK");

    private final String value;

    IntegrationTypes(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static IntegrationTypes fromValue(String value) {
        for (IntegrationTypes type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message status: " + value);
    }
}
