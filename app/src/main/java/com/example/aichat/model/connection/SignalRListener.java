package com.example.aichat.model.connection;

public interface SignalRListener {
    void onConnected();
    void onDisconnected();
    void onError(Throwable throwable);
    void onCommandReceived(SignalRCommand<?> command);;
}
