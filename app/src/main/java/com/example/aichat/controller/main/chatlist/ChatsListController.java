package com.example.aichat.controller.main.chatlist;

import android.util.Log;

import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Command;
import com.example.aichat.view.main.chatlist.ChatsListFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ChatsListController {
    private boolean isChatSearching = false;
    private ChatsListFragment fragment;
    private ConnectionManager connectionManager;
    private OnConnectionEvents connectionEvents = new OnConnectionEvents() {
        @Override
        public void OnCommandGot(Command command) {
            if(command.getOperation().equals("CreateChat")){
                Chat chat = command.getData("chat", Chat.class);
                fragment.createChat(chat);
                fragment.setFabAddChatState(true);
                isChatSearching = false;
            }
            else if(command.getOperation().equals("EndChat")){
                Chat chat = command.getData("chat", Chat.class);
                fragment.endChat(chat);
            }
        }

        @Override
        public void OnConnectionFailed() {

        }

        @Override
        public void OnOpen() {

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
        isChatSearching = !isChatSearching;
        fragment.setFabAddChatState(isChatSearching);
    }
    public void Destroy(){
        connectionManager.removeConnectionEvent(connectionEvents);
    }
}