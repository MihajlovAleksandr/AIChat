package com.example.aichat.model.connection;

import com.example.aichat.model.entities.Command;

public interface OnConnectionEvents {
    void OnCommandGot(Command command);
    void OnConnectionFailed();
    void OnOpen();
}
