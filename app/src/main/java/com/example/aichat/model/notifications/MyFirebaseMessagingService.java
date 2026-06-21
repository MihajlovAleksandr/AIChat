package com.example.aichat.model.notifications;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.aichat.dto.request.UpdateNotificationTokenRequest;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    public static final String ACTION_CHAT_DATA_CHANGED = "com.example.aichat.ACTION_CHAT_DATA_CHANGED";
    public static final String EXTRA_CHAT_ID = "chatId";
    public static final String EXTRA_BODY = "body";
    public static final String PREFS_CHAT_PUSH = "chat_push_updates";
    public static final String PREF_PENDING_CHAT_IDS = "pending_chat_ids";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage == null || remoteMessage.getData() == null || remoteMessage.getData().isEmpty()) return;

        Log.d(TAG, "Message data: " + remoteMessage.getData());

        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");
        String chatIdStr = remoteMessage.getData().get("chatId");
        String isBodyPrompt = remoteMessage.getData().get("isBodyPrompt");

        if (isChatDataChangedPush(body, chatIdStr)) {
            savePendingChatDataChanged(chatIdStr);
            sendLocalChatDataChangedBroadcast(chatIdStr, body);
        }

        try {
            UUID chatId = chatIdStr != null ? UUID.fromString(chatIdStr) : null;
            boolean isBodyPromptBool = isBodyPrompt != null && Boolean.parseBoolean(isBodyPrompt);

            if (isBodyPromptBool && body != null) {
                try {
                    int resId = getResources().getIdentifier(body, "string", getPackageName());
                    if (resId != 0) body = getString(resId);
                } catch (Exception e) {
                    Log.e(TAG, "Error getting string resource", e);
                }
            }

            if (title != null && body != null) {
                NotificationHelper notificationHelper = NotificationSingleton.getInstance().getNotificationHelper();

                if (notificationHelper == null) {
                    notificationHelper = new NotificationHelper();
                    NotificationSingleton.getInstance().setNotificationHelper(notificationHelper);
                    Log.d(TAG, "Create new NotificationHelper");
                }

                notificationHelper.sendNotification(this, title, body, chatId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Invalid chatId format", e);
        }
    }

    private boolean isChatDataChangedPush(String body, String chatIdStr) {
        if (body == null || chatIdStr == null) return false;
        return "add_user".equals(body) || "new_chat".equals(body);
    }

    private void sendLocalChatDataChangedBroadcast(String chatIdStr, String body) {
        try {
            Intent intent = new Intent(ACTION_CHAT_DATA_CHANGED);
            intent.setPackage(getPackageName());
            intent.putExtra(EXTRA_CHAT_ID, chatIdStr);
            intent.putExtra(EXTRA_BODY, body);
            sendBroadcast(intent);
            Log.d(TAG, "Local chat data changed broadcast sent. chatId=" + chatIdStr + ", body=" + body);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send local chat data changed broadcast", e);
        }
    }

    private void savePendingChatDataChanged(String chatIdStr) {
        if (chatIdStr == null || chatIdStr.trim().isEmpty()) return;

        SharedPreferences preferences = getSharedPreferences(PREFS_CHAT_PUSH, Context.MODE_PRIVATE);
        Set<String> current = preferences.getStringSet(PREF_PENDING_CHAT_IDS, new HashSet<>());
        Set<String> updated = new HashSet<>();

        if (current != null) updated.addAll(current);
        updated.add(chatIdStr);

        preferences.edit().putStringSet(PREF_PENDING_CHAT_IDS, updated).apply();
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        NotificationTokenManager.onNewToken(this, token);
    }

    public static void sendRegistrationTokenToServer(String token) {
        if (token == null) return;

        ConnectionDispatcher dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();

        if (dispatcher == null) {
            Log.w(TAG, "ConnectionDispatcher is null — delaying FCM token send");
            return;
        }

        dispatcher.sendHttpRequestAsync("/api/session/token", HttpClient.HTTPMethod.PUT, new UpdateNotificationTokenRequest(token), false)
                .thenAccept(cmd -> {
                    if (!cmd.isSuccess()) Log.e(TAG, "notificationToken error/ Error code: " + cmd.getCode());
                });
    }
}
