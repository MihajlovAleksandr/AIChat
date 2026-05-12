package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VerificationCodeRequest {

    @JsonProperty("code")
    private String code;

    public VerificationCodeRequest(String code) {
        this.code = code;
    }
}
