package com.example.aichat.model.connection;

import android.content.Context;
import android.util.Log;

import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.database.DatabaseSaver;
import com.example.aichat.model.entities.WSSCommand;

import java.util.UUID;

public class InAppConnection {

    private final ConnectionManager connectionManager;
    private final DatabaseSaver databaseSaver;
    private final OnConnectionEvents events;

    private boolean isPaused = false;
    private boolean destroyed = false;

    public InAppConnection(ConnectionManager connectionManager, Context context, UUID userId) {

        this.connectionManager = connectionManager;

        Context appContext = context.getApplicationContext();
        this.databaseSaver = new DatabaseSaver(DatabaseManager.getDatabase(), userId);

        this.events = new OnConnectionEvents() {
            @Override
            public void OnCommandGot(WSSCommand command) {
                if (!isPaused && !destroyed) {
                    try {
                        databaseSaver.commandGot(command, appContext);
                    } catch (Exception e) {
                        Log.e("InAppConnection", "Error in commandGot()", e);
                    }
                }
            }

            @Override public void OnConnectionFailed() {}
            @Override public void OnOpen() {}
        };

        safeAddListener();
    }

    private void safeAddListener() {
        if (!destroyed) {
            connectionManager.addConnectionEvent(events);
        }
    }

    private void safeRemoveListener() {
        try {
            connectionManager.removeConnectionEvent(events);
        } catch (Exception ignored) {}
    }

    public void destroy() {
        destroyed = true;
        safeRemoveListener();
    }

    public void pause() {
        isPaused = true;
        safeRemoveListener();
        connectionManager.setAppInForeground(false);
    }

    public void resume() {
        isPaused = false;
        connectionManager.setAppInForeground(true);

        if (!destroyed) {
            safeAddListener();

        }
    }
}
