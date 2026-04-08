package com.example.aichat.controller;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import com.example.aichat.R;
import com.example.aichat.dto.request.RegistrationRequest;
import com.example.aichat.model.LocaleManager;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.util.InputValidator;
import com.example.aichat.view.RegistrationActivity;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.google.android.material.textfield.TextInputLayout;

public class RegistrationController {

    private final RegistrationActivity activity;
    private final PasswordController passwordController;

    private final TextInputLayout emailInputLayout;
    private final EditText emailEditText;
    private final Button registrationButton;

    private boolean isEmailValidFlag = false;
    private final ConnectionManager connectionManager;

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

        this.connectionManager = new ConnectionManager("");

        setupConnectionCallbacks();
        setupEmailListener();
        setupRegistrationButton();

        registrationButton.setEnabled(false);
    }

    private void setupConnectionCallbacks() {

        connectionManager.clearConnectionEvents();
        connectionManager.addConnectionEvent(new OnConnectionEvents() {

            @Override
            public void OnCommandGot(WSSCommand command) {
                switch (command.getOperation()) {

                    case "EmailIsBusy":
                        activity.runOnUiThread(() ->
                                emailInputLayout.setError(activity.getString(R.string.email_in_use_error))
                        );
                        break;

                    case "VerificationCodeSend":
                        ConnectionSingleton.getInstance().setConnectionManager(connectionManager);

                        Intent intent = new Intent(activity, com.example.aichat.view.VerifyEmailActivity.class);
                        activity.startActivity(intent);
                        activity.finish();
                        break;
                }
            }

            @Override
            public void OnConnectionFailed() {
            }

            @Override
            public void OnOpen() {}
        });

        connectionManager.connect();
    }

    private void setupEmailListener() {
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail();
                enableRegistrationButton();
            }
        });

        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                emailInputLayout.setError(null);
                isEmailValidFlag = InputValidator.isEmailValid(s.toString().trim());
                enableRegistrationButton();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRegistrationButton() {
        registrationButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();

            WSSCommand command = new WSSCommand(
                    "Registration",
                    new RegistrationRequest(
                            email,
                            passwordController.getPassword(),
                            LocaleManager.getLocale(activity).toString()
                    )
            );

            connectionManager.SendCommand(command);
        });
    }

    private void validateEmail() {
        String email = emailEditText.getText().toString().trim();
        if (!InputValidator.isEmailValid(email)) {
            emailInputLayout.setError(activity.getString(R.string.invalid_email_error));
            isEmailValidFlag = false;
        } else {
            emailInputLayout.setError(null);
            isEmailValidFlag = true;
        }
    }

    private void enableRegistrationButton() {
        registrationButton.setEnabled(
                isEmailValidFlag &&
                        passwordController.isPasswordValid() &&
                        passwordController.isConfirmPasswordValid()
        );
    }
}
