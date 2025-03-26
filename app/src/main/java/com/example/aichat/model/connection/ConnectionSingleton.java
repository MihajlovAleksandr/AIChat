package com.example.aichat.model.connection;

public class ConnectionSingleton {
    private static final ConnectionSingleton instance = new ConnectionSingleton();
    private ConnectionManager connectionManager;

    private ConnectionSingleton() {}

    public static ConnectionSingleton getInstance() {
        return instance;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
}
