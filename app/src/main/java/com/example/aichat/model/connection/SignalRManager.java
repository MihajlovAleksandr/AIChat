package com.example.aichat.model.connection;

import android.os.Build;
import android.util.Log;

import com.example.aichat.BuildConfig;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public class SignalRManager {

    private static final String TAG = "SignalRManager";

    private volatile boolean shouldReconnect = true;
    private final String hubUrl;
    private final String jwtToken;
    private volatile boolean isStarted = false;
    private HubConnection hubConnection;

    private final Set<SignalRListener> listeners = new CopyOnWriteArraySet<>();
    private final CompositeDisposable disposables = new CompositeDisposable();

    /**
     * eventName -> dispatcher
     */
    private final Map<String, EventDispatcher<?>> dispatchers = new ConcurrentHashMap<>();

    public SignalRManager(String hubUrl, String jwtToken) {
        String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        this.hubUrl = BuildConfig.SERVER_URL + hubUrl + "?device=" + deviceModel;
        Log.e(TAG,  jwtToken);
        this.jwtToken = jwtToken;
        initConnection();
    }

    private void initConnection() {
        hubConnection = HubConnectionBuilder
                .create(hubUrl)
                .withHeader("Authorization", "Bearer " + jwtToken)
                .build();

        hubConnection.onClosed(error -> {
            Log.e(TAG, "Connection closed", error);

            isStarted = false;
            notifyDisconnected();

            if (error != null) {
                notifyError(error);
            }

            if (!shouldReconnect) {
                Log.d(TAG, "⛔ Reconnect disabled");
                return;
            }

            new Thread(() -> {
                try {
                    Thread.sleep(2000);

                    if (!shouldReconnect) return;

                    Log.d(TAG, "🔁 Attempting reconnect...");
                    connect();

                } catch (InterruptedException ignored) {}
            }).start();
        });
    }

    public void connect() {
        shouldReconnect = true;

        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) return;

        Disposable disposable = hubConnection.start()
                .subscribe(
                        () -> {
                            isStarted = true;
                            notifyConnected();
                        },
                        this::notifyError
                );

        disposables.add(disposable);
    }

    public void disconnect() {
        shouldReconnect = false;

        if (hubConnection.getConnectionState() == HubConnectionState.DISCONNECTED) return;

        Disposable disposable = hubConnection.stop()
                .subscribe(
                        () -> {
                            isStarted = false;
                            notifyDisconnected();
                        },
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

    public <T> void invoke(String method, Class<T> returnType,
                           SignalRResultListener<T> callback,
                           Object... args) {

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

    // ================= SUBSCRIBE =================

    // В SignalRManager.java, в методе subscribe для событий с данными:
    public <T> void subscribe(String eventName, Class<T> clazz) {
        ensureStarted();

        if (dispatchers.containsKey(eventName)) return;

        EventDispatcher<T> dispatcher = new EventDispatcher<>();
        dispatchers.put(eventName, dispatcher);

        Log.e(TAG, "📡 Subscribing to event: " + eventName + ", isConnected=" + hubConnection.getConnectionState());

        hubConnection.on(eventName, data -> {
            Log.e(TAG, "🔥🔥🔥 SignalR RAW EVENT RECEIVED: " + eventName + " 🔥🔥🔥");
            Log.e(TAG, "Data: " + (data != null ? data.toString() : "null"));
            dispatcher.emit(new SignalRCommand<>(eventName, data));
            notifyRaw(eventName, data);
        }, clazz);
    }

    public void subscribe(String eventName) {
        if (dispatchers.containsKey(eventName)) return;

        EventDispatcher<Void> dispatcher = new EventDispatcher<>();
        dispatchers.put(eventName, dispatcher);

        // ✅ ВАЖНО: пустой массив типов
        hubConnection.on(eventName, () -> {
            dispatcher.emit(new SignalRCommand<>(eventName, null));
            notifyRaw(eventName, null);
        });
    }

    // ================= UNSUBSCRIBE =================

    public void unsubscribe(String eventName) {
        dispatchers.remove(eventName);

        // ✅ правильная отписка для SignalR
        hubConnection.remove(eventName);
    }

    public void unsubscribeAll() {
        for (String eventName : dispatchers.keySet()) {
            hubConnection.remove(eventName);
        }
        dispatchers.clear();
    }

    // ================= HANDLERS =================

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

    // ================= NOTIFY =================

    private void notifyRaw(String name, Object payload) {
        SignalRCommand<Object> command = new SignalRCommand<>(name, payload);

        for (SignalRListener l : listeners) {
            l.onCommandReceived(command);
        }
    }
    private void ensureStarted() {
        if (!isStarted) {
            throw new IllegalStateException("SignalR is not connected yet");
        }
    }
    public void addListener(SignalRListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SignalRListener listener) {
        listeners.remove(listener);
    }

    public boolean isConnected(){
        return hubConnection.getConnectionState() == HubConnectionState.CONNECTED;
    }

    private void notifyConnected() {
        for (SignalRListener l : listeners) l.onConnected();
    }

    private void notifyDisconnected() {
        for (SignalRListener l : listeners) l.onDisconnected();
    }

    private void notifyError(Throwable throwable) {
        for (SignalRListener l : listeners) l.onError(throwable);
    }

    // ================= CLEANUP =================

    public void clear() {
        disposables.clear();
    }

    public void dispose() {
        unsubscribeAll();
        disposables.dispose();
    }

    // ================= INTERNAL =================

    private static class EventDispatcher<T> {

        private volatile SignalRCommandHandler<T> handler;

        void setHandler(SignalRCommandHandler<T> handler) {
            this.handler = handler;
        }

        void emit(SignalRCommand<T> command) {
            SignalRCommandHandler<T> h = handler;
            if (h != null) {
                h.handle(command);
            }
        }
    }
}