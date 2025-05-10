package com.example.aichat.model.entities;

import androidx.annotation.NonNull;

import com.example.aichat.model.utils.JsonHelper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class Command {

    @JsonProperty
    private final String operation;

    @JsonProperty
    private final Map<String, String> data;

    @JsonIgnore
    public String getOperation() {
        return operation;
    }
    @JsonIgnore
    public Command(String operation) {
        this.operation = operation;
        this.data = new HashMap<>();
    }
    public Command(){
        operation="";
        this.data = new HashMap<>();
    }
    @JsonIgnore
    public <T> void addData(String name, T obj) {
        data.put(name, JsonHelper.Serialize(obj));
    }
    @JsonIgnore
    public <T> T getData(String name, Class<T> type) {
        if (data.containsKey(name)) {
            return JsonHelper.Deserialize(data.get(name), type);
        }
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        return operation + ": \nData count: " + data.size();
    }
}