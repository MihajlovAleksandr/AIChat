package com.example.aichat;

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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText usernameEditText = findViewById(R.id.username);
        final EditText passwordEditText = findViewById(R.id.password);
        final Button loginButton = findViewById(R.id.login);
        final TextView usernameErrorText = findViewById(R.id.usernameError);
        final TextView passwordErrorText = findViewById(R.id.passwordError);
        final CheckBox showPasswordCheckBox = findViewById(R.id.showPassword);

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

        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (loginButton.isEnabled()) {
                        // Вход в систему
                    }
                }
                return false;
            }
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
