package com.example.aichat.controller.main.chat.actions;

import android.util.Log;

import com.example.aichat.dto.request.MessageRequest;
import com.example.aichat.dto.request.UpdateMessageStatusRequest;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.dto.response.UpdateMessageStatusResponse;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.database.DatabaseSaver;
import com.example.aichat.model.entities.FileType;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageReply;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.model.utils.TimeConverter;
import com.example.aichat.model.utils.mappers.MessageMapper;
import com.example.aichat.view.main.chat.ChatFragment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ChatMessageActions {

    private static final String TAG = "ChatMessageActions";

    private final ChatFragment fragment;
    private final UUID chatId;
    private final UUID currentUserId;

    private final AppDatabase database;
    private final DatabaseSaver saver;
    private final MessageMapper messageMapper;
    private final ConnectionDispatcher dispatcher;

    private List<UUID> usersInChat = new ArrayList<>();

    public ChatMessageActions(ChatFragment fragment, UUID chatId, UUID currentUserId) {
        this.fragment = fragment;
        this.chatId = chatId;
        this.currentUserId = currentUserId;

        this.database = DatabaseManager.getDatabase();
        this.saver = new DatabaseSaver(database, currentUserId);
        this.messageMapper = new MessageMapper(currentUserId);
        this.dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
    }

    public void updateUsers(List<UUID> users) {
        this.usersInChat = users != null ? new ArrayList<>(users) : new ArrayList<>();
    }

    /**
     * Отправка сообщения
     */
    public void sendMessage(String text, List<UUID> fileIds, HashMap<UUID, FileType> fileTypes) {
        if ((text == null || text.trim().isEmpty()) && (fileIds == null || fileIds.isEmpty())) return;

        HashMap<UUID, MessageStatus> statuses = new HashMap<>();
        for (UUID id : usersInChat) {
            if (!id.equals(currentUserId)) {
                statuses.put(id, MessageStatus.SENDING);
            }
        }

        Message message = new Message(
                UUID.randomUUID(),
                text,
                currentUserId,
                chatId,
                TimeConverter.getString(LocalDateTime.now()),
                TimeConverter.getString(LocalDateTime.now()),
                new ArrayList<>(),
                statuses,
                fileIds != null ? fileIds : new ArrayList<>(),
                new HashMap<>(),
                fileTypes != null ? fileTypes : new HashMap<>()
        );

        new Thread(() -> saver.sendMessage(message)).start();

        if (fragment.isAdded() && fragment.getActivity() != null) {
            fragment.getActivity().runOnUiThread(() -> {
                fragment.addOrUpdateMessage(message);

                if (fragment.getActivity() instanceof com.example.aichat.view.main.MainActivity) {
                    ((com.example.aichat.view.main.MainActivity) fragment.getActivity())
                            .updateChatLastMessage(message);
                }
            });
        }

        MessageRequest request = new MessageRequest(
                message.getId(),
                chatId,
                text,
                new ArrayList<>(),
                null
        );

        dispatcher.sendHttpRequestAsync("/api/messages", HttpClient.HTTPMethod.POST, request, true)
                .thenAccept(cmd -> {
                    if (cmd.isSuccess()) {
                        updateLocalMessageStatus(message.getId(), MessageStatus.SENT);
                        Log.d(TAG, "Message sent: " + message.getId());
                    } else {
                        Log.e(TAG, "Send failed: " + message.getId() + ", code: " + cmd.getCode());
                        showErrorToast();
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Exception sending message", throwable);
                    showErrorToast();
                    return null;
                });
    }

    /**
     * Показать Toast об ошибке
     */
    private void showErrorToast() {
        if (fragment.isAdded() && fragment.getActivity() != null) {
            fragment.getActivity().runOnUiThread(() ->
                    android.widget.Toast.makeText(fragment.getContext(),
                            "Ошибка отправки сообщения", android.widget.Toast.LENGTH_SHORT).show()
            );
        }
    }

    /**
     * Локальное обновление статуса сообщения
     */
    private void updateLocalMessageStatus(UUID messageId, MessageStatus status) {
        new Thread(() -> {
            Message msg = database.messageDao().getMessageById(messageId);
            if (msg != null && msg.getStatuses() != null) {
                for (UUID userId : msg.getStatuses().keySet()) {
                    msg.getStatuses().put(userId, status);
                }
                saver.updateMessage(msg);

                if (fragment.isAdded() && fragment.getActivity() != null) {
                    fragment.getActivity().runOnUiThread(() ->
                            fragment.updateMessageStatus(messageId, currentUserId, status));
                }
            }
        }).start();
    }

    /**
     * Ответ на сообщение
     */
    public void replyToMessage(Message original, String text) {
        if (original == null || text == null || text.trim().isEmpty()) return;

        List<MessageReply> replies = new ArrayList<>();
        replies.add(new MessageReply(original.getId()));

        Message message = new Message(
                UUID.randomUUID(),
                text,
                currentUserId,
                chatId,
                TimeConverter.getString(LocalDateTime.now()),
                TimeConverter.getString(LocalDateTime.now()),
                replies,
                new HashMap<>(),
                null,
                null,
                null
        );

        for (UUID id : usersInChat) {
            if (!id.equals(currentUserId)) {
                message.getStatuses().put(id, MessageStatus.SENDING);
            }
        }

        new Thread(() -> saver.sendMessage(message)).start();

        if (fragment.isAdded() && fragment.getActivity() != null) {
            fragment.getActivity().runOnUiThread(() -> {
                fragment.addOrUpdateMessage(message);

                if (fragment.getActivity() instanceof com.example.aichat.view.main.MainActivity) {
                    ((com.example.aichat.view.main.MainActivity) fragment.getActivity())
                            .updateChatLastMessage(message);
                }
            });
        }

        MessageRequest request = new MessageRequest(
                message.getId(), chatId, text, replies, null
        );

        dispatcher.sendHttpRequestAsync("/api/messages", HttpClient.HTTPMethod.POST, request, true)
                .thenAccept(cmd -> {
                    if (cmd.isSuccess()) {
                        updateLocalMessageStatus(message.getId(), MessageStatus.SENT);
                    } else {
                        showErrorToast();
                    }
                })
                .exceptionally(throwable -> {
                    showErrorToast();
                    return null;
                });
    }

    /**
     * Редактирование сообщения
     */
    public void editMessage(Message original, String newText) {
        if (original == null || newText == null || newText.trim().isEmpty()) return;

        original.setText(newText);
        original.setLastUpdate(TimeConverter.getString(LocalDateTime.now()));

        if (fragment.isAdded()) {
            fragment.updateMessage(original);
        }

        new Thread(() -> saver.sendMessage(original)).start();

        MessageRequest request = new MessageRequest(
                original.getId(), chatId, newText, original.getReplyMessages(), null
        );

        dispatcher.sendHttpRequestAsync("/api/messages", HttpClient.HTTPMethod.PUT, request, true)
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to edit message", throwable);
                    showErrorToast();
                    return null;
                });
    }

    /**
     * Удаление сообщения
     */
    public void deleteMessage(Message message) {
        if (message == null) return;

        if (fragment.isAdded()) {
            fragment.removeMessage(message.getId());
        }

        new Thread(() -> saver.deleteMessage(message.getId())).start();

        dispatcher.sendHttpRequestAsync("/api/messages/" + message.getId(),
                        HttpClient.HTTPMethod.DELETE, null, false)
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to delete message", throwable);
                    return null;
                });
    }

    /**
     * Получение сообщения от сервера
     */
    public void onMessageReceived(MessageResponse response) {
        if (response == null || !response.chatId.equals(chatId)) return;

        new Thread(() -> {

            saver.saveMessageFromResponse(response);

            if (fragment.isAdded() && fragment.getActivity() != null) {
                fragment.getActivity().runOnUiThread(() ->
                        fragment.reloadMessagesFromDatabase()
                );
            }

        }).start();
    }

    /**
     * Обновление статуса от сервера
     */
    public void onMessageStatusUpdated(UpdateMessageStatusResponse status) {
        if (!status.chatId.equals(chatId)) return;

        new Thread(() -> saver.updateMessageStatus(status)).start();

        if (fragment.isAdded()) {
            for (UUID messageId : status.messageIds) {
                fragment.updateMessageStatus(messageId, status.userId, status.status);
            }
        }
    }

    /**
     * Отметка прочитанных сообщений
     */
    public void readMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return;

        new Thread(() -> {
            List<UUID> ids = new ArrayList<>();
            for (Message m : messages) {
                ids.add(m.getId());
                if (m.getStatuses() != null) {
                    m.getStatuses().put(currentUserId, MessageStatus.READ);
                    saver.updateMessage(m);
                }
            }

            try {
                dispatcher.sendSignalRRequestAsync("UpdateMessageStatus",
                        new UpdateMessageStatusRequest(ids, MessageStatus.READ));
            } catch (Exception e) {
                Log.e(TAG, "Failed to send read status", e);
            }
        }).start();
    }

    public void sendMessage(String text) {
        sendMessage(text, null, null);
    }

    /**
     * Поиск сообщений
     */
    public void findMessages(String text) {
        if (text == null || text.trim().isEmpty()) return;

        new Thread(() -> {
            List<Message> messages = database.messageDao()
                    .getMessagesByText("%" + text + "%", chatId);

            if (fragment.getActivity() == null || !fragment.isAdded()) return;

            fragment.getActivity().runOnUiThread(() ->
                    fragment.showSearchResults(messages, text));
        }).start();
    }
}