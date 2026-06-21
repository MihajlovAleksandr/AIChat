package com.example.aichat.controller;

import android.content.Intent;
import android.util.Log;
import android.widget.EditText;
import com.example.aichat.dto.request.VerificationCodeRequest;
import com.example.aichat.dto.response.ApiError;
import com.example.aichat.dto.response.RegisterResponse;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.entities.RegistrationState;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.view.UserDataActivity;
import com.example.aichat.view.VerifyEmailActivity;

public class VerifyEmailController {

    private static final String TAG = "EmailVerification";

    private final EditText[] codeFields;
    private final VerifyEmailActivity activity;
    private final ConnectionDispatcher dispatcher;

    public VerifyEmailController(VerifyEmailActivity activity, EditText[] codeFields) {
        this.activity = activity;
        this.codeFields = codeFields;
        this.dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
    }

    public void verifyCode() {
        String fullCode = activity.getCurrentCodeFromFields();

        try {
            Integer.parseInt(fullCode);
        } catch (NumberFormatException e) {
            activity.showCodeErrorAnimation();
            return;
        }

        String registrationToken = SecurePreferencesManager.getAuthToken(activity);

        if (registrationToken == null || registrationToken.trim().isEmpty()) {
            Log.e(TAG, "Registration token is empty before email verification");
            activity.showCodeErrorAnimation();
            return;
        }

        dispatcher.setToken(registrationToken);

        VerificationCodeRequest request = new VerificationCodeRequest(fullCode);

        dispatcher.sendHttpRequestAsync(
                "/api/auth/register/verify",
                HttpClient.HTTPMethod.POST,
                request,
                false
        ).thenAccept(cmd -> activity.runOnUiThread(() -> {
            if (cmd != null && cmd.isSuccess()) {
                RegisterResponse response = cmd.getData(RegisterResponse.class);

                if (response != null
                        && response.state == RegistrationState.EMAIL_VERIFIED
                        && response.token != null
                        && !response.token.trim().isEmpty()) {

                    activity.showCodeSuccessAnimation();

                    SecurePreferencesManager.saveAuthToken(activity, response.token);
                    dispatcher.setToken(response.token);

                    Intent intent = new Intent(activity, UserDataActivity.class);
                    activity.startActivity(intent);
                    activity.finish();

                } else {
                    Log.e(TAG, "Invalid verification response");
                    activity.showCodeErrorAnimation();
                }

            } else {
                if (cmd != null) {
                    ApiError error = cmd.getData(ApiError.class);

                    if (error != null) {
                        Log.e(TAG, error.toString());
                    } else {
                        Log.e(TAG, "Email verification failed. Code: " + cmd.getCode());
                    }
                } else {
                    Log.e(TAG, "Email verification command is null");
                }

                activity.showCodeErrorAnimation();
            }
        })).exceptionally(throwable -> {
            activity.runOnUiThread(activity::showCodeErrorAnimation);
            Log.e(TAG, "Ошибка проверки кода", throwable);
            return null;
        });
    }

    public boolean handleKeyEvent(int keyCode, int currentIndex) {
        if (keyCode == 67
                && codeFields[currentIndex].getText().length() == 0
                && currentIndex > 0) {

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

                if (c == ' ') {
                    continue;
                }

                codeFields[curIndex].setText(String.valueOf(c));
                curIndex++;
            }
        }
    }

    private String getClipboardText() {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) activity.getSystemService(VerifyEmailActivity.CLIPBOARD_SERVICE);

        if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
            android.content.ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);

            if (item != null && item.getText() != null) {
                return item.getText().toString();
            }
        }

        return null;
    }
}
