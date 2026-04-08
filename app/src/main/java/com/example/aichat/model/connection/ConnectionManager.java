package com.example.aichat.model.connection;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.aichat.BuildConfig;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.PendingCommand;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.model.utils.JsonHelper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ConnectionManager {

    private WebSocket webSocket;
    private OkHttpClient client;

    private volatile boolean connected = false;
    private volatile boolean reconnectAllowed = true;
    private volatile boolean appInForeground = true;

    private volatile boolean manualCloseInProgress = false;

    private String currentToken = null;
    private Request request;

    private final List<OnConnectionEvents> connectionEvents = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> reconnectTask;

    private static final long RECONNECT_INTERVAL_MS = 1500;

    private final WebSocketListener webSocketListener;

    public ConnectionManager(String token) {
        this();
        setToken(token);
    }

    public ConnectionManager() {
        client = createUnsafeClient();

        webSocketListener = new WebSocketListener() {

            @Override
            public void onOpen(@NonNull WebSocket ws, @NonNull Response response) {
                connected = true;
                reconnectAllowed = true;
                manualCloseInProgress = false;
                Log.d("Connection", "WebSocket opened");

                invokeOnOpen();

                new Thread(() -> {
                    AppDatabase db = DatabaseManager.getDatabase();
                    List<PendingCommand> unsent = db.pendingCommandDao().getAllCommands();
                    for (PendingCommand c : unsent) {
                        SendCommand(c.getCommandFormat());
                        db.pendingCommandDao().deleteCommand(c);
                    }
                }).start();
            }

            @Override
            public void onClosed(@NonNull WebSocket ws, int code, @NonNull String reason) {
                connected = false;
                Log.d("Connection", "WebSocket closed: " + reason);

                if (manualCloseInProgress) {
                    Log.d("Connection", "Manual close in progress — skip reconnect");
                    return;
                }

                if (reconnectAllowed) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t, @Nullable Response response) {
                connected = false;
                Log.e("Connection", "WebSocket failure", t);

                invokeOnConnectionFailed();

                if (reconnectAllowed) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
                WSSCommand command = JsonHelper.Deserialize(text, WSSCommand.class);
                if (command == null) return;

                if ("Logout".equals(command.getOperation())) {
                    reconnectAllowed = false;
                }

                invokeOnCommandGot(command);
            }
        };
    }

    public synchronized void setToken(String token) {
        this.currentToken = token;
        this.request = buildRequest(token);
    }

    public synchronized void connect() {
        if (currentToken == null) {
            Log.w("Connection", "Cannot connect: token not set");
            return;
        }

        Log.d("Connection", "connect() called — creating WebSocket");

        reconnectAllowed = true;
        connected = false;
        manualCloseInProgress = true;
        closeWebSocketInternal(false);
        cancelReconnectTask();

        webSocket = client.newWebSocket(request, webSocketListener);
    }

    public synchronized void setAppInForeground(boolean foreground) {
        this.appInForeground = foreground;

        if (foreground && !connected && reconnectAllowed) {
            scheduleReconnect();
        } else if (!foreground) {
            cancelReconnectTask();
        }
    }
    private synchronized void scheduleReconnect() {
        cancelReconnectTask();

        if (!appInForeground || !reconnectAllowed) return;

        reconnectTask = scheduler.schedule(
                this::connect,
                RECONNECT_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );

        Log.d("Connection", "Reconnect scheduled");
    }

    private synchronized void cancelReconnectTask() {
        if (reconnectTask != null && !reconnectTask.isDone()) {
            reconnectTask.cancel(true);
        }
        reconnectTask = null;
    }

    public void SendCommand(WSSCommand command) {
        if (command == null) return;

        String json = JsonHelper.Serialize(command);

        if (webSocket != null && connected) {
            webSocket.send(json);
            Log.d("SendingCommand", "Command sent: " + json);
        } else {
            PendingCommand pending = new PendingCommand(command);
            new Thread(() ->
                    DatabaseManager.getDatabase().pendingCommandDao().insertCommand(pending)
            ).start();
        }
    }

    public synchronized void Close() {
        Log.d("Connection", "Close() called");

        reconnectAllowed = false;
        connected = false;

        cancelReconnectTask();
        manualCloseInProgress = true;
        closeWebSocketInternal(false);
    }
    public synchronized void dispose() {
        Log.d("Connection", "dispose() called");

        reconnectAllowed = false;
        connected = false;

        cancelReconnectTask();
        manualCloseInProgress = true;
        closeWebSocketInternal(true);

        scheduler.shutdownNow();
    }

    private synchronized void closeWebSocketInternal(boolean clearListeners) {
        if (webSocket != null) {
            try {
                webSocket.close(1000, "Manual close");
            } catch (Exception ignored) {}
        }
        webSocket = null;

        if (clearListeners) {
            connectionEvents.clear();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void addConnectionEvent(OnConnectionEvents listener) {
        if (listener != null && !connectionEvents.contains(listener)) {
            connectionEvents.add(listener);
        }
    }

    public void removeConnectionEvent(OnConnectionEvents listener) {
        connectionEvents.remove(listener);
    }

    public void clearConnectionEvents() {
        connectionEvents.clear();
    }

    private void invokeOnOpen() {
        for (OnConnectionEvents e : connectionEvents) {
            try { e.OnOpen(); } catch (Exception ex) {
                Log.e("Connection", "Listener error in OnOpen", ex);
            }
        }
    }

    private void invokeOnCommandGot(WSSCommand command) {
        for (OnConnectionEvents e : connectionEvents) {
            try { e.OnCommandGot(command); } catch (Exception ex) {
                Log.e("Connection", "Listener error in OnCommandGot", ex);
            }
        }
    }

    private void invokeOnConnectionFailed() {
        for (OnConnectionEvents e : connectionEvents) {
            try { e.OnConnectionFailed(); } catch (Exception ex) {
                Log.e("Connection", "Listener error in OnConnectionFailed", ex);
            }
        }
    }
    private Request buildRequest(String token) {
        Request.Builder builder = new Request.Builder()
                .url(BuildConfig.SERVER_URL)
                .addHeader("device", Build.MODEL);

        if (token != null) builder.addHeader("token", token);

        return builder.build();
    }

    private OkHttpClient createUnsafeClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(2, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
