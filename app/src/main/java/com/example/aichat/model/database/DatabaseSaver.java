package com.example.aichat.model.database;

import android.content.Context;

import com.example.aichat.R;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.Notification;

public class DatabaseSaver {
    private AppDatabase appDatabase;
    private int currentUserId;
    public DatabaseSaver(AppDatabase appDatabase, int currentUserId){
        this.appDatabase = appDatabase;
        this.currentUserId = currentUserId;
    }
    public DatabaseSaver(AppDatabase appDatabase){
        this.appDatabase = appDatabase;
        this.currentUserId = -1;
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
    public void setCurrentUserId(int currentUserId){
        this.currentUserId = currentUserId;
    }
    public Notification commandGot(Command command, Context context){
        switch (command.getOperation()) {
            case "SendMessage":
                Message message = command.getData("message", Message.class);
                sendMessage(message);
                if(!message.isMyMessage(currentUserId) && currentUserId!=-1)
                    return new Notification(context.getString(R.string.new_message),  message.getText());
                break;
            case "CreateChat":
                Chat createdChat = command.getData("chat", Chat.class);
                createChat(createdChat);
                return new Notification(context.getString(R.string.new_chat), context.getString(R.string.start_chatting));
            case "EndChat":
                Chat endedChat = command.getData("chat", Chat.class);
                endChat(endedChat);
                return new Notification(context.getString(R.string.end_chat), context.getString(R.string.start_new_chat));
        }
        return null;
    }

}
