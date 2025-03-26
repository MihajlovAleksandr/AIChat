package com.example.aichat.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Preference {
    @JsonProperty("id")
    private int id;

    @JsonProperty("minAge")
    private int minAge;

    @JsonProperty("maxAge")
    private int maxAge;

    @JsonProperty("gender")
    private String gender;

    public Preference(){

    }
    public Preference(int minAge, int maxAge, String gender) {
        id = 0;
        this.gender = gender;
        this.maxAge = maxAge;
        this.minAge = minAge;
    }

    @JsonIgnore
    public int getId() {
        return id;
    }
    @JsonIgnore

    public void setId(int id) {
        this.id = id;
    }
    @JsonIgnore

    public int getMinAge() {
        return minAge;
    }
    @JsonIgnore

    public void setMinAge(int minAge) {
        this.minAge = minAge;
    }
    @JsonIgnore

    public int getMaxAge() {
        return maxAge;
    }
    @JsonIgnore

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }
    @JsonIgnore

    public String getGender() {
        return gender;
    }
    @JsonIgnore

    public void setGender(String gender) {
        this.gender = gender;
    }

    @Override
    public String toString() {
        return String.format("Preference {%d}:\n%s%d-%d", id, gender, minAge, maxAge);
    }

}