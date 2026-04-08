package com.example.aichat.model.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.aichat.model.utils.TimeConverter;
import com.example.aichat.model.utils.UuidListConverter;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(tableName = "Chats")
@TypeConverters(UuidListConverter.class)
public class Chat implements Comparable<Chat> {

    @PrimaryKey
    @NonNull
    private UUID id;

    private String name;
    private String creationTime;
    private String endTime;
    private List<UUID> users;

    private boolean pinned = false;

    private boolean group = false;

    // -------------------- CONSTRUCTORS --------------------

    public Chat() {
        this.creationTime = TimeConverter.getString(LocalDateTime.now());
        this.users = new ArrayList<>();
    }

    @Ignore
    public Chat(@NotNull UUID id, String name, String creationTime, String endTime, List<UUID> users) {
        this.id = id;
        this.name = name;
        this.creationTime = creationTime;
        this.endTime = endTime;
        this.users = (users != null ? users : new ArrayList<>());
        this.group = (this.users != null && this.users.size() > 2);
    }
    @NonNull
    public UUID getId() {
        return id;
    }

    public void setId(@NonNull UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public LocalDateTime getCreationTimeFormat() {
        return TimeConverter.getLocalDateTime(creationTime);
    }

    public LocalDateTime getEndTimeFormat() {
        return TimeConverter.getLocalDateTime(endTime);
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void addUser(UUID userId) {
        if (users == null) users = new ArrayList<>();
        users.add(userId);
        this.group = users.size() > 2;
    }

    public void removeUser(UUID userId) {
        if (users == null) return;
        users.remove(userId);

        this.group = users.size() > 2;
    }

    public List<UUID> getUsers() {
        return users;
    }

    public void setUsers(List<UUID> users) {
        this.users = (users != null ? users : new ArrayList<>());
        this.group = this.users.size() > 2;
    }

    public void end() {
        endTime = TimeConverter.getString(LocalDateTime.now());
    }

    public boolean isActive() {
        return endTime == null;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    // -------------------- GROUP --------------------

    public boolean isGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    @Override
    public int compareTo(Chat other) {
        return this.getCreationTimeFormat().compareTo(other.getCreationTimeFormat());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Chat)) return false;
        Chat chat = (Chat) o;
        return Objects.equals(id, chat.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
