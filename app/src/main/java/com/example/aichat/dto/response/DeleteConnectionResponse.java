package com.example.aichat.dto.response;

import com.example.aichat.model.entities.ConnectionInfo;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeleteConnectionResponse {
    public final ConnectionInfo connectionInfo;
    public final int[] count;

    @JsonCreator
    public DeleteConnectionResponse(
            @JsonProperty("connectionInfo") ConnectionInfo connectionInfo,
            @JsonProperty("count") int[] count) {
        this.connectionInfo = connectionInfo;
        this.count = count;
    }
}
