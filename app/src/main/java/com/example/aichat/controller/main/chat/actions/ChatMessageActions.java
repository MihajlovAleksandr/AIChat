package com.example.aichat.controller.main.chat.actions;

import com.example.aichat.dto.request.MessageRequest;
import com.example.aichat.dto.request.UpdateMessageStatusRequest;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.dto.response.UpdateMessageStatusResponse;

import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageReply;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.model.utils.ChatPdfExporter;
import com.example.aichat.model.utils.TimeConverter;
import com.example.aichat.model.utils.mappers.MessageMapper;
import com.example.aichat.view.main.chat.ChatFragment;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ChatMessageActions {

    private final ChatFragment fragment;
    private final UUID chatId;
    private final UUID currentUserId;

    private final AppDatabase database;
    private final MessageMapper messageMapper;
    private final ConnectionManager connectionManager;

    private List<UUID> usersInChat = new ArrayList<>();

    public ChatMessageActions(ChatFragment fragment, UUID chatId, UUID currentUserId) {
        this.fragment = fragment;
        this.chatId = chatId;
        this.currentUserId = currentUserId;

        this.database = DatabaseManager.getDatabase();
        this.messageMapper = new MessageMapper(currentUserId);
        this.connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
    }

    public void updateUsers(List<UUID> users) {
        this.usersInChat = users != null ? new ArrayList<>(users) : new ArrayList<>();
    }

    public void sendMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;

        HashMap<UUID, MessageStatus> statuses = new HashMap<>();
        for (UUID id : usersInChat) {
            if (!id.equals(currentUserId)) {
                statuses.put(id, MessageStatus.SENDING);
            }
        }

        List<MessageReply> replies = new ArrayList<>();

        Message message = new Message(
                UUID.randomUUID(),
                text,
                currentUserId,
                chatId,
                TimeConverter.getString(LocalDateTime.now()),
                TimeConverter.getString(LocalDateTime.now()),
                replies,
                statuses
        );

        connectionManager.SendCommand(
                new WSSCommand("SendMessage",
                        new MessageRequest(message.getId(), chatId, currentUserId, text, replies))
        );

        if (fragment.isAdded()) {
            fragment.addOrUpdateMessage(message);
        }

        new Thread(() -> database.messageDao().upsertMessage(message)).start();
    }

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
                null
        );

        connectionManager.SendCommand(
                new WSSCommand("SendMessage",
                        new MessageRequest(message.getId(), chatId, currentUserId, text, replies))
        );

        if (fragment.isAdded()) {
            fragment.addOrUpdateMessage(message);
        }

        new Thread(() -> database.messageDao().upsertMessage(message)).start();
    }

    public void editMessage(Message original, String newText) {
        if (original == null || newText == null || newText.trim().isEmpty()) return;

        original.setText(newText);
        original.setLastUpdate(TimeConverter.getString(LocalDateTime.now()));

        connectionManager.SendCommand(
                new WSSCommand("EditMessage",
                        new MessageRequest(original.getId(), chatId, currentUserId, newText, original.getReplyMessages()))
        );

        if (fragment.isAdded()) {
            fragment.updateMessage(original);
        }

        new Thread(() -> database.messageDao().upsertMessage(original)).start();
    }

    public void deleteMessage(Message message) {
        if (message == null) return;

        connectionManager.SendCommand(
                new WSSCommand("DeleteMessage",
                        new MessageRequest(message.getId(), chatId, currentUserId, message.getText(), message.getReplyMessages()))
        );

        if (fragment.isAdded()) {
            fragment.removeMessage(message.getId());
        }

        new Thread(() -> database.messageDao().deleteMessages(message.getId())).start();
    }

    public void onMessageReceived(MessageResponse response) {
        Message message = messageMapper.ToModel(response);
        if (!message.getChat().equals(chatId)) return;

        if (fragment.isAdded()) {
            fragment.addOrUpdateMessage(message);
        }

        new Thread(() -> database.messageDao().upsertMessage(message)).start();
    }
    public void onMessageStatusUpdated(UpdateMessageStatusResponse status) {
        if (!status.chatId.equals(chatId)) return;

        if (fragment.isAdded()) {
            for (UUID messageId : status.messageIds) {
                fragment.updateMessageStatus(messageId, status.userId, status.status);
            }
        }
    }
    public void readMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return;

        new Thread(() -> {
            List<UUID> ids = new ArrayList<>();
            for (Message m : messages) {
                ids.add(m.getId());
                database.messageDao().updateMessage(m);
            }

            connectionManager.SendCommand(
                    new WSSCommand("UpdateMessageStatus",
                            new UpdateMessageStatusRequest(ids, MessageStatus.READ))
            );
        }).start();
    }
    public void findMessages(String text) {
        if (text == null || text.trim().isEmpty()) return;

        new Thread(() -> {
            List<Message> messages =
                    database.messageDao().getMessagesByText("%" + text + "%", chatId);

            if (fragment.getActivity() == null || !fragment.isAdded()) return;

            fragment.getActivity().runOnUiThread(() ->
                    fragment.showSearchResults(messages, text)
            );
        }).start();
    }
    public void exportChat() {
        new Thread(() -> {
            try {
                List<Message> messages = database.messageDao().getMessagesByChatId(chatId);
                if (messages == null || messages.isEmpty()) return;

                String chatName = "";
                try {
                    com.example.aichat.model.entities.Chat chat =
                            database.chatDao().getChatById(chatId);
                    if (chat != null) chatName = chat.getName();
                } catch (Exception ignored) {}

                if (fragment.getActivity() == null || !fragment.isAdded()) return;

                ChatPdfExporter exporter =
                        new ChatPdfExporter(fragment.getActivity(), currentUserId);

                exporter.exportChat(messages, chatName);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
