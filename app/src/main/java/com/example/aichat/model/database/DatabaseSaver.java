package com.example.aichat.model.database;

import android.content.Context;

import com.example.aichat.R;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.Notification;

public class DatabaseSaver {
    private AppDatabase appDatabase;
    public DatabaseSaver(AppDatabase appDatabase){
        this.appDatabase = appDatabase;
    }
    private void sendMessage(Message message){
        appDatabase.messageDao().insertMessage(message);
    }
    private void createChat(Chat chat){
        appDatabase.chatDao().insertChat(chat);
    }
    private void endChat(Chat endedChat){
        appDatabase.chatDao().endChat(endedChat.getId(),  endedChat.getEndTime());
    }
    public void commandGot(Command command, Context context){
        switch (command.getOperation()) {
            case "SendMessage":
                Message message = command.getData("message", Message.class);
                sendMessage(message);
                break;
            case "CreateChat":
                Chat createdChat = command.getData("chat", Chat.class);
                createChat(createdChat);
                break;
            case "EndChat":
                Chat endedChat = command.getData("chat", Chat.class);
                endChat(endedChat);
                break;
            case "Logout":
                new Thread(()-> {
                    SecurePreferencesManager.removeAuthToken(context);
                    SecurePreferencesManager.removeUserId(context);
                    appDatabase.chatDao().clearTable();
                    appDatabase.messageDao().clearTable();
                }).start();
                break;
        }
    }

}
