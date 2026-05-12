package com.example.aichat.model.connection;

import android.app.Activity;
import android.content.Intent;

import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.notifications.NotificationTokenManager;
import com.example.aichat.model.utils.FileDownloadProgressManager;
import com.example.aichat.model.utils.FileManagerHolder;
import com.example.aichat.view.LoginActivity;

public class LogoutHelper {

    public static void logout(Activity activity) {
        SecurePreferencesManager.removeAuthToken(activity);
        SecurePreferencesManager.removeUserId(activity);
        NotificationTokenManager.saveLastSentToken(activity, null);

        DatabaseManager.getDatabase().chatDao().clearTable();
        DatabaseManager.getDatabase().messageDao().clearTable();
        DatabaseManager.getDatabase().pendingCommandDao().clearTable();
        DatabaseManager.getDatabase().fileDao().clearAll();

        try {
            FileManagerHolder.get(
                    activity.getApplicationContext(),
                    new FileDownloadProgressManager()
            ).cleanupTempFiles();
        } catch (Exception ignored) {}

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        activity.startActivity(intent);
        activity.finish();
    }
}