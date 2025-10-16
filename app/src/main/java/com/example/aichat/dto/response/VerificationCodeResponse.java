package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VerificationCodeResponse {
    public final int answer;

    @JsonCreator
    public VerificationCodeResponse(@JsonProperty("answer") int answer) {
        this.answer = answer;
    }

    @Override
    public String toString() {
        return "VerificationCodeResponse { answer=*** }";
    }
}
