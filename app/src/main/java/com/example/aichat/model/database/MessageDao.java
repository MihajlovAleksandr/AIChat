package com.example.aichat.model.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.aichat.model.entities.Message;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insertMessage(Message message);

    @Query("SELECT * FROM Messages WHERE chat = :chatId")
    List<Message> getMessagesByChatId(int chatId);

    @Query("SELECT * FROM Messages WHERE Id = :messageId LIMIT 1")
    Message getMessageById(int messageId);
    @Query("SELECT * FROM messages WHERE Chat = :chatId ORDER BY Time DESC LIMIT 1")
    Message getLastMessageInChat(int chatId);
    @Update
    void updateMessage(Message message);
    @Query("DELETE FROM Messages")
    void clearTable();
}
