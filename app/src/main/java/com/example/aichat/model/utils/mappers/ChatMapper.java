package com.example.aichat.model.utils.mappers;

import com.example.aichat.dto.response.ChatResponse;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.ChatType;

public class ChatMapper implements MapperResponse<Chat, ChatResponse> {

    @Override
    public Chat ToModel(ChatResponse chatResponse) {
        if (chatResponse == null) return null;

        return new Chat(
                chatResponse.id,
                chatResponse.name,
                chatResponse.joinTime,
                chatResponse.endTime,
                chatResponse.users,
                chatResponse.type
        );
    }
}
