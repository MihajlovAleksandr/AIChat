package com.example.aichat;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private ConnectionManager connectionManager;

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ImageView imageView;

    private boolean isEmailValidFlag = false;
    private boolean isPasswordValidFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Инициализация элементов интерфейса
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);
        imageView = findViewById(R.id.imageView);

        // Инициализация ConnectionManager
        connectionManager = new ConnectionManager(this);
        connectionManager.SetCommandGot(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                switch (command.getOperation()) {
                    case "EntryToken":
                        Log.d("Token", command.getData("token", String.class));
                        Bitmap bitmap = QRCodeGenerator.generateQRCodeImage(command.getData("token", String.class), 400, 400);
                        runOnUiThread(() -> {
                            imageView.setImageBitmap(bitmap);
                        });
                        break;
                }
            }

            @Override
            public void OnConnectionFailed() {
                runOnUiThread(() -> {
                    imageView.setImageResource(R.drawable.loading);
                });
            }

            @Override
            public void OnOpen() {
                connectionManager.SendCommand(new Command("GetEntryToken"));
            }
        });

        // Установка слушателей потери фокуса для полей ввода
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

        // Добавление TextWatcher для поля email
        emailEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Не требуется
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Не требуется
            }

            @Override
            public void afterTextChanged(Editable s) {
                String email = emailEditText.getText().toString().trim();
                if (emailInputLayout.getError() != null) {
                    if (isEmailValid(email)) {
                        emailInputLayout.setError(null);
                        isEmailValidFlag = true;
                        enableLoginButton();
                    } else {
                        isEmailValidFlag = false;
                        enableLoginButton();
                    }
                }
            }
        });

        // Добавление TextWatcher для поля пароля
        passwordEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Не требуется
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Не требуется
            }

            @Override
            public void afterTextChanged(Editable s) {
                String password = passwordEditText.getText().toString().trim();
                if (passwordInputLayout.getError() != null) {
                    if (isPasswordValid(password)) {
                        passwordInputLayout.setError(null);
                        isPasswordValidFlag = true;
                        enableLoginButton();
                    } else {
                        isPasswordValidFlag = false;
                        enableLoginButton();
                    }
                }
            }
        });

        // Кнопка входа
        loginButton.setOnClickListener(v -> {
            Command command = new Command("LoginIn");
            command.addData("email", emailEditText.getText().toString().trim());
            command.addData("password", passwordEditText.getText().toString().trim());
            connectionManager.SendCommand(command);
        });

        // Изначально кнопка отключена
        loginButton.setEnabled(false);
    }

    // Проверка валидности email
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

    // Проверка валидности пароля
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

    // Включение/отключение кнопки входа
    private void enableLoginButton() {
        loginButton.setEnabled(isEmailValidFlag && isPasswordValidFlag);
    }

    // Проверка email с помощью регулярного выражения
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
