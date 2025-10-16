package com.example.aichat.dto.request;

import androidx.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class DeleteConnectionRequest {
    @Nullable
    public final UUID connectionId;

    @JsonCreator
    public DeleteConnectionRequest(@JsonProperty("connectionId") @Nullable UUID connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public String toString() {
        return "DeleteConnectionRequest { connectionId=" + connectionId + " }";
    }
}
