package com.example.aichat.controller.main.chat;

import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.entities.Message;
import com.example.aichat.view.main.chat.ChatFragment;

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
                    fragment.sendMessage(message);
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
    public void Destroy(){
        connectionManager.removeConnectionEvent(connectionEvents);
    }

}
