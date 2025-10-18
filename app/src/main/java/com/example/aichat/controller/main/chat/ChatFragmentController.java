package com.example.aichat.controller.main.chat;

import com.example.aichat.dto.request.EndChatRequest;
import com.example.aichat.dto.request.MessageRequest;
import com.example.aichat.dto.request.UsersInChatRequest;
import com.example.aichat.dto.response.ChatResponse;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.dto.response.SyncDBResponse;
import com.example.aichat.dto.response.UserDataResponse;
import com.example.aichat.dto.response.UserOnlineChangesResponse;
import com.example.aichat.dto.response.UsersInChatResponse;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.User;
import com.example.aichat.model.entities.UserData;
import com.example.aichat.model.utils.TimeConverter;
import com.example.aichat.model.utils.mappers.Mapper;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.model.utils.mappers.MessageMapper;
import com.example.aichat.model.utils.mappers.UserDataMapper;
import com.example.aichat.view.main.chat.ChatFragment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ChatFragmentController {
    private final UUID chatId;
    private final ConnectionManager connectionManager;
    private final UUID currentUserId;
    private final ChatFragment fragment;
    private final Mapper<MessageRequest, Message, MessageResponse> messageMapper = new MessageMapper();
    private final MapperResponse<UserData, UserDataResponse> userDataUserDataResponseMapperResponse = new UserDataMapper();
    private final OnConnectionEvents connectionEvents = new OnConnectionEvents() {
        @Override
        public void OnCommandGot(Command command) {
            switch (command.getOperation()){
                case "SendMessage":
                    MessageResponse messageResponse = command.getData(MessageResponse.class);

                    Message message = messageMapper.ToModel(messageResponse);
                    if(message.getChat().equals(chatId))
                        fragment.sendMessage(message);
                    break;
                case "SyncDB":
                    Message[] newMessages = ParseMessage(command.getData(SyncDBResponse.class).newMessages);
                    for (Message newMessage: newMessages) {
                        if(newMessage.getChat().equals(chatId))
                            fragment.sendMessage(newMessage);
                    }
                    Message[] oldMessages = ParseMessage(command.getData(SyncDBResponse.class).oldMessages);
                    for (Message oldMessage: oldMessages) {
                        //
                    }
                    break;
                case "EndChat":
                    ChatResponse chatResponse = command.getData(ChatResponse.class);
                    if(chatResponse.id.equals(chatId))
                        fragment.endChat();
                    break;
                case "LoadUsersInChat":
                    UsersInChatResponse usersInChatResponse = command.getData(UsersInChatResponse.class);
                   List<User> users = new ArrayList<User>();
                    for(int i=0;i<usersInChatResponse.ids.length;i++){
                        users.add(new User(usersInChatResponse.ids[i], userDataUserDataResponseMapperResponse.ToModel(usersInChatResponse.userData[i]), usersInChatResponse.isOnline[i]));
                    }
                    fragment.loadUsers(users);
                    break;
                case "UserOnlineChanges":
                    UserOnlineChangesResponse userOnlineChangesResponse = command.getData(UserOnlineChangesResponse.class);
                    fragment.updateOnlineState(userOnlineChangesResponse.userId, userOnlineChangesResponse.isOnline);
                    break;

            }
        }

        private Message[] ParseMessage(MessageResponse[] responses){
            Message[] messages = new Message[responses.length];
            for (int i = 0; i < responses.length; i++) {
                messages[i] = messageMapper.ToModel(responses[i]);
            }
            return  messages;
        }

        @Override
        public void OnConnectionFailed() {

        }

        @Override
        public void OnOpen() {

        }
    };
    public void findMessages(String text) {
        if (!Objects.equals(text, "")) {
            new Thread(() -> {
                List<Message> messages = DatabaseManager.getDatabase().messageDao().getMessagesByText("%" + text + "%", chatId);
                fragment.findMessages(messages, text);
            }).start();
        }
    }

    public ChatFragmentController(ChatFragment fragment,ConnectionManager connectionManager, UUID chatId, UUID currentUserId) {
        this.fragment = fragment;
        this.currentUserId = currentUserId;
        this.connectionManager = connectionManager;
        connectionManager.addConnectionEvent(connectionEvents);
        this.chatId = chatId;
    }
    public void sendMessage(String text) {
        Message message = new Message(UUID.randomUUID(), text, currentUserId,
                chatId, TimeConverter.getString(LocalDateTime.now()),
                TimeConverter.getString(LocalDateTime.now()));
        Command command = new Command("SendMessage", new MessageRequest(message.getId(), chatId, currentUserId, text));
        connectionManager.SendCommand(command);
        fragment.sendMessage(message);
        new Thread(() -> {
            DatabaseManager.getDatabase().messageDao().upsertMessage(message);
        }).start();
    }
    public void endChat(){
        Command command = new Command("EndChat", new EndChatRequest(chatId));
        connectionManager.SendCommand(command);
    }
    public void loadUsers(){
        Command command = new Command("LoadUsersInChat", new UsersInChatRequest(chatId));
        connectionManager.SendCommand(command);
    }
    public void destroy(){
        connectionManager.removeConnectionEvent(connectionEvents);
    }

}
