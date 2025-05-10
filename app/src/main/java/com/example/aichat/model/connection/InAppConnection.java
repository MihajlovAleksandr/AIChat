package com.example.aichat.model.connection;

import android.content.Context;

import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.database.DatabaseSaver;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.entities.Notification;
import com.example.aichat.model.notifications.NotificationHelper;
import com.example.aichat.model.notifications.NotificationSettingsManager;

public class InAppConnection {
    private final ConnectionManager connectionManager;
    private final DatabaseSaver databaseSaver;
    private final OnConnectionEvents events;
    public InAppConnection(ConnectionManager connectionManager,  Context context, int currentUserId){
        this.connectionManager = connectionManager;
        databaseSaver = new DatabaseSaver(DatabaseManager.getDatabase(), currentUserId);
        events = new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                Notification value = databaseSaver.commandGot(command,context);
                if(value!=null) {
                    if (NotificationSettingsManager.canSendNotifications(context)) {
                        if (NotificationSettingsManager.areInAppNotificationsEnabled(context)) {
                            NotificationHelper.sendNotification(context, value);
                        }
                    }
                }
            }

            @Override
            public void OnConnectionFailed() {

            }

            @Override
            public void OnOpen() {

            }
        };
        connectionManager.addConnectionEvent(events);
    }
    public void destroy(){
        connectionManager.clearConnectionEvents();
        connectionManager.Close();
    }

}
