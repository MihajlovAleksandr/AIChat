package com.example.aichat.model.connection;

import android.os.Build;
import android.util.Log;
import com.example.aichat.BuildConfig;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Map;
import java.util.Set;

public class SignalRManager {

    private static final String TAG = "SignalRManager";
    private static final String PONG_EVENT = "Pong";

    private volatile boolean shouldReconnect = true;
    private final String hubUrl;
    private final String jwtToken;
    private HubConnection hubConnection;

    private final Set<SignalRListener> listeners = new CopyOnWriteArraySet<>();
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final Map<String, EventDispatcher<?>> dispatchers = new ConcurrentHashMap<>();

    public SignalRManager(String hubUrl, String jwtToken) {
        String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        this.hubUrl = BuildConfig.SERVER_URL + hubUrl + "?device=" + deviceModel;
        this.jwtToken = jwtToken;
        initConnection();
    }

    private void initConnection() {
        hubConnection = HubConnectionBuilder
                .create(hubUrl)
                .withHeader("Authorization", "Bearer " + jwtToken)
                .build();

        hubConnection.onClosed(error -> {
            logConnectionClosed(error);

            notifyDisconnected();

            if (error != null) {
                notifyError(error);
            }

            if (!shouldReconnect) {
                Log.d(TAG, "⛔ Reconnect disabled");
                return;
            }
        });
    }


    private void logConnectionClosed(Throwable error) {
        if (error == null) {
            Log.d(TAG, "Connection closed");
            return;
        }

        if (isRecoverableConnectionClose(error)) {
            Log.w(TAG, "Connection closed, reconnect will be attempted: " + error.getMessage());
            return;
        }

        Log.e(TAG, "Connection closed", error);
    }

    private boolean isRecoverableConnectionClose(Throwable error) {
        if (!shouldReconnect || error == null) return false;

        String message = error.getMessage();

        if (message == null) return false;

        return message.contains("Software caused connection abort")
                || message.contains("Socket closed")
                || message.contains("Canceled")
                || message.contains("timeout")
                || message.contains("ECONNRESET");
    }

    public void connect() {
        shouldReconnect = true;

        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) return;

        Disposable disposable = hubConnection.start()
                .subscribe(
                        this::notifyConnected,
                        this::notifyError
                );

        disposables.add(disposable);
    }

    public void disconnect() {
        shouldReconnect = false;

        if (hubConnection.getConnectionState() == HubConnectionState.DISCONNECTED) return;

        Disposable disposable = hubConnection.stop()
                .subscribe(
                        this::notifyDisconnected,
                        this::notifyError
                );

        disposables.add(disposable);
    }

    public void send(String method, Object args) {
        if (hubConnection.getConnectionState() != HubConnectionState.CONNECTED) return;

        try {
            hubConnection.send(method, args);
        } catch (Exception e) {
            notifyError(e);
        }
    }

    public <T> void invoke(String method, Class<T> returnType, SignalRResultListener<T> callback, Object... args) {
        if (hubConnection.getConnectionState() != HubConnectionState.CONNECTED) return;

        Disposable disposable = hubConnection.invoke(returnType, method, args)
                .subscribe(
                        callback::onResult,
                        error -> {
                            notifyError(error);
                            callback.onError(error);
                        }
                );

        disposables.add(disposable);
    }

    public <T> void subscribe(String eventName, Class<T> clazz) {
        if (dispatchers.containsKey(eventName)) return;

        EventDispatcher<T> dispatcher = new EventDispatcher<>();
        dispatchers.put(eventName, dispatcher);

        if (shouldLogEvent(eventName)) {
            Log.e(TAG, "📡 Subscribing to event: " + eventName + ", isConnected=" + hubConnection.getConnectionState());
        }

        hubConnection.on(eventName, data -> {
            if (shouldLogEvent(eventName)) {
                Log.e(TAG, "🔥🔥🔥 SignalR RAW EVENT RECEIVED: " + eventName + " 🔥🔥🔥");
                Log.e(TAG, "Data: " + (data != null ? data.toString() : "null"));
            }

            dispatcher.emit(new SignalRCommand<>(eventName, data));
            notifyRaw(eventName, data);
        }, clazz);
    }

    public void subscribe(String eventName) {
        if (dispatchers.containsKey(eventName)) return;

        EventDispatcher<Void> dispatcher = new EventDispatcher<>();
        dispatchers.put(eventName, dispatcher);

        if (shouldLogEvent(eventName)) {
            Log.e(TAG, "📡 Subscribing to event: " + eventName + ", isConnected=" + hubConnection.getConnectionState());
        }

        hubConnection.on(eventName, () -> {
            dispatcher.emit(new SignalRCommand<>(eventName, null));
            notifyRaw(eventName, null);
        });
    }

    public void unsubscribe(String eventName) {
        dispatchers.remove(eventName);
        hubConnection.remove(eventName);
    }

    public void unsubscribeAll() {
        for (String eventName : dispatchers.keySet()) {
            hubConnection.remove(eventName);
        }

        dispatchers.clear();
    }

    public <T> void addHandler(String eventName, SignalRCommandHandler<T> handler) {
        EventDispatcher<T> dispatcher = getDispatcher(eventName);
        dispatcher.setHandler(handler);
    }

    @SuppressWarnings("unchecked")
    private <T> EventDispatcher<T> getDispatcher(String eventName) {
        EventDispatcher<?> dispatcher = dispatchers.get(eventName);

        if (dispatcher == null) {
            throw new IllegalStateException("Event not registered: " + eventName);
        }

        return (EventDispatcher<T>) dispatcher;
    }

    private void notifyRaw(String name, Object payload) {
        SignalRCommand<Object> command = new SignalRCommand<>(name, payload);

        for (SignalRListener listener : listeners) {
            listener.onCommandReceived(command);
        }
    }

    private void ensureStarted() {
        if (hubConnection.getConnectionState() != HubConnectionState.CONNECTED) {
            throw new IllegalStateException("SignalR is not connected yet");
        }
    }

    private boolean shouldLogEvent(String eventName) {
        return !PONG_EVENT.equals(eventName);
    }

    public void addListener(SignalRListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SignalRListener listener) {
        listeners.remove(listener);
    }

    public boolean isConnected() {
        return hubConnection.getConnectionState() == HubConnectionState.CONNECTED;
    }

    private void notifyConnected() {
        for (SignalRListener listener : listeners) {
            listener.onConnected();
        }
    }

    private void notifyDisconnected() {
        for (SignalRListener listener : listeners) {
            listener.onDisconnected();
        }
    }

    private void notifyError(Throwable throwable) {
        for (SignalRListener listener : listeners) {
            listener.onError(throwable);
        }
    }

    public void clear() {
        disposables.clear();
    }

    public void dispose() {
        unsubscribeAll();
        disposables.dispose();
    }

    private static class EventDispatcher<T> {

        private volatile SignalRCommandHandler<T> handler;

        void setHandler(SignalRCommandHandler<T> handler) {
            this.handler = handler;
        }

        void emit(SignalRCommand<T> command) {
            SignalRCommandHandler<T> currentHandler = handler;

            if (currentHandler != null) {
                currentHandler.handle(command);
            }
        }
    }
}
