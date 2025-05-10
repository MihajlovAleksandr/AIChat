package com.example.aichat.model.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;

import com.example.aichat.model.entities.Chat;

import java.time.LocalDateTime;
import java.util.List;

@Dao
public interface ChatDao {
    @Insert (onConflict = OnConflictStrategy.IGNORE)
    void insertChat(Chat chat);

    @Query("SELECT * FROM Chats")
    List<Chat> getAllChats();

    @Query("SELECT * FROM Chats WHERE id = :chatId LIMIT 1")
    Chat getChatById(int chatId);
    @Query("UPDATE Chats SET endTime = :endTime WHERE id=:id")
    void endChat(int id, String endTime);
    @Update
    void updateChat(Chat chat);
    @Query("DELETE FROM Chats")
    void clearTable();
}
