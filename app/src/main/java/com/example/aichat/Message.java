package com.example.aichat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
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

    public Message(String text, int sender, int chat) {
        this.text = text;
        this.sender = sender;
        this.chat = chat;
        time = "2025-03-22T20:27:12.961465Z";
    }

    public Message() {
        time = "2025-03-22T20:27:12.961465Z";
    }

    @JsonIgnore
    public int getId() {
        return id;
    }
    @JsonIgnore
    public String getText() {
        return text;
    }
    @JsonIgnore
    public int getSender() {
        return sender;
    }
    @JsonIgnore
    public int getChat() {
        return chat;
    }
    @JsonIgnore
    public LocalDateTime getTime() {
        return getLocalTime(time);
    }
    @JsonIgnore
    public LocalDateTime getLastUpdate() {
        return getLocalTime(lastUpdate);
    }

    @JsonIgnore
    private LocalDateTime getLocalTime(String  time) {
        ZonedDateTime utcDateTime = ZonedDateTime.parse(time, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return utcDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    @JsonIgnore
    public boolean isMyMessage(int userId) {
        return userId == sender;
    }
}