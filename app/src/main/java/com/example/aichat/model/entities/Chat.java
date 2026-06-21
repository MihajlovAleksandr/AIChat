package com.example.aichat.model.entities;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.aichat.model.utils.time.TimeConverter;
import com.example.aichat.model.utils.database.converters.UuidListConverter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

@Entity(tableName = "Chats")
@TypeConverters(UuidListConverter.class)
public class Chat implements Comparable<Chat> {

    @PrimaryKey
    @NonNull
    private UUID id;
    private ChatType type;
    private String name;
    private String creationTime;
    private String endTime;
    private List<UUID> users;
    private boolean pinned = false;
    private boolean isRandomChatGuessCompleted = false;
    private boolean group = false;

    public Chat() {
        this.creationTime = TimeConverter.getString(LocalDateTime.now());
        this.users = new ArrayList<>();
        this.group = false;
        this.type = ChatType.Human;
        isRandomChatGuessCompleted = true;
    }

    @Ignore
    public Chat(@NotNull UUID id, String name, String creationTime, String endTime, List<UUID> users) {
        this.id = id;
        this.name = name;
        this.creationTime = creationTime;
        this.endTime = endTime;
        this.users = users != null ? users : new ArrayList<>();
        this.group = this.users.size() > 2;
        this.type = ChatType.Group;
    }

    @Ignore
    public Chat(@NotNull UUID id, String name, String creationTime, String endTime, List<UUID> users, ChatType type) {
        this.id = id;
        this.name = name;
        this.creationTime = creationTime;
        this.endTime = endTime;
        this.users = users != null ? users : new ArrayList<>();
        this.type = resolveSafeType(type, this.users);
        if(this.type == ChatType.Random){
            isRandomChatGuessCompleted = false;
        }

        if (this.type == ChatType.Group) {
            this.group = true;
        } else {
            this.group = this.users.size() > 2;
        }
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
        if (userId == null) return;

        if (users == null) {
            users = new ArrayList<>();
        }

        if (!users.contains(userId)) {
            users.add(userId);
        }

        if (getType() == ChatType.Group || users.size() > 2) {
            group = true;
        }
    }

    public void removeUser(UUID userId) {
        if (users == null || userId == null) return;

        users.remove(userId);

        if (getType() == ChatType.Group) {
            group = true;
        } else {
            group = users.size() > 2;
        }
    }

    public List<UUID> getUsers() {
        return users;
    }

    public void setUsers(List<UUID> users) {
        this.users = users != null ? users : new ArrayList<>();

        if (getType() == ChatType.Group) {
            this.group = true;
        } else {
            this.group = this.users.size() > 2;
        }
    }

    public void end() {
        endTime = TimeConverter.getString(LocalDateTime.now());
    }

    public boolean isActive() {
        if (endTime == null) return true;

        Log.d("isChatActive: ", getEndTimeFormat() + "\t\t" + LocalDateTime.now() + "\t\t" + !getEndTimeFormat().isBefore(LocalDateTime.now()));

        return !getEndTimeFormat().isBefore(LocalDateTime.now());
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;

        if (group && getType() == ChatType.Human) {
            this.type = ChatType.Group;
        }
    }

    public String getChatTypeHint() {
        ChatType safeType = resolveSafeType(type, users);

        switch (safeType) {
            case AI:
                return "AI";
            case Random:
                return "R";
            case Group:
                return "G";
            case Human:
            default:
                return "H";
        }
    }

    public ChatType getType() {
       return resolveSafeType(type, users);
    }

    public void setType(ChatType type) {
        this.type = resolveSafeType(type, users);

        if (this.type == ChatType.Group) {
            this.group = true;
        } else {
            this.group = users != null && users.size() > 2;
        }
    }

    private static ChatType resolveSafeType(ChatType type, List<UUID> users) {
        if (type != null) {
            return type;
        }

        return users != null && users.size() > 2
                ? ChatType.Group
                : ChatType.Human;
    }

    public boolean getIsRandomChatGuessCompleted(){
        return isRandomChatGuessCompleted;
    }

    public void setIsRandomChatGuessCompleted(boolean isRandomChatGuessCompleted){
        this.isRandomChatGuessCompleted = isRandomChatGuessCompleted;
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
