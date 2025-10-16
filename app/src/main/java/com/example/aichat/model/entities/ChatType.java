package com.example.aichat.model.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ChatType {
    AI("AI"),
    HUMAN("Human"),
    RANDOM("Random");

    private final String value;

    ChatType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ChatType fromValue(String value) {
        for (ChatType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown chat type: " + value);
    }
}
