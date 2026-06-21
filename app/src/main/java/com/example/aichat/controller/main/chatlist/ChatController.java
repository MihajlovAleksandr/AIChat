package com.example.aichat.controller.main.chatlist;

import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageStatus;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;

public class ChatController {

    public static String getFormattedTime(LocalDateTime time) {
        if (time == null) return "";

        LocalDateTime now = LocalDateTime.now();

        if (now.toLocalDate().equals(time.toLocalDate())) {
            return time.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        if (time.isAfter(now.minusDays(7))) {
            return time.format(DateTimeFormatter.ofPattern("EE"));
        }
        if (time.isAfter(now.minusYears(1))) {
            return time.format(DateTimeFormatter.ofPattern("dd.MM"));
        }
        return time.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    public static int getStatusIcon(Chat chat) {
        return chat.isActive()
                ? android.R.drawable.presence_online
                : android.R.drawable.presence_invisible;
    }

    public static Message[] getLastMessages(Message[] messages) {
        if (messages == null || messages.length == 0) return new Message[0];

        Arrays.sort(messages, Comparator.comparing(Message::getTimeFormat));

        List<Message> result = new ArrayList<>();
        result.add(messages[messages.length - 1]);

        for (int i = messages.length - 2; i >= 0; i--) {
            UUID currentChat = messages[i].getChat();
            UUID nextChat = messages[i + 1].getChat();

            if (!currentChat.equals(nextChat)) {
                result.add(messages[i]);
            }
        }

        return result.toArray(new Message[0]);
    }

    private static MessageStatus getMaxStatus(Map<UUID, MessageStatus> statuses) {
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

    public static List<Message> getUnreadMessages(List<Message> messages, UUID userId) {
        if (messages == null || userId == null) {
            return new ArrayList<>();
        }

        List<Message> unread = new ArrayList<>();

        for (Message msg : messages) {
            if (msg == null || msg.getSender() == null) {
                continue;
            }

            // Непрочитанные - это сообщения НЕ от текущего пользователя
            if (!msg.getSender().equals(userId)) {
                MessageStatus status = getMaxStatus(msg.getStatuses());
                if (status != MessageStatus.READ) {
                    unread.add(msg);
                }
            }
        }

        return unread;
    }
}
