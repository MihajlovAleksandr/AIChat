package com.example.aichat;
import android.util.Log;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public final class EchoWebSocketListener extends WebSocketListener {
    private final List<GetCommandEventListener> getCommandEventListeners = new ArrayList<>();
    private final List<CloseEventListener> closeEventListeners = new ArrayList<>();
    public void addGetCommandEvent(GetCommandEventListener listener){
        getCommandEventListeners.add(listener);
    }
    public void addCloseEvent(CloseEventListener listener){
        closeEventListeners.add(listener);
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        Command command = JsonHelper.Deserialize(text, Command.class);
        commandGot(webSocket, command);
    }

    @Override
    public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        Close();
    }

    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, okhttp3.Response response) {
        Log.e("connection", "connaction failed");
    }
    private void commandGot(WebSocket socket, Command command){
        GetCommandEvent event = new GetCommandEvent(socket, command);
        Message msg = command.getData("message", Message.class);
        Log.d("1", msg.getTime().toString());
        for (GetCommandEventListener eventListener: getCommandEventListeners) {
            eventListener.onCommandGot(event);
        }
    }
    private void Close(){
        for (CloseEventListener listener : closeEventListeners) {
            listener.onClose();
        }
    }
}