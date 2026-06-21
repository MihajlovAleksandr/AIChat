package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class UserResponse {

    public final UUID id;
    public final String email;
    public final String region;
    public final boolean isPremium;
    public final UserDataResponse userData;
    public final PreferenceResponse preference;
    public final String language;

    @JsonCreator
    public UserResponse(
            @JsonProperty("id") UUID id,
            @JsonProperty("email") String email,
            @JsonProperty("region") String region,
            @JsonProperty("isPremium") boolean isPremium,
            @JsonProperty("userData") UserDataResponse userData,
            @JsonProperty("preference") PreferenceResponse preference,
            @JsonProperty("language") String language
    ) {
        this.id = id;
        this.email = email;
        this.region = region;
        this.isPremium = isPremium;
        this.userData = userData;
        this.preference = preference;
        this.language = language;
    }

    @Override
    public String toString() {
        return "UserResponse { " +
                "id=" + id +
                ", email='***'" +
                ", region='" + region + '\'' +
                ", isPremium=" + isPremium +
                ", userData=" + userData +
                ", preference=" + preference +
                ", language='" + language + '\'' +
                " }";
    }
}
