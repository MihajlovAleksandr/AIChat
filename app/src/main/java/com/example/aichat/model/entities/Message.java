package com.example.aichat.model.entities;

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

@Entity(tableName = "Messages",
        foreignKeys = @ForeignKey(entity = Chat.class,
                parentColumns = "id",
                childColumns = "chat",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "chat")})
public class Message {

    @PrimaryKey
    @JsonProperty
    private int id;

    @JsonProperty
    private String text;

    @JsonProperty
    private int sender;

    @JsonProperty
    private int chat;

    @JsonProperty
    private String time;

    @JsonProperty
    private String lastUpdate;
    @Ignore
    public Message(String text, int sender, int chat) {
        this.text = text;
        this.sender = sender;
        this.chat = chat;
        time = null;
    }

    public Message() {
    }

    @JsonIgnore
    public int getId() {
        return id;
    }

    public void setId(int id) {
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
    public int getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    @JsonIgnore
    public int getChat() {
        return chat;
    }

    public void setChat(int chat) {
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
    public boolean isMyMessage(int userId) {
        return userId == sender;
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
