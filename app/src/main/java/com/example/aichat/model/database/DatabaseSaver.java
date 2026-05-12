package com.example.aichat.model.database;

import android.content.Context;
import android.util.Log;

import com.example.aichat.dto.request.MessageRequest;
import com.example.aichat.dto.response.AddUserToChatResponse;
import com.example.aichat.dto.response.ChatResponse;
import com.example.aichat.dto.response.ChatUserActionResponse;
import com.example.aichat.dto.response.DeleteChatResponse;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.dto.response.RemoveUserFromChatResponse;
import com.example.aichat.dto.response.SyncResponse;
import com.example.aichat.dto.response.UpdateMessageStatusResponse;

import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.utils.mappers.ChatMapper;
import com.example.aichat.model.utils.mappers.Mapper;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.model.utils.mappers.MessageMapper;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseSaver {

    private static final String TAG = "DatabaseSaver";
    private final AppDatabase appDatabase;
    private final Mapper<MessageRequest, Message, MessageResponse> messageMapper;
    private final MapperResponse<Chat, ChatResponse> chatMapper = new ChatMapper();

    public DatabaseSaver(AppDatabase appDatabase, UUID userId) {
        this.appDatabase = appDatabase;
        this.messageMapper = new MessageMapper(userId);
    }

    // ---------------- PUBLIC SAVE METHODS ----------------

    public void sendMessage(Message message) {
        appDatabase.messageDao().upsertMessage(message);
    }

    public void createChat(Chat chat) {
        appDatabase.chatDao().upsertChat(chat);
    }

    public void updateMessage(Message message) {
        appDatabase.messageDao().updateMessage(message);
    }

    public void deleteMessage(UUID messageId) {
        appDatabase.messageDao().deleteMessages(messageId);
    }

    public void updateChatName(UUID chatId, String name){
        appDatabase.chatDao().updateChatName(chatId, name);
    }

    public void removeChat(UUID chatId) {
        Log.d(TAG, "removeChat called: chatId=" + chatId);
        appDatabase.chatDao().deleteChat(chatId);
    }

    public void endChat(Chat endedChat) {
        Log.d(TAG, "endChat called: chatId=" + endedChat.getId() + ", endTime=" + endedChat.getEndTime());
        if (endedChat == null || endedChat.getId() == null) {
            Log.e(TAG, "endChat: invalid chat");
            return;
        }
        appDatabase.chatDao().endChat(endedChat.getId(), endedChat.getEndTime());
        Log.d(TAG, "endChat: completed");
    }

    public void syncDatabase(SyncResponse sync) {
        // --- CHATS ---
        // --- CHATS ---
        for (ChatResponse cr : sync.chats.newChats) {
            Chat chat = chatMapper.ToModel(cr);

            Chat existing = appDatabase.chatDao().getChatById(chat.getId());
            if (existing != null && existing.getChatTypeHint() != null) {
                chat.setChatTypeHint(existing.getChatTypeHint());
            }

            appDatabase.chatDao().upsertChat(chat);
        }

        for (ChatResponse cr : sync.chats.updatedChats) {
            Chat chat = chatMapper.ToModel(cr);

            Chat existing = appDatabase.chatDao().getChatById(chat.getId());
            if (existing != null && existing.getChatTypeHint() != null) {
                chat.setChatTypeHint(existing.getChatTypeHint());
            }

            appDatabase.chatDao().upsertChat(chat);
        }

        for (ChatResponse cr : sync.chats.updatedChats) {
            Chat chat = chatMapper.ToModel(cr);
            appDatabase.chatDao().upsertChat(chat);
        }

        for (UUID cr : sync.chats.deletedChats) {
            appDatabase.chatDao().deleteChat(cr);
        }

        // --- MESSAGES ---
        for (MessageResponse mr : sync.messages.newMessages) {
            Message msg = messageMapper.ToModel(mr);
            appDatabase.messageDao().upsertMessage(msg);
        }

        for (MessageResponse mr : sync.messages.updatedMessages) {
            Message msg = messageMapper.ToModel(mr);
            appDatabase.messageDao().upsertMessage(msg);
        }

        for (UUID mr : sync.messages.deletedMessages) {
            appDatabase.messageDao().deleteMessages(mr);
        }

        SearchingHandler handler = ChatStatusSingleton.getInstance().getHandler();
        handler.setChatSearching(sync.matchmaking.chatMatchmaking.isSearching);
        handler.setGroupSearchModel(
                new GroupSearchModel(
                        sync.matchmaking.groupMatchmaking.isSearching,
                        sync.matchmaking.groupMatchmaking.chatId));

        Log.d("DB", "SyncDB applied successfully");
    }

    public void saveMessageFromResponse(MessageResponse response) {
        Message newMessage = messageMapper.ToModel(response);

        Message existing = appDatabase.messageDao().getMessageById(response.id);

        if (existing != null) {
            newMessage.setFileTypes(existing.getFileTypes());
            newMessage.setFileMimeTypes(existing.getFileMimeTypes());
        }

        appDatabase.messageDao().upsertMessage(newMessage);
    }

    public void saveChatFromResponse(ChatResponse chatResponse) {
        Chat createdChat = chatMapper.ToModel(chatResponse);

        Chat existing = appDatabase.chatDao().getChatById(createdChat.getId());
        if (existing != null && existing.getChatTypeHint() != null) {
            createdChat.setChatTypeHint(existing.getChatTypeHint());
        }

        createChat(createdChat);
    }

    public void saveChat(Chat chat) {
        if (chat == null) return;
        appDatabase.chatDao().upsertPreservingType(chat);
    }

    public void endChatFromResponse(ChatResponse chatResponse) {
        Chat endedChat = chatMapper.ToModel(chatResponse);
        endChat(endedChat);
    }

    public void deleteChatFromResponse(DeleteChatResponse deleteChatResponse) {
        Log.d(TAG, "deleteChatFromResponse: chatId=" + deleteChatResponse.chatId);
        removeChat(deleteChatResponse.chatId);
    }

    public void updateMessageStatus(UpdateMessageStatusResponse update) {
        // ✅ Добавить защиту от null
        if (update == null || update.messageIds == null || update.userId == null) {
            Log.e(TAG, "updateMessageStatus: invalid parameters");
            return;
        }

        for (UUID messageId : update.messageIds) {
            if (messageId == null) continue;

            appDatabase.messageDao().updateMessageStatusForUser(
                    messageId,
                    update.userId,
                    update.status  // может быть null, но это обработает DAO
            );
        }
    }

    public void addUserToChat(AddUserToChatResponse add) {
        Log.d(TAG, "addUserToChat: userId=" + add.userId + ", chatId=" + add.chatId);
        appDatabase.chatDao().addUserToChat(add.userId, add.chatId);
    }

    public void removeUserFromChat(RemoveUserFromChatResponse remove) {
        Log.d(TAG, "removeUserFromChat (RemoveUserFromChatResponse): userId=" + remove.userId + ", chatId=" + remove.chatId);
        appDatabase.chatDao().removeUserFromChat(remove.userId, remove.chatId);
    }

    public void removeUserFromChat(ChatUserActionResponse response) {
        Log.d(TAG, "removeUserFromChat (ChatUserActionResponse): userId=" + response.userId + ", chatId=" + response.chatId);
        appDatabase.chatDao().removeUserFromChat(response.userId, response.chatId);
    }

    public void logout(Context context) {
        appDatabase.chatDao().clearTable();
        appDatabase.messageDao().clearTable();
        Log.d(TAG, "Logout complete. Database cleared.");
    }
}