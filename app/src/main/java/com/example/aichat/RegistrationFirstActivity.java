package com.example.aichat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

public class RegistrationFirstActivity extends AppCompatActivity {
    private ConnectionManager connectionManager;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button loginButton;
    private boolean isEmailValidFlag = false;
    private boolean isPasswordValidFlag = false;
    private boolean isConfirmPasswordValidFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TokenManager.removeToken(this);
        setContentView(R.layout.activity_registration_first);
        connectionManager = Singleton.getInstance().getConnectionManager();
        if(connectionManager == null) {
            connectionManager = new ConnectionManager(TokenManager.getToken(this));
        }
        connectionManager.SetCommandGot(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                switch (command.getOperation()) {
                    case "EmailIsBusy":
                        runOnUiThread(()->{
                            emailInputLayout.setError("email уже используется...");
                        });
                        break;
                    case "VerificationCodeSend":
                        Singleton.getInstance().setConnectionManager(connectionManager);
                        Intent intent = new Intent(RegistrationFirstActivity.this, VerifyEmailActivity.class);
                        startActivity(intent);
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

        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.confirmPassword);
        loginButton = findViewById(R.id.login);

        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail();
                enableLoginButton();
            }
        });

        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validatePassword();
                enableLoginButton();
            }
        });

        confirmPasswordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateConfirmPassword();
                enableLoginButton();
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
                enableLoginButton();
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
                enableLoginButton();
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
                enableLoginButton();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            Command command = new Command("Registration");
            command.addData("email", email);
            command.addData("password", password);
            connectionManager.SendCommand(command);
        });

        loginButton.setEnabled(false);
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

    private void enableLoginButton() {
        loginButton.setEnabled(isEmailValidFlag && isPasswordValidFlag && isConfirmPasswordValidFlag);
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