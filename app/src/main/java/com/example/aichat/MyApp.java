package com.example.aichat;

import android.app.Application;
import com.example.aichat.controller.main.chatlist.ChatsListController;
import com.example.aichat.model.database.DatabaseManager;

import java.util.UUID;

public class MyApp extends Application {

    private static MyApp instance;

    private ChatsListController chatsListController;
    private UUID currentUserId;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        DatabaseManager.init(this);
    }

    public static MyApp getInstance() {
        return instance;
    }

    public ChatsListController getChatsListController() {
        return chatsListController;
    }

    public void setChatsListController(ChatsListController controller) {
        this.chatsListController = controller;
    }

    public UUID getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
    }
}