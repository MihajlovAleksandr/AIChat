package com.example.aichat.model.entities;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpCommand {

    @JsonProperty
    private CommandOperation operation;

    @JsonProperty
    private JsonNode data;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @JsonIgnore
    public CommandOperation getOperation() {
        return operation;
    }

    @JsonIgnore
    public int getCode() {
        return operation != null ? operation.getCode() : -1;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return operation != null
                && operation.getCode() >= 200
                && operation.getCode() < 300;
    }
    @JsonIgnore
    public HttpCommand(CommandOperation operation) {
        this.operation = operation;
    }

    @JsonIgnore
    public HttpCommand(CommandOperation operation, JsonNode data) {
        this.operation = operation;
        this.data = data;
    }

    public <T> T getData(Class<T> type) {
        if (data == null) return null;

        try {
            return objectMapper.treeToValue(data, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert data to type: " + type.getSimpleName(), e);
        }
    }

    public <T> T getData(TypeReference<T> typeReference) {
        if (data == null) return null;

        try {
            return objectMapper.readValue(data.toString(), typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert data to type: " + typeReference.getType(), e);
        }
    }

    @NonNull
    @Override
    public String toString() {
        String op = operation != null ? operation.name() : "NULL_OPERATION";
        int code = operation != null ? operation.getCode() : -1;

        return data != null
                ? op + " (code=" + code + "):\n" + data.toString()
                : op + " (code=" + code + ")";
    }
}
