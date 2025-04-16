package com.example.aichat.model.entities;

public class User {
    int id;
    private UserData userData;
    private boolean isOnline;

    public int getId() {
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

    public User(int id, UserData userData, boolean isOnline){
        this.id = id;
        this.userData = userData;
        this.isOnline = isOnline;
    }

}
