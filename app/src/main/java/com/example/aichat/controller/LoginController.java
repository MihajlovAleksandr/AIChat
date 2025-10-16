package com.example.aichat.controller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.aichat.dto.request.AuthRequest;
import com.example.aichat.dto.request.GoogleTokenRequest;
import com.example.aichat.dto.response.EntryTokenResponse;
import com.example.aichat.dto.response.LoginInResponse;
import com.example.aichat.dto.response.TokenResponse;
import com.example.aichat.dto.response.UseOtherLoginInServiceResponse;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.view.LoginActivity;
import com.example.aichat.view.UserDataActivity;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.R;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.QRCodeGenerator;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.UUID;

public class LoginController {
    private static final int RC_SIGN_IN = 1001;

    private final LoginActivity activity;
    private final ConnectionManager connectionManager;
    private GoogleSignInClient googleSignInClient;
    private final TextInputLayout emailInputLayout;
    private final TextInputLayout passwordInputLayout;
    private final EditText emailEditText;
    private final EditText passwordEditText;
    private final Button loginButton;
    private final ImageView imageView;

    private boolean isEmailValidFlag = false;
    private boolean isPasswordValidFlag = false;

    public LoginController(LoginActivity activity,
                           TextInputLayout emailInputLayout,
                           TextInputLayout passwordInputLayout,
                           EditText emailEditText,
                           EditText passwordEditText,
                           Button loginButton,
                           ImageView imageView,
                           SignInButton googleSignInButton) {
        ConnectionManager manager;
        this.activity = activity;
        this.emailInputLayout = emailInputLayout;
        this.passwordInputLayout = passwordInputLayout;
        this.emailEditText = emailEditText;
        this.passwordEditText = passwordEditText;
        this.loginButton = loginButton;
        this.imageView = imageView;

        manager = ConnectionSingleton.getInstance().getConnectionManager();
        if (manager == null) {
            ConnectionSingleton.getInstance().setConnectionManager(new ConnectionManager(""));
            manager = ConnectionSingleton.getInstance().getConnectionManager();

        } else {
            manager.SendCommand(new Command("GetEntryToken"));
        }

        connectionManager = manager;
        setupConnectionCallbacks();
        setupFieldListeners();
        setupLoginButton();
        setupGoogleSignIn(googleSignInButton);
        loginButton.setEnabled(false);
    }

    private void setupGoogleSignIn(SignInButton googleSignInButton) {
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
        googleSignInButton.setSize(SignInButton.SIZE_WIDE);
        googleSignInButton.setOnClickListener(v -> startGoogleSignIn());
    }

    private void startGoogleSignIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void handleGoogleSignInResult(int requestCode, @Nullable Intent data) {
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String idToken = account.getIdToken();
                if (idToken != null) {
                    sendGoogleTokenToServer(idToken);
                } else {
                    Toast.makeText(activity, "Google Sign-In failed: no token", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                Log.e("LoginController", "Google Sign-In failed", e);
                Toast.makeText(activity, "Google Sign-In failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendGoogleTokenToServer(String idToken) {
        Command command = new Command("SendGoogleTokenCommand", new GoogleTokenRequest(idToken));
        connectionManager.SendCommand(command);
        Log.d("LoginController", "Google token sent to server");
    }

    private String loadWebClientId() {
        try (InputStream is = activity.getAssets().open("google-secret.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            JSONObject root = new JSONObject(builder.toString());
            return root.getJSONObject("web").getString("client_id");
        } catch (Exception e) {
            Log.e("LoginController", "Error loading client ID", e);
            return null;
        }
    }

    private void setupConnectionCallbacks() {
        connectionManager.setConnectionEvent(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                switch (command.getOperation()) {
                    case "EntryToken":
                        handleEntryToken(command);
                        break;
                    case "CreateToken":
                        handleCreateToken(command);
                        break;
                    case "LoginIn":
                        handleLoginSuccess(command);
                        break;
                    case "GoogleRegistrationSuccess":
                        GoogleRegistrationSuccess();
                        break;
                    case "UseOtherLoginInService":
                        UseOtherLoginInServiceResponse response = command.getData(UseOtherLoginInServiceResponse.class);
                        if(Objects.equals(response.service, "Password")){
                            activity.runOnUiThread(()-> {
                                validatePassword();
                            });
                        }
                        else if(Objects.equals(response.service, "Google")) {
                            activity.runOnUiThread(() -> {
                                    passwordInputLayout.setError("Use Google auth");
                                }
                            );
                        }
                        break;
                }
            }

            @Override
            public void OnConnectionFailed() {
                activity.runOnUiThread(() ->
                        imageView.setImageResource(R.drawable.loading));
            }

            @Override
            public void OnOpen() {
                connectionManager.SendCommand(new Command("GetEntryToken"));
            }
        });
    }

    private void handleEntryToken(Command command) {
        EntryTokenResponse entryTokenResponse = command.getData(EntryTokenResponse.class);
        Log.d("LoginController", "Entry token received: " + entryTokenResponse.token);
        Bitmap bitmap = QRCodeGenerator.generateQRCodeImage(entryTokenResponse.token, 400, 400);
        activity.runOnUiThread(() -> imageView.setImageBitmap(bitmap));
    }

    private void handleCreateToken(Command command) {
        TokenResponse tokenResponse = command.getData(TokenResponse.class);
        SecurePreferencesManager.saveAuthToken(activity, tokenResponse.token);
        connectionManager.setToken(tokenResponse.token);
    }

    private void handleLoginSuccess(Command command) {
        LoginInResponse loginInResponse = command.getData(LoginInResponse.class);
        SecurePreferencesManager.saveUserId(activity, loginInResponse.userId);

        activity.runOnUiThread(() -> {
            Intent intent = new Intent(activity, MainActivity.class);
            intent.putExtra("userId", loginInResponse.userId.toString());
            activity.startActivity(intent);
            activity.finish();
        });
    }

    private void GoogleRegistrationSuccess() {
        Intent intent = new Intent(activity, UserDataActivity.class);
        activity.startActivity(intent);
        activity.finish();
    }

    private void setupFieldListeners() {
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail();
            }
        });

        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validatePassword();
            }
        });

        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String email = emailEditText.getText().toString().trim();
                if (isEmailValid(email)) {
                    emailInputLayout.setError(null);
                    isEmailValidFlag = true;
                } else {
                    isEmailValidFlag = false;
                }
                enableLoginButton();
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = passwordEditText.getText().toString().trim();
                if (isPasswordValid(password)) {
                    passwordInputLayout.setError(null);
                    isPasswordValidFlag = true;
                } else {
                    isPasswordValidFlag = false;
                }
                enableLoginButton();
            }
        });
    }

    private void setupLoginButton() {
        loginButton.setOnClickListener(v -> {
            Command command = new Command("LoginIn", new AuthRequest(emailEditText.getText().toString().trim(), passwordEditText.getText().toString().trim()));
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
        enableLoginButton();
    }

    private void validatePassword() {
        String password = passwordEditText.getText().toString().trim();
        if (!isPasswordValid(password)) {
            passwordInputLayout.setError(activity.getString(R.string.invalid_password_error));
            isPasswordValidFlag = false;
        } else {
            passwordInputLayout.setError(null);
            isPasswordValidFlag = true;
        }
        enableLoginButton();
    }

    private void enableLoginButton() {
        loginButton.setEnabled(isEmailValidFlag && isPasswordValidFlag);
    }

    private boolean isEmailValid(String email) {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,6}$";
        return !TextUtils.isEmpty(email) && email.matches(emailPattern);
    }

    private boolean isPasswordValid(String password) {
        if (password.length() < 8) {
            return false;
        }
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-zа-я])(?=.*[A-ZА-Я])(?=.*[@#$%^&+=!]).{8,}$";
        return password.matches(passwordPattern);
    }
}