package com.example.aichat.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.UUID;

public class UserData implements Serializable {
    @JsonProperty
    private Gender gender;
    @JsonProperty
    private String name;
    @JsonProperty
    private int age;

    public UserData(){

    }
    public  UserData(String name, int age, Gender gender)
    {
        this.name = name;
        this.age = age;
        this.gender = gender;
    }
    @JsonIgnore
    public Gender getGender() {
        return gender;
    }
    @JsonIgnore
    public void setGender(Gender gender) {
        this.gender = gender;
    }
    @JsonIgnore
    public String getName() {
        return name;
    }
    @JsonIgnore
    public void setName(String name) {
        this.name = name;
    }
    @JsonIgnore
    public int getAge() {
        return age;
    }
    @JsonIgnore
    public void setAge(int age) {
        this.age = age;
    }
    @JsonIgnore
    @Override
    public String toString() {
        return String.format("%s (%s%d)", name, gender.toString(), age);
    }

}
