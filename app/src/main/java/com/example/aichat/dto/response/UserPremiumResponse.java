package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public class UserPremiumResponse {

    public final UUID id;
    public final String startTime;
    public final String endTime;
    public final boolean isAutoRenew;

    @JsonCreator
    public UserPremiumResponse(
            @JsonProperty("id") UUID id,
            @JsonProperty("startTime") String startTime,
            @JsonProperty("endTime") String endTime,
            @JsonProperty("isAutoRenew") boolean isAutoRenew
    ) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isAutoRenew = isAutoRenew;
    }
}
