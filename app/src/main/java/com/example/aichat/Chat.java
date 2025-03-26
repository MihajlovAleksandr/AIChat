package com.example.aichat;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity(tableName = "Chats")
public class Chat implements Comparable<Chat> {

    @PrimaryKey
    private int id;

    private String creationTime;
    private String endTime;

    public Chat() {
        this.creationTime = TimeConverter.getString(LocalDateTime.now());
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCreationTime() {
        return creationTime;
    }
    public LocalDateTime getCreationTimeFormat(){
        return TimeConverter.getLocalDateTime(creationTime);
    }
    public LocalDateTime getEndTimeFormat(){
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


    public boolean isActive() {
        return endTime == null;
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
}
