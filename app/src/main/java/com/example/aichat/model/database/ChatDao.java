package com.example.aichat.model.database;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Upsert;

import com.example.aichat.model.entities.Chat;

import java.util.List;
import java.util.UUID;

@Dao
public interface ChatDao {
    @Upsert
    void upsertChat(Chat chat);

    @Query("SELECT * FROM Chats")
    List<Chat> getAllChats();

    @Query("SELECT * FROM Chats WHERE id = :chatId LIMIT 1")
    Chat getChatById(UUID chatId);
    @Query("UPDATE Chats SET endTime = :endTime WHERE id=:id")
    void endChat(UUID id, String endTime);
    @Update
    void updateChat(Chat chat);
    @Query("DELETE FROM Chats")
    void clearTable();
}
