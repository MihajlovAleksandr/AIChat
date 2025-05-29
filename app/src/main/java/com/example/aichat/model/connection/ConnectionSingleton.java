package com.example.aichat.model.connection;

public class ConnectionSingleton {
    private static final ConnectionSingleton instance = new ConnectionSingleton();
    private ConnectionManager connectionManager;
    private boolean availableToClose;

    private ConnectionSingleton() {
        availableToClose = true;
    }

    public static ConnectionSingleton getInstance() {
        return instance;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public boolean isAvailableToClose() {
        return availableToClose;
    }

    public void setAvailableToClose(boolean availableToClose) {
        this.availableToClose = availableToClose;
    }
}
