package com.example.aichat.model.database;

import java.util.UUID;
import javax.annotation.Nullable;

public class GroupSearchModel {
    private boolean isSearching;
    @Nullable
    private UUID chatId;

    public GroupSearchModel(){
        isSearching = false;
        chatId = null;
    }

    public GroupSearchModel(boolean isSearching, @Nullable UUID chatId){
        this.isSearching = isSearching;
        this.chatId = chatId;
    }

    public void setSearching(boolean isSearching) {
        this.isSearching = isSearching;
    }

    public void setChatId(@Nullable UUID chatId) {
        this.chatId = chatId;
    }

    public boolean getIsSearching(){
        return isSearching;
    }

    @Nullable
    public UUID getChatId() {
        return chatId;
    }
}
