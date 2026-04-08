package com.example.aichat.model.connection;

import android.util.Log;

import com.example.aichat.model.database.DatabaseManager;

public class ConnectionSingleton {

    private static final ConnectionSingleton instance = new ConnectionSingleton();

    private ConnectionManager connectionManager;
    private InAppConnection inAppConnection;
    private String savedToken;

    private final HttpClient httpClient = new HttpClient();

    private boolean availableToClose = true;

    private ConnectionSingleton() {}

    public static ConnectionSingleton getInstance() {
        return instance;
    }


    public synchronized ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public synchronized void setConnectionManager(ConnectionManager manager) {

        if (this.connectionManager == manager) return;

        destroyInAppConnectionInternal();
        destroyConnectionManagerInternal();

        this.connectionManager = manager;
    }
    public synchronized void setInAppConnection(InAppConnection conn) {
        destroyInAppConnectionInternal();
        this.inAppConnection = conn;
    }

    public synchronized InAppConnection getInAppConnection() {
        return inAppConnection;
    }

    public synchronized void destroyInAppConnection() {
        destroyInAppConnectionInternal();
    }

    private void destroyInAppConnectionInternal() {
        if (inAppConnection != null) {
            try { inAppConnection.destroy(); } catch (Exception ignored) {}
            inAppConnection = null;
        }
    }

    public synchronized void setToken(String token) {
        this.savedToken = token;
    }

    public synchronized String getToken() {
        return savedToken;
    }
    public HttpClient getHttpClient() {
        return httpClient;
    }

    public synchronized boolean isAvailableToClose() {
        return availableToClose;
    }

    public synchronized void setAvailableToClose(boolean availableToClose) {
        this.availableToClose = availableToClose;
    }

    public synchronized void resetConnectionOnly() {

        Log.d("ConnectionSingleton", "resetConnectionOnly()");

        destroyInAppConnectionInternal();
        destroyConnectionManagerInternal();

        connectionManager = null;
        savedToken = null;

        availableToClose = true;

        try {
            DatabaseManager.getDatabase().pendingCommandDao().clearTable();
        } catch (Exception ignored) {}
    }

    public synchronized void resetFull() {

        Log.d("ConnectionSingleton", "resetFull()");

        resetConnectionOnly();

        try {
            DatabaseManager.getDatabase().clearAllTables();
            Log.d("DB", "Logout complete. Database cleared.");
        } catch (Exception ignored) {}
    }

    private void destroyConnectionManagerInternal() {
        if (connectionManager != null) {
            try { connectionManager.clearConnectionEvents(); } catch (Exception ignored) {}
            try { connectionManager.Close(); } catch (Exception ignored) {}
            try { connectionManager.dispose(); } catch (Exception ignored) {}
        }
        connectionManager = null;
    }
}
