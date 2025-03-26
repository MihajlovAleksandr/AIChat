package com.example.aichat.controller.main;

import com.example.aichat.model.entities.Command;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.OnConnectionEvents;

public class ChatPageController {
    private int currentChatId = -1;
    private ConnectionManager connectionManager;

    public ChatPageController(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.connectionManager.SetCommandGot(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                // Обработка команды
            }

            @Override
            public void OnConnectionFailed() {
                // Обработка ошибки подключения
            }

            @Override
            public void OnOpen() {
                // Обработка открытия подключения
            }
        });
    }

    public int getCurrentChatId() {
        return currentChatId;
    }

    public void setCurrentChatId(int chatId) {
        this.currentChatId = chatId;
    }
}
