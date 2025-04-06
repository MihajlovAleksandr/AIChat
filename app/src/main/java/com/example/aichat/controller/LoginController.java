package com.example.aichat.controller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;

import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.view.LoginActivity;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.R;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.QRCodeGenerator;
import com.google.android.material.textfield.TextInputLayout;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class LoginController {

    private LoginActivity activity;
    private ConnectionManager connectionManager;

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ImageView imageView;

    private boolean isEmailValidFlag = false;
    private boolean isPasswordValidFlag = false;

    public LoginController(LoginActivity activity,
                           TextInputLayout emailInputLayout,
                           TextInputLayout passwordInputLayout,
                           EditText emailEditText,
                           EditText passwordEditText,
                           Button loginButton,
                           ImageView imageView) {
        this.activity = activity;
        this.emailInputLayout = emailInputLayout;
        this.passwordInputLayout = passwordInputLayout;
        this.emailEditText = emailEditText;
        this.passwordEditText = passwordEditText;
        this.loginButton = loginButton;
        this.imageView = imageView;

        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        if (connectionManager == null) {
            ConnectionSingleton.getInstance().setConnectionManager(new ConnectionManager(""));
            connectionManager = ConnectionSingleton.getInstance().getConnectionManager();

        } else {
            connectionManager.SendCommand(new Command("GetEntryToken"));
        }

        setupConnectionCallbacks();
        setupFieldListeners();
        setupLoginButton();
        loginButton.setEnabled(false);
    }

    private void setupConnectionCallbacks() {
        connectionManager.setConnectionEvent(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                switch (command.getOperation()) {
                    case "EntryToken":
                        String token = command.getData("token", String.class);
                        Log.d("Token", token);
                        Bitmap bitmap = QRCodeGenerator.generateQRCodeImage(token, 400, 400);
                        activity.runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                        break;
                    case "CreateToken":
                        String tokenCreated = command.getData("token", String.class);
                        SecurePreferencesManager.saveAuthToken(activity, tokenCreated);
                        break;
                    case "LoginIn":
                        ConnectionSingleton.getInstance().setConnectionManager(connectionManager);
                        int userId = command.getData("userId", int.class);
                        SecurePreferencesManager.saveUserId(activity, userId);
                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.putExtra("userId", userId);
                        activity.startActivity(intent);
                        activity.finish();
                        break;
                }
            }

            @Override
            public void OnConnectionFailed() {
                activity.runOnUiThread(() -> imageView.setImageResource(R.drawable.loading));
            }

            @Override
            public void OnOpen() {
                connectionManager.SendCommand(new Command("GetEntryToken"));
            }
        });
    }

    private void setupFieldListeners() {
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail();
            }
        });

        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validatePassword();
            }
        });

        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String email = emailEditText.getText().toString().trim();
                if (isEmailValid(email)) {
                    emailInputLayout.setError(null);
                    isEmailValidFlag = true;
                } else {
                    isEmailValidFlag = false;
                }
                enableLoginButton();
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String password = passwordEditText.getText().toString().trim();
                if (isPasswordValid(password)) {
                    passwordInputLayout.setError(null);
                    isPasswordValidFlag = true;
                } else {
                    isPasswordValidFlag = false;
                }
                enableLoginButton();
            }
        });
    }

    private void setupLoginButton() {
        loginButton.setOnClickListener(v -> {
            Command command = new Command("LoginIn");
            command.addData("email", emailEditText.getText().toString().trim());
            command.addData("password", passwordEditText.getText().toString().trim());
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
        enableLoginButton();
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
        enableLoginButton();
    }

    private void enableLoginButton() {
        loginButton.setEnabled(isEmailValidFlag && isPasswordValidFlag);
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
