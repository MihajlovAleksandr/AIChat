package com.example.aichat.controller.main.chat.actions;

import com.example.aichat.dto.request.EndChatRequest;
import com.example.aichat.dto.response.DeleteChatResponse;
import com.example.aichat.dto.response.UpdateChatNameResponse;

import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.view.main.chat.ChatFragment;

import java.util.UUID;

public class ChatStateActions {

    private final ChatFragment fragment;
    private final UUID chatId;

    private final ConnectionManager connectionManager;

    public ChatStateActions(ChatFragment fragment, UUID chatId) {
        this.fragment = fragment;
        this.chatId = chatId;

        this.connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
    }
    public void endChat() {

        connectionManager.SendCommand(
                new WSSCommand("EndChat", new EndChatRequest(chatId))
        );

        new Thread(() -> {

            Chat chat = DatabaseManager.getDatabase().chatDao().getChatById(chatId);
            if (chat != null && chat.getEndTime() == null) {
                chat.setEndTime(String.valueOf(System.currentTimeMillis()));
                DatabaseManager.getDatabase().chatDao().updateChat(chat);
            }

            if (fragment.isAdded()) {
                fragment.requireActivity().runOnUiThread(fragment::showChatEnded);
            }

        }).start();
    }

    public void onChatEnded() {
        if (!fragment.isAdded()) return;

        fragment.requireActivity().runOnUiThread(fragment::showChatEnded);
    }

    public void onChatDeleted(DeleteChatResponse response) {
        if (!chatId.equals(response.chatId)) return;
        if (!fragment.isAdded()) return;

        fragment.requireActivity().runOnUiThread(fragment::navigateBack);
    }

    public void onChatRenamed(UpdateChatNameResponse response) {
        if (!response.chatId.equals(chatId)) return;
        if (!fragment.isAdded()) return;

        fragment.requireActivity().runOnUiThread(() ->
                fragment.setChatTitle(response.name)
        );
    }
}
