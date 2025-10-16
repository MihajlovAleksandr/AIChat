package com.example.aichat.model.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.aichat.model.utils.TimeConverter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity(tableName = "Chats")
public class Chat implements Comparable<Chat> {

    @PrimaryKey
    @NonNull
    private UUID id;
    private String name;
    private String creationTime;
    private String endTime;

    public Chat() {
        this.creationTime = TimeConverter.getString(LocalDateTime.now());
    }

    @Ignore
    public Chat(UUID id, String name, String creationTime, String endTime){
        this.id = id;
        this.name = name;
        this.creationTime = creationTime;
        this.endTime = endTime;
    }

    @NonNull
    public UUID getId() {
        return id;
    }

    @NonNull
    public void setId(UUID id) {
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
    public void end(){
        endTime = TimeConverter.getString(LocalDateTime.now());
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
