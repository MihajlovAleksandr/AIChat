package com.example.aichat.controller;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.aichat.dto.request.RegistrationRequest;
import com.example.aichat.dto.response.ApiError;
import com.example.aichat.dto.response.RegisterResponse;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.TokenStorage;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.R;
import com.example.aichat.view.helpers.InputValidator;
import com.example.aichat.view.PreferenceActivity;
import com.example.aichat.view.RegistrationActivity;
import com.example.aichat.view.UserDataActivity;
import com.example.aichat.view.VerifyEmailActivity;
import com.google.android.material.textfield.TextInputLayout;
import java.util.concurrent.CompletableFuture;

public class RegistrationController {

    private static final String TAG = "Registration";

    private final RegistrationActivity activity;
    private final PasswordController passwordController;

    private final TextInputLayout emailInputLayout;
    private final EditText emailEditText;
    private final Button registrationButton;
    private final LoadingDialogController loadingDialog;

    private boolean isEmailValidFlag = false;
    private boolean destroyed = false;

    private final ConnectionDispatcher dispatcher;
    private CompletableFuture<?> registrationFuture;

    public RegistrationController(
            RegistrationActivity activity,
            TextInputLayout emailInputLayout,
            TextInputLayout passwordInputLayout,
            TextInputLayout confirmPasswordInputLayout,
            EditText emailEditText,
            EditText passwordEditText,
            EditText confirmPasswordEditText,
            Button registrationButton
    ) {
        this.activity = activity;
        this.emailInputLayout = emailInputLayout;
        this.emailEditText = emailEditText;
        this.registrationButton = registrationButton;
        this.loadingDialog = new LoadingDialogController(activity);

        this.passwordController = new PasswordController(
                passwordInputLayout,
                confirmPasswordInputLayout,
                passwordEditText,
                confirmPasswordEditText,
                this::enableRegistrationButton
        );

        TokenStorage tokenStorage = new TokenStorage(activity);
        tokenStorage.saveToken(null);

        dispatcher = new ConnectionDispatcher(tokenStorage);

        setupEmailListener();
        setupRegistrationButton();

        registrationButton.setEnabled(false);
    }

    private void setupEmailListener() {
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail();
                enableRegistrationButton();
            }
        });

        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                emailInputLayout.setError(null);

                String email = s != null ? s.toString().trim() : "";

                isEmailValidFlag = InputValidator.isEmailValid(email);

                enableRegistrationButton();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupRegistrationButton() {
        registrationButton.setOnClickListener(v -> {
            if (isRegistrationInProgress()) {
                return;
            }

            String email = emailEditText.getText() != null
                    ? emailEditText.getText().toString().trim()
                    : "";

            if (!InputValidator.isEmailValid(email)) {
                emailInputLayout.setError(activity.getString(R.string.invalid_email_error));
                isEmailValidFlag = false;
                enableRegistrationButton();
                return;
            }

            RegistrationRequest request = new RegistrationRequest(
                    email,
                    passwordController.getPassword(),
                    "EMAIL_PASSWORD"
            );

            startRegistrationLoading();

            registrationFuture = dispatcher.sendHttpRequestAsync(
                            "/api/auth/register",
                            HttpClient.HTTPMethod.POST,
                            request,
                            false
                    )
                    .thenAccept(cmd -> activity.runOnUiThread(() -> {
                        if (!isActivityUsable()) {
                            return;
                        }

                        loadingDialog.dismiss();
                        registrationFuture = null;

                        if (cmd == null) {
                            enableRegistrationButton();

                            Toast.makeText(
                                    activity,
                                    "Сервер не вернул ответ",
                                    Toast.LENGTH_SHORT
                            ).show();

                            Log.e(TAG, "Registration command is null");
                            return;
                        }

                        if (cmd.isSuccess()) {
                            handleSuccess(cmd.getData(RegisterResponse.class));
                        } else {
                            enableRegistrationButton();
                            handleError(cmd.getData(ApiError.class), cmd.getCode());
                        }
                    }))
                    .exceptionally(throwable -> {
                        activity.runOnUiThread(() -> {
                            if (!isActivityUsable()) {
                                return;
                            }

                            loadingDialog.dismiss();
                            registrationFuture = null;
                            enableRegistrationButton();

                            Toast.makeText(
                                    activity,
                                    "Ошибка регистрации. Проверьте подключение",
                                    Toast.LENGTH_SHORT
                            ).show();

                            Log.e(TAG, "Registration request failed", throwable);
                        });

                        return null;
                    });
        });
    }

    private void startRegistrationLoading() {
        registrationButton.setEnabled(false);

        loadingDialog.show(
                "Регистрация",
                "Пожалуйста, подождите. Создаём аккаунт..."
        );
    }

    private boolean isRegistrationInProgress() {
        return registrationFuture != null && !registrationFuture.isDone();
    }

    private boolean isActivityUsable() {
        return !destroyed
                && !activity.isFinishing()
                && !activity.isDestroyed();
    }

    private void handleSuccess(RegisterResponse response) {
        if (response == null) {
            enableRegistrationButton();

            Toast.makeText(
                    activity,
                    "Некорректный ответ сервера",
                    Toast.LENGTH_SHORT
            ).show();

            Log.e(TAG, "RegisterResponse is null");
            return;
        }

        if (response.state == null) {
            enableRegistrationButton();

            Toast.makeText(
                    activity,
                    "Сервер не вернул состояние регистрации",
                    Toast.LENGTH_SHORT
            ).show();

            Log.e(TAG, "Registration state is null");
            return;
        }

        if (response.token == null || response.token.trim().isEmpty()) {
            enableRegistrationButton();

            Toast.makeText(
                    activity,
                    "Сервер не вернул токен авторизации",
                    Toast.LENGTH_SHORT
            ).show();

            Log.e(TAG, "Registration token is null or empty");
            return;
        }

        Class<?> type;

        switch (response.state) {
            case CREATED:
                type = VerifyEmailActivity.class;
                break;

            case EMAIL_VERIFIED:
                type = UserDataActivity.class;
                break;

            case USER_DATA_COMPLETED:
                type = PreferenceActivity.class;
                break;

            default:
                enableRegistrationButton();

                Toast.makeText(
                        activity,
                        "Неизвестное состояние регистрации",
                        Toast.LENGTH_SHORT
                ).show();

                Log.e(TAG, "Unknown registration state: " + response.state);
                return;
        }

        saveTokenEverywhere(response.token);

        Intent intent = new Intent(activity, type);
        activity.startActivityClean(intent, true);
    }

    private void saveTokenEverywhere(String token) {
        SecurePreferencesManager.saveAuthToken(activity, token);

        dispatcher.setToken(token);

        try {
            ConnectionDispatcher singletonDispatcher =
                    ConnectionSingleton.getInstance().getConnectionDispatcher();

            if (singletonDispatcher != null) {
                singletonDispatcher.setToken(token);
            }

            Log.d(TAG, "Registration token saved to local and singleton dispatchers");

        } catch (Exception e) {
            Log.w(TAG, "ConnectionSingleton dispatcher is not ready, token saved only locally", e);
        }
    }

    private void handleError(ApiError error, int code) {
        String message = "Ошибка регистрации";

        if (error != null) {
            message = error.toString();
            Log.e(TAG, message);
        } else {
            Log.e(TAG, "Registration failed. Code: " + code);
        }

        Toast.makeText(
                activity,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void validateEmail() {
        String email = emailEditText.getText() != null
                ? emailEditText.getText().toString().trim()
                : "";

        if (!InputValidator.isEmailValid(email)) {
            emailInputLayout.setError(activity.getString(R.string.invalid_email_error));
            isEmailValidFlag = false;
        } else {
            emailInputLayout.setError(null);
            isEmailValidFlag = true;
        }
    }

    private void enableRegistrationButton() {
        registrationButton.setEnabled(
                !isRegistrationInProgress()
                        && isEmailValidFlag
                        && passwordController.isPasswordValid()
                        && passwordController.isConfirmPasswordValid()
        );
    }

    public void destroy() {
        destroyed = true;
        registrationFuture = null;
        loadingDialog.dismiss();
    }
}
