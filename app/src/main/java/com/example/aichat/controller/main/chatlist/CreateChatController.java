package com.example.aichat.controller.main.chatlist;

import android.util.Log;
import com.example.aichat.dto.request.AddUserToChatRequest;
import com.example.aichat.model.entities.ChatType;
import java.util.UUID;

public class CreateChatController {

    private static final String TAG = "CreateChatController";
    private final UUID userId;
    private boolean isChatSearching = false;
    private boolean isUserAdding = false;

    public CreateChatController(UUID userId) {
        this.userId = userId;
    }

    public void addChat(ChatType type) {
        setIsChatSearching(true);
    }

    public void addUserToChat() {
        setIsUserAdding(true);
    }

    public void stopSearchingChat() {
        setIsChatSearching(false);
    }

    public void stopAddingUserToChat() {
        setIsUserAdding(false);
    }

    public boolean getIsChatSearching() { return isChatSearching; }
    public boolean getIsUserAdding() { return isUserAdding; }

    private void setIsChatSearching(boolean value) { isChatSearching = value; }
    private void setIsUserAdding(boolean value) { isUserAdding = value; }
}
