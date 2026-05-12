package com.example.aichat;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aichat.controller.PasswordController;
import com.example.aichat.view.FullScreenHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputLayout currentPasswordInputLayout, newPasswordInputLayout, confirmPasswordInputLayout;
    private EditText currentPasswordEditText, newPasswordEditText, confirmPasswordEditText;
    private Button changePasswordButton;
    private PasswordController passwordController;
    private FloatingActionButton btnBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_AIChat_Dark);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        FullScreenHelper.enableFullScreen(getWindow());

        initViews();
        setupBackButton();
        setupListeners();

        if (savedInstanceState != null) {
            currentPasswordEditText.setText(savedInstanceState.getString("currentPassword", ""));
            newPasswordEditText.setText(savedInstanceState.getString("newPassword", ""));
            confirmPasswordEditText.setText(savedInstanceState.getString("confirmPassword", ""));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentPassword", currentPasswordEditText.getText().toString());
        outState.putString("newPassword", newPasswordEditText.getText().toString());
        outState.putString("confirmPassword", confirmPasswordEditText.getText().toString());
    }

    private void initViews() {
        currentPasswordInputLayout = findViewById(R.id.currentPasswordInputLayout);
        newPasswordInputLayout = findViewById(R.id.newPasswordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);

        currentPasswordEditText = findViewById(R.id.currentPasswordEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);

        changePasswordButton = findViewById(R.id.changePasswordButton);
        btnBack = findViewById(R.id.btnBack);

        passwordController = new PasswordController(
                newPasswordInputLayout,
                confirmPasswordInputLayout,
                newPasswordEditText,
                confirmPasswordEditText,
                this::updateChangePasswordButtonState
        );
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        currentPasswordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateCurrentPassword();
        });

        changePasswordButton.setOnClickListener(v -> changePassword());
        updateChangePasswordButtonState();
    }

    private void validateCurrentPassword() {
        String pwd = currentPasswordEditText.getText().toString().trim();
        currentPasswordInputLayout.setError(pwd.isEmpty() ? "Enter current password" : null);
        updateChangePasswordButtonState();
    }

    private void updateChangePasswordButtonState() {
        boolean isValid = !currentPasswordEditText.getText().toString().trim().isEmpty()
                && passwordController.isPasswordValid()
                && passwordController.isConfirmPasswordValid();
        changePasswordButton.setEnabled(isValid);
    }

    private void changePassword() {
        String current = currentPasswordEditText.getText().toString().trim();
        String next = passwordController.getPassword();
        if(!current.equals(next)) {
            Log.e("changePassword: ", "Я вам ЗАПРЕЩАЮ менять пароль");
        } else {
            currentPasswordInputLayout.setError("The new password must be different from the old one.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}