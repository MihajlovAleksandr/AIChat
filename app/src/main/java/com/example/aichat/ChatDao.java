package com.example.aichat;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatDao {
    @Insert
    void insertChat(Chat chat);

    @Query("SELECT * FROM Chats")
    List<Chat> getAllChats();

    @Query("SELECT * FROM Chats WHERE id = :chatId LIMIT 1")
    Chat getChatById(int chatId);
}
