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

    public Message(String text, int sender, int chat) {
        this.text = text;
        this.sender = sender;
        this.chat = chat;
    }

    @JsonIgnore
    public LocalDateTime getTime() {
        ZonedDateTime utcDateTime = ZonedDateTime.parse(time, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return utcDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    public Message() {
    }
}
