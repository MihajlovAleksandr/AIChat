package com.example.aichat.dto.response;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiError {

    @JsonProperty
    private String code;

    @JsonProperty
    private JsonNode data;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @JsonIgnore
    public String getCode() {
        return code;
    }

    @JsonIgnore
    public boolean hasData() {
        return data != null && !data.isNull();
    }

    @JsonIgnore
    public <T> T getData(Class<T> type) {
        if (data == null || data.isNull()) return null;

        try {
            return objectMapper.treeToValue(data, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to convert data to type: " + type.getSimpleName(), e
            );
        }
    }

    @NonNull
    @Override
    public String toString() {
        return data != null
                ? "ApiError: " + code + " " + data.toString()
                : "ApiError: " + code;
    }
}
