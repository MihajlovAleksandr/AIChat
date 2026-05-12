package com.example.aichat.controller;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.example.aichat.R;
import com.example.aichat.dto.request.RegistrationRequest;
import com.example.aichat.dto.response.ApiError;
import com.example.aichat.dto.response.RegisterResponse;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.util.InputValidator;
import com.example.aichat.view.PreferenceActivity;
import com.example.aichat.view.RegistrationActivity;
import com.example.aichat.view.UserDataActivity;
import com.example.aichat.view.VerifyEmailActivity;
import com.google.android.material.textfield.TextInputLayout;

public class RegistrationController {

    private final RegistrationActivity activity;
    private final PasswordController passwordController;

    private final TextInputLayout emailInputLayout;
    private final EditText emailEditText;
    private final Button registrationButton;

    private boolean isEmailValidFlag = false;
    private final ConnectionDispatcher dispatcher;

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

        dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
        dispatcher.setToken(null);

        setupEmailListener();
        setupRegistrationButton();

        registrationButton.setEnabled(false);
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
            RegistrationRequest request = new RegistrationRequest(email, passwordController.getPassword(), "EMAIL_PASSWORD");

            dispatcher.sendHttpRequestAsync("/api/auth/register", HttpClient.HTTPMethod.POST, request,false).thenAccept((cmd)->
            {
                if(cmd.isSuccess()){
                    RegisterResponse response = cmd.getData(RegisterResponse.class);
                    Class type;
                    switch (response.state) {
                        case CREATED:
                            type = VerifyEmailActivity.class;
                            break;
                        case EMAIL_VERIFIED:
                            type = UserDataActivity.class;
                            break;
                        case USER_DATA_COMPLETED:
                            type = PreferenceActivity.class;
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    SecurePreferencesManager.saveAuthToken(activity, response.token);
                    dispatcher.setToken(response.token);
                    Intent intent = new Intent(activity, type);
                    activity.startActivity(intent);
                    activity.finish();
                }
                else{
                    Log.e("Registration", cmd.getData(ApiError.class).toString());
                }
            });
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
