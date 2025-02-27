package com.example.aichat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class Command {

    @JsonProperty
    private String operation;

    @JsonProperty
    private Map<String, String> data;

    @JsonIgnore
    public String getOperation() {
        return operation;
    }

    @JsonIgnore

    public Command(String operation) {
        this.operation = operation;
        this.data = new HashMap<>();
    }

    public Command() {
        this.data = new HashMap<>();
    }

    public <T> void addData(String name, T obj) {
        data.put(name, JsonHelper.Serialize(obj));
    }
    public void addDataInJson(String name, String json){

    }

    public <T> T getData(String name, Class<T> type) {
        if (data.containsKey(name)) {
            return JsonHelper.Deserialize(data.get(name), type);
        }
        return null;
    }


    @Override
    public String toString() {
        return operation + ": \nData count: " + data.size();
    }
}