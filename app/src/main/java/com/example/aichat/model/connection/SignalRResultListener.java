package com.example.aichat.model.connection;

public interface SignalRResultListener<T> {
    void onResult(T result);
    void onError(Throwable error);
}
