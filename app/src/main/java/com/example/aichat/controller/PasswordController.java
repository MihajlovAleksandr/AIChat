package com.example.aichat.controller;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import com.example.aichat.R;
import com.example.aichat.view.helpers.InputValidator;
import com.google.android.material.textfield.TextInputLayout;

public class PasswordController {

    private final TextInputLayout passwordInputLayout;
    private final TextInputLayout confirmPasswordInputLayout;
    private final EditText passwordEditText;
    private final EditText confirmPasswordEditText;
    private final Runnable onValidationChanged;

    private boolean isPasswordValidFlag = false;
    private boolean isConfirmPasswordValidFlag = false;

    private boolean passwordErrorWasShown = false;
    private boolean confirmPasswordErrorWasShown = false;

    public PasswordController(
            TextInputLayout passwordInputLayout,
            TextInputLayout confirmPasswordInputLayout,
            EditText passwordEditText,
            EditText confirmPasswordEditText,
            Runnable onValidationChanged
    ) {
        this.passwordInputLayout = passwordInputLayout;
        this.confirmPasswordInputLayout = confirmPasswordInputLayout;
        this.passwordEditText = passwordEditText;
        this.confirmPasswordEditText = confirmPasswordEditText;
        this.onValidationChanged = onValidationChanged;

        setupPasswordListeners();

        validatePassword(false);
        validateConfirmPassword(false);
        notifyValidationChanged();
    }

    private void setupPasswordListeners() {
        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && hasPasswordText()) {
                passwordErrorWasShown = true;
            }

            validatePassword(passwordErrorWasShown);
            validateConfirmPassword(confirmPasswordErrorWasShown);
            notifyValidationChanged();
        });

        confirmPasswordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && hasConfirmPasswordText()) {
                confirmPasswordErrorWasShown = true;
            }

            validateConfirmPassword(confirmPasswordErrorWasShown);
            notifyValidationChanged();
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword(passwordErrorWasShown);
                validateConfirmPassword(confirmPasswordErrorWasShown);
                notifyValidationChanged();
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
                validateConfirmPassword(confirmPasswordErrorWasShown);
                notifyValidationChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public void validatePassword() {
        passwordErrorWasShown = true;
        validatePassword(true);
        validateConfirmPassword(confirmPasswordErrorWasShown);
        notifyValidationChanged();
    }

    private void validatePassword(boolean showError) {
        String password = getPassword();

        isPasswordValidFlag = InputValidator.isPasswordValid(password);

        if (password.isEmpty()) {
            passwordInputLayout.setError(null);
            passwordInputLayout.setErrorEnabled(false);
            passwordErrorWasShown = false;
            return;
        }

        if (isPasswordValidFlag) {
            passwordInputLayout.setError(null);
            passwordInputLayout.setErrorEnabled(false);
            return;
        }

        if (showError) {
            passwordInputLayout.setError(
                    passwordInputLayout.getContext().getString(R.string.invalid_password_error)
            );
            passwordInputLayout.setErrorEnabled(true);
        } else {
            passwordInputLayout.setError(null);
            passwordInputLayout.setErrorEnabled(false);
        }
    }

    public void validateConfirmPassword(boolean showError) {
        String password = getPassword();
        String confirmPassword = getConfirmPassword();

        isConfirmPasswordValidFlag =
                !confirmPassword.isEmpty()
                        && confirmPassword.equals(password);

        if (confirmPassword.isEmpty()) {
            confirmPasswordInputLayout.setError(null);
            confirmPasswordInputLayout.setErrorEnabled(false);
            confirmPasswordErrorWasShown = false;
            return;
        }

        if (isConfirmPasswordValidFlag) {
            confirmPasswordInputLayout.setError(null);
            confirmPasswordInputLayout.setErrorEnabled(false);
            return;
        }

        if (showError) {
            confirmPasswordInputLayout.setError(
                    confirmPasswordInputLayout.getContext().getString(R.string.password_mismatch_error)
            );
            confirmPasswordInputLayout.setErrorEnabled(true);
        } else {
            confirmPasswordInputLayout.setError(null);
            confirmPasswordInputLayout.setErrorEnabled(false);
        }
    }

    private boolean hasPasswordText() {
        return !getPassword().isEmpty();
    }

    private boolean hasConfirmPasswordText() {
        return !getConfirmPassword().isEmpty();
    }

    public boolean isPasswordValid() {
        return isPasswordValidFlag;
    }

    public boolean isConfirmPasswordValid() {
        return isConfirmPasswordValidFlag;
    }

    public String getPassword() {
        return passwordEditText.getText() != null
                ? passwordEditText.getText().toString().trim()
                : "";
    }

    private String getConfirmPassword() {
        return confirmPasswordEditText.getText() != null
                ? confirmPasswordEditText.getText().toString().trim()
                : "";
    }

    private void notifyValidationChanged() {
        if (onValidationChanged != null) {
            onValidationChanged.run();
        }
    }
}
