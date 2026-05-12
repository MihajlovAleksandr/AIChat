package com.example.aichat.model.connection;

import android.util.Log;

import androidx.annotation.Nullable;

import com.example.aichat.dto.request.UploadFileRequest;
import com.example.aichat.dto.response.ConnectionResponse;
import com.example.aichat.model.connection.files.UploadProgressListener;
import com.example.aichat.model.entities.HttpCommand;
import com.example.aichat.model.exceptions.ConnectionTokenNotInitializeException;
import com.example.aichat.model.exceptions.SignalRNotConnectedException;
import com.example.aichat.model.exceptions.UnauthorizedException;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionDispatcher {

    private static final String TAG = "ConnectionDispatcher";

    private final HttpClient http;
    private final TokenStorage tokenStorage;

    @Nullable
    private SignalRManager signalR;

    private final ReconnectController reconnectController;

    private final Map<String, List<EventHandler<?>>> eventHandlers = new HashMap<>();
    private final List<ConnectionStateListener> stateListeners = new ArrayList<>();
    private final Set<String> registeredEvents = new HashSet<>();
    private final Map<String, Class<?>> eventTypes = new HashMap<>();

    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private volatile ConnectionState state = ConnectionState.DISCONNECTED;

    public ConnectionDispatcher(TokenStorage tokenStorage) {
        this.tokenStorage = tokenStorage;
        this.http = new HttpClient(tokenStorage.getToken());

        // ==================== УСТАНОВКА ОБРАБОТЧИКА 401 ====================
        this.http.setUnauthorizedHandler(this::handleUnauthorized);

        this.reconnectController = new ReconnectController(
                5,
                2000,
                this::reconnect,
                this::onReconnectFailed
        );
    }

    public CompletableFuture<Void> connect() {
        if (state == ConnectionState.CONNECTED) {
            return CompletableFuture.completedFuture(null);
        }

        if (!isConnecting.compareAndSet(false, true)) {
            return failedFuture(new IllegalStateException("Connection already in progress"));
        }

        state = ConnectionState.CONNECTING;

        String token = tokenStorage.getToken();
        if (token == null) {
            isConnecting.set(false);
            return failedFuture(new ConnectionTokenNotInitializeException());
        }
        Log.e(TAG, token);

        reconnectController.reset();

        return http.fetchAsync("/api/session/connect", HttpClient.HTTPMethod.GET, null, false)
                .thenCompose(cmd -> {

                    if (!cmd.isSuccess()) {
                        isConnecting.set(false);

                        if (cmd.getCode() == 401) {
                            return failedFuture(new UnauthorizedException());
                        }

                        return failedFuture(new RuntimeException("Connection init failed"));
                    }

                    ConnectionResponse response = cmd.getData(ConnectionResponse.class);

                    if (response.refreshedToken) {
                        updateToken(response.token);
                    }

                    CompletableFuture<Void> future = new CompletableFuture<>();

                    signalR = new SignalRManager(response.hubUrl, token);

                    signalR.addListener(internalListener);
                    signalR.addListener(createConnectListener(future));

                    signalR.connect();

                    return future;
                });
    }

    public void disconnect() {
        reconnectController.stop();

        if (signalR != null) {
            signalR.disconnect();
        }

        state = ConnectionState.DISCONNECTED;
        notifyDisconnected();
    }

    // ==================== ОБРАБОТКА 401 UNAUTHORIZED ====================
    private void handleUnauthorized() {
        Log.e(TAG, "Handling 401 Unauthorized - clearing connection and notifying listeners");

        // Разрываем соединение
        disconnect();

        // Очищаем токен
        tokenStorage.saveToken(null);

        // Оповещаем слушателей о фатальной ошибке
        UnauthorizedException exception = new UnauthorizedException();
        for (ConnectionStateListener listener : stateListeners) {
            listener.onFatalError(exception);
        }
    }

    public void setToken(@Nullable String token) {
        disconnect();
        updateToken(token);
        http.setToken(token);
    }

    private void updateToken(String token) {
        if (token != null && JwtUtils.decodePayload(token) == null)
            throw new IllegalArgumentException();

        tokenStorage.saveToken(token);
    }

    public CompletableFuture<HttpCommand> sendHttpRequestAsync(
            String url,
            HttpClient.HTTPMethod method,
            @Nullable Object body,
            boolean isCritical
    ) {
        return http.fetchAsync(url, method, body, isCritical);
    }

    public CompletableFuture<HttpCommand> uploadFile(
            String url,
            UploadFileRequest request,
            File file,
            UUID fileId,
            UploadProgressListener listener
    ) {
        return http.uploadAsync(
                url,
                request,
                file,
                fileId,
                listener
        );
    }
    public void sendSignalRRequestAsync(String method, @Nullable Object arg) {
        if (state != ConnectionState.CONNECTED || signalR == null) {
            throw new SignalRNotConnectedException();
        }

        signalR.send(method, arg);
    }

    public <T> void addEventListener(
            String eventName,
            Class<T> clazz,
            EventHandler<T> handler
    ) {
        eventHandlers
                .computeIfAbsent(eventName, k -> new ArrayList<>())
                .add(handler);

        eventTypes.putIfAbsent(eventName, clazz);

        tryRegister(eventName);
    }

    public void addEventListener(
            String eventName,
            EventHandler<Void> handler
    ) {
        eventHandlers
                .computeIfAbsent(eventName, k -> new ArrayList<>())
                .add(handler);

        eventTypes.putIfAbsent(eventName, Void.class);

        tryRegister(eventName);
    }

    private void tryRegister(String eventName) {
        if (signalR == null) return;
        if (registeredEvents.contains(eventName)) return;
        if (!signalR.isConnected()) return;

        registerEvent(eventName);
    }

    public void addStateListener(ConnectionStateListener listener) {
        stateListeners.add(listener);
    }

    private void flushPendingSubscriptions() {
        if (signalR == null) return;

        for (String eventName : eventHandlers.keySet()) {
            if (!registeredEvents.contains(eventName)) {
                registerEvent(eventName);
            }
        }
    }

    private void registerEvent(String eventName) {
        registeredEvents.add(eventName);

        Class<?> clazz = eventTypes.get(eventName);

        if (clazz == Void.class) {
            signalR.subscribe(eventName);
        } else {
            signalR.subscribe(eventName, clazz);
        }
    }

    public <T> void removeEventListener(String eventName, EventHandler<T> handler) {
        List<EventHandler<?>> handlers = eventHandlers.get(eventName);
        if (handlers == null) return;

        handlers.remove(handler);

        if (handlers.isEmpty()) {
            eventHandlers.remove(eventName);
            unregisterEvent(eventName);
        }
    }

    public void removeEventListeners(String eventName) {
        if (!eventHandlers.containsKey(eventName)) return;

        eventHandlers.remove(eventName);
        unregisterEvent(eventName);
    }

    public TokenStorage getTokenStorage() {
        return tokenStorage;
    }

    private void unregisterEvent(String eventName) {
        registeredEvents.remove(eventName);

        if (signalR != null) {
            signalR.unsubscribe(eventName);
        }
    }

    private final SignalRListener internalListener = new SignalRListener() {

        @Override
        public void onConnected() {
            flushPendingSubscriptions();

            state = ConnectionState.CONNECTED;
            reconnectController.stop();
            http.onConnected();
            notifyConnected();
        }

        @Override
        public void onDisconnected() {
            if (state == ConnectionState.CONNECTED) {
                state = ConnectionState.RECONNECTING;
                notifyReconnecting();
                reconnectController.start();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (isConnecting.get()) return;

            if (isFatal(throwable)) {
                reconnectController.stop();
                notifyFatalError(throwable);
            } else {
                state = ConnectionState.RECONNECTING;
                notifyReconnecting();
                reconnectController.start();
            }
        }

        @Override
        public void onCommandReceived(SignalRCommand<?> command) {
            dispatch(command);
        }
    };

    @SuppressWarnings("unchecked")
    private <T> void dispatch(SignalRCommand<?> rawCommand) {

        String eventName = rawCommand.getName();
        List<EventHandler<?>> handlers = eventHandlers.get(eventName);
        if (handlers == null) return;

        for (EventHandler<?> rawHandler : handlers) {

            EventHandler<T> handler = (EventHandler<T>) rawHandler;
            SignalRCommand<T> command = (SignalRCommand<T>) rawCommand;

            Log.d(TAG, "EventHandler invoked: event=" + eventName +
                    ", payloadType=" + (command.getPayload() != null
                    ? command.getPayload().getClass().getSimpleName()
                    : "null"));

            handler.handle(command);
        }
    }

    private SignalRListener createConnectListener(CompletableFuture<Void> future) {
        return new SignalRListener() {

            @Override
            public void onConnected() {
                isConnecting.set(false);
                future.complete(null);
                removeSelf();
            }

            @Override
            public void onError(Throwable throwable) {
                isConnecting.set(false);
                future.completeExceptionally(throwable);
                removeSelf();
            }

            @Override
            public void onDisconnected() {
                isConnecting.set(false);
                future.completeExceptionally(
                        new RuntimeException("Disconnected during connect")
                );
                removeSelf();
            }

            @Override
            public void onCommandReceived(SignalRCommand<?> command) {}

            private void removeSelf() {
                if (signalR != null) {
                    signalR.removeListener(this);
                }
            }
        };
    }

    public @Nullable UUID getUserId() {
        try {
            JSONObject result = JwtUtils.decodePayload(tokenStorage.getToken());
            if (result == null)
                return null;
            return UUID.fromString(result.getString("sub"));
        } catch (Exception ex) {
            return null;
        }
    }

    public @Nullable UUID getConnectionId() {
        try {
            JSONObject result = JwtUtils.decodePayload(tokenStorage.getToken());
            if (result == null)
                return null;
            return UUID.fromString(result.getString("connectionId"));
        } catch (Exception ex) {
            return null;
        }
    }

    private CompletableFuture<Void> reconnect() {
        Log.d("Reconnect", "Attempting reconnect...");
        return connect();
    }

    private void onReconnectFailed() {
        state = ConnectionState.DISCONNECTED;
        notifyDisconnected();
    }

    private boolean isFatal(Throwable t) {
        return t instanceof UnauthorizedException;
    }

    private <T> CompletableFuture<T> failedFuture(Throwable ex) {
        CompletableFuture<T> f = new CompletableFuture<>();
        f.completeExceptionally(ex);
        return f;
    }

    private void notifyConnected() {
        for (ConnectionStateListener l : stateListeners) {
            l.onConnected();
        }
    }

    private void notifyDisconnected() {
        for (ConnectionStateListener l : stateListeners) {
            l.onDisconnected();
        }
    }

    private void notifyReconnecting() {
        for (ConnectionStateListener l : stateListeners) {
            l.onReconnecting();
        }
    }


    private void notifyFatalError(Throwable t) {
        for (ConnectionStateListener l : stateListeners) {
            l.onFatalError(t);
        }
    }
}