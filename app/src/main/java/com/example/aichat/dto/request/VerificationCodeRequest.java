package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VerificationCodeRequest {
    public final int code;

    @JsonCreator
    public VerificationCodeRequest(@JsonProperty("code") int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "VerificationCodeRequest { code=*** }";
    }
}
