package com.example.aichat.model.entities;

import androidx.annotation.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ChatType {
    Human,
    AI,
    Random,
    Group;

    @JsonCreator
    public static ChatType fromJson(@Nullable Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        String value = String.valueOf(rawValue).trim();

        if (value.isEmpty()) {
            return null;
        }

        String normalized = value
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "")
                .toLowerCase();

        switch (normalized) {
            case "human":
            case "person":
            case "private":
            case "single":
            case "dialog":
                return Human;
            case "ai":
            case "ia":
            case "artificialintelligence":
            case "assistant":
                return AI;
            case "random":
            case "matchmaking":
                return Random;
            case "group":
            case "chatgroup":
                return Group;
            default:
                for (ChatType type : values()) {
                    if (type.name().equalsIgnoreCase(value)) {
                        return type;
                    }
                }
                return null;
        }
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}
