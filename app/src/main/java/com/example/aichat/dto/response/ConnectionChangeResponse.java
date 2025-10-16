package com.example.aichat.dto.response;

import com.example.aichat.model.entities.ConnectionInfo;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConnectionChangeResponse {
    public final ConnectionInfo connectionInfo;
    public final int[] count;
    public final boolean isOnline;

    @JsonCreator
    public ConnectionChangeResponse(
            @JsonProperty("connectionInfo") ConnectionInfo connectionInfo,
            @JsonProperty("count") int[] count,
            @JsonProperty("isOnline") boolean isOnline) {
        this.connectionInfo = connectionInfo;
        this.count = count;
        this.isOnline = isOnline;
    }
}
