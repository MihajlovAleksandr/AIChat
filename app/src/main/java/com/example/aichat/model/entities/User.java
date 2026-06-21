package com.example.aichat.model.entities;

import androidx.annotation.NonUiContext;
import androidx.annotation.Nullable;
import com.example.aichat.model.utils.time.TimeConverter;
import java.time.LocalDateTime;
import java.util.UUID;

public class User {
    UUID id;
    private UserData userData;
    @Nullable
    private String lastOnline;

    private String region;

    public UUID getId() {
        return id;
    }

    public UserData getUserData() {
        return userData;
    }
    public String getRegion(){
        return region;
    }

    public boolean isOnline() {
        return lastOnline == null;
    }

    public void setLastOnline(@Nullable String lastOnline){
        this.lastOnline = lastOnline;
    }
    public LocalDateTime getLastOnline(){
        return TimeConverter.getLocalDateTime(lastOnline);
    }

    public User(UUID id, UserData userData, @Nullable String lastOnline, String region){
        this.id = id;
        this.userData = userData;
        this.lastOnline = lastOnline;
        this.region = region;
    }

}
