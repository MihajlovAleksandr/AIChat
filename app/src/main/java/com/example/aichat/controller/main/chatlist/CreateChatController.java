package com.example.aichat.controller.main.chatlist;

import android.util.Log;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.entities.ChatType;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.dto.request.SearchChatRequest;
import com.example.aichat.dto.request.AddUserToChatRequest;

import java.util.UUID;

public class CreateChatController {

    private static final String TAG = "CreateChatController";

    private final ConnectionManager connectionManager;
    private final UUID userId;
    private boolean isChatSearching = false;
    private boolean isUserAdding = false;

    public CreateChatController(ConnectionManager connectionManager, UUID userId) {
        this.connectionManager = connectionManager;
        this.userId = userId;
    }

    public void addChat(ChatType type) {
        if (connectionManager != null /* && connectionManager.isConnected() */) { // проверка подключения при необходимости
            Log.d(TAG, "Adding chat of type " + type);
            connectionManager.SendCommand(new WSSCommand("SearchChat", new SearchChatRequest(type)));
            setIsChatSearching(true);
        } else {
            Log.e(TAG, "Cannot add chat, ConnectionManager not ready");
        }
    }

    public void addUserToChat() {
        if (connectionManager != null) {
            Log.d(TAG, "Adding user to group chat");
            connectionManager.SendCommand(new WSSCommand("AddUserToChat", new AddUserToChatRequest(ChatType.GROUP, "AllMatch")));
            setIsUserAdding(true);
        } else {
            Log.e(TAG, "Cannot add user, ConnectionManager not ready");
        }
    }

    public void stopSearchingChat() {
        if (connectionManager != null) {
            connectionManager.SendCommand(new WSSCommand("StopSearchingChat"));
            setIsChatSearching(false);
        }
    }

    public void stopAddingUserToChat() {
        if (connectionManager != null) {
            connectionManager.SendCommand(new WSSCommand("StopAddingUserToChat"));
            setIsUserAdding(false);
        }
    }

    public boolean getIsChatSearching() { return isChatSearching; }
    public boolean getIsUserAdding() { return isUserAdding; }

    private void setIsChatSearching(boolean value) { isChatSearching = value; }
    private void setIsUserAdding(boolean value) { isUserAdding = value; }
}