package com.example.aichat.dto.response;

import androidx.annotation.NonNull;
import com.example.aichat.model.entities.ConnectionInfo;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ConnectionChangeResponse {
    public final List<ConnectionInfo> connections;

    @JsonCreator
    public ConnectionChangeResponse(
            @JsonProperty("connections") List<ConnectionInfo> connections) {
        this.connections = connections;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Connection Count: ");
        stringBuilder.append(connections.size());
        stringBuilder.append("\n");
        for (ConnectionInfo info: connections) {
            stringBuilder.append(info.toString());
            stringBuilder.append("\n\n");
        }
        return stringBuilder.toString();
    }
}
