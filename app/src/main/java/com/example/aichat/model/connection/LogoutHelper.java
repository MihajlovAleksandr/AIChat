package com.example.aichat.model.connection;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.notifications.NotificationTokenManager;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.utils.files.FileDownloadProgressManager;
import com.example.aichat.model.utils.files.FileManagerHolder;
import com.example.aichat.view.LoginActivity;

public class LogoutHelper {

    private static final String TAG = "LogoutHelper";

    public static void logout(Activity activity) {
        if (activity == null) return;

        SecurePreferencesManager.removeAuthToken(activity);
        SecurePreferencesManager.removeUserId(activity);
        NotificationTokenManager.saveLastSentToken(activity, null);

        new Thread(() -> {
            try {
                DatabaseManager.getDatabase().chatDao().clearTable();
                DatabaseManager.getDatabase().messageDao().clearTable();
                DatabaseManager.getDatabase().pendingCommandDao().clearTable();
                DatabaseManager.getDatabase().fileDao().clearAll();

                Log.d(TAG, "Local database cleared after logout");
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear local database after logout", e);
            }

            try {
                FileManagerHolder.get(
                        activity.getApplicationContext(),
                        new FileDownloadProgressManager()
                ).cleanupTempFiles();

                Log.d(TAG, "Temporary files cleaned after logout");
            } catch (Exception e) {
                Log.e(TAG, "Failed to cleanup temp files after logout", e);
            }
        }).start();

        activity.runOnUiThread(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;

            Intent intent = new Intent(activity, LoginActivity.class);

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            activity.startActivity(intent);
            activity.finish();
        });
    }
}
