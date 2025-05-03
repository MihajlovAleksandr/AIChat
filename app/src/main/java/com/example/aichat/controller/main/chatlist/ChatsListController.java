package com.example.aichat.controller.main.chatlist;

import android.content.Intent;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.example.aichat.SettingsActivity;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageChat;
import com.example.aichat.view.main.chatlist.ChatsListFragment;

import java.util.ArrayList;
import java.util.List;

public class ChatsListController {
    private boolean isChatSearching = false;
    private ChatsListFragment fragment;
    private ConnectionManager connectionManager;
    private OnConnectionEvents connectionEvents = new OnConnectionEvents() {
        @Override
        public void OnCommandGot(Command command) {
            switch (command.getOperation()) {
                case "SendMessage":
                    Message message =  command.getData("message", Message.class);
                    fragment.updateLastMessage(message);
                    break;
                case "CreateChat":
                    Chat createdChat = command.getData("chat", Chat.class);
                    fragment.createChat(createdChat);
                    fragment.setFabAddChatState(true);
                    isChatSearching = false;
                    break;
                case "EndChat":
                    Chat endedChat = command.getData("chat", Chat.class);
                    fragment.endChat(endedChat);
                    break;
                case "SyncDB":
                    Chat[] newChats = command.getData("newChats", Chat[].class);
                    Message[] messages =  command.getData("newMessages", Message[].class);
                    List<Message> messageList = ChatController.getLastMessages(messages);
                    int currentMessage=0;
                    List<MessageChat> messageChats = new ArrayList<>();
                    for(int i=0;i<newChats.length;i++){
                        if(!newChats[i].isActive()){
                            messageChats.add(new MessageChat(null, newChats[i]));
                        }
                        boolean isAdded = false;
                        for (int j = currentMessage; j < messageList.size(); j++) {
                            if(newChats[i].getId()==messageList.get(j).getChat()){
                                fragment.createChat(new MessageChat(messageList.get(j), newChats[i]));
                                currentMessage=j+1;
                                isAdded=true;
                                break;
                            }
                            if(newChats[i].getId()<messageList.get(j).getChat()){
                                currentMessage=j;
                                break;
                            }
                            else{
                                fragment.updateLastMessage(messageList.get(j));
                                currentMessage=j+1;
                            }
                        }
                        if(!isAdded){
                            fragment.createChat(newChats[i]);
                        }
                    }
                    Chat[] oldChats = command.getData("oldChats", Chat[].class);
                    for (Chat chat : oldChats) {
                        fragment.endChat(chat);
                    }
                    break;
            }
        }

        @Override
        public void OnConnectionFailed() {
            fragment.setConnectionSuccess(false);
        }

        @Override
        public void OnOpen() {
            fragment.setConnectionSuccess(true);
        }
    };

    public ChatsListController(ChatsListFragment fragment, ConnectionManager connectionManager) {
        this.fragment  =  fragment;
        Log.d("ChatsListController", "Constructor started");
        connectionManager.addConnectionEvent(connectionEvents);
        this.connectionManager = connectionManager;
    }


    public void addStopChat() {
        if (!isChatSearching) {
            connectionManager.SendCommand(new Command("SearchChat"));
        }
        else {
            connectionManager.SendCommand(new Command("StopSearchingChat"));
        }
    }
    public void setInChatSearching(boolean isChatSearching){
        this.isChatSearching = isChatSearching;
        fragment.setFabAddChatState(!isChatSearching);
    }
    public void openSettings(FragmentActivity activity) {
        Intent intent = new Intent(activity, SettingsActivity.class);
        activity.startActivity(intent);
    }
    public void Destroy(){
        connectionManager.removeConnectionEvent(connectionEvents);
    }
}