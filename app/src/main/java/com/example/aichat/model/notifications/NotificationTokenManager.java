package com.example.aichat.model.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import com.example.aichat.dto.request.UpdateNotificationTokenRequest;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.SecurePreferencesManager;

public class NotificationTokenManager {

    private static final String TAG = "NotificationTokenMgr";
    private static final String PREFS_NAME = "notification_meta";
    private static final String LAST_SENT_TOKEN_KEY = "last_sent_token";

    private static String pendingToken = null;

    public static void onNewToken(Context context, String token) {
        if (token == null || token.isEmpty()) return;

        Context appContext = context.getApplicationContext();

        Log.d(TAG, "New FCM token received");

        SecurePreferencesManager.saveNotificationToken(appContext, token);
        pendingToken = token;

        trySend(appContext);
    }

    public static void onConnected(Context context) {
        Context appContext = context.getApplicationContext();

        Log.d(TAG, "Connection established, trying to sync FCM token");

        trySend(appContext);
    }

    private static void trySend(Context context) {
        Context appContext = context.getApplicationContext();

        String token = resolveToken(appContext);
        if (token == null || token.isEmpty()) return;

        String lastSentToken = getLastSentToken(appContext);

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
                false,
                false
        ).thenAccept(cmd -> {
            if (cmd != null && cmd.isSuccess()) {
                saveLastSentToken(appContext, token);
                pendingToken = null;
                Log.d(TAG, "FCM token synced successfully");
                return;
            }

            if (cmd != null && cmd.getCode() == 401) {
                Log.e(TAG, "FCM token sync unauthorized. Waiting for valid session token");
                pendingToken = token;
                return;
            }

            Log.e(TAG, "Failed to send FCM token, will retry later");

            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> trySend(appContext),
                    5000
            );
        }).exceptionally(throwable -> {
            Log.e(TAG, "FCM token sync error", throwable);

            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> trySend(appContext),
                    5000
            );

            return null;
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
