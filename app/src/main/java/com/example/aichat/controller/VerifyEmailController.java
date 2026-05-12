package com.example.aichat.controller;

import android.content.Intent;
import android.util.Log;
import android.widget.EditText;

import com.example.aichat.dto.request.VerificationCodeRequest;
import com.example.aichat.dto.response.ApiError;
import com.example.aichat.dto.response.RegisterResponse;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.entities.RegistrationState;
import com.example.aichat.view.UserDataActivity;
import com.example.aichat.view.VerifyEmailActivity;

import java.util.Objects;

public class VerifyEmailController {

    private final EditText[] codeFields;
    private final VerifyEmailActivity activity;
    private boolean lastValidationResult = false;
    private final ConnectionDispatcher dispatcher;

    public VerifyEmailController(VerifyEmailActivity activity, EditText[] codeFields) {
        this.activity = activity;
        this.codeFields = codeFields;
        dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
    }
    public boolean handleAfterTextChanged(CharSequence s, int currentIndex) {
        if (s.length() == 1 && currentIndex == codeFields.length - 1) {
            String fullCode = getFullCode();
            try {
                Integer.parseInt(fullCode);
                VerificationCodeRequest request = new VerificationCodeRequest(fullCode);
                dispatcher.sendHttpRequestAsync("/api/auth/register/verify", HttpClient.HTTPMethod.POST, request, false)
                        .thenAccept((cmd)->{
                    if(cmd.isSuccess()) {
                        RegisterResponse response = cmd.getData(RegisterResponse.class);
                        if (Objects.requireNonNull(response.state) == RegistrationState.EMAIL_VERIFIED) {
                            SecurePreferencesManager.saveAuthToken(activity, response.token);
                            dispatcher.setToken(response.token);
                            Intent intent = new Intent(activity, UserDataActivity.class);
                            activity.startActivity(intent);
                            activity.finish();
                        } else {
                            throw new IllegalArgumentException();
                        }
                    }
                    else {
                        Log.e("EmailVerification", cmd.getData(ApiError.class).toString());
                    }
                });
                lastValidationResult = true;
            } catch (NumberFormatException e) {
                lastValidationResult = false;
            }
        }
        return lastValidationResult;
    }

    public boolean handleKeyEvent(int keyCode, int currentIndex) {
        if (keyCode == 67 && codeFields[currentIndex].getText().length() == 0 && currentIndex > 0) {
            codeFields[currentIndex - 1].requestFocus();
        }
        return false;
    }

    public void handlePaste() {
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
}
