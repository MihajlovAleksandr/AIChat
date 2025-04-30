package com.example.aichat.model.notifications;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionUtils {

    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    /**
     * Проверяет и запрашивает разрешение на уведомления (для Android 13+)
     * @param activity Активность, из которой идет запрос
     * @return true - если разрешение уже есть, false - если запрошено
     */
    public static boolean requestNotificationPermission(Activity activity) {
        // Для API < 33 разрешение не требуется
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }

        // Проверяем, есть ли уже разрешение
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        // Если разрешения нет, запрашиваем его
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_PERMISSION_REQUEST_CODE
        );
        return false;
    }
}