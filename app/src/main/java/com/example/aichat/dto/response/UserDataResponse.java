package com.example.aichat.dto.response;

import com.example.aichat.model.entities.Gender;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDataResponse {
    public final String name;
    public final int age;
    public final Gender gender;

    @JsonCreator
    public UserDataResponse(
            @JsonProperty("name") String name,
            @JsonProperty("age") int age,
            @JsonProperty("gender") Gender gender) {
        this.name = name;
        this.age = age;
        this.gender = gender;
    }
}
