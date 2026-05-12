package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectionResponse {

    public final String hubUrl;
    public final boolean refreshedToken;
    public final String token;
    public final UUID connectionId;
    public final UUID userId;

    @JsonCreator
    public ConnectionResponse(
            @JsonProperty("hubUrl") String hubUrl,
            @JsonProperty("refreshedToken") boolean refreshedToken,
            @JsonProperty("token") String token,
            @JsonProperty("connectionId") UUID connectionId,
            @JsonProperty("userId") UUID userId
    ) {
        this.hubUrl = hubUrl;
        this.refreshedToken = refreshedToken;
        this.token = refreshedToken ? token : null;
        this.connectionId = connectionId;
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "ConnectionResponse { " +
                "hubUrl='" + hubUrl + '\'' +
                ", refreshedToken=" + refreshedToken +
                ", token=" + (token != null ? "***" : null) +
                ", connectionId=" + connectionId +
                ", userId=" + userId +
                " }";
    }
}
