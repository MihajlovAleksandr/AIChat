package com.example.aichat.controller.main;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.example.aichat.dto.request.MessageRequest;
import com.example.aichat.dto.response.ChatResponse;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.dto.response.SyncDBResponse;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.utils.mappers.ChatMapper;
import com.example.aichat.model.utils.mappers.Mapper;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.model.utils.mappers.MessageMapper;
import com.example.aichat.view.LoginActivity;
import com.example.aichat.view.main.MainActivityAdapter;

import java.util.UUID;

public class MainActivityController {
    private UUID currentChatId;
    private ConnectionManager connectionManager;
    private AppDatabase appDatabase;
    MainActivityAdapter mainActivityAdapter;
    public OnConnectionEvents events;
    private boolean isLogout = false;
    private final Mapper<MessageRequest, Message, MessageResponse> messageMapper = new MessageMapper();
    private final MapperResponse<Chat, ChatResponse> chatMapper = new ChatMapper();


    public MainActivityController(ConnectionManager connectionManager, Activity activity, MainActivityAdapter mainActivityAdapter, boolean isNewActivity) {
        Log.d("Loading", "MainActivityController");
        this.mainActivityAdapter =  mainActivityAdapter;
        appDatabase = DatabaseManager.getDatabase();
        this.connectionManager = connectionManager;
        events = new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                switch (command.getOperation()) {
                    case "SyncDB":
                        Log.e("AllMessagesInDBBeforeSync", ""+ appDatabase.messageDao().getMessages().size());
                        SyncDBResponse syncDBResponse = command.getData(SyncDBResponse.class);
                        Log.e("AllMessagesInDBBeforeSyncChat", ""+ appDatabase.messageDao().getMessages().size());
                        Chat[] newChats = ParseChat(syncDBResponse.newChats);
                        for (Chat chat:newChats) {
                            appDatabase.chatDao().upsertChat(chat);
                        }
                        Log.e("AllMessagesInDBAfterInsertNewChat", ""+ appDatabase.messageDao().getMessages().size());

                        Chat[] oldChats = ParseChat(syncDBResponse.oldChats);
                        for (Chat chat:oldChats) {
                            appDatabase.chatDao().updateChat(chat);
                        }
                        Log.e("AllMessagesInDBAfterOldInsert", ""+ appDatabase.messageDao().getMessages().size());

                        Message[] newMessages = ParseMessage(syncDBResponse.newMessages);
                        for (Message newMessage: newMessages) {
                            appDatabase.messageDao().upsertMessage(newMessage);
                        }
                        Log.e("AllMessagesInDBAfterInsertNewMessage", ""+ appDatabase.messageDao().getMessages().size());

                        Message[] oldMessages = ParseMessage(syncDBResponse.oldMessages);
                        for (Message oldMessage: oldMessages) {
                            appDatabase.messageDao().updateMessage(oldMessage);
                        }
                        Log.e("AllMessagesInDBAfterInsertOldMessages", ""+ appDatabase.messageDao().getMessages().size());

                        Log.e("AllMessagesInDBAfterDBSync", ""+ appDatabase.messageDao().getMessages().size());
                        mainActivityAdapter.loadChatList();
                        Log.e("AllMessagesInDBAfterUISync", ""+ appDatabase.messageDao().getMessages().size());

                        break;
                    case "Logout":
                        logout(activity);
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
        };
        if(isNewActivity)
            connectionManager.setConnectionEvent(events);
        else
            connectionManager.addConnectionEvent(events);
    }
    private Message[] ParseMessage(MessageResponse[] responses){
        Message[] messages = new Message[responses.length];
        for (int i = 0; i < responses.length; i++) {
            messages[i] = messageMapper.ToModel(responses[i]);
        }
        return  messages;
    }
    private Chat[] ParseChat(ChatResponse[] responses){
        Chat[] chats = new Chat[responses.length];
        for (int i = 0; i < responses.length; i++) {
            chats[i] = chatMapper.ToModel(responses[i]);
        }
        return chats;
    }
    public void logout(Activity activity) {
        if (!isLogout) {
            isLogout = true;
            ConnectionSingleton.getInstance().setConnectionManager(connectionManager);
            Intent intent = new Intent(activity, LoginActivity.class);
            activity.startActivity(intent);
            activity.finish();
        }
    }
    public UUID getCurrentChatId() {
        return currentChatId;
    }

    public void setCurrentChatId(UUID chatId) {
        this.currentChatId = chatId;
    }
    public ConnectionManager getConnectionManager(){
        return connectionManager;
    }
    public boolean destroy(){
        connectionManager.removeConnectionEvent(events);
        return isLogout;
    }
}
