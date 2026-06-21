package com.example.aichat.controller.main.chat;

import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.R;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
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
        return message != null && message.isMyMessage(currentUserId);
    }

    public static MessageStatus getMaxStatus(Map<UUID, MessageStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return MessageStatus.SENT;
        }

        MessageStatus maxStatus = null;

        for (MessageStatus status : statuses.values()) {
            if (status == null) continue;

            if (maxStatus == null || status.getPriority() > maxStatus.getPriority()) {
                maxStatus = status;
            }
        }

        return maxStatus != null ? maxStatus : MessageStatus.SENT;
    }

    public static int getStatusIconRes(MessageStatus status) {
        if (status == null) {
            return R.drawable.ic_time;
        }

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
