package com.example.aichat.model.entities;

import android.health.connect.datatypes.StepsCadenceRecord;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

import com.example.aichat.model.utils.TimeConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity(tableName = "Messages",
        foreignKeys = @ForeignKey(entity = Chat.class,
                parentColumns = "id",
                childColumns = "chat",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "chat")})
public class Message {

    @PrimaryKey
    @JsonProperty
    @NonNull
    private UUID id;

    @JsonProperty
    private String text;

    @JsonProperty
    private UUID sender;

    @JsonProperty
    private UUID chat;

    @JsonProperty
    private String time;

    @JsonProperty
    private String lastUpdate;
    @Ignore
    public Message(String text, UUID sender, UUID chat) {
        this.text = text;
        this.sender = sender;
        this.chat = chat;
        time = null;
    }
    @Ignore
    public Message(@NonNull UUID id, String text, UUID sender, UUID chat, String time, String lastUpdate){
        this.id = id;
        this.text = text;
        this.sender = sender;
        this.chat = chat;
        this.time = time;
        this.lastUpdate = lastUpdate;
    }

    public Message() {
    }

    @JsonIgnore
    @NonNull
    public UUID getId() {
        return id;
    }

    @NonNull
    public void setId(UUID id) {
        this.id = id;
    }

    @JsonIgnore
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @JsonIgnore
    public UUID getSender() {
        return sender;
    }

    public void setSender(UUID sender) {
        this.sender = sender;
    }

    @JsonIgnore
    public UUID getChat() {
        return chat;
    }

    public void setChat(UUID chat) {
        this.chat = chat;
    }

    @JsonIgnore
    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @JsonIgnore
    public LocalDateTime getTimeFormat() {
        return TimeConverter.getLocalDateTime(time);
    }

    @JsonIgnore
    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @JsonIgnore
    public LocalDateTime getLastUpdateFormat() {
        return TimeConverter.getLocalDateTime(lastUpdate);
    }

    @JsonIgnore
    public boolean isMyMessage(UUID userId) {
        return userId.equals(sender);
    }
    @JsonIgnore
    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj==null) return false;
        if(obj.getClass()!=Message.class)return false;
        Message other = (Message)obj;
        return other.chat==chat;
    }
}
