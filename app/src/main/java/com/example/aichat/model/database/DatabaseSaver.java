package com.example.aichat.model.database;

import android.content.Context;
import android.util.Log;

import com.example.aichat.dto.request.MessageRequest;
import com.example.aichat.dto.response.ChatResponse;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.utils.mappers.ChatMapper;
import com.example.aichat.model.utils.mappers.Mapper;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.model.utils.mappers.MessageMapper;

public class DatabaseSaver {
    private AppDatabase appDatabase;
    private final Mapper<MessageRequest, Message, MessageResponse> messageMapper = new MessageMapper();
    private final MapperResponse<Chat, ChatResponse> chatMapper = new ChatMapper();

    public DatabaseSaver(AppDatabase appDatabase){
        this.appDatabase = appDatabase;
    }
    private void sendMessage(Message message){
        appDatabase.messageDao().upsertMessage(message);
        Log.e("AllMessagesInDB", ""+ appDatabase.messageDao().getMessages().size());
    }
    private void createChat(Chat chat){
        appDatabase.chatDao().upsertChat(chat);
    }
    private void endChat(Chat endedChat){
        appDatabase.chatDao().endChat(endedChat.getId(),  endedChat.getEndTime());
    }
    public void commandGot(Command command, Context context){
        switch (command.getOperation()) {
            case "SendMessage":
                Message message = messageMapper.ToModel(command.getData(MessageResponse.class));
                sendMessage(message);
                break;
            case "CreateChat":
                Chat createdChat = chatMapper.ToModel(command.getData(ChatResponse.class));
                createChat(createdChat);
                break;
            case "EndChat":
                Chat endedChat = chatMapper.ToModel(command.getData(ChatResponse.class));
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
