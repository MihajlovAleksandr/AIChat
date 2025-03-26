package com.example.aichat.controller;

import com.example.aichat.model.Chat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatController {

    public String getFormattedTime(Chat chat) {
        LocalDateTime displayTime = chat.isActive() ? chat.getCreationTimeFormat() : chat.getEndTimeFormat();
        return displayTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public int getStatusIcon(Chat chat) {
        return chat.isActive() ?
                android.R.drawable.presence_online :
                android.R.drawable.presence_invisible;
    }
}
