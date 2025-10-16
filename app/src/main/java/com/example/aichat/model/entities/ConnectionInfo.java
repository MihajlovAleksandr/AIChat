package com.example.aichat.model.entities;

import android.util.Log;

import androidx.annotation.Nullable;

import com.example.aichat.model.utils.TimeConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class ConnectionInfo implements Serializable {

    @JsonProperty
    private UUID id;

    @JsonProperty
    private UUID userId;

    @JsonProperty
    private String device;

    @JsonProperty
    private String lastOnline;

    public ConnectionInfo(UUID userId, String device) {
        this.userId = userId;
        this.device = device;
        this.lastOnline = null;
    }

    public ConnectionInfo() {
    }

    @JsonIgnore
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @JsonIgnore
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
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
                Log.e("", other.id.toString() + "\n"+ id.toString()+ "\n" + (id==other.id) + "\n" + (id.equals(other.id)));

                return other.id.equals(id);
            }
        }
        return false;
    }
}