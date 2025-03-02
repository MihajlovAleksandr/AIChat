package com.example.aichat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    private EditText messageEditText;
    private ConnectionManager connectionManager;
    private int userId;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messageEditText = findViewById(R.id.tiMessage);
        Button sendButton = findViewById(R.id.bSendMessage);
        sendButton.setOnClickListener(v -> {
            Message message = new Message(Build.MODEL, 1, 1);
            Command command = new Command("SendMessage");
            command.addData("message", message);
            connectionManager.SendCommand(command);
        });
        connectionManager = Singleton.getInstance().getConnectionManager();
        if(connectionManager == null) {
            connectionManager = new ConnectionManager(TokenManager.getToken(this));
        }
        connectionManager.SetCommandGot(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                Log.d("CommandToMaiActivity", command.toString());
            }
            @Override
            public void OnConnectionFailed() {
                //Ignore
            }

            @Override
            public void OnOpen() {
                //Ignore
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectionManager.Close();
    }
}
