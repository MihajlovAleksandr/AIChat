package com.example.aichat.controller;

import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import com.example.aichat.view.RegistrationActivity;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.TokenManager;
import com.google.android.material.textfield.TextInputLayout;

public class RegistrationController {

    private RegistrationActivity activity;
    private ConnectionManager connectionManager;

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button registrationButton;
    private boolean isEmailValidFlag = false;
    private boolean isPasswordValidFlag = false;
    private boolean isConfirmPasswordValidFlag = false;

    public RegistrationController(RegistrationActivity activity,
                                  TextInputLayout emailInputLayout,
                                  TextInputLayout passwordInputLayout,
                                  TextInputLayout confirmPasswordInputLayout,
                                  EditText emailEditText,
                                  EditText passwordEditText,
                                  EditText confirmPasswordEditText,
                                  Button registrationButton) {
        this.activity = activity;
        this.emailInputLayout = emailInputLayout;
        this.passwordInputLayout = passwordInputLayout;
        this.confirmPasswordInputLayout = confirmPasswordInputLayout;
        this.emailEditText = emailEditText;
        this.passwordEditText = passwordEditText;
        this.confirmPasswordEditText = confirmPasswordEditText;
        this.registrationButton = registrationButton;

        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        if (connectionManager == null) {
            ConnectionSingleton.getInstance().setConnectionManager(new ConnectionManager(TokenManager.getToken(activity)));
            connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        }

        setupConnectionCallbacks();
        setupFieldListeners();
        setupRegistrationButton();

        registrationButton.setEnabled(false);
    }
    private void setupConnectionCallbacks() {
        connectionManager.SetCommandGot(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                switch (command.getOperation()) {
                    case "EmailIsBusy":
                        activity.runOnUiThread(() -> emailInputLayout.setError("email уже используется..."));
                        break;
                    case "VerificationCodeSend":
                        ConnectionSingleton.getInstance().setConnectionManager(connectionManager);
                        Intent intent = new Intent(activity, com.example.aichat.view.VerifyEmailActivity.class);
                        activity.startActivity(intent);
                        activity.finish();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void OnConnectionFailed() {
            }

            @Override
            public void OnOpen() {
            }
        });
    }
    private void setupFieldListeners() {
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail();
                enableRegistrationButton();
            }
        });
        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validatePassword();
                enableRegistrationButton();
            }
        });
        confirmPasswordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateConfirmPassword();
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
                isEmailValidFlag = isEmailValid(s.toString().trim());
                enableRegistrationButton();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordInputLayout.setError(null);
                isPasswordValidFlag = isPasswordValid(s.toString().trim());
                validateConfirmPassword();
                enableRegistrationButton();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        confirmPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                confirmPasswordInputLayout.setError(null);
                isConfirmPasswordValidFlag = s.toString().trim().equals(passwordEditText.getText().toString().trim());
                enableRegistrationButton();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupRegistrationButton() {
        registrationButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            Command command = new Command("Registration");
            command.addData("email", email);
            command.addData("password", password);
            connectionManager.SendCommand(command);
        });
    }

    private void validateEmail() {
        String email = emailEditText.getText().toString().trim();
        if (!isEmailValid(email)) {
            emailInputLayout.setError("Пожалуйста, введите корректный email адрес");
            isEmailValidFlag = false;
        } else {
            emailInputLayout.setError(null);
            isEmailValidFlag = true;
        }
    }

    private void validatePassword() {
        String password = passwordEditText.getText().toString().trim();
        if (!isPasswordValid(password)) {
            passwordInputLayout.setError("Пароль должен содержать не менее 8 символов, включая заглавные и строчные буквы, цифры и специальные символы");
            isPasswordValidFlag = false;
        } else {
            passwordInputLayout.setError(null);
            isPasswordValidFlag = true;
        }
    }

    private void validateConfirmPassword() {
        if (!confirmPasswordEditText.getText().toString().isEmpty()) {
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();
            if (!confirmPassword.equals(password)) {
                confirmPasswordInputLayout.setError("Пароли не совпадают");
                isConfirmPasswordValidFlag = false;
            } else {
                confirmPasswordInputLayout.setError(null);
                isConfirmPasswordValidFlag = true;
            }
        }
    }

    private void enableRegistrationButton() {
        registrationButton.setEnabled(isEmailValidFlag && isPasswordValidFlag && isConfirmPasswordValidFlag);
    }

    private boolean isEmailValid(String email) {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,6}$";
        return !TextUtils.isEmpty(email) && email.matches(emailPattern);
    }

    private boolean isPasswordValid(String password) {
        if (password.length() < 8) {
            return false;
        }
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-zа-я])(?=.*[A-ZА-Я])(?=.*[@#$%^&+=!]).{8,}$";
        return password.matches(passwordPattern);
    }
}
