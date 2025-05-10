package com.example.aichat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aichat.R;
import com.example.aichat.controller.PasswordController;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Command;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class ChangePasswordActivity extends AppCompatActivity {
    private TextInputLayout currentPasswordInputLayout;
    private TextInputLayout newPasswordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private EditText currentPasswordEditText;
    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;
    private Button changePasswordButton;
    private PasswordController passwordController;
    private ConnectionManager connectionManager;
    private OnConnectionEvents events;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();

        initViews();
        setupListeners();
        setupConnectionEvents();
    }

    private void initViews() {
        currentPasswordInputLayout = findViewById(R.id.currentPasswordInputLayout);
        newPasswordInputLayout = findViewById(R.id.newPasswordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        currentPasswordEditText = findViewById(R.id.currentPasswordEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        changePasswordButton = findViewById(R.id.changePasswordButton);

        passwordController = new PasswordController(
                newPasswordInputLayout,
                confirmPasswordInputLayout,
                newPasswordEditText,
                confirmPasswordEditText,
                this::updateChangePasswordButtonState
        );
    }

    private void setupListeners() {
        currentPasswordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateCurrentPassword();
            }
        });

        changePasswordButton.setOnClickListener(v -> changePassword());
        updateChangePasswordButtonState();
    }

    private void setupConnectionEvents() {
        events = new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                runOnUiThread(() -> {
                    if (Objects.equals(command.getOperation(), "PasswordChanged")) {
                        Toast.makeText(ChangePasswordActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void OnConnectionFailed() {
                runOnUiThread(() ->
                        Toast.makeText(ChangePasswordActivity.this, "Connection failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void OnOpen() {
                // Можно добавить обработку при необходимости
            }
        };
        connectionManager.addConnectionEvent(events);
    }

    private void validateCurrentPassword() {
        String currentPassword = currentPasswordEditText.getText().toString().trim();
        if (currentPassword.isEmpty()) {
            currentPasswordInputLayout.setError("Enter current password");
        } else {
            currentPasswordInputLayout.setError(null);
        }
        updateChangePasswordButtonState();
    }

    private void updateChangePasswordButtonState() {
        boolean isCurrentPasswordValid = !currentPasswordEditText.getText().toString().trim().isEmpty();
        boolean isNewPasswordValid = passwordController.isPasswordValid();
        boolean isConfirmPasswordValid = passwordController.isConfirmPasswordValid();

        changePasswordButton.setEnabled(isCurrentPasswordValid && isNewPasswordValid && isConfirmPasswordValid);
    }

    private void changePassword() {
        String currentPassword = currentPasswordEditText.getText().toString().trim();
        String newPassword = passwordController.getPassword();
        if(!currentPassword.equals(newPassword)) {
            Command command = new Command("ChangePassword");
            command.addData("currentPassword", currentPassword);
            command.addData("newPassword", newPassword);
            connectionManager.SendCommand(command);
        }
        else{
            currentPasswordInputLayout.setError("The new password must be different from the old one.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectionManager != null && events != null) {
            connectionManager.removeConnectionEvent(events);
        }
    }
}