package com.example.aichat;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public abstract class EchoWebSocketListener extends WebSocketListener {
    private final List<GetCommandEventListener> getCommandEventListeners = new ArrayList<>();
    public void addGetCommandEvent(GetCommandEventListener listener) {
        getCommandEventListeners.add(listener);
    }
    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        Command command = JsonHelper.Deserialize(text, Command.class);
        commandGot(webSocket, command);
    }

    private void commandGot(WebSocket socket, Command command) {
        GetCommandEvent event = new GetCommandEvent(socket, command);
        Message msg = command.getData("message", Message.class);
        Log.d("1", msg.getTime().toString());
        for (GetCommandEventListener eventListener : getCommandEventListeners) {
            eventListener.onCommandGot(event);
        }
    }
}
