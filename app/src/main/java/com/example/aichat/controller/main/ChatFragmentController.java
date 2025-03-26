package com.example.aichat.controller.main;

import com.example.aichat.model.entities.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatFragmentController {
    private int chatId;
    private List<Message> messages;

    public ChatFragmentController(int chatId) {
        this.chatId = chatId;
        this.messages = new ArrayList<>();
    }

    public List<Message> loadMessages() {
        return messages;
    }

    // Отправка сообщения: создаём Message и сохраняем его
    public Message sendMessage(String text, int currentUserId) {
        Message message = new Message(text, currentUserId, chatId);
        messages.add(message);
        // Добавьте сохранение сообщения в БД или отправку на сервер, если нужно
        return message;
    }

    public int getMessagesSize() {
        return messages.size();
    }
}
