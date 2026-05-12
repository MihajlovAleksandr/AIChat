package com.example.aichat.model.connection;

public interface SignalRCommandHandler<T> {
    void handle(SignalRCommand<T> command);
}
