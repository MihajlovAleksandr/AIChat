package com.example.aichat.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserData {
    @JsonProperty
    private int id;
    @JsonProperty
    private char gender;
    @JsonProperty
    private String name;
    @JsonProperty
    private int age;

    public UserData(){

    }
    public  UserData(String name, int age, char gender)
    {
        id = 0;
        this.name = name;
        this.age = age;
        this.gender = gender;
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
    public char getGender() {
        return gender;
    }
    @JsonIgnore
    public void setGender(char gender) {
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
        return String.format("UserData {%d}\n%c%d\n%s", id, gender, age, name);
    }

}
