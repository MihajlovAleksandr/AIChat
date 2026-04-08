package com.example.aichat.model.connection;

import com.example.aichat.model.entities.WSSCommand;

public interface OnConnectionEvents {
    void OnCommandGot(WSSCommand WSSCommand);
    void OnConnectionFailed();
    void OnOpen();
}
