package com.example.aichat.model.entities;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

public class WSSCommand {

    @JsonProperty
    private String operation;

    @JsonProperty
    private JsonNode data;

    private static final ObjectMapper objectMapper = new ObjectMapper();


    public WSSCommand() {
        this.operation = "Unknown";
        this.data = null;
    }

    @JsonIgnore
    public WSSCommand(String operation) {
        this.operation = (operation != null && !operation.isEmpty()) ? operation : "Unknown";
        this.data = null;
    }

    @JsonIgnore
    public WSSCommand(String operation, Object data) {
        this.operation = (operation != null && !operation.isEmpty()) ? operation : "Unknown";
        setData(data);
    }


    public <T> T getData(Class<T> type) {
        if (data == null) return null;
        try {
            return objectMapper.treeToValue(data, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert data to type: " + type.getSimpleName(), e);
        }
    }

    @JsonIgnore
    public void setData(Object obj) {
        if (obj == null) {
            this.data = null;
        } else {
            this.data = objectMapper.valueToTree(obj);
        }
    }

    @JsonIgnore
    public String getOperation() {
        return operation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WSSCommand)) return false;
        WSSCommand that = (WSSCommand) o;
        return Objects.equals(operation, that.operation) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation, data);
    }

    @NonNull
    @Override
    public String toString() {
        if (data == null) {
            return "WSSCommand{operation='" + operation + "', data=null}";
        }

        String shortJson = data.toString();
        if (shortJson.length() > 200) {
            shortJson = shortJson.substring(0, 200) + "...";
        }

        return "WSSCommand{operation='" + operation + "', data=" + shortJson + "}";
    }
}
