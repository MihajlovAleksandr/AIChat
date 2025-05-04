package com.example.aichat.model.connection;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.example.aichat.R;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.entities.Message;

public class NetworkService extends Service {
    private static final String TAG = "NetworkService";
    private static final String CHANNEL_ID = "NetworkServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private ConnectionManager connectionManager;
    private final OnConnectionEvents connectionEvents = new OnConnectionEvents() {
        @Override
        public void OnOpen() {
            Log.d(TAG, "WebSocket connection established");
            updateNotification("Connected to server");
        }

        @Override
        public void OnCommandGot(Command command) {
            Log.d("NetworkService", "GotCommand");
            AppDatabase appDatabase = DatabaseManager.getDatabase();
            switch (command.getOperation()) {
                case "SendMessage":
                    Message message = command.getData("message", Message.class);
                    appDatabase.messageDao().insertMessage(message);
                    break;
                case "CreateChat":
                    Chat createdChat = command.getData("chat", Chat.class);
                    appDatabase.chatDao().insertChat(createdChat);
                    break;
                case "EndChat":
                    Chat endedChat = command.getData("chat", Chat.class);
                    appDatabase.chatDao().endChat(endedChat.getId(),  endedChat.getEndTime());
                    break;
            }
        }

        @Override
        public void OnConnectionFailed() {
            Log.d(TAG, "WebSocket connection failed");
            updateNotification("Connection lost - reconnecting...");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        createNotificationChannel();
        initializeConnection();
    }

    private void initializeConnection() {
        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        connectionManager.addConnectionEvent(connectionEvents);
    }

    private void updateNotification(String text) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Сетевое соединение")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_info_outline)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Сетевое соединение")
                .setContentText(connectionManager.Connected ? "Connected" : "Connecting...")
                .setSmallIcon(R.drawable.ic_info_outline)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        if (connectionManager != null) {
            connectionManager.Close();
            connectionManager.clearConnectionEvents();
        }
        super.onDestroy();
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Network Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private boolean isAppInForeground() {
        return ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}