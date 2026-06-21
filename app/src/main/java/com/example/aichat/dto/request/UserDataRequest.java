package com.example.aichat.dto.request;

import com.example.aichat.model.entities.Gender;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDataRequest {
    public final String name;
    public final int age;
    public final Gender gender;

    @JsonCreator
    public UserDataRequest(
            @JsonProperty("name") String name,
            @JsonProperty("age") int age,
            @JsonProperty("gender") Gender gender) {
        this.name = name;
        this.age = age;
        this.gender = gender;
    }

    @Override
    public String toString() {
        return "UserDataRequest { name='" + name + "', age=" + age + ", gender=" + gender + " }";
    }
}
