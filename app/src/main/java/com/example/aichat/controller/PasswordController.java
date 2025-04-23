package com.example.aichat.controller;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.example.aichat.R;
import com.google.android.material.textfield.TextInputLayout;

public class PasswordController {
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private boolean isPasswordValidFlag = false;
    private boolean isConfirmPasswordValidFlag = false;
    private Runnable onValidationChanged;

    public PasswordController(TextInputLayout passwordInputLayout,
                              TextInputLayout confirmPasswordInputLayout,
                              EditText passwordEditText,
                              EditText confirmPasswordEditText,
                              Runnable onValidationChanged) {
        this.passwordInputLayout = passwordInputLayout;
        this.confirmPasswordInputLayout = confirmPasswordInputLayout;
        this.passwordEditText = passwordEditText;
        this.confirmPasswordEditText = confirmPasswordEditText;
        this.onValidationChanged = onValidationChanged;

        setupPasswordListeners();
    }

    private void setupPasswordListeners() {
        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validatePassword();
            }
        });

        confirmPasswordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateConfirmPassword();
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordInputLayout.setError(null);
                isPasswordValidFlag = isPasswordValid(s.toString().trim());
                validateConfirmPassword();
                if (onValidationChanged != null) {
                    onValidationChanged.run();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        confirmPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                confirmPasswordInputLayout.setError(null);
                isConfirmPasswordValidFlag = s.toString().trim().equals(passwordEditText.getText().toString().trim());
                if (onValidationChanged != null) {
                    onValidationChanged.run();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public void validatePassword() {
        String password = passwordEditText.getText().toString().trim();
        if (!isPasswordValid(password)) {
            passwordInputLayout.setError(passwordInputLayout.getContext().getString(R.string.invalid_password_error));
            isPasswordValidFlag = false;
        } else {
            passwordInputLayout.setError(null);
            isPasswordValidFlag = true;
        }
        if (onValidationChanged != null) {
            onValidationChanged.run();
        }
    }

    public void validateConfirmPassword() {
        if (!confirmPasswordEditText.getText().toString().isEmpty()) {
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();
            if (!confirmPassword.equals(password)) {
                confirmPasswordInputLayout.setError(confirmPasswordInputLayout.getContext().getString(R.string.password_mismatch_error));
                isConfirmPasswordValidFlag = false;
            } else {
                confirmPasswordInputLayout.setError(null);
                isConfirmPasswordValidFlag = true;
            }
        }
        if (onValidationChanged != null) {
            onValidationChanged.run();
        }
    }

    public boolean isPasswordValid() {
        return isPasswordValidFlag;
    }

    public boolean isConfirmPasswordValid() {
        return isConfirmPasswordValidFlag;
    }

    private boolean isPasswordValid(String password) {
        if (password.length() < 8) {
            return false;
        }
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-zа-я])(?=.*[A-ZА-Я])(?=.*[@#$%^&+=!]).{8,}$";
        return password.matches(passwordPattern);
    }

    public String getPassword() {
        return passwordEditText.getText().toString().trim();
    }
}