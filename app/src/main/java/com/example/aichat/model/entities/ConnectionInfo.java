package com.example.aichat.model.entities;

import androidx.annotation.Nullable;

import com.example.aichat.model.utils.TimeConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ConnectionInfo implements Serializable {

    @JsonProperty
    private int id;

    @JsonProperty
    private int userId;

    @JsonProperty
    private String device;

    @JsonProperty
    private String lastOnline;

    public ConnectionInfo(int userId, String device) {
        this.userId = userId;
        this.device = device;
        this.lastOnline = null;
    }

    public ConnectionInfo() {
    }

    @JsonIgnore
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @JsonIgnore
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @JsonIgnore
    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    @JsonIgnore
    public String getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(String lastOnline) {
        this.lastOnline = lastOnline;
    }

    @JsonIgnore
    public LocalDateTime getLastOnlineFormat() {
        return TimeConverter.getLocalDateTime(lastOnline);
    }

    @Override
    public String toString() {
        return "ConnectionInfo " + id + "/" + userId + ":\n" +
                device + " in " + lastOnline;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj!=null){
            if(obj.getClass()==ConnectionInfo.class){
                ConnectionInfo other = (ConnectionInfo) obj;
                return other.id==id;
            }
        }
        return false;
    }
}