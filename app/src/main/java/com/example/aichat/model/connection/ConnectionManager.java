package com.example.aichat.model.connection;
import android.os.Build;
import android.util.Log;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.aichat.BuildConfig;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.utils.JsonHelper;
import com.example.aichat.model.entities.PendingCommand;
import com.example.aichat.model.database.DatabaseManager;

import java.security.cert.CertificateException;
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
    private List<PendingCommand> unsendedCommands;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private OnConnectionEvents connectionEvent;
    public ConnectionManager(String token) {
        request = getRequest(token);
        new Thread(() -> unsendedCommands = DatabaseManager.getDatabase().pendingCommandDao().getAllCommands() ).start();
        webSocketListener = new WebSocketListener(){
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Connected = true;
                new Thread(() -> connectionEvent.OnOpen()).start();
                for(PendingCommand c : unsendedCommands)
                {
                    SendCommand(c.getCommandFormat());
                    new Thread(() -> DatabaseManager.getDatabase().pendingCommandDao().deleteCommand(c));
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
                new Thread(() -> connectionEvent.OnConnectionFailed()).start();
                retryInitialize();
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d("Command", text);
                Command command = JsonHelper.Deserialize(text, Command.class);
                if (command == null) throw new AssertionError();
                Log.d("Command", command.toString());
                new Thread(() -> connectionEvent.OnCommandGot(command)).start();
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

    private Request getRequest(String token) {
        String URL = BuildConfig.SERVER_URL;

        if (token != null) {
            return new Request.Builder()
                    .url(URL)
                    .addHeader("token", token)
                    .addHeader("device", Build.MODEL)
                    .build();
        } else {
            return new Request.Builder()
                    .url(URL)
                    .addHeader("device", Build.MODEL)
                    .build();
        }
    }


    private void retryInitialize() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInitializeTime >= RECONNECT_INTERVAL_MS) {
            Initialize();
        } else {
            Log.d("Connection", "Retrying too soon, waiting...");
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
            PendingCommand pendingCommand = new PendingCommand(command);
            unsendedCommands.add(pendingCommand);
            new Thread(() -> DatabaseManager.getDatabase().pendingCommandDao().insertCommand(pendingCommand));
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
    public void SetCommandGot(OnConnectionEvents listener){
        connectionEvent = listener;
    }
}
