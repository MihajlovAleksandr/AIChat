package com.example.aichat;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insertMessage(Message message);

    @Query("SELECT * FROM Messages WHERE chat = :chatId")
    List<Message> getMessagesByChatId(int chatId);
}
