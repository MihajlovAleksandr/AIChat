package com.example.aichat.controller.main;

import com.example.aichat.model.entities.Message;

import java.time.format.DateTimeFormatter;

public class MessageController {
    private int currentUserId;

    public MessageController(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    public String getFormattedMessageTime(Message message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return message.getTime().format(String.valueOf(formatter)) + " (Чат " + message.getChat() + ")";
    }

    public boolean isMyMessage(Message message) {
        return message.isMyMessage(currentUserId);
    }
}
