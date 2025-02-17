package com.example.aichat;

import android.util.Log;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.core.type.TypeReference;

import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class ConnectionManager {
    private static final String TAG = "MessengerClient";
    private WebSocket webSocket;
    private OkHttpClient client;
    private int userId = 1;

    public ConnectionManager() {
        Log.d(TAG, "Initializing ConnectionManager");
        client = getUnsafeOkHttpClient();
        Request request = new Request.Builder()
                .url("wss://192.168.100.11:8888/")
                .addHeader("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwianRpIjoiNzkwN2NlMDctNjhmMy00YTA3LTk3NzYtMzFkMzliYjk1NjI2IiwiaWF0IjoxNzM5Mzk4Mjk0LCJleHAiOjE3NDE5OTAyOTQsImlzcyI6ImFpY2hhdCIsImF1ZCI6ImFpY2hhdCJ9.BD60ilqteAO2YvFGAl8GiNpNE3u7loyfB8j63mKa1x0")
                .build();
        EchoWebSocketListener webSocketListener = new EchoWebSocketListener();
        webSocketListener.addGetCommandEvent(new GetCommandEventListener() {
            @Override
            public void onCommandGot(GetCommandEvent event) {
                Message msg = event.getCommand().getData("message", Message.class);
                Log.d("commanndGot", JsonHelper.Serialize(msg));
                Log.d("commanndGot", msg.getTime().toString());

            }
        });
        webSocket = client.newWebSocket(request, webSocketListener);
        Log.d(TAG, "WebSocket initialized");
    }

    public void SendCommand(Command command) {
        String commandString = JsonHelper.Serialize(command);
        if (webSocket != null && !commandString.isEmpty()) {
            webSocket.send(commandString);
            Log.d(TAG, "Command sent: " + commandString);
        }
    }

    public void Close() {
        if (webSocket != null) {
            webSocket.close(1000, "Приложение закрывается");
            Log.d(TAG, "WebSocket closed");
            webSocket = null;
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            Log.d(TAG, "OkHttpClient shutdown");
        }
    }

    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException { }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException { }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{ };
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            builder.connectTimeout(10, TimeUnit.SECONDS);
            builder.readTimeout(10, TimeUnit.SECONDS);
            builder.writeTimeout(10, TimeUnit.SECONDS);

            return builder.build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public int getUserId(){
        return userId;
    }
}
