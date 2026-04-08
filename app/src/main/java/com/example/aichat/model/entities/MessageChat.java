package com.example.aichat.model.entities;

import android.content.res.Resources;

import com.example.aichat.R;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MessageChat implements Comparable<MessageChat> {

    private Message message;
    private Chat chat;
    private final List<Message> unreadMessages;

    public Message getMessage() {
        return message;
    }

    public Chat getChat() {
        return chat;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public MessageChat(Message message, Chat chat, List<Message> unreadMessages) {
        if (message != null) {
            if (!message.getChat().equals(chat.getId()))
                throw new IllegalArgumentException("Message is not from chat");
            this.message = message;
        }
        this.unreadMessages = unreadMessages;
        this.chat = chat;
    }

    public void updateLastMessageStatus(UUID userId, MessageStatus status) {
        if (message != null && message.getStatuses() != null) {
            message.getStatuses().put(userId, status);
        }
    }

    @Override
    public int compareTo(MessageChat other) {
        boolean thisPinned = chat.isPinned();
        boolean otherPinned = other.chat.isPinned();

        if (thisPinned && !otherPinned) return -1;
        if (!thisPinned && otherPinned) return 1;
        boolean thisHasUnread = getUnreadMessagesCount() > 0;
        boolean otherHasUnread = other.getUnreadMessagesCount() > 0;

        if (thisHasUnread && !otherHasUnread) return -1;
        if (!thisHasUnread && otherHasUnread) return 1;

        boolean thisGroup = chat.isGroup();
        boolean otherGroup = other.chat.isGroup();

        if (thisGroup && !otherGroup) return -1;
        if (!thisGroup && otherGroup) return 1;

        if (this.isEnded() && !other.isEnded()) return 1;
        if (!this.isEnded() && other.isEnded()) return -1;

        int timeCompare = other.getTime().compareTo(this.getTime());
        if (timeCompare != 0) return timeCompare;

        return this.chat.getId().compareTo(other.chat.getId());
    }

    public LocalDateTime getTime() {
        LocalDateTime time = chat.getEndTimeFormat();
        if (time != null) return time;

        if (message != null) {
            return message.getTimeFormat();
        }

        return chat.getCreationTimeFormat();
    }

    private boolean ended = false;

    public boolean isEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    public String getText(Resources resources) {
        if (chat.isActive()) {
            if (message == null)
                return resources.getString(R.string.chat_created);
            return message.getText();
        }
        return resources.getString(R.string.chat_ended);
    }

    public void addUnreadMessage(Message msg) {
        if (!unreadMessages.contains(msg))
            unreadMessages.add(msg);
    }

    public void clearUnreadMessages() {
        unreadMessages.clear();
    }

    public void removeUnreadMessage(UUID messageId) {
        unreadMessages.removeIf(message -> message.getId().equals(messageId));
    }

    public int getUnreadMessagesCount() {
        return unreadMessages.size();
    }
}
