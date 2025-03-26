package com.example.aichat.controller;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.util.Log;
import android.widget.EditText;

import com.example.aichat.view.UserDataActivity;
import com.example.aichat.view.VerifyEmailActivity;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.TokenManager;

public class VerifyEmailController {

    private EditText[] codeFields;
    private ConnectionManager connectionManager;
    private VerifyEmailActivity activity;

    public VerifyEmailController(VerifyEmailActivity activity, EditText[] codeFields) {
        this.activity = activity;
        this.codeFields = codeFields;

        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        if (connectionManager == null) {
            ConnectionSingleton.getInstance().setConnectionManager(new ConnectionManager(TokenManager.getToken(activity)));
            connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        }

        connectionManager.SetCommandGot(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                if (command.getOperation().equals("VerificationCodeAnswer")) {
                    handleVerificationResponse(command);
                }
            }

            @Override
            public void OnConnectionFailed() {
                Log.e("VerifyEmailController", "Connection failed");
            }

            @Override
            public void OnOpen() {
                Log.d("VerifyEmailController", "Connection opened");
            }
        });
    }

    private void handleVerificationResponse(Command command) {
        if (command.getData("answer", int.class) == 1) {
            ConnectionSingleton.getInstance().setConnectionManager(connectionManager);
            Intent intent = new Intent(activity, UserDataActivity.class);
            activity.startActivity(intent);
            activity.finish();
        } else {
            new Thread(() -> changeColorForEditText(50, Color.RED)).start();
        }
    }

    public void changeColorForEditText(int delay, int color) {
        for (EditText editText : codeFields) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            activity.runOnUiThread(() -> {
                GradientDrawable drawable = (GradientDrawable) editText.getBackground();
                drawable.setStroke(5, color);
            });
        }
    }

    public void handleTextChanged(CharSequence s, int currentIndex) {
        if (s.length() == 1 && currentIndex < codeFields.length - 1) {
            codeFields[currentIndex + 1].requestFocus();
        } else if (s.length() == 0 && currentIndex > 0) {
            codeFields[currentIndex - 1].requestFocus();
        }
    }

    public void handleAfterTextChanged(Editable s, int currentIndex) {
        if (s.length() == 1 && currentIndex == codeFields.length - 1) {
            String fullCode = getFullCode();
            Log.d("VerifyEmailController", "Full code: " + fullCode);
            Command command = new Command("VerificationCode");
            command.addData("code", fullCode);
            connectionManager.SendCommand(command);
        }
    }

    public boolean handleKeyEvent(int keyCode, int currentIndex) {
        if (keyCode == 67 && codeFields[currentIndex].getText().length() == 0 && currentIndex > 0) {
            codeFields[currentIndex - 1].requestFocus();
        }
        return false;
    }

    public void handlePaste() {
        String clipboardText = getClipboardText();
        if (clipboardText != null && clipboardText.length() == 6) {
            for (int i = 0; i < 6; i++) {
                codeFields[i].setText(String.valueOf(clipboardText.charAt(i)));
            }
        } else if (clipboardText != null && clipboardText.length() == 7 && clipboardText.indexOf(' ') == 3) {
            int curIndex = 0;
            for (int i = 0; i < 7; i++) {
                if (i == 3) continue;
                codeFields[curIndex].setText(String.valueOf(clipboardText.charAt(i)));
                curIndex++;
            }
        }
    }

    private String getFullCode() {
        StringBuilder code = new StringBuilder();
        for (EditText field : codeFields) {
            code.append(field.getText().toString());
        }
        return code.toString();
    }

    private String getClipboardText() {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) activity.getSystemService(activity.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            android.content.ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            return item.getText().toString();
        }
        return null;
    }
}
