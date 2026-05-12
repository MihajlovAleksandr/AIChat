package com.example.aichat.model.connection;

@FunctionalInterface
public interface EventHandler<T> {
    void handle(SignalRCommand<T> command);
}