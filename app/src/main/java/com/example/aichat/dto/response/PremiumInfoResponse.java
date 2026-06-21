package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class PremiumInfoResponse {

    public final String startTime;
    public final String endTime;
    public final boolean isAutoRenew;

    @JsonCreator
    public PremiumInfoResponse(
            @JsonProperty("startTime") String startTime,
            @JsonProperty("endTime") String endTime,
            @JsonProperty("isAutoRenew") boolean isAutoRenew
    ) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.isAutoRenew = isAutoRenew;
    }
}
