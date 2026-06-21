package com.example.aichat.model.utils.mappers;

import com.example.aichat.dto.request.MessageRequest;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.model.entities.Message;
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
                model.getText(),
                model.getReplyMessages(),
                null
        );
    }

    @Override
    public Message ToModel(MessageResponse response) {
        return new Message(
                response.id,
                normalizeFileTags(response.text),
                response.userId,
                response.chatId,
                response.time,
                response.lastUpdate,
                response.replies,
                response.statuses,
                response.files,
                null,
                null
        );
    }

    private String normalizeFileTags(String text) {
        if (text == null || text.isEmpty()) return text;

        return text
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
