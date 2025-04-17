package com.example.aichat.controller.main.chat;

import com.example.aichat.model.entities.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class MessageController {
    private int currentUserId;

    public MessageController(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    public static String getFormattedMessageTime(Message message) {
        return message.getLastUpdateFormat().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public boolean isMyMessage(Message message) {
        return message.isMyMessage(currentUserId);
    }
}
