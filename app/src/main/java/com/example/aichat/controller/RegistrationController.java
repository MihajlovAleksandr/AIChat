package com.example.aichat.controller;

import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import com.example.aichat.R;
import com.example.aichat.view.RegistrationActivity;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.google.android.material.textfield.TextInputLayout;

public class RegistrationController {
    private RegistrationActivity activity;
    private ConnectionManager connectionManager;
    private PasswordController passwordController;

    private TextInputLayout emailInputLayout;
    private EditText emailEditText;
    private Button registrationButton;
    private boolean isEmailValidFlag = false;

    public RegistrationController(RegistrationActivity activity,
                                  TextInputLayout emailInputLayout,
                                  TextInputLayout passwordInputLayout,
                                  TextInputLayout confirmPasswordInputLayout,
                                  EditText emailEditText,
                                  EditText passwordEditText,
                                  EditText confirmPasswordEditText,
                                  Button registrationButton) {
        this.activity = activity;
        this.emailInputLayout = emailInputLayout;
        this.emailEditText = emailEditText;
        this.registrationButton = registrationButton;

        this.passwordController = new PasswordController(
                passwordInputLayout,
                confirmPasswordInputLayout,
                passwordEditText,
                confirmPasswordEditText,
                this::enableRegistrationButton
        );

        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        if (connectionManager == null) {
            ConnectionSingleton.getInstance().setConnectionManager(new ConnectionManager(""));
            connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        }

        setupConnectionCallbacks();
        setupEmailListener();
        setupRegistrationButton();

        registrationButton.setEnabled(false);
    }

    private void setupConnectionCallbacks() {
        connectionManager.setConnectionEvent(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                switch (command.getOperation()) {
                    case "EmailIsBusy":
                        activity.runOnUiThread(() -> emailInputLayout.setError(activity.getString(R.string.email_in_use_error)));
                        break;
                    case "VerificationCodeSend":
                        ConnectionSingleton.getInstance().setConnectionManager(connectionManager);
                        Intent intent = new Intent(activity, com.example.aichat.view.VerifyEmailActivity.class);
                        activity.startActivity(intent);
                        activity.finish();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void OnConnectionFailed() {}

            @Override
            public void OnOpen() {}
        });
    }

    private void setupEmailListener() {
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail();
                enableRegistrationButton();
            }
        });

        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                emailInputLayout.setError(null);
                isEmailValidFlag = isEmailValid(s.toString().trim());
                enableRegistrationButton();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRegistrationButton() {
        registrationButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            Command command = new Command("Registration");
            command.addData("email", email);
            command.addData("password", passwordController.getPassword());
            connectionManager.SendCommand(command);
        });
    }

    private void validateEmail() {
        String email = emailEditText.getText().toString().trim();
        if (!isEmailValid(email)) {
            emailInputLayout.setError(activity.getString(R.string.invalid_email_error));
            isEmailValidFlag = false;
        } else {
            emailInputLayout.setError(null);
            isEmailValidFlag = true;
        }
    }

    private void enableRegistrationButton() {
        registrationButton.setEnabled(isEmailValidFlag &&
                passwordController.isPasswordValid() &&
                passwordController.isConfirmPasswordValid());
    }

    private boolean isEmailValid(String email) {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,6}$";
        return !TextUtils.isEmpty(email) && email.matches(emailPattern);
    }
}