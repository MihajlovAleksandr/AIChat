package com.example.aichat.model.database;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Upsert;

import com.example.aichat.model.entities.Message;

import java.util.List;
import java.util.UUID;

@Dao
public interface MessageDao {
    @Upsert
    void upsertMessage(Message message);
    @Query("SELECT * FROM Messages WHERE chat = :chatId")
    List<Message> getMessagesByChatId(UUID chatId);

    @Query("SELECT * FROM Messages WHERE Id = :messageId LIMIT 1")
    Message getMessageById(UUID messageId);
    @Query("SELECT * FROM messages WHERE Chat = :chatId ORDER BY Time DESC LIMIT 1")
    Message getLastMessageInChat(UUID chatId);

    @Query("SELECT * FROM Messages WHERE Time IN (SELECT MAX(Time) FROM Messages WHERE chat IN (:chats) GROUP BY chat) ORDER BY chat")
    List<Message> getLastMessages(List<UUID> chats);
    @Query("SELECT * FROM Messages")
    List<Message> getMessages();
    @Query("SELECT m.* FROM messages m INNER JOIN (SELECT chat, MIN(id) as first_message_id FROM messages WHERE text LIKE :message GROUP BY chat) first_msgs ON m.id = first_msgs.first_message_id AND m.Chat = first_msgs.Chat;")
    List<Message> getMessagesByText(String message);
    @Query("SELECT * FROM messages WHERE Text LIKE :message AND Chat = :chatId")
    List<Message> getMessagesByText(String message, UUID chatId);
    @Update
    void updateMessage(Message message);
    @Query("DELETE FROM Messages")
    void clearTable();
}
