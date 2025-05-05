package com.example.aichat.model.notifications;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class NotificationSettingsManager {
    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_ENABLED = "notifications_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    private static final String KEY_BACKGROUND_NOTIFICATIONS_ENABLED = "background_notifications_enabled";
    private static final String KEY_IN_APP_NOTIFICATIONS_ENABLED = "in_app_notifications_enabled";
    private static final String KEY_BACKGROUND_WORK_ENABLED = "background_work_enabled";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    public static boolean canSendNotifications(Context context) {
        return areNotificationsEnabled(context) && hasNotificationPermission(context);
    }

    public static void requestNotificationPermissionIfNeeded(Activity activity) {
        if (!hasNotificationPermission(activity) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_REQUEST_CODE);
        }
    }

    public static boolean isBackgroundWorkAllowed(Context context) {
        return isBackgroundUsageAllowed(context);
    }

    public static void setBackgroundWorkAllowed(Context context, boolean allowed) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_BACKGROUND_WORK_ENABLED, allowed)
                .apply();
    }

    public static boolean areBackgroundNotificationsEnabled(Context context) {
        return isBackgroundUsageAllowed(context) &&
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getBoolean(KEY_BACKGROUND_NOTIFICATIONS_ENABLED, true);
    }

    public static void setBackgroundNotificationsEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_BACKGROUND_NOTIFICATIONS_ENABLED, enabled)
                .apply();
    }

    public static boolean areInAppNotificationsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IN_APP_NOTIFICATIONS_ENABLED, true);
    }

    public static void setInAppNotificationsEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IN_APP_NOTIFICATIONS_ENABLED, enabled)
                .apply();
    }

    public static boolean areNotificationsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLED, true);
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply();
    }

    public static boolean isVibrationEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_VIBRATION_ENABLED, true);
    }

    public static void setVibrationEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_VIBRATION_ENABLED, enabled)
                .apply();
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void handlePermissionResult(Context context, int requestCode,
                                              int[] grantResults, NotificationCallback callback) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED;
            callback.onPermissionResult(granted);
        }
    }
    public static boolean isBackgroundUsageAllowed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_BACKGROUND_WORK_ENABLED, true)) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null && activityManager.isBackgroundRestricted()) {
                return false;
            }
        }
        return true;
    }

    public interface NotificationCallback {
        void onPermissionResult(boolean granted);
    }
}