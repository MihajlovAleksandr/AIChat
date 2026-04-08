package com.example.aichat.model.database;

import android.content.Context;
import android.util.Log;

import com.example.aichat.dto.request.MessageRequest;
import com.example.aichat.dto.response.AddUserToChatResponse;
import com.example.aichat.dto.response.ChatResponse;
import com.example.aichat.dto.response.DeleteChatResponse;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.dto.response.RemoveUserFromChatResponse;
import com.example.aichat.dto.response.SyncDBResponse;
import com.example.aichat.dto.response.UpdateMessageStatusResponse;

import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.model.utils.mappers.ChatMapper;
import com.example.aichat.model.utils.mappers.Mapper;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.model.utils.mappers.MessageMapper;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseSaver {

    private final AppDatabase appDatabase;
    private final Mapper<MessageRequest, Message, MessageResponse> messageMapper;
    private final MapperResponse<Chat, ChatResponse> chatMapper = new ChatMapper();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DatabaseSaver(AppDatabase appDatabase, UUID userId){
        this.appDatabase = appDatabase;
        this.messageMapper = new MessageMapper(userId);
    }

    // ---------------- INTERNAL SAVE METHODS ----------------

    private void sendMessage(Message message){
        appDatabase.messageDao().upsertMessage(message);
        Log.d("DB", "Message saved: " + message.getId());
    }

    private void createChat(Chat chat){
        appDatabase.chatDao().upsertChat(chat);
    }

    private void removeChat(UUID chatId){
        appDatabase.chatDao().deleteChat(chatId);
    }

    private void endChat(Chat endedChat){
        appDatabase.chatDao().endChat(endedChat.getId(), endedChat.getEndTime());
    }

    // ---------------- PUBLIC ENTRY POINT ----------------

    public void commandGot(WSSCommand command, Context context){
        executor.execute(() -> handleCommandSafe(command, context));
    }

    // ---------------- SAFE WRAPPER ----------------

    private void handleCommandSafe(WSSCommand command, Context context) {
        try {
            handleCommand(command, context);
        } catch (Exception e) {
            Log.e("DB", "Error handling command: " + command.getOperation(), e);
        }
    }

    // ---------------- COMMAND HANDLER ----------------

    private void handleCommand(WSSCommand command, Context context) {

        switch (command.getOperation()) {

            case "SyncDB": {
                SyncDBResponse sync = command.getData(SyncDBResponse.class);

                // --- CHATS ---
                for (ChatResponse cr : sync.newChats) {
                    Chat chat = chatMapper.ToModel(cr);
                    appDatabase.chatDao().upsertChat(chat);
                }

                for (ChatResponse cr : sync.oldChats) {
                    Chat chat = chatMapper.ToModel(cr);
                    appDatabase.chatDao().upsertChat(chat);
                }

                for (ChatResponse cr : sync.deletedChats) {
                    appDatabase.chatDao().deleteChat(cr.id);
                }

                // --- MESSAGES ---
                for (MessageResponse mr : sync.newMessages) {
                    Message msg = messageMapper.ToModel(mr);
                    appDatabase.messageDao().upsertMessage(msg);
                }

                for (MessageResponse mr : sync.oldMessages) {
                    Message msg = messageMapper.ToModel(mr);
                    appDatabase.messageDao().upsertMessage(msg);
                }

                for (MessageResponse mr : sync.deletedMessages) {
                    appDatabase.messageDao().deleteMessages(mr.id);
                }

                Log.d("DB", "SyncDB applied successfully");
                break;
            }

            case "SendMessage": {
                Message message = messageMapper.ToModel(command.getData(MessageResponse.class));
                sendMessage(message);
                break;
            }

            case "AddChat":
            case "CreateChat": {
                Chat createdChat = chatMapper.ToModel(command.getData(ChatResponse.class));
                createChat(createdChat);
                break;
            }

            case "EndChat": {
                Chat endedChat = chatMapper.ToModel(command.getData(ChatResponse.class));
                endChat(endedChat);
                break;
            }

            case "DeleteChat": {
                DeleteChatResponse deleteChatResponse = command.getData(DeleteChatResponse.class);
                removeChat(deleteChatResponse.chatId);
                break;
            }

            case "UpdateMessageStatus": {
                UpdateMessageStatusResponse update = command.getData(UpdateMessageStatusResponse.class);
                for (UUID messageId : update.messageIds) {
                    appDatabase.messageDao().updateMessageStatusForUser(
                            messageId,
                            update.userId,
                            update.status
                    );
                }
                break;
            }

            case "AddUserToChat": {
                AddUserToChatResponse add = command.getData(AddUserToChatResponse.class);
                appDatabase.chatDao().addUserToChat(add.userId, add.chatId);
                break;
            }

            case "RemoveUserFromChat": {
                RemoveUserFromChatResponse remove = command.getData(RemoveUserFromChatResponse.class);
                appDatabase.chatDao().removeUserFromChat(remove.userId, remove.chatId);
                break;
            }

            case "Logout": {
                handleLogout(context);
                break;
            }

            default:
                // ❗ Просто игнорируем команды, не относящиеся к БД
                return;
        }
    }

    // ---------------- LOGOUT HANDLING ----------------

    private void handleLogout(Context context) {

        // ❗ НЕ трогаем pendingCommandDao — это делает ConnectionSingleton.reset()

        // ❗ НЕ трогаем executor — он должен жить весь жизненный цикл DatabaseSaver

        // ❗ НЕ трогаем токен — это делает ConnectionManager + SecurePreferencesManager

        appDatabase.chatDao().clearTable();
        appDatabase.messageDao().clearTable();

        Log.d("DB", "Logout complete. Database cleared.");
    }
}
