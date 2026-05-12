package com.example.aichat.model.database;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.room.Upsert;

import com.example.aichat.controller.main.chatlist.ChatController;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageStatus;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Dao
public interface MessageDao {

    @Upsert
    void upsertMessage(Message message);

    @Query("SELECT * FROM Messages WHERE chat = :chatId ORDER BY Time")
    List<Message> getMessagesByChatId(UUID chatId);

    @Query("SELECT * FROM Messages WHERE Id = :messageId LIMIT 1")
    Message getMessageById(UUID messageId);

    @Query("SELECT * FROM messages WHERE Chat = :chatId ORDER BY Time DESC LIMIT 1")
    Message getLastMessageInChat(UUID chatId);

    @Query("SELECT m.* FROM Messages m " +
            "INNER JOIN (" +
            "   SELECT chat, MAX(Time) as max_time " +
            "   FROM Messages " +
            "   WHERE chat IN (:chats) " +
            "   GROUP BY chat" +
            ") grouped " +
            "ON m.chat = grouped.chat AND m.Time = grouped.max_time")
    List<Message> getLastMessages(List<UUID> chats);

    @Query("SELECT * FROM Messages")
    List<Message> getMessages();

    @Query("SELECT m.* FROM messages m INNER JOIN (SELECT chat, MIN(id) as first_message_id FROM messages WHERE text LIKE :message GROUP BY chat) first_msgs ON m.id = first_msgs.first_message_id AND m.Chat = first_msgs.Chat;")
    List<Message> getMessagesByText(String message);

    @Query("SELECT * FROM messages WHERE Text LIKE :message AND Chat = :chatId")
    List<Message> getMessagesByText(String message, UUID chatId);

    @Update
    void updateMessage(Message message);

    @Transaction
    default void updateMessageStatusForUser(UUID messageId, UUID userId, MessageStatus newStatus) {
        Message message = getMessageById(messageId);

        if (message != null) {
            if (message.getStatuses() == null) {
                message.setStatuses(new HashMap<>());
            }

            message.getStatuses().put(userId, newStatus);
            updateMessage(message);
        }
    }

    @Transaction
    default HashMap<UUID, List<Message>> getUnreadMessages(List<UUID> chatIds, UUID userId) {
        HashMap<UUID, List<Message>> unreadMessages = new HashMap<>();

        // ✅ Защита от null
        if (chatIds == null || userId == null) {
            return unreadMessages;
        }

        for (UUID chatId : chatIds) {
            // ✅ Защита от null chatId
            if (chatId == null) continue;

            List<Message> messages = getMessagesByChatId(chatId);
            if (messages != null) {
                unreadMessages.put(chatId,
                        ChatController.getUnreadMessages(messages, userId));
            }
        }

        return unreadMessages;
    }

    @Query("DELETE FROM Messages WHERE Id = :messageId")
    void deleteMessages(UUID messageId);

    @Query("DELETE FROM Messages")
    void clearTable();
}
