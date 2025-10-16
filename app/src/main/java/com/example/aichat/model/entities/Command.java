package com.example.aichat.model.entities;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Command {

    @JsonProperty
    private String operation;

    @JsonProperty
    private JsonNode data;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @JsonIgnore
    public String getOperation() {
        return operation;
    }
    @JsonIgnore
    public Command(String operation){
        this.operation = operation;
    }
    public Command(){
        operation = "";
    }
    @JsonIgnore
    public Command(String operation, Object data) {
        this.operation = operation;
        if (data != null) {
            this.data = objectMapper.valueToTree(data);
        }
    }

    public <T> T getData(Class<T> type) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.treeToValue(data, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert data to type: " + type.getSimpleName(), e);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return data != null ? operation + ": \nData count: " + data.size() : operation;
    }
}