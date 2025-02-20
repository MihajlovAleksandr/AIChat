package com.example.aichat;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
    private final OkHttpClient client;
    private boolean Connected = false;
    private final Request request;
    private final WebSocketListener webSocketListener;
    private long lastInitializeTime = 0;
    private static final long RECONNECT_INTERVAL_MS = 1000;
    private final List<Command> unsendedCommands = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ConnectionManager(Context context) {
        request = getRequest();
        webSocketListener = new WebSocketListener(){
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Connected = true;
                for(Command c : unsendedCommands)
                {
                    SendCommand(c);
                }
                unsendedCommands.clear();
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                Connected = false;
                Log.d("connection", "close");
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                Connected = false;
                Log.d("connection", "retry");
                retryInitialize();
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Command command = JsonHelper.Deserialize(text, Command.class);
                Log.d("Command", command.toString());
            }
        };
        client = getUnsafeOkHttpClient();
        Initialize();
    }


    private void Initialize() {
        Log.d("Connection", "Initializing ConnectionManager");
        webSocket = client.newWebSocket(request, webSocketListener);
        Log.d("Connection", "WebSocket initialized");
        lastInitializeTime = System.currentTimeMillis();
    }

    private Request getRequest() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwianRpIjoiMzkyYTI4ODAtOGFkMy00N2NlLTgzZTktOWM2ODU2NTkwMDI1IiwiaWF0IjoxNzQwMDA5Mzc4LCJleHAiOjE3NDI2MDEzNzgsImlzcyI6ImFpY2hhdCIsImF1ZCI6ImFpY2hhdCJ9.2I2EaDB7mmkXeShLLvH2AkPcQ5SeZVJRtA2oGDWX7RI";
        String URL = "wss://192.168.165.151:8888/";
        if (token != null) {
            return new Request.Builder()
                    .url(URL)
                    .addHeader("token", token)
                    .build();
        } else {
            return new Request.Builder()
                    .url(URL)
                    .build();
        }
    }


    private void retryInitialize() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInitializeTime >= RECONNECT_INTERVAL_MS) {
            Initialize();
        } else {
            Log.d("Connection", "Retrying too soon, waiting...");
            // Schedule to retry after the remaining time to complete 1 second
            long delay = RECONNECT_INTERVAL_MS - (currentTime - lastInitializeTime);
            scheduler.schedule(this::Initialize, delay, TimeUnit.MILLISECONDS);
        }
    }

    public void SendCommand(Command command) {
        String commandString = JsonHelper.Serialize(command);
        if (webSocket != null && !commandString.isEmpty() && Connected) {
            webSocket.send(commandString);
            Log.d("SendingCommand", "Command sent: " + commandString);
        }
        else{
            unsendedCommands.add(command);
        }
    }

    public void Close() {
        if (webSocket != null) {
            webSocket.close(1000, "Приложение закрывается");
            Log.d("Connection", "WebSocket closed");
            webSocket = null;
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            Log.d("Connection", "OkHttpClient shutdown");
        }
    }

    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            builder.connectTimeout(1, TimeUnit.SECONDS);
            builder.readTimeout(10, TimeUnit.SECONDS);
            builder.writeTimeout(10, TimeUnit.SECONDS);

            return builder.build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getUserId() {
        int userId = 1;
        return userId;
    }
}
