package com.example.aichat.view.helpers;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

public class LoginTextWatcher implements TextWatcher {

    private final EditText emailEditText;
    private final EditText passwordEditText;
    private final Button loginButton;

    public LoginTextWatcher(EditText emailEditText, EditText passwordEditText, Button loginButton) {
        this.emailEditText = emailEditText;
        this.passwordEditText = passwordEditText;
        this.loginButton = loginButton;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        checkFields();
    }

    @Override
    public void afterTextChanged(Editable s) { }

    private void checkFields() {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString().trim() : "";

        boolean isPasswordValid = password.length() >= 8 && password.matches(".*\\d.*");
        loginButton.setEnabled(!email.isEmpty() && isPasswordValid);
    }
}
