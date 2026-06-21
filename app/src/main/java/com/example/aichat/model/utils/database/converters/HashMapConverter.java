package com.example.aichat.model.utils.database.converters;

import androidx.room.TypeConverter;
import com.example.aichat.model.entities.MessageStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class HashMapConverter {

    private static final ObjectMapper mapper = new ObjectMapper();

    @TypeConverter
    public static String fromMap(HashMap<UUID, MessageStatus> map) {
        if (map == null) return null;
        try {
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert HashMap to String", e);
        }
    }

    @TypeConverter
    public static HashMap<UUID, MessageStatus> toMap(String json) {
        if (json == null) return new HashMap<>();
        try {
            return mapper.readValue(json, new TypeReference<HashMap<UUID, MessageStatus>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert String to HashMap", e);
        }
    }
}
