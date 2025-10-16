package com.example.aichat.model.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PreferenceGender {
    MALE("Male"),
    FEMALE("Female"),
    ANY("Any");

    private final String value;

    PreferenceGender(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PreferenceGender fromValue(String value) {
        for (PreferenceGender gender : values()) {
            if (gender.value.equalsIgnoreCase(value)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Unknown preference gender: " + value);
    }
}
