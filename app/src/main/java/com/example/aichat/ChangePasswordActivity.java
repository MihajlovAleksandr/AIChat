package com.example.aichat;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aichat.controller.PasswordController;
import com.example.aichat.dto.request.ChangePasswordRequest;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.view.FullScreenHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputLayout currentPasswordInputLayout, newPasswordInputLayout, confirmPasswordInputLayout;
    private EditText currentPasswordEditText, newPasswordEditText, confirmPasswordEditText;
    private Button changePasswordButton;
    private PasswordController passwordController;
    private ConnectionManager connectionManager;
    private OnConnectionEvents events;
    private FloatingActionButton btnBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_AIChat_Dark);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        FullScreenHelper.enableFullScreen(getWindow());

        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();

        initViews();
        setupBackButton();
        setupListeners();
        setupConnectionEvents();

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

    private void setupConnectionEvents() {
        events = new OnConnectionEvents() {
            @Override
            public void OnCommandGot(WSSCommand WSSCommand) {
                runOnUiThread(() -> {
                    if (Objects.equals(WSSCommand.getOperation(), "PasswordChanged")) {
                        Toast.makeText(ChangePasswordActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void OnConnectionFailed() {
                runOnUiThread(() -> Toast.makeText(ChangePasswordActivity.this, "Connection failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void OnOpen() {}
        };
        connectionManager.addConnectionEvent(events);
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
            WSSCommand cmd = new WSSCommand("ChangePassword", new ChangePasswordRequest(current, next));
            connectionManager.SendCommand(cmd);
        } else {
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