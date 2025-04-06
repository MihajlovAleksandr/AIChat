package com.example.aichat.controller.main;

import android.util.Log;

import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Message;

public class MainActivityController {
    private int currentChatId = -1;
    private ConnectionManager connectionManager;
    private AppDatabase appDatabase;

    public MainActivityController(ConnectionManager connectionManager) {
        Log.d("Loading", "MainActivityController");
        appDatabase = DatabaseManager.getDatabase();
        this.connectionManager = connectionManager;
        connectionManager.setConnectionEvent(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                new Thread(()->{
                switch (command.getOperation()) {
                    case "SendMessage":
                        Message message = command.getData("message", Message.class);
                        appDatabase.messageDao().insertMessage(message);
                        break;
                    case "CreateChat":
                        Chat createdChat = command.getData("chat", Chat.class);
                        appDatabase.chatDao().insertChat(createdChat);
                        break;
                    case "EndChat":
                        Chat endedChat = command.getData("chat", Chat.class);
                        appDatabase.chatDao().endChat(endedChat.getId(),  endedChat.getEndTime());
                        break;
                }}).start();
            }

            @Override
            public void OnConnectionFailed() {
                // Обработка ошибки подключения
            }

            @Override
            public void OnOpen() {
                // Обработка открытия подключения
            }
        });
    }

    public int getCurrentChatId() {
        return currentChatId;
    }

    public void setCurrentChatId(int chatId) {
        this.currentChatId = chatId;
    }
    public ConnectionManager getConnectionManager(){
        return connectionManager;
    }
}
