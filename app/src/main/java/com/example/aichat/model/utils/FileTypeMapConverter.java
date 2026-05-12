package com.example.aichat.model.utils;

import androidx.room.TypeConverter;

import com.example.aichat.model.entities.FileType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.UUID;

public class FileTypeMapConverter {

    private static final ObjectMapper mapper = new ObjectMapper();

    @TypeConverter
    public static String fromMap(HashMap<UUID, FileType> map) {
        try {
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
    @TypeConverter
    public static HashMap<UUID, FileType> toMap(String value) {
        try {
            return mapper.readValue(value, new TypeReference<HashMap<UUID, FileType>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}