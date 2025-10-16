package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UseOtherLoginInServiceResponse {
    public final String service;

    @JsonCreator
    public UseOtherLoginInServiceResponse(@JsonProperty("service") String service) {
        this.service = service;
    }
}
