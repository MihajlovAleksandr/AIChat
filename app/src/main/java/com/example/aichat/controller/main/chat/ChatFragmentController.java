package com.example.aichat.controller.main.chat;

import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.User;
import com.example.aichat.model.entities.UserData;
import com.example.aichat.view.main.chat.ChatFragment;

import java.util.ArrayList;
import java.util.List;

public class ChatFragmentController {
    private int chatId;
    private ConnectionManager connectionManager;
    private int currentUserId;
    private ChatFragment fragment;
    private OnConnectionEvents connectionEvents = new OnConnectionEvents() {
        @Override
        public void OnCommandGot(Command command) {
            switch (command.getOperation()){
                case "SendMessage":
                    Message message =  command.getData("message", Message.class);
                    if(message.getChat()==chatId)
                        fragment.sendMessage(message);
                    break;
                case "SyncDB":
                    Message[] newMessages = command.getData("newMessages", Message[].class);
                    for (Message newMessage: newMessages) {
                        if(newMessage.getChat()==chatId)
                            fragment.sendMessage(newMessage);
                    }
                    Message[] oldMessages = command.getData("oldMessages", Message[].class);
                    for (Message oldMessage: oldMessages) {
                        //
                    }
                    break;
                case "EndChat":
                    Chat endedChat = command.getData("chat", Chat.class);
                    if(endedChat.getId()==chatId)
                        fragment.endChat();
                    break;
                case "LoadUsersInChat":
                    int[] ids = command.getData("ids", int[].class);
                    UserData[] userData = command.getData("userData", UserData[].class);
                    boolean[] online = command.getData("isOnline", boolean[].class);
                    List<User> users = new ArrayList<User>();
                    for(int i=0;i<ids.length;i++){
                        users.add(new User(ids[i], userData[i], online[i]));
                    }
                    fragment.loadUsers(users);
                    break;
                case "UserOnlineChanges":
                    int userId = command.getData("userId", int.class);
                    boolean isOnline = command.getData("isOnline", boolean.class);
                    fragment.updateOnlineState(userId, isOnline);
                    break;

            }
        }

        @Override
        public void OnConnectionFailed() {

        }

        @Override
        public void OnOpen() {

        }
    };

    public ChatFragmentController(ChatFragment fragment,ConnectionManager connectionManager, int chatId, int currentUserId) {
        this.fragment = fragment;
        this.currentUserId = currentUserId;
        this.connectionManager = connectionManager;
        connectionManager.addConnectionEvent(connectionEvents);
        this.chatId = chatId;
    }
    public void sendMessage(String text) {
        Message message = new Message(text, currentUserId, chatId);
        Command command = new Command("SendMessage");
        command.addData("message", message);
        connectionManager.SendCommand(command);
    }
    public void endChat(){
        Command command = new Command("EndChat");
        command.addData("chatId", chatId);
        connectionManager.SendCommand(command);
    }
    public void loadUsers(){
        Command command = new Command("LoadUsersInChat");
        command.addData("chatId", chatId);
        connectionManager.SendCommand(command);
    }
    public void Destroy(){
        connectionManager.removeConnectionEvent(connectionEvents);
    }

}
