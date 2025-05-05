package com.example.aichat.model.connection;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.example.aichat.R;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.database.DatabaseSaver;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.notifications.NotificationHelper;
import com.example.aichat.model.notifications.NotificationSettingsManager;

public class NetworkService extends Service {
    private static final String TAG = "NetworkService";
    private static final String CHANNEL_ID = "NetworkServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static DatabaseSaver databaseSaver;
    private ConnectionManager connectionManager;
    private final OnConnectionEvents connectionEvents = new OnConnectionEvents() {
        @Override
        public void OnOpen() {
            Log.d(TAG, "WebSocket connection established");
            updateNotification("Connected to server");
        }

        @Override
        public void OnCommandGot(Command command) {
            Log.d("NetworkService", "GotCommand, IsAppActive "+ isAppActive());
            com.example.aichat.model.entities.Notification value =  databaseSaver.commandGot(command,NetworkService.this);
            if(value!=null) {
                Context context = NetworkService.this;
                if (NotificationSettingsManager.canSendNotifications(context)) {
                    if (isAppActive()) {
                        if (NotificationSettingsManager.areInAppNotificationsEnabled(context)) {
                            NotificationHelper.sendNotification(context, value);
                        }
                    }
                    else{
                        if (NotificationSettingsManager.areBackgroundNotificationsEnabled(context)) {
                            NotificationHelper.sendNotification(context, value);
                        }
                    }
                }
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
        initializeDatabaseSaver();
        initializeConnection();
    }

    private void initializeConnection() {
        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        connectionManager.addConnectionEvent(connectionEvents);
    }


    private void initializeDatabaseSaver() {
        AppDatabase appDatabase = DatabaseManager.getDatabase();
        databaseSaver = new DatabaseSaver(appDatabase);
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
        databaseSaver.setCurrentUserId(intent.getIntExtra("currentUserId", -1));
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

    private boolean isAppActive() {
        return ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}