package com.example.aichat.controller.main;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.example.aichat.dto.response.ChatResponse;
import com.example.aichat.dto.response.UpdateChatNameResponse;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.model.utils.mappers.ChatMapper;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.view.LoginActivity;
import com.example.aichat.view.main.MainActivityAdapter;

import java.util.UUID;
public class MainActivityController {

    private UUID currentChatId;
    private final ConnectionManager connectionManager;
    private final AppDatabase appDatabase;
    private final MainActivityAdapter mainActivityAdapter;

    private boolean isLogout = false;

    private final MapperResponse<Chat, ChatResponse> chatMapper = new ChatMapper();

    public final OnConnectionEvents events;

    public MainActivityController(
            Activity activity,
            MainActivityAdapter mainActivityAdapter,
            UUID userId,
            boolean isNewActivity
    ) {
        Log.d("Loading", "MainActivityController");

        this.mainActivityAdapter = mainActivityAdapter;
        this.appDatabase = DatabaseManager.getDatabase();
        this.connectionManager = ConnectionSingleton.getInstance().getConnectionManager();

        events = new OnConnectionEvents() {

            @Override
            public void OnCommandGot(WSSCommand command) {

                switch (command.getOperation()) {

                    case "CreateChat": {
                        ChatResponse response = command.getData(ChatResponse.class);
                        if (response != null) {
                            Chat chat = chatMapper.ToModel(response);
                            activity.runOnUiThread(() ->
                                    mainActivityAdapter.onChatCreated(chat)
                            );
                        }
                        break;
                    }

                    case "UpdateChatName": {
                        UpdateChatNameResponse resp =
                                command.getData(UpdateChatNameResponse.class);

                        if (resp != null) {
                            activity.runOnUiThread(mainActivityAdapter::reloadChats);
                        }
                        break;
                    }

                    case "Logout":
                        logout(activity);
                        break;

                    default:
                        break;
                }
            }

            @Override
            public void OnConnectionFailed() {
                Log.d("MainController", "Connection failed");
            }

            @Override
            public void OnOpen() {
                Log.d("MainController", "WebSocket opened");
            }
        };

        if (isNewActivity) {
            connectionManager.clearConnectionEvents();
            connectionManager.addConnectionEvent(events);
        } else {
            connectionManager.addConnectionEvent(events);
        }
    }
    public void logout(Activity activity) {
        if (!isLogout) {
            isLogout = true;

            connectionManager.clearConnectionEvents();

            try { connectionManager.Close(); } catch (Exception ignored) {}

            SecurePreferencesManager.removeAuthToken(activity);
            SecurePreferencesManager.removeUserId(activity);
            SecurePreferencesManager.saveNotificationToken(activity, null);
            ConnectionSingleton.getInstance().resetFull();
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
        }
    }
    public UUID getCurrentChatId() {
        return currentChatId;
    }

    public void setCurrentChatId(UUID chatId) {
        this.currentChatId = chatId;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public boolean destroy() {
        if (!isLogout) {
            connectionManager.removeConnectionEvent(events);
        }
        return isLogout;
    }
}
