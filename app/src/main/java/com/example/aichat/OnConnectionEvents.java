package com.example.aichat;

public interface OnConnectionEvents {
    void OnCommandGot(Command command);
    void OnConnectionFailed();
    void OnOpen();
}
