package com.example.aichat;

import android.icu.util.TimeZone;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;




public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MessengerClient";
    private EditText messageEditText;
    private Button sendButton;
    private ConnectionManager connectionManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String token = TokenManager.getToken(this);
        if(token != null)
            Log.d("Token", token);
        setContentView(R.layout.activity_main);
        messageEditText = findViewById(R.id.tiMessage);
        sendButton = findViewById(R.id.bSendMessage);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = new Message(messageEditText.getText().toString(), connectionManager.getUserId(), 1);
                Command command = new Command("SendMessage");
                command.addData("message", message);
                connectionManager.SendCommand(command);
            }
        });
        connectionManager = new ConnectionManager(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectionManager.Close();
    }
}
