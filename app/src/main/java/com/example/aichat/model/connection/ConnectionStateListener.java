package com.example.aichat.model.connection;

public interface ConnectionStateListener {

    default void onConnected() {}

    default void onDisconnected() {}

    default void onReconnecting() {}

    default void onFatalError(Throwable t) {}
}