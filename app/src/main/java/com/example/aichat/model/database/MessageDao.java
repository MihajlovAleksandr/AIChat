package com.example.aichat.model.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.aichat.model.entities.Message;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert (onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(Message message);
    @Query("SELECT * FROM Messages WHERE chat = :chatId")
    List<Message> getMessagesByChatId(int chatId);

    @Query("SELECT * FROM Messages WHERE Id = :messageId LIMIT 1")
    Message getMessageById(int messageId);
    @Query("SELECT * FROM messages WHERE Chat = :chatId ORDER BY Time DESC LIMIT 1")
    Message getLastMessageInChat(int chatId);

    @Query("SELECT * FROM Messages WHERE Time IN (SELECT MAX(Time) FROM Messages WHERE chat IN (:chats) GROUP BY chat) ORDER BY chat")
    List<Message> getLastMessages(List<Integer> chats);
    @Query("SELECT * FROM Messages")
    List<Message> getMessages();
    @Query("SELECT m.* FROM messages m INNER JOIN (SELECT chat, MIN(id) as first_message_id FROM messages WHERE text LIKE :message GROUP BY chat) first_msgs ON m.id = first_msgs.first_message_id AND m.Chat = first_msgs.Chat;")
    List<Message> getMessagesByText(String message);
    @Query("SELECT * FROM messages WHERE Text LIKE :message AND Chat = :chatId")
    List<Message> getMessagesByText(String message, int  chatId);
    @Update
    void updateMessage(Message message);
    @Query("DELETE FROM Messages")
    void clearTable();
}
