package com.example.aichat.model.utils.mappers;

import com.example.aichat.dto.request.MessageRequest;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageStatus;

import java.util.HashMap;
import java.util.UUID;

public class MessageMapper implements Mapper<MessageRequest, Message, MessageResponse> {

    private final UUID userId;

    public MessageMapper(UUID userId){
        this.userId = userId;
    }

    @Override
    public MessageRequest ToDTO(Message model) {
        return new MessageRequest(
                model.getId(),
                model.getChat(),
                model.getSender(),
                model.getText(),
                model.getReplyMessages()
        );
    }

    @Override
    public Message ToModel(MessageResponse response) {

        // 1. Создаём объект из сервера
        Message mapped = new Message(
                response.id,
                response.text,
                response.sender,
                response.chat,
                response.time,
                response.lastUpdate,
                response.replyMessages,
                response.statuses
        );

        // 2. Проверяем, есть ли локальная версия сообщения
        AppDatabase db = DatabaseManager.getDatabase();
        Message local = db.messageDao().getMessageById(response.id);

        if (local != null && local.getStatuses() != null) {

            // 3. Объединяем статусы: серверные + локальные
            HashMap<UUID, MessageStatus> serverStatuses = mapped.getStatuses();
            HashMap<UUID, MessageStatus> localStatuses = local.getStatuses();

            if (serverStatuses != null) {
                serverStatuses.putAll(localStatuses);
            } else {
                mapped.setStatuses(localStatuses);
            }
        }

        return mapped;
    }
}
