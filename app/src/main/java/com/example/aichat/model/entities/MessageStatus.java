package com.example.aichat.model.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageStatus {
    SENDING ("Sending", 0),
    SENT ("Sent", 1),
    READ("Read", 2);

    private final String value;
    private final int priority;

    MessageStatus(String value, int priority) {
        this.value = value;
        this.priority = priority;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonIgnore
    public int getPriority(){
        return priority;
    }

    @JsonCreator
    public static MessageStatus fromValue(String value) {
        for (MessageStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown message status: " + value);
    }
}
