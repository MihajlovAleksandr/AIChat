package com.example.aichat.model.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;
import com.example.aichat.R;
import com.example.aichat.view.main.MainActivity;
import java.util.Random;

public class NotificationHelper {
    private static final String CHANNEL_ID = "default_channel";
    private static final String CHANNEL_NAME = "Основные уведомления";
    private static final long[] DEFAULT_VIBRATION_PATTERN = {0, 500, 500, 500};

    public static void vibrate(Context context){
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(DEFAULT_VIBRATION_PATTERN, -1);
        }
    }

    public static void sendNotification(Context context, String title, String message) {
        if (!NotificationSettingsManager.canSendNotifications(context)) {
            return;
        }

        createNotificationChannel(context);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = buildNotification(context, title, message);
        manager.notify(new Random().nextInt(), notification);

        if(NotificationSettingsManager.isVibrationEnabled(context)){
            vibrate(context);
        }
    }

    public static void sendNotification(Context context, com.example.aichat.model.entities.Notification notification){
        sendNotification(context,  notification.getTitle(), notification.getMessage());
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH); // Изменено на IMPORTANCE_HIGH
            channel.enableVibration(true);
            channel.setVibrationPattern(DEFAULT_VIBRATION_PATTERN);

            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private static Notification buildNotification(Context context, String title, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.dot_done)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(DEFAULT_VIBRATION_PATTERN)
                .setFullScreenIntent(pendingIntent, true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }
}