package com.example.aichat.model.utils.database.converters;

import androidx.room.TypeConverter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

public class UuidListConverter {

    @TypeConverter
    public static String fromUuidList(List<UUID> uuidList) {
        if (uuidList == null) {
            return null;
        }
        return uuidList.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));
    }

    @TypeConverter
    public static List<UUID> toUuidList(String uuidString) {
        if (uuidString == null || uuidString.isEmpty()) {
            return null;
        }
        return Arrays.stream(uuidString.split(","))
                .map(String::trim)
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }
}
