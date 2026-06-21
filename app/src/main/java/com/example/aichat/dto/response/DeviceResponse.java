package com.example.aichat.dto.response;

import com.example.aichat.model.entities.ConnectionInfo;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class DeviceResponse {
    public final ConnectionInfo[] connectionInfo;
    public final UUID currentConnection;

    @JsonCreator
    public DeviceResponse(
            @JsonProperty("connectionInfo") ConnectionInfo[] connectionInfo,
            @JsonProperty("currentConnection") UUID currentConnection) {
        this.connectionInfo = connectionInfo;
        this.currentConnection = currentConnection;
    }
}
