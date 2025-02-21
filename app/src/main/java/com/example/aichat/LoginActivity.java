package com.example.aichat;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.WriterException;

public class LoginActivity extends AppCompatActivity {

    private ConnectionManager connectionManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectionManager = new ConnectionManager(this);
        setContentView(R.layout.activity_login);

        final EditText usernameEditText = findViewById(R.id.username);
        final EditText passwordEditText = findViewById(R.id.password);
        final Button loginButton = findViewById(R.id.login);
        final TextView usernameErrorText = findViewById(R.id.usernameError);
        final TextView passwordErrorText = findViewById(R.id.passwordError);
        final CheckBox showPasswordCheckBox = findViewById(R.id.showPassword);
        final ImageView imageView = findViewById(R.id.imageView);
        try {
            imageView.setImageResource(R.drawable.loading);
            // Генерация QR-кода
            Bitmap qrCodeBitmap = QRCodeGenerator.generateQRCodeImage("Hello, World!", 300, 300);
            // Установка QR-кода в ImageView
            //imageView.setImageBitmap(qrCodeBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Command command = new Command("LoginIn");
                command.addData("username", usernameEditText.getText().toString().trim());
                command.addData("password", passwordEditText.getText().toString().trim());
                connectionManager.SendCommand(command);
            }
        });
        // Добавим слушатель изменения текста для username
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // игнорируем
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // игнорируем
            }

            @Override
            public void afterTextChanged(Editable s) {
                String username = usernameEditText.getText().toString().trim();
                if (isUsernameValid(username)) {
                    usernameErrorText.setVisibility(View.GONE);
                }
                enableLoginButton(usernameEditText, passwordEditText, loginButton);
            }
        });

        // Добавим слушатель изменения текста для password
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // игнорируем
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // игнорируем
            }

            @Override
            public void afterTextChanged(Editable s) {
                String password = passwordEditText.getText().toString().trim();
                if (isPasswordValid(password)) {
                    passwordErrorText.setVisibility(View.GONE);
                }
                enableLoginButton(usernameEditText, passwordEditText, loginButton);
            }
        });

        // Добавим слушатель ухода фокуса для username
        usernameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String username = usernameEditText.getText().toString().trim();
                    if (!isUsernameValid(username)) {
                        usernameErrorText.setText("Имя пользователя должно быть не менее 3 символов");
                        usernameErrorText.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        // Добавим слушатель ухода фокуса для password
        passwordEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String password = passwordEditText.getText().toString().trim();
                    if (!isPasswordValid(password)) {
                        passwordErrorText.setText("Пароль должен быть не менее 8 символов");
                        passwordErrorText.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        showPasswordCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                passwordEditText.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                passwordEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            passwordEditText.setSelection(passwordEditText.getText().length());
        });
    }

    private boolean isUsernameValid(String username) {
        return !TextUtils.isEmpty(username) && username.length() >= 3;
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 8;
    }

    private void enableLoginButton(EditText usernameEditText, EditText passwordEditText, Button loginButton) {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        loginButton.setEnabled(isUsernameValid(username) && isPasswordValid(password));
    }
}
