package com.example.aichat.controller.main.chat;

import com.example.aichat.R;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

public class MessageController {

    private final UUID currentUserId;

    public MessageController(UUID currentUserId) {
        this.currentUserId = currentUserId;
    }

    public static String getFormattedMessageTime(Message message) {
        LocalDateTime t = message.getTimeFormat();
        if (t == null) return "";
        return t.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public boolean isMyMessage(Message message) {
        return message.isMyMessage(currentUserId);
    }

    public static MessageStatus getMaxStatus(Map<UUID, MessageStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return MessageStatus.SENT; // корректное поведение
        }
        return Collections.max(statuses.values(), Comparator.comparingInt(MessageStatus::getPriority));
    }

    public static int getStatusIconRes(MessageStatus status) {
        switch (status) {
            case READ:
                return R.drawable.ic_done_all;
            case SENT:
                return R.drawable.ic_done;
            case SENDING:
            default:
                return R.drawable.ic_time;
        }
    }
}
