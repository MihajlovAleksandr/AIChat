package com.example.aichat.model.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.aichat.model.utils.HashMapConverter;
import com.example.aichat.model.utils.ListMessageReplyConverter;
import com.example.aichat.model.utils.TimeConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(
        tableName = "Messages",
        foreignKeys = @ForeignKey(
                entity = Chat.class,
                parentColumns = "id",
                childColumns = "chat",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = "chat")}
)
@TypeConverters({HashMapConverter.class, ListMessageReplyConverter.class})
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

    @JsonProperty
    private List<MessageReply> replyMessages;

    @JsonProperty
    private HashMap<UUID, MessageStatus> statuses;

    public Message() {
        this.replyMessages = new ArrayList<>();
        this.statuses = new HashMap<>();
    }

    @Ignore
    public Message(String text, UUID sender, UUID chat) {
        this();
        this.text = text;
        this.sender = sender;
        this.chat = chat;
    }

    @Ignore
    public Message(
            @NonNull UUID id,
            String text,
            UUID sender,
            UUID chat,
            String time,
            String lastUpdate,
            @Nullable List<MessageReply> replyMessages,
            @Nullable HashMap<UUID, MessageStatus> statuses
    ) {
        this.id = id;
        this.text = text;
        this.sender = sender;
        this.chat = chat;
        this.time = time;
        this.lastUpdate = lastUpdate;
        this.replyMessages = replyMessages != null ? replyMessages : new ArrayList<>();
        this.statuses = statuses != null ? statuses : new HashMap<>();
    }

    @JsonIgnore
    @NonNull
    public UUID getId() {
        return id;
    }

    public void setId(@NonNull UUID id) {
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
        return time != null ? TimeConverter.getLocalDateTime(time) : null;
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
        return lastUpdate != null ? TimeConverter.getLocalDateTime(lastUpdate) : null;
    }

    public List<MessageReply> getReplyMessages() {
        return replyMessages;
    }

    public void setReplyMessages(List<MessageReply> replyMessages) {
        this.replyMessages = replyMessages != null ? replyMessages : new ArrayList<>();
    }

    public HashMap<UUID, MessageStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(HashMap<UUID, MessageStatus> statuses) {
        this.statuses = statuses != null ? statuses : new HashMap<>();
    }

    @JsonIgnore
    public boolean isMyMessage(UUID userId) {
        return userId != null && userId.equals(sender);
    }


    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Message)) return false;
        Message other = (Message) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
