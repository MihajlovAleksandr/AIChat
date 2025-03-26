package com.example.aichat.model;

public interface OnConnectionEvents {
    void OnCommandGot(Command command);
    void OnConnectionFailed();
    void OnOpen();
}
