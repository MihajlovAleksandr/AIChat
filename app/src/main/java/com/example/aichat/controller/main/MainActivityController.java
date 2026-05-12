package com.example.aichat.controller.main;

import android.app.Activity;
import android.util.Log;

import com.example.aichat.controller.main.chat.actions.SendMessageController;
import com.example.aichat.dto.response.AddUserToChatResponse;
import com.example.aichat.dto.response.ChatEndedResponse;
import com.example.aichat.dto.response.ChatNameUpdatedResponse;
import com.example.aichat.dto.response.ChatResponse;
import com.example.aichat.dto.response.ChatSearchingStatusResponse;
import com.example.aichat.dto.response.ChatUserActionResponse;
import com.example.aichat.dto.response.DeleteChatResponse;
import com.example.aichat.dto.response.GroupSearchingStatusResponse;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.dto.response.RemoveUserFromChatResponse;
import com.example.aichat.dto.response.SyncResponse;
import com.example.aichat.dto.response.UpdateMessageStatusResponse;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.EventHandler;
import com.example.aichat.model.connection.LogoutHelper;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.SignalRCommand;
import com.example.aichat.model.connection.UploadProgress;
import com.example.aichat.model.connection.files.UploadProgressListener;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseEventHandler;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.database.DatabaseSaver;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.exceptions.UnauthorizedException;
import com.example.aichat.model.utils.mappers.ChatMapper;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.view.main.MainActivityAdapter;
import com.example.aichat.view.main.chatlist.ChatsListFragment;

import java.util.UUID;

public class MainActivityController {

    private static final String TAG = "MainActivityController";

    private UUID currentChatId;
    private final ConnectionDispatcher connectionDispatcher;
    private final AppDatabase appDatabase;
    private final MainActivityAdapter mainActivityAdapter;
    private Activity activity;
    private boolean isLogout = false;

    private final MapperResponse<Chat, ChatResponse> chatMapper = new ChatMapper();
    private DatabaseEventHandler databaseEventHandler;
    private ChatsListFragment cachedChatsListFragment;

    // Для логирования количества вызовов
    private int messageSentCallCount = 0;
    private int messageStatusUpdatedCallCount = 0;

    public MainActivityController(
            Activity activity,
            MainActivityAdapter mainActivityAdapter,
            UUID userId
    ) {
        Log.d("Loading", "MainActivityController");
        connectionDispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
        this.mainActivityAdapter = mainActivityAdapter;
        this.appDatabase = DatabaseManager.getDatabase();
        this.activity = activity;

        AppDatabase db = DatabaseManager.getDatabase();
        DatabaseSaver saver = new DatabaseSaver(db, userId);
        databaseEventHandler = new DatabaseEventHandler(saver, userId);
    }

    public void logout(Activity activity) {
        if (!isLogout) {
            isLogout = true;
            LogoutHelper.logout(activity);
        }
    }

    /**
     * Установить ссылку на ChatsListFragment для UI обновлений
     * И передать её в DatabaseEventHandler
     */
    public void setChatsListFragment(ChatsListFragment fragment) {
        this.cachedChatsListFragment = fragment;
        if (databaseEventHandler != null) {
            databaseEventHandler.setChatsListFragment(fragment);
            Log.d(TAG, "ChatsListFragment set to DatabaseEventHandler");
        }
    }

    public ChatsListFragment getChatsListFragment() {
        return cachedChatsListFragment;
    }

    public void connect(){
        Log.e(TAG, "🔌 connect() called");
        connectionDispatcher.connect().exceptionally((throwable) -> {
            Log.e(TAG, "❌ Connection failed: " + throwable.getMessage());
            if (throwable instanceof UnauthorizedException) {
                logout(activity);
            }
            return null;
        }).thenRun(() -> {
            Log.e(TAG, "✅ Connection successful, registering handlers...");
            // ✅ РЕГИСТРИРУЕМ ВСЕ ОБРАБОТЧИКИ В ОДНОМ МЕСТЕ
            registerAllHandlers();

            SendMessageController controller = new SendMessageController(
                    activity,
                    connectionDispatcher,
                    new UploadProgressListener() {
                        @Override
                        public void onProgress(UploadProgress progress) {
                            Log.d("File loading Progress",
                                    progress.getFileId() + ": " + progress.getPercent() + "%");
                        }
                    }
            );
            Log.e(TAG, "✅ SendMessageController created");
        });
    }

    /**
     * ✅ ЕДИНАЯ РЕГИСТРАЦИЯ ВСЕХ ОБРАБОТЧИКОВ
     */
    private void registerAllHandlers() {
        Log.e(TAG, "========================================");
        Log.e(TAG, "Registering ALL DatabaseEventHandlers");
        Log.e(TAG, "Dispatcher hashCode: " + connectionDispatcher.hashCode());
        Log.e(TAG, "========================================");

        // 1. Основные события чатов
        connectionDispatcher.addEventListener("ChatCreated", ChatResponse.class, databaseEventHandler.onChatCreated());
        Log.d(TAG, "✓ ChatCreated registered");

        connectionDispatcher.addEventListener("ChatEnded", ChatEndedResponse.class, databaseEventHandler.onChatEnded());
        Log.d(TAG, "✓ ChatEnded registered");

        connectionDispatcher.addEventListener("DeleteChat", DeleteChatResponse.class, databaseEventHandler.onDeleteChat());
        Log.d(TAG, "✓ DeleteChat registered");

        connectionDispatcher.addEventListener("ChatUserRemoved", ChatUserActionResponse.class, databaseEventHandler.onChatUserRemoved());
        Log.d(TAG, "✓ ChatUserRemoved registered");

        // 2. Сообщения и статусы - ИСПРАВЛЕНЫ ИМЕНА СОБЫТИЙ
        // Сервер отправляет "MessageSent" (из nameof(MessageSent))
        connectionDispatcher.addEventListener("MessageSent", MessageResponse.class, new EventHandler<MessageResponse>() {
            @Override
            public void handle(SignalRCommand<MessageResponse> command) {
                messageSentCallCount++;
                MessageResponse response = command.getPayload();

                Log.e(TAG, "╔════════════════════════════════════════════════╗");
                Log.e(TAG, "║ 🔥🔥🔥 MessageSent EVENT RECEIVED! 🔥🔥🔥");
                Log.e(TAG, "╠════════════════════════════════════════════════╣");
                Log.e(TAG, "║ Call count: " + messageSentCallCount);
                Log.e(TAG, "║ Response chatId: " + (response != null ? response.chatId : "NULL"));
                Log.e(TAG, "║ Response userId: " + (response != null ? response.userId : "NULL"));
                Log.e(TAG, "║ Response text: " + (response != null ? response.text : "NULL"));
                Log.e(TAG, "║ Current chatId: " + currentChatId);
                Log.e(TAG, "║ Thread: " + Thread.currentThread().getName());
                Log.e(TAG, "╚════════════════════════════════════════════════╝");

                if (response == null) {
                    Log.e(TAG, "❌ Response is NULL!");
                    return;
                }

                // Вызываем DatabaseEventHandler
                Log.d(TAG, "📦 Calling databaseEventHandler.onSendMessage()");
                databaseEventHandler.onSendMessage().handle(command);
                Log.d(TAG, "✅ databaseEventHandler.onSendMessage() completed");
            }
        });

        // Сервер отправляет "MessageStatusUpdated" (из nameof(MessageStatusUpdated))
        connectionDispatcher.addEventListener("MessageStatusUpdated", UpdateMessageStatusResponse.class, new EventHandler<UpdateMessageStatusResponse>() {
            @Override
            public void handle(SignalRCommand<UpdateMessageStatusResponse> command) {
                messageStatusUpdatedCallCount++;
                UpdateMessageStatusResponse response = command.getPayload();

                Log.e(TAG, "╔════════════════════════════════════════════════╗");
                Log.e(TAG, "║ 🔄 MessageStatusUpdated EVENT RECEIVED! 🔄");
                Log.e(TAG, "╠════════════════════════════════════════════════╣");
                Log.e(TAG, "║ Call count: " + messageStatusUpdatedCallCount);
                Log.e(TAG, "║ Response chatId: " + (response != null ? response.chatId : "NULL"));
                Log.e(TAG, "║ Status: " + (response != null ? response.status : "NULL"));
                Log.e(TAG, "║ Message ids count: " + (response != null && response.messageIds != null ? response.messageIds.size() : 0));
                Log.e(TAG, "║ Current chatId: " + currentChatId);
                Log.e(TAG, "╚════════════════════════════════════════════════╝");

                if (response == null) {
                    Log.e(TAG, "❌ Response is NULL!");
                    return;
                }

                Log.d(TAG, "📦 Calling databaseEventHandler.onUpdateMessageStatus()");
                databaseEventHandler.onUpdateMessageStatus().handle(command);
                Log.d(TAG, "✅ databaseEventHandler.onUpdateMessageStatus() completed");
            }
        });

        // 3. Участники чатов
        connectionDispatcher.addEventListener("AddUserToChat", AddUserToChatResponse.class, databaseEventHandler.onAddUserToChat());
        connectionDispatcher.addEventListener("RemoveUserFromChat", RemoveUserFromChatResponse.class, databaseEventHandler.onRemoveUserFromChat());
        Log.d(TAG, "✓ User events registered");

        // 4. Обновление имени чата
        connectionDispatcher.addEventListener("ChatNameUpdated", ChatNameUpdatedResponse.class, databaseEventHandler.onChatNameUpdated());
        Log.d(TAG, "✓ ChatNameUpdated registered");

        // 5. Синхронизация
        connectionDispatcher.addEventListener("SyncDB", SyncResponse.class, new EventHandler<SyncResponse>() {
            @Override
            public void handle(SignalRCommand<SyncResponse> command) {
                Log.d(TAG, "🔄 SyncDB event received");
                databaseEventHandler.onSyncDB().handle(command);
            }
        });
        Log.d(TAG, "✓ SyncDB registered");

        // 6. Статусы поиска
        connectionDispatcher.addEventListener("ChatSearchingStatusUpdated", ChatSearchingStatusResponse.class, databaseEventHandler.onChatSearchingStatusUpdated());
        connectionDispatcher.addEventListener("GroupSearchingStatusUpdated", GroupSearchingStatusResponse.class, databaseEventHandler.onGroupSearchingStatusUpdated());
        Log.d(TAG, "✓ Search status events registered");

        // 7. Обработчик Logout
        connectionDispatcher.addEventListener("Logout", new EventHandler<Void>() {
            @Override
            public void handle(SignalRCommand<Void> command) {
                Log.e(TAG, "🚪 Logout event received");
                logout(activity);
            }
        });

        Log.e(TAG, "✅ All DatabaseEventHandlers registered successfully");
        Log.e(TAG, "========================================");
    }

    public UUID getCurrentChatId() {
        return currentChatId;
    }

    public void setCurrentChatId(UUID chatId) {
        Log.d(TAG, "setCurrentChatId: " + chatId + " (was: " + currentChatId + ")");
        this.currentChatId = chatId;
    }

    public boolean destroy() {
        return isLogout;
    }
}