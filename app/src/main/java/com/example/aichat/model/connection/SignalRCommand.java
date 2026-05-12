package com.example.aichat.model.connection;

import androidx.annotation.Nullable;

public class SignalRCommand<T> {

    private final String name;
    @Nullable
    private final T payload;

    public SignalRCommand(String name, @Nullable T payload) {
        this.name = name;
        this.payload = payload;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public T getPayload() {
        return payload;
    }
}