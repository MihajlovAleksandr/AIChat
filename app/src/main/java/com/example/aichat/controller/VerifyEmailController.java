package com.example.aichat.controller;

import android.content.Intent;
import android.widget.EditText;

import com.example.aichat.dto.request.VerificationCodeRequest;
import com.example.aichat.dto.response.VerificationCodeResponse;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.view.UserDataActivity;
import com.example.aichat.view.VerifyEmailActivity;

public class VerifyEmailController {

    private final EditText[] codeFields;
    private final ConnectionManager connectionManager;
    private final VerifyEmailActivity activity;
    private boolean lastValidationResult = false;

    private boolean isInitialized = false;

    public VerifyEmailController(VerifyEmailActivity activity, EditText[] codeFields) {
        this.activity = activity;
        this.codeFields = codeFields;

        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        if (connectionManager == null) {
            ConnectionSingleton.getInstance().setConnectionManager(new ConnectionManager(""));
        }
        connectionManager.clearConnectionEvents();
        connectionManager.addConnectionEvent(new OnConnectionEvents() {

            @Override
            public void OnCommandGot(WSSCommand command) {
                if ("VerificationCodeAnswer".equals(command.getOperation())) {
                    handleVerificationResponse(command);
                }
            }

            @Override public void OnConnectionFailed() {}
            @Override public void OnOpen() {}
        });
    }

    private void handleVerificationResponse(WSSCommand command) {
        VerificationCodeResponse response = command.getData(VerificationCodeResponse.class);
        if (response.answer == 1) {
            lastValidationResult = true;
            Intent intent = new Intent(activity, UserDataActivity.class);
            activity.startActivity(intent);
            activity.finish();
        } else {
            lastValidationResult = false;
            activity.runOnUiThread(activity::showCodeErrorAnimation);
        }
    }

    public boolean handleAfterTextChanged(CharSequence s, int currentIndex) {
        if (!isInitialized) return false;

        if (s.length() == 1 && currentIndex == codeFields.length - 1) {
            String fullCode = getFullCode();
            try {
                int code = Integer.parseInt(fullCode);
                WSSCommand command = new WSSCommand("VerificationCode", new VerificationCodeRequest(code));
                connectionManager.SendCommand(command);
            } catch (NumberFormatException e) {
                lastValidationResult = false;
            }
        }
        return lastValidationResult;
    }

    public boolean handleKeyEvent(int keyCode, int currentIndex) {
        if (!isInitialized) return false;
        if (keyCode == 67 && codeFields[currentIndex].getText().length() == 0 && currentIndex > 0) {
            codeFields[currentIndex - 1].requestFocus();
        }
        return false;
    }

    public void handlePaste() {
        if (!isInitialized) return;

        String clipboardText = getClipboardText();
        if (clipboardText != null) {
            int curIndex = 0;
            for (int i = 0; i < clipboardText.length() && curIndex < codeFields.length; i++) {
                char c = clipboardText.charAt(i);
                if (c == ' ') continue;
                codeFields[curIndex].setText(String.valueOf(c));
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
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) activity.getSystemService(activity.CLIPBOARD_SERVICE);

        if (clipboard != null && clipboard.hasPrimaryClip()) {
            android.content.ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            return item.getText().toString();
        }
        return null;
    }

    public void markInitialized() {
        this.isInitialized = true;
    }
}
