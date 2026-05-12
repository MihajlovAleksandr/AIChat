package com.example.aichat.model.utils;

import androidx.room.TypeConverter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.UUID;

public class FileMimeMapConverter {

    private static final ObjectMapper mapper = new ObjectMapper();

    @TypeConverter
    public static String fromMap(HashMap<UUID, String> map) {
        try {
            return map == null ? null : mapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }
    @TypeConverter
    public static HashMap<UUID, String> toMap(String value) {
        try {
            return value == null ? new HashMap<>() :
                    mapper.readValue(value, new TypeReference<HashMap<UUID, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}