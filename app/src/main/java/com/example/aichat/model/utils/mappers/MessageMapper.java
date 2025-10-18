package com.example.aichat.model.utils.mappers;

import com.example.aichat.dto.request.MessageRequest;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.model.entities.Message;

public class MessageMapper implements Mapper<MessageRequest, Message, MessageResponse> {

    @Override
    public MessageRequest ToDTO(Message model) {
        return new MessageRequest(model.getId(), model.getChat(), model.getSender(), model.getText());
    }

    @Override
    public Message ToModel(MessageResponse response) {
        return new Message(response.id, response.text, response.sender, response.chat, response.time, response.lastUpdate);
    }
}
