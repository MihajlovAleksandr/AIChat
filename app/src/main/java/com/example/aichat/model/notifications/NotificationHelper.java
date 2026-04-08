package com.example.aichat.model.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import com.example.aichat.R;
import com.example.aichat.view.main.MainActivity;
import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer;

import java.util.Random;
import java.util.UUID;

public class NotificationHelper {
    private static final String CHANNEL_ID = "default_channel";
    private static final String CHANNEL_NAME = "Default notifications";
    private static final long[] DEFAULT_VIBRATION_PATTERN = {0, 500, 500, 500};
    private UUID currentChatId;

    public NotificationHelper(){
        currentChatId = null;
    }

    public void vibrate(Context context) {
        if (!NotificationSettingsManager.isVibrationEnabled(context)) {
            return;
        }
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(DEFAULT_VIBRATION_PATTERN, -1);
        }
    }

    public void sendNotification(Context context, String title, String message, UUID chatId) {
        if (!NotificationSettingsManager.canSendNotifications(context)) {
            return;
        }
        if(chatId.equals(currentChatId)) return;
        createNotificationChannel(context);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = buildNotification(context, title, message, chatId);
        manager.notify(new Random().nextInt(), notification);

        if(NotificationSettingsManager.isVibrationEnabled(context)){
            vibrate(context);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);

            if (NotificationSettingsManager.isVibrationEnabled(context)) {
                channel.enableVibration(true);
                channel.setVibrationPattern(DEFAULT_VIBRATION_PATTERN);
            } else {
                channel.enableVibration(false);
            }

            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(Context context, String title, String message, UUID chatId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("chatId", chatId.toString());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.dot_done)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (NotificationSettingsManager.isVibrationEnabled(context)) {
            builder.setVibrate(DEFAULT_VIBRATION_PATTERN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }

        return builder.build();
    }
    public void setCurrentChatId(UUID currentChatId){
        this.currentChatId = currentChatId;
    }
}