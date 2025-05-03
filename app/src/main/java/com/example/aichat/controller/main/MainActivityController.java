package com.example.aichat.controller.main;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Message;
import com.example.aichat.view.LoginActivity;
import com.example.aichat.view.main.MainActivityAdapter;

public class MainActivityController {
    private int currentChatId = -1;
    private ConnectionManager connectionManager;
    private AppDatabase appDatabase;
    MainActivityAdapter mainActivityAdapter;

    public MainActivityController(ConnectionManager connectionManager, Activity activity, MainActivityAdapter mainActivityAdapter) {
        Log.d("Loading", "MainActivityController");
        this.mainActivityAdapter =  mainActivityAdapter;
        appDatabase = DatabaseManager.getDatabase();
        this.connectionManager = connectionManager;
        connectionManager.setConnectionEvent(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
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
                    case "SyncDB":
                        mainActivityAdapter.setIsChatSearching(command.getData("isChatSearching", boolean.class));
                        Chat[] newChats = command.getData("newChats", Chat[].class);
                        for (Chat chat:newChats) {
                            appDatabase.chatDao().insertChat(chat);
                        }
                        Chat[] oldChats = command.getData("oldChats", Chat[].class);
                        for (Chat chat:oldChats) {
                            appDatabase.chatDao().updateChat(chat);
                        }
                        Message[] newMessages = command.getData("newMessages", Message[].class);
                        for (Message newMessage: newMessages) {
                            appDatabase.messageDao().insertMessage(newMessage);
                        }
                        Message[] oldMessages = command.getData("oldMessages", Message[].class);
                        for (Message oldMessage: oldMessages) {
                            appDatabase.messageDao().updateMessage(oldMessage);
                        }
                        mainActivityAdapter.loadChatList();
                        break;
                    case "Logout":
                        ConnectionSingleton.getInstance().setConnectionManager(connectionManager);
                        Intent intent = new Intent(activity, LoginActivity.class);
                        appDatabase.chatDao().clearTable();
                        appDatabase.messageDao().clearTable();
                        activity.startActivity(intent);
                        activity.finish();
                        break;
                    case "SearchChat":
                        mainActivityAdapter.setIsChatSearching(command.getData("isChatSeaching", boolean.class));
                        break;
                }
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
