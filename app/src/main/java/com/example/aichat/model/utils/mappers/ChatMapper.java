package com.example.aichat.model.utils.mappers;

import com.example.aichat.dto.response.ChatResponse;
import com.example.aichat.model.entities.Chat;

public class ChatMapper implements MapperResponse<Chat, ChatResponse> {

    @Override
    public Chat ToModel(ChatResponse chatResponse) {
        return new Chat(chatResponse.id, chatResponse.name, chatResponse.creationTime, chatResponse.endTime);
    }
}
