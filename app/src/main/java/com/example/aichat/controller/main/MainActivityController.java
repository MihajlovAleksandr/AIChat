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
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.EventHandler;
import com.example.aichat.model.connection.files.UploadProgressListener;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.LogoutHelper;
import com.example.aichat.model.connection.SignalRCommand;
import com.example.aichat.model.connection.files.UploadProgress;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.ChatStatusSingleton;
import com.example.aichat.model.database.DatabaseEventHandler;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.database.DatabaseSaver;
import com.example.aichat.model.database.GroupSearchModel;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.exceptions.UnauthorizedException;
import com.example.aichat.model.utils.mappers.ChatMapper;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.view.main.chatlist.ChatsListFragment;
import com.example.aichat.view.main.MainActivityAdapter;
import java.util.UUID;

@androidx.media3.common.util.UnstableApi
public class MainActivityController {

    private static final String TAG = "MainActivityController";

    private UUID currentChatId;
    private final ConnectionDispatcher connectionDispatcher;
    private final AppDatabase appDatabase;
    private final DatabaseSaver saver;
    private final MainActivityAdapter mainActivityAdapter;

    private Activity activity;
    private boolean isLogout = false;
    private boolean handlersRegistered = false;

    private final MapperResponse<Chat, ChatResponse> chatMapper = new ChatMapper();
    private DatabaseEventHandler databaseEventHandler;
    private ChatsListFragment cachedChatsListFragment;

    private int messageSentCallCount = 0;
    private int messageStatusUpdatedCallCount = 0;

    public MainActivityController(Activity activity, MainActivityAdapter mainActivityAdapter, UUID userId) {
        Log.d("Loading", "MainActivityController");

        this.connectionDispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
        this.appDatabase = DatabaseManager.getDatabase();
        this.activity = activity;
        this.mainActivityAdapter = mainActivityAdapter;
        this.saver = new DatabaseSaver(appDatabase, userId);
        this.databaseEventHandler = new DatabaseEventHandler(saver, userId);
    }

    public void logout(Activity activity) {
        if (!isLogout) {
            isLogout = true;
            LogoutHelper.logout(activity);
        }
    }

    public void setChatsListFragment(ChatsListFragment fragment) {
        this.cachedChatsListFragment = fragment;

        if (databaseEventHandler != null) {
            databaseEventHandler.setChatsListFragment(fragment);
            Log.d(TAG, "ChatsListFragment set to DatabaseEventHandler");
        }
    }

    public ChatsListFragment getChatsListFragment() {
        return getCurrentChatsListFragment();
    }

    public void connect() {
        Log.e(TAG, "connect() called");

        connectionDispatcher.connect()
                .exceptionally(throwable -> {
                    Log.e(TAG, "Connection failed: " + throwable.getMessage());

                    if (throwable instanceof UnauthorizedException) logout(activity);

                    return null;
                })
                .thenRun(() -> {
                    Log.e(TAG, "Connection successful, registering handlers...");
                    registerAllHandlers();

                    new SendMessageController(
                            activity,
                            connectionDispatcher,
                            new UploadProgressListener() {
                                @Override
                                public void onProgress(UploadProgress progress) {
                                    Log.d("File loading Progress", progress.getFileId() + ": " + progress.getPercent() + "%");
                                }
                            }
                    );

                    Log.e(TAG, "SendMessageController created");
                });
    }

    private void registerAllHandlers() {
        if (handlersRegistered) {
            Log.d(TAG, "Handlers already registered, skip");
            return;
        }

        handlersRegistered = true;

        Log.e(TAG, "========================================");
        Log.e(TAG, "Registering ALL DatabaseEventHandlers");
        Log.e(TAG, "Dispatcher hashCode: " + connectionDispatcher.hashCode());
        Log.e(TAG, "========================================");

        connectionDispatcher.addEventListener("ChatCreated", ChatResponse.class, command -> {
            databaseEventHandler.onChatCreated().handle(command);

            ChatResponse response = command.getPayload();

            if (response != null) {
                stopGroupSearchBecauseMatchFound();
                safelyReloadChatsList();
            }
        });

        Log.d(TAG, "ChatCreated registered");

        connectionDispatcher.addEventListener("ChatEnded", ChatEndedResponse.class, databaseEventHandler.onChatEnded());
        Log.d(TAG, "ChatEnded registered");

        connectionDispatcher.addEventListener("DeleteChat", DeleteChatResponse.class, databaseEventHandler.onDeleteChat());
        Log.d(TAG, "DeleteChat registered");

        connectionDispatcher.addEventListener("ChatUserRemoved", ChatUserActionResponse.class, databaseEventHandler.onChatUserRemoved());
        Log.d(TAG, "ChatUserRemoved registered");

        connectionDispatcher.addEventListener("MessageSent", MessageResponse.class, new EventHandler<MessageResponse>() {
            @Override
            public void handle(SignalRCommand<MessageResponse> command) {
                messageSentCallCount++;

                MessageResponse response = command.getPayload();

                Log.e(TAG, "MessageSent EVENT RECEIVED. Count=" + messageSentCallCount + ", chatId=" + (response != null ? response.chatId : "NULL"));

                if (response == null) return;

                databaseEventHandler.onSendMessage().handle(command);
            }
        });

        connectionDispatcher.addEventListener("MessageStatusUpdated", UpdateMessageStatusResponse.class, new EventHandler<UpdateMessageStatusResponse>() {
            @Override
            public void handle(SignalRCommand<UpdateMessageStatusResponse> command) {
                messageStatusUpdatedCallCount++;

                UpdateMessageStatusResponse response = command.getPayload();

                Log.e(TAG, "MessageStatusUpdated EVENT RECEIVED. Count=" + messageStatusUpdatedCallCount + ", chatId=" + (response != null ? response.chatId : "NULL"));

                if (response == null) return;

                databaseEventHandler.onUpdateMessageStatus().handle(command);
            }
        });

        connectionDispatcher.addEventListener("AddUserToChat", AddUserToChatResponse.class, command -> {
            databaseEventHandler.onAddUserToChat().handle(command);

            AddUserToChatResponse response = command.getPayload();

            if (response != null) {
                stopGroupSearchBecauseMatchFound();
                forceLoadChatFromServer(response.chatId);
            }
        });

        connectionDispatcher.addEventListener("RemoveUserFromChat", RemoveUserFromChatResponse.class, databaseEventHandler.onRemoveUserFromChat());
        Log.d(TAG, "User events registered");

        connectionDispatcher.addEventListener("ChatNameUpdated", ChatNameUpdatedResponse.class, databaseEventHandler.onChatNameUpdated());
        Log.d(TAG, "ChatNameUpdated registered");

        connectionDispatcher.addEventListener("SyncDB", SyncResponse.class, new EventHandler<SyncResponse>() {
            @Override
            public void handle(SignalRCommand<SyncResponse> command) {
                Log.d(TAG, "SyncDB event received");
                databaseEventHandler.onSyncDB().handle(command);
            }
        });

        Log.d(TAG, "SyncDB registered");

        connectionDispatcher.addEventListener("ChatSearchingStatusUpdated", ChatSearchingStatusResponse.class, databaseEventHandler.onChatSearchingStatusUpdated());
        connectionDispatcher.addEventListener("GroupSearchingStatusUpdated", GroupSearchingStatusResponse.class, databaseEventHandler.onGroupSearchingStatusUpdated());

        Log.d(TAG, "Search status events registered");

        connectionDispatcher.addEventListener("Logout", new EventHandler<Void>() {
            @Override
            public void handle(SignalRCommand<Void> command) {
                Log.e(TAG, "Logout event received");
                logout(activity);
            }
        });

        Log.e(TAG, "All DatabaseEventHandlers registered successfully");
        Log.e(TAG, "========================================");
    }

    public void handleChatDataPush(UUID chatId, String reason) {
        if (chatId == null) return;

        Log.d(TAG, "handleChatDataPush: chatId=" + chatId + ", reason=" + reason);

        stopGroupSearchBecauseMatchFound();
        forceLoadChatFromServer(chatId);
    }

    private void stopGroupSearchBecauseMatchFound() {
        try {
            GroupSearchModel currentSearch = ChatStatusSingleton.getInstance()
                    .getHandler()
                    .getGroupSearchModel();

            if (currentSearch == null || !currentSearch.getIsSearching()) return;

            ChatStatusSingleton.getInstance()
                    .getHandler()
                    .setGroupSearchModel(new GroupSearchModel(false, null));

            Activity currentActivity = activity;

            if (currentActivity != null) {
                currentActivity.runOnUiThread(() -> {
                    ChatsListFragment fragment = getCurrentChatsListFragment();

                    if (fragment != null) {
                        fragment.setGroupSearchingStatus();
                        fragment.reloadChatsFromDatabaseSafe();
                    }
                });
            }

            Log.d(TAG, "Group search stopped because match was found");

        } catch (Exception e) {
            Log.e(TAG, "Failed to stop group search because match was found", e);
        }
    }

    public void forceLoadChatFromServer(UUID chatId) {
        if (chatId == null || connectionDispatcher == null) return;

        connectionDispatcher.sendHttpRequestAsync("/api/chat/" + chatId, HttpClient.HTTPMethod.GET, null, true)
                .thenAccept(cmd -> {
                    if (cmd == null || !cmd.isSuccess()) {
                        Log.w(TAG, "forceLoadChatFromServer failed. chatId=" + chatId + ", code=" + (cmd != null ? cmd.getCode() : "NULL"));
                        safelyReloadChatsList();
                        return;
                    }

                    try {
                        ChatResponse response = cmd.getData(ChatResponse.class);

                        if (response == null) {
                            Log.w(TAG, "forceLoadChatFromServer: empty ChatResponse");
                            safelyReloadChatsList();
                            return;
                        }

                        saver.saveChatFromResponse(response);

                        Chat chat = chatMapper.ToModel(response);

                        if (chat != null) safelyCreateOrReloadChat(chat);
                        else safelyReloadChatsList();

                    } catch (Exception e) {
                        Log.e(TAG, "forceLoadChatFromServer parse/save error", e);
                        safelyReloadChatsList();
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "forceLoadChatFromServer request error", throwable);
                    safelyReloadChatsList();
                    return null;
                });
    }

    private void safelyCreateOrReloadChat(Chat chat) {
        if (chat == null) {
            safelyReloadChatsList();
            return;
        }

        Activity currentActivity = activity;

        if (currentActivity == null) return;

        currentActivity.runOnUiThread(() -> {
            ChatsListFragment fragment = getCurrentChatsListFragment();

            if (fragment != null) {
                fragment.createChat(chat);
                fragment.reloadChatsFromDatabaseSafe();

                Log.d(TAG, "Chat loaded from server and UI reloaded: " + chat.getId());
            }
        });
    }

    private void safelyReloadChatsList() {
        Activity currentActivity = activity;

        if (currentActivity == null) return;

        currentActivity.runOnUiThread(() -> {
            ChatsListFragment fragment = getCurrentChatsListFragment();

            if (fragment != null) {
                fragment.reloadChatsFromDatabaseSafe();
                Log.d(TAG, "Chats list safely reloaded");
            }
        });
    }

    private ChatsListFragment getCurrentChatsListFragment() {
        if (cachedChatsListFragment != null) return cachedChatsListFragment;

        if (mainActivityAdapter != null) {
            cachedChatsListFragment = mainActivityAdapter.getChatsListFragment();
        }

        return cachedChatsListFragment;
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
