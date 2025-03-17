package com.example.aichat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
        TextView registerTextView = findViewById(R.id.registerTextView);
        String text = "Еще нет аккаунта? Зарегистрироваться";
        SpannableString spannableString = new SpannableString(text);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Singleton.getInstance().setConnectionManager(connectionManager);
                Intent intent = new Intent(LoginActivity.this, RegistrationFirstActivity.class);
                startActivity(intent);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(ContextCompat.getColor(LoginActivity.this, R.color.link_color));
                ds.setUnderlineText(true);
            }
        };

        int startIndex = text.indexOf("Зарегистрироваться");
        int endIndex = startIndex + "Зарегистрироваться".length();
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        registerTextView.setText(spannableString);
        registerTextView.setMovementMethod(LinkMovementMethod.getInstance());
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);
        imageView = findViewById(R.id.imageView);
        connectionManager = Singleton.getInstance().getConnectionManager();
        if(connectionManager == null) {
            connectionManager = new ConnectionManager(TokenManager.getToken(this));
        }
        else{
            connectionManager.SendCommand(new Command("GetEntryToken"));
        }
        connectionManager.SetCommandGot(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                switch (command.getOperation()) {
                    case "EntryToken":
                        Log.d("Token", command.getData("token", String.class));
                        Bitmap bitmap = QRCodeGenerator.generateQRCodeImage(command.getData("token", String.class), 400, 400);
                        runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                        break;
                    case "CreateToken":
                        String token = command.getData("token", String.class);
                        TokenManager.saveToken(LoginActivity.this, token);
                        break;
                    case "LoginIn":
                        Singleton.getInstance().setConnectionManager(connectionManager);
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("userId", command.getData("userId", int.class));
                        startActivity(intent);
                        break;
                }
            }
            @Override
            public void OnConnectionFailed() {
                runOnUiThread(() -> imageView.setImageResource(R.drawable.loading));
            }

            @Override
            public void OnOpen() {
                connectionManager.SendCommand(new Command("GetEntryToken"));
            }
        });

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
                    enableLoginButton();
                } else {
                    isEmailValidFlag = false;
                    enableLoginButton();
                }
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
                    enableLoginButton();
                } else {
                    isPasswordValidFlag = false;
                    enableLoginButton();
                }
            }
        });

        loginButton.setOnClickListener(v -> {
            Command command = new Command("LoginIn");
            command.addData("email", emailEditText.getText().toString().trim());
            command.addData("password", passwordEditText.getText().toString().trim());
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