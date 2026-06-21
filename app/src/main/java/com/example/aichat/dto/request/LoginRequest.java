package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginRequest {

    public final String identifier;
    public final String secret;
    public final String identityProviderCode;

    @JsonCreator
    public LoginRequest(
            @JsonProperty("identifier") String identifier,
            @JsonProperty("secret") String secret,
            @JsonProperty("identityProviderCode") String identityProviderCode
    ) {
        this.identifier = identifier;
        this.secret = secret;
        this.identityProviderCode = identityProviderCode;
    }
}
