package com.example.aichat.controller;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.example.aichat.R;
import com.example.aichat.util.InputValidator;
import com.google.android.material.textfield.TextInputLayout;

public class PasswordController {
    private final TextInputLayout passwordInputLayout;
    private final TextInputLayout confirmPasswordInputLayout;
    private final EditText passwordEditText;
    private final EditText confirmPasswordEditText;
    private boolean isPasswordValidFlag = false;
    private boolean isConfirmPasswordValidFlag = false;
    private final Runnable onValidationChanged;

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
            if (!hasFocus) validatePassword();
        });

        confirmPasswordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateConfirmPassword(true);
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                isPasswordValidFlag = InputValidator.isPasswordValid(s.toString().trim());

                if (!passwordEditText.hasFocus() && s.length() > 0) {
                    passwordInputLayout.setError(isPasswordValidFlag ? null :
                            passwordInputLayout.getContext().getString(R.string.invalid_password_error));
                }

                validateConfirmPassword(false);

                if (onValidationChanged != null) onValidationChanged.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        confirmPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                isConfirmPasswordValidFlag = s.toString().trim().equals(passwordEditText.getText().toString().trim());

                if (!confirmPasswordEditText.hasFocus() && s.length() > 0) {
                    confirmPasswordInputLayout.setError(isConfirmPasswordValidFlag ? null :
                            confirmPasswordInputLayout.getContext().getString(R.string.password_mismatch_error));
                }

                if (onValidationChanged != null) onValidationChanged.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    public void validatePassword() {
        String password = passwordEditText.getText().toString().trim();
        isPasswordValidFlag = InputValidator.isPasswordValid(password);

        if (!isPasswordValidFlag) {
            passwordInputLayout.setError(passwordInputLayout.getContext().getString(R.string.invalid_password_error));
        } else {
            passwordInputLayout.setError(null);
        }

        validateConfirmPassword(false);

        if (onValidationChanged != null) onValidationChanged.run();
    }

    public void validateConfirmPassword(boolean showError) {
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        isConfirmPasswordValidFlag = confirmPassword.equals(password) && !confirmPassword.isEmpty();

        if (showError) {
            if (!isConfirmPasswordValidFlag) {
                confirmPasswordInputLayout.setError(
                        confirmPasswordInputLayout.getContext().getString(R.string.password_mismatch_error));
            } else {
                confirmPasswordInputLayout.setError(null);
            }
        }
    }

    public boolean isPasswordValid() {
        return isPasswordValidFlag;
    }

    public boolean isConfirmPasswordValid() {
        return isConfirmPasswordValidFlag;
    }

    public String getPassword() {
        return passwordEditText.getText().toString().trim();
    }
}