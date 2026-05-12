package com.example.aichat.model.utils;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.example.aichat.R;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Message;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ChatExportService {

    private final Context context;
    private final UUID userId;

    public ChatExportService(Context context, UUID userId) {
        this.context = context;
        this.userId = userId;
    }

    public void exportChat(UUID chatId, String chatName) {
        new Thread(() -> {
            try {
                List<Message> messages = DatabaseManager.getDatabase()
                        .messageDao()
                        .getMessagesByChatId(chatId);

                if (messages == null || messages.isEmpty()) {
                    showToast("Нет сообщений для экспорта");
                    return;
                }

                String finalChatName = chatName;
                if (finalChatName == null || finalChatName.isEmpty()) {
                    Chat chat = DatabaseManager.getDatabase()
                            .chatDao()
                            .getChatById(chatId);
                    finalChatName = chat != null ? chat.getName() : "Chat";
                }

                if (context instanceof Activity) {
                    ChatPdfExporter exporter = new ChatPdfExporter((Activity) context, userId);
                    exporter.exportChat(messages, finalChatName);
                    showToast("Чат успешно экспортирован");
                } else {
                    showToast("Ошибка: контекст не является Activity");
                }

            } catch (IOException e) {
                e.printStackTrace();
                showToast("Ошибка при экспорте чата: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Неожиданная ошибка при экспорте");
            }
        }).start();
    }

    private void showToast(String message) {
        if (context != null) {
            ((Activity) context).runOnUiThread(() ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            );
        }

    }
}