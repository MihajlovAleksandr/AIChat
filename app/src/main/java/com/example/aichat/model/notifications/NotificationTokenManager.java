package com.example.aichat.model.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.example.aichat.dto.request.UpdateNotificationTokenRequest;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;

public class NotificationTokenManager {

    private static final String TAG = "NotificationTokenMgr";
    private static final String PREFS_NAME = "notification_meta";
    private static final String LAST_SENT_TOKEN_KEY = "last_sent_token";

    private static String pendingToken = null;

    public static void onNewToken(Context context, String token) {
        if (token == null || token.isEmpty()) return;

        Log.d(TAG, "New FCM token received");

        SecurePreferencesManager.saveNotificationToken(context, token);
        pendingToken = token;

        trySend(context);
    }

    public static void onConnected(Context context) {
        Log.d(TAG, "Connection established, trying to sync FCM token");
        trySend(context);
    }

    private static void trySend(Context context) {
        String token = resolveToken(context);
        if (token == null || token.isEmpty()) return;

        String lastSentToken = getLastSentToken(context);

        if (token.equals(lastSentToken)) {
            Log.d(TAG, "Token already synced with server");
            pendingToken = null;
            return;
        }

        ConnectionDispatcher dispatcher;
        try {
            dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
        } catch (Exception e) {
            Log.w(TAG, "Dispatcher not ready");
            return;
        }

        if (dispatcher == null) {
            Log.w(TAG, "Dispatcher is null");
            return;
        }

        dispatcher.sendHttpRequestAsync(
                "/api/session/token",
                HttpClient.HTTPMethod.PUT,
                new UpdateNotificationTokenRequest(token),
                false
        ).thenAccept(cmd -> {
            if (cmd != null && cmd.isSuccess()) {
                saveLastSentToken(context, token);
                pendingToken = null;
            } else {
                Log.e(TAG, "Failed to send FCM token, will retry later");

                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> trySend(context),
                        5000
                );
            }
        });
    }

    private static String resolveToken(Context context) {
        if (pendingToken != null && !pendingToken.isEmpty()) {
            return pendingToken;
        }
        return SecurePreferencesManager.getNotificationToken(context);
    }

    public static void saveLastSentToken(Context context, @Nullable String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(LAST_SENT_TOKEN_KEY, token).apply();
    }

    private static String getLastSentToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LAST_SENT_TOKEN_KEY, null);
    }
}