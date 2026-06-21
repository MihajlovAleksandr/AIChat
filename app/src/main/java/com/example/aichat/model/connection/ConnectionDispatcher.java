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
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.json.JSONObject;

public class ConnectionDispatcher {

    private static final String TAG = "ConnectionDispatcher";

    private static final long PING_INTERVAL_MS = 2000;
    private static final long PONG_TIMEOUT_MS = 10000;
    private static final String PING_METHOD = "Ping";
    private static final String PONG_EVENT = "Pong";
    private static final int MAX_MISSED_PONGS = 2;

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

    private ScheduledExecutorService pingScheduler;
    private ScheduledFuture<?> pingTask;
    private final AtomicBoolean isPingActive = new AtomicBoolean(false);
    private volatile long lastPongTime = System.currentTimeMillis();
    private final AtomicBoolean isWaitingForPong = new AtomicBoolean(false);
    private int missedPongs = 0;
    private boolean pongHandlerRegistered = false;

    public ConnectionDispatcher(TokenStorage tokenStorage) {
        this.tokenStorage = tokenStorage;
        this.http = new HttpClient(tokenStorage.getToken());
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

        boolean wasReconnecting = state == ConnectionState.RECONNECTING;

        if (!isConnecting.compareAndSet(false, true)) {
            return failedFuture(new IllegalStateException("Connection already in progress"));
        }

        state = ConnectionState.CONNECTING;

        String token = tokenStorage.getToken();

        if (token == null || token.trim().isEmpty()) {
            isConnecting.set(false);
            state = ConnectionState.DISCONNECTED;
            return failedFuture(new ConnectionTokenNotInitializeException());
        }

        if (!wasReconnecting) {
            reconnectController.reset();
        }

        return http.fetchAsync("/api/session/connect", HttpClient.HTTPMethod.GET, null, false)
                .thenCompose(cmd -> {
                    if (cmd == null || !cmd.isSuccess()) {
                        isConnecting.set(false);
                        state = ConnectionState.DISCONNECTED;

                        if (cmd != null && cmd.getCode() == 401) {
                            return failedFuture(new UnauthorizedException());
                        }

                        return failedFuture(new RuntimeException("Connection init failed"));
                    }

                    ConnectionResponse response = cmd.getData(ConnectionResponse.class);

                    if (response == null || response.hubUrl == null || response.hubUrl.trim().isEmpty()) {
                        isConnecting.set(false);
                        state = ConnectionState.DISCONNECTED;
                        return failedFuture(new RuntimeException("Invalid connection response"));
                    }

                    String signalRToken = token;

                    if (response.refreshedToken
                            && response.token != null
                            && !response.token.trim().isEmpty()) {

                        updateToken(response.token);
                        http.setToken(response.token);
                        signalRToken = response.token;
                    }

                    registeredEvents.clear();

                    CompletableFuture<Void> future = new CompletableFuture<>();

                    signalR = new SignalRManager(response.hubUrl, signalRToken);
                    signalR.addListener(internalListener);
                    signalR.addListener(createConnectListener(future));
                    signalR.connect();

                    return future;
                });
    }

    public void disconnect() {
        stopPingMechanism();
        reconnectController.stop();

        if (signalR != null) {
            signalR.disconnect();
        }

        isConnecting.set(false);
        state = ConnectionState.DISCONNECTED;
        notifyDisconnected();
    }

    private void startPingMechanism() {
        if (pingScheduler == null || pingScheduler.isShutdown()) {
            pingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "PingThread");
                thread.setDaemon(true);
                return thread;
            });
        }

        if (pingTask != null && !pingTask.isDone()) {
            pingTask.cancel(false);
        }

        isPingActive.set(true);
        lastPongTime = System.currentTimeMillis();
        missedPongs = 0;
        isWaitingForPong.set(false);

        pingTask = pingScheduler.scheduleAtFixedRate(
                this::sendPing,
                PING_INTERVAL_MS,
                PING_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    private void stopPingMechanism() {
        isPingActive.set(false);

        if (pingTask != null) {
            pingTask.cancel(false);
            pingTask = null;
        }

        if (pingScheduler != null) {
            pingScheduler.shutdownNow();

            try {
                pingScheduler.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            pingScheduler = null;
        }

        isWaitingForPong.set(false);
        missedPongs = 0;
    }

    private void sendPing() {
        if (!isPingActive.get()) return;

        if (state != ConnectionState.CONNECTED || signalR == null || !signalR.isConnected()) return;

        if (isWaitingForPong.get()) {
            long timeSinceLastPong = System.currentTimeMillis() - lastPongTime;

            if (timeSinceLastPong > PONG_TIMEOUT_MS) {
                missedPongs++;

                if (missedPongs >= MAX_MISSED_PONGS) {
                    handlePingFailure();
                    return;
                }
            }
        }

        try {
            signalR.send(PING_METHOD, LocalDateTime.now().toString());
            isWaitingForPong.set(true);
        } catch (Exception e) {
            handlePingFailure();
        }
    }

    private void handlePong(String time) {
        lastPongTime = System.currentTimeMillis();
        isWaitingForPong.set(false);
        missedPongs = 0;
    }

    private void handlePingFailure() {
        if (!isPingActive.get()) return;

        startReconnectIfNeeded("ping failure");
    }

    private synchronized void startReconnectIfNeeded(String reason) {
        if (state == ConnectionState.RECONNECTING) {
            Log.d(TAG, "Reconnect already active, skip duplicate event. reason=" + reason);
            return;
        }

        if (state != ConnectionState.CONNECTED) {
            Log.d(TAG, "Reconnect skipped because state=" + state + ", reason=" + reason);
            return;
        }

        stopPingMechanism();

        state = ConnectionState.RECONNECTING;
        notifyReconnecting();
        reconnectController.start();
    }

    private void handleUnauthorized() {
        Log.e(TAG, "Handling 401 Unauthorized - clearing connection and notifying listeners");

        disconnect();
        tokenStorage.saveToken(null);
        http.setToken(null);

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

    private void updateToken(@Nullable String token) {
        if (token != null && JwtUtils.decodePayload(token) == null) {
            throw new IllegalArgumentException();
        }

        tokenStorage.saveToken(token);
    }

    public CompletableFuture<HttpCommand> sendHttpRequestAsync(
            String url,
            HttpClient.HTTPMethod method,
            @Nullable Object body,
            boolean isCritical
    ) {
        return sendHttpRequestAsync(url, method, body, isCritical, true);
    }

    public CompletableFuture<HttpCommand> sendHttpRequestAsync(
            String url,
            HttpClient.HTTPMethod method,
            @Nullable Object body,
            boolean isCritical,
            boolean triggerUnauthorizedHandler
    ) {
        return http.fetchAsync(url, method, body, isCritical, triggerUnauthorizedHandler);
    }

    public CompletableFuture<HttpCommand> uploadFile(
            String url,
            UploadFileRequest request,
            File file,
            UUID fileId,
            @Nullable String uploadFileName,
            UploadProgressListener listener
    ) {
        return http.uploadAsync(url, request, file, fileId, uploadFileName, listener);
    }

    public void sendSignalRRequestAsync(String method, @Nullable Object arg) {
        if (state != ConnectionState.CONNECTED || signalR == null) {
            throw new SignalRNotConnectedException();
        }

        signalR.send(method, arg);
    }

    public <T> void addEventListener(String eventName, Class<T> clazz, EventHandler<T> handler) {
        eventHandlers.computeIfAbsent(eventName, k -> new ArrayList<>()).add(handler);
        eventTypes.putIfAbsent(eventName, clazz);
        tryRegister(eventName);
    }

    public void addEventListener(String eventName, EventHandler<Void> handler) {
        eventHandlers.computeIfAbsent(eventName, k -> new ArrayList<>()).add(handler);
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
            registerPongHandler();

            state = ConnectionState.CONNECTED;
            isConnecting.set(false);

            reconnectController.stop();
            http.onConnected();
            notifyConnected();

            startPingMechanism();
        }

        @Override
        public void onDisconnected() {
            startReconnectIfNeeded("SignalR disconnected");
        }

        @Override
        public void onError(Throwable throwable) {
            if (isConnecting.get()) return;

            if (isFatal(throwable)) {
                stopPingMechanism();
                reconnectController.stop();
                notifyFatalError(throwable);
            } else {
                String reason = throwable != null
                        ? "SignalR error: " + throwable.getClass().getSimpleName()
                        : "SignalR error";
                startReconnectIfNeeded(reason);
            }
        }

        @Override
        public void onCommandReceived(SignalRCommand<?> command) {
            dispatch(command);
        }
    };

    private void registerPongHandler() {
        if (signalR == null) return;
        if (pongHandlerRegistered) return;

        pongHandlerRegistered = true;

        addEventListener(PONG_EVENT, String.class, new EventHandler<String>() {
            @Override
            public void handle(SignalRCommand<String> command) {
                handlePong(command.getPayload());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> void dispatch(SignalRCommand<?> rawCommand) {
        String eventName = rawCommand.getName();

        List<EventHandler<?>> handlers = eventHandlers.get(eventName);

        if (handlers == null) return;

        for (EventHandler<?> rawHandler : handlers) {
            EventHandler<T> handler = (EventHandler<T>) rawHandler;
            SignalRCommand<T> command = (SignalRCommand<T>) rawCommand;

            if (!PONG_EVENT.equals(eventName)) {
                Log.d(TAG, "EventHandler invoked: event=" + eventName
                        + ", payloadType=" + (command.getPayload() != null
                        ? command.getPayload().getClass().getSimpleName()
                        : "null"));
            }

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
                state = ConnectionState.DISCONNECTED;
                future.completeExceptionally(throwable);
                removeSelf();
            }

            @Override
            public void onDisconnected() {
                isConnecting.set(false);
                state = ConnectionState.DISCONNECTED;
                future.completeExceptionally(new RuntimeException("Disconnected during connect"));
                removeSelf();
            }

            @Override
            public void onCommandReceived(SignalRCommand<?> command) {
            }

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

            if (result == null) return null;

            return UUID.fromString(result.getString("sub"));
        } catch (Exception ex) {
            return null;
        }
    }

    public @Nullable UUID getConnectionId() {
        try {
            JSONObject result = JwtUtils.decodePayload(tokenStorage.getToken());

            if (result == null) return null;

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
        stopPingMechanism();
        state = ConnectionState.DISCONNECTED;
        notifyDisconnected();
    }

    private boolean isFatal(Throwable t) {
        return t instanceof UnauthorizedException;
    }

    private <T> CompletableFuture<T> failedFuture(Throwable ex) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }

    private void notifyConnected() {
        for (ConnectionStateListener listener : stateListeners) {
            listener.onConnected();
        }
    }

    private void notifyDisconnected() {
        for (ConnectionStateListener listener : stateListeners) {
            listener.onDisconnected();
        }
    }

    private void notifyReconnecting() {
        for (ConnectionStateListener listener : stateListeners) {
            listener.onReconnecting();
        }
    }

    private void notifyFatalError(Throwable throwable) {
        for (ConnectionStateListener listener : stateListeners) {
            listener.onFatalError(throwable);
        }
    }
}
