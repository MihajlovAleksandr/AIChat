package com.example.aichat.model.entities;

import java.util.UUID;

public class User {
    UUID id;
    private UserData userData;
    private boolean isOnline;

    public UUID getId() {
        return id;
    }

    public UserData getUserData() {
        return userData;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public User(UUID id, UserData userData, boolean isOnline){
        this.id = id;
        this.userData = userData;
        this.isOnline = isOnline;
    }

}
