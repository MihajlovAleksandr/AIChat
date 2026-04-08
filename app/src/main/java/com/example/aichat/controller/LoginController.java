package com.example.aichat.controller;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.aichat.R;
import com.example.aichat.dto.request.AuthRequest;
import com.example.aichat.dto.request.GoogleTokenRequest;
import com.example.aichat.dto.response.LoginInResponse;
import com.example.aichat.dto.response.TokenResponse;
import com.example.aichat.dto.response.UseOtherLoginInServiceResponse;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.util.InputValidator;
import com.example.aichat.view.LoginActivity;
import com.example.aichat.view.UserDataActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.UUID;

public class LoginController {

    public interface LoginErrorCallback {
        void onLoginError(String message);
    }

    public interface OnLoginSuccessListener {
        void onLoginSuccess(UUID userId, String token);
    }

    private static final int RC_SIGN_IN = 1001;

    private final LoginActivity activity;
    private final LoginErrorCallback loginErrorCallback;

    private final TextInputLayout emailInputLayout;
    private final TextInputLayout passwordInputLayout;
    private final EditText emailEditText;
    private final EditText passwordEditText;
    private final Button loginButton;

    private GoogleSignInClient googleSignInClient;
    private ConnectionManager connectionManager;

    private boolean isEmailValidFlag = false;
    private boolean isPasswordValidFlag = false;
    private boolean isLoginInProgress = false;

    private OnLoginSuccessListener onLoginSuccessListener;

    private String pendingEmail;
    private String pendingPassword;
    private String pendingGoogleIdToken;

    private final UUID[] userIdHolder = new UUID[1];
    private final String[] tokenHolder = new String[1];

    public LoginController(
            LoginActivity activity,
            TextInputLayout emailInputLayout,
            TextInputLayout passwordInputLayout,
            EditText emailEditText,
            EditText passwordEditText,
            Button loginButton,
            ImageView googleSignInButton,
            LoginErrorCallback loginErrorCallback
    ) {
        this.activity = activity;
        this.emailInputLayout = emailInputLayout;
        this.passwordInputLayout = passwordInputLayout;
        this.emailEditText = emailEditText;
        this.passwordEditText = passwordEditText;
        this.loginButton = loginButton;
        this.loginErrorCallback = loginErrorCallback;

        initConnectionManager();
        setupFieldListeners();
        setupLoginButton();
        setupGoogleSignIn(googleSignInButton);

        loginButton.setEnabled(false);
    }

    public void setOnLoginSuccessListener(OnLoginSuccessListener listener) {
        this.onLoginSuccessListener = listener;
    }

    private void initConnectionManager() {
        ConnectionSingleton singleton = ConnectionSingleton.getInstance();
        ConnectionManager old = singleton.getConnectionManager();
        if (old != null) {
            try { old.clearConnectionEvents(); } catch (Exception ignored) {}
            try { old.Close(); } catch (Exception ignored) {}
            try { old.dispose(); } catch (Exception ignored) {}
        }

        connectionManager = new ConnectionManager("");
        singleton.setConnectionManager(connectionManager);
        singleton.setToken(null);

        connectionManager.clearConnectionEvents();
        connectionManager.addConnectionEvent(new OnConnectionEvents() {

            @Override
            public void OnOpen() {
                if (pendingEmail != null && pendingPassword != null) {
                    connectionManager.SendCommand(
                            new WSSCommand("LoginIn", new AuthRequest(pendingEmail, pendingPassword))
                    );
                } else if (pendingGoogleIdToken != null) {
                    connectionManager.SendCommand(
                            new WSSCommand("SendGoogleTokenCommand", new GoogleTokenRequest(pendingGoogleIdToken))
                    );
                }
            }

            @Override
            public void OnCommandGot(WSSCommand command) {
                switch (command.getOperation()) {

                    case "CreateToken": {
                        TokenResponse tokenResponse = command.getData(TokenResponse.class);
                        if (tokenResponse == null) break;

                        tokenHolder[0] = tokenResponse.token;
                        SecurePreferencesManager.saveAuthToken(activity, tokenResponse.token);
                        ConnectionSingleton.getInstance().setToken(tokenResponse.token);
                        connectionManager.setToken(tokenResponse.token);

                        if (userIdHolder[0] != null) {
                            notifyLoginSuccess(userIdHolder[0], tokenHolder[0]);
                        }
                        break;
                    }

                    case "LoginIn": {
                        LoginInResponse loginInResponse = command.getData(LoginInResponse.class);
                        if (loginInResponse == null) break;

                        userIdHolder[0] = loginInResponse.userId;
                        SecurePreferencesManager.saveUserId(activity, loginInResponse.userId);

                        if (tokenHolder[0] != null) {
                            notifyLoginSuccess(userIdHolder[0], tokenHolder[0]);
                        }
                        break;
                    }

                    case "GoogleRegistrationSuccess": {
                        Intent intent = new Intent(activity, UserDataActivity.class);
                        activity.startActivity(intent);
                        activity.finish();
                        break;
                    }

                    case "UseOtherLoginInService": {
                        UseOtherLoginInServiceResponse response =
                                command.getData(UseOtherLoginInServiceResponse.class);
                        if (response != null && Objects.equals(response.service, "Password")) {
                            activity.runOnUiThread(LoginController.this::validatePassword);
                        }
                        break;
                    }
                }
            }

            @Override
            public void OnConnectionFailed() {
                isLoginInProgress = false;
                activity.runOnUiThread(() -> {
                    enableLoginButton();
                    if (loginErrorCallback != null) {
                        loginErrorCallback.onLoginError("Connection failed");
                    }
                });
            }
        });

        connectionManager.connect();
    }

    private void notifyLoginSuccess(UUID userId, String token) {
        isLoginInProgress = false;
        pendingEmail = null;
        pendingPassword = null;
        pendingGoogleIdToken = null;

        activity.runOnUiThread(() -> {
            enableLoginButton();
            if (onLoginSuccessListener != null) {
                onLoginSuccessListener.onLoginSuccess(userId, token);
            }
        });
    }

    public void login(String email, String password) {
        if (isLoginInProgress) return;
        isLoginInProgress = true;
        loginButton.setEnabled(false);

        pendingEmail = email;
        pendingPassword = password;

        connectionManager.SendCommand(
                new WSSCommand("LoginIn", new AuthRequest(email, password))
        );
    }

    private void sendGoogleTokenToServer(String idToken) {
        if (isLoginInProgress) return;
        isLoginInProgress = true;
        loginButton.setEnabled(false);

        pendingGoogleIdToken = idToken;

        connectionManager.SendCommand(
                new WSSCommand("SendGoogleTokenCommand", new GoogleTokenRequest(idToken))
        );
    }

    private void setupFieldListeners() {
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateEmail();
        });
        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validatePassword();
        });

        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                isEmailValidFlag = InputValidator.isEmailValid(s.toString().trim());
                emailInputLayout.setError(null);
                enableLoginButton();
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                isPasswordValidFlag = InputValidator.isPasswordValid(s.toString().trim());
                passwordInputLayout.setError(null);
                enableLoginButton();
            }
        });
    }

    private void setupLoginButton() {
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            login(email, password);
        });
    }

    private void validateEmail() {
        String text = emailEditText.getText().toString().trim();
        boolean isValid = InputValidator.isEmailValid(text);
        emailInputLayout.setError(isValid ? null : activity.getString(R.string.invalid_email_error));
        enableLoginButton();
    }

    private void validatePassword() {
        String text = passwordEditText.getText().toString().trim();
        boolean isValid = InputValidator.isPasswordValid(text);
        passwordInputLayout.setError(isValid ? null : activity.getString(R.string.invalid_password_error));
        enableLoginButton();
    }

    private void enableLoginButton() {
        if (isLoginInProgress) {
            loginButton.setEnabled(false);
        } else {
            loginButton.setEnabled(isEmailValidFlag && isPasswordValidFlag);
        }
    }

    private void setupGoogleSignIn(ImageView googleSignInButton) {
        String clientId = loadWebClientId();
        if (clientId == null) {
            Toast.makeText(activity, "Error configuring Google Sign-In", Toast.LENGTH_SHORT).show();
            return;
        }
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(activity, gso);
        googleSignInButton.setOnClickListener(v -> startGoogleSignIn());
    }

    private void startGoogleSignIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void handleGoogleSignInResult(int requestCode, @Nullable Intent data) {
        if (requestCode != RC_SIGN_IN) return;
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null && account.getIdToken() != null) {
                sendGoogleTokenToServer(account.getIdToken());
            }
        } catch (ApiException e) {
            Toast.makeText(activity, "Google Sign-In failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
        }
    }

    private String loadWebClientId() {
        try (InputStream is = activity.getAssets().open("google-secret.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
            JSONObject root = new JSONObject(builder.toString());
            return root.getJSONObject("web").getString("client_id");
        } catch (Exception e) {
            return null;
        }
    }
}