package com.example.aichat.controller.main.chat;

import com.example.aichat.model.entities.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public class MessageController {
    private UUID currentUserId;

    public MessageController(UUID currentUserId) {
        this.currentUserId = currentUserId;
    }

    public static String getFormattedMessageTime(Message message) {
        return message.getTimeFormat().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public boolean isMyMessage(Message message) {
        return message.isMyMessage(currentUserId);
    }
}
