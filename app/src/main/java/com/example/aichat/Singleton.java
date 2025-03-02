package com.example.aichat;

public class Singleton {
    private static final Singleton instance = new Singleton();
    private ConnectionManager connectionManager;

    private Singleton() {}

    public static Singleton getInstance() {
        return instance;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
}
