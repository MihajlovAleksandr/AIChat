package com.example.aichat.controller.main;

import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.database.DatabaseManager;

import java.util.ArrayList;
import java.util.List;

public class ChatsListController {
    private List<Chat> chats;

    public ChatsListController() {
        chats = new ArrayList<>();
        loadInitialChats();
    }

    private void loadInitialChats() {
        new Thread(() -> chats = DatabaseManager.getDatabase().chatDao().getAllChats());
    }

    public List<Chat> getChats() {
        return chats;
    }

    public void addChat(Chat newChat) {
        chats.add(newChat);
        new Thread(() -> DatabaseManager.getDatabase().chatDao().insertChat(newChat));
    }
}
