package com.example.aichat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

public class Chat implements Comparable<Chat> {
    private int id;
    private String creationTime;
    private String endTime;
    private int[] users;
    private String lastMessage;

    public Chat() {
        this.creationTime = ZonedDateTime.now(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public Chat(int id, int[] users, String lastMessage, boolean isActive) {
        this.id = id; // Убедитесь, что ID сохраняется
        this.users = users;
        this.lastMessage = lastMessage;
        this.creationTime = ZonedDateTime.now(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        if (!isActive) {
            this.endTime = ZonedDateTime.now(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }
    private LocalDateTime getLocalTime(String time) {
        if (time == null) return null;
        ZonedDateTime utcDateTime = ZonedDateTime.parse(time, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return utcDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    public LocalDateTime getCreationTime() {
        return getLocalTime(creationTime);
    }

    public LocalDateTime getEndTime() {
        return getLocalTime(endTime);
    }

    @Override
    public int compareTo(Chat other) {
        return this.getCreationTime().compareTo(other.getCreationTime());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return id == chat.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public int getId() {
        return id;
    }

    public int[] getUsers() {
        return users;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean isActive() {
        return endTime == null;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void endChat() {
        this.endTime = ZonedDateTime.now(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}