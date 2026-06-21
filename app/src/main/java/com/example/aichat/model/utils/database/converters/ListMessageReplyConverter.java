package com.example.aichat.model.utils.database.converters;

import androidx.room.TypeConverter;
import com.example.aichat.model.entities.MessageReply;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ListMessageReplyConverter {

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

    @TypeConverter
    public static String fromList(List<MessageReply> list) {
        if (list == null) return null;
        try {
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert List<MessageReply> to String", e);
        }
    }

    @TypeConverter
    public static List<MessageReply> toList(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("null")) {
            return new ArrayList<>();
        }
        try {
            return mapper.readValue(json, new TypeReference<List<MessageReply>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert String to List<MessageReply>: " + json, e);
        }
    }
}
