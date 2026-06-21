package com.example.aichat.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.UUID;

public class Preference implements Serializable {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("minAge")
    private int minAge;

    @JsonProperty("maxAge")
    private int maxAge;

    @JsonProperty("gender")
    private PreferenceGender gender;

    public Preference(){

    }
    public Preference(int minAge, int maxAge, PreferenceGender gender) {
        id = UUID.randomUUID();
        this.gender = gender;
        this.maxAge = maxAge;
        this.minAge = minAge;
    }

    @JsonIgnore
    public UUID getId() {
        return id;
    }

    @JsonIgnore
    public void setId(UUID id) {
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

    public PreferenceGender getGender() {
        return gender;
    }
    @JsonIgnore

    public void setGender(PreferenceGender gender) {
        this.gender = gender;
    }

    @Override
    public String toString() {
        return String.format("%s %d-%d", gender, minAge, maxAge);
    }

}
