package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LeaderboardItemResponse {
    public final UserInfoResponse userInfo;
    public final int pointsCount;

    @JsonCreator
    public LeaderboardItemResponse(
            @JsonProperty("userInfo") UserInfoResponse userInfo,
            @JsonProperty("points") int pointsCount){
        this.userInfo = userInfo;
        this.pointsCount = pointsCount;
    }
}
