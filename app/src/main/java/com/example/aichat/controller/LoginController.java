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
import com.example.aichat.dto.request.GoogleTokenRequest;
import com.example.aichat.dto.request.LoginRequest;
import com.example.aichat.dto.response.ApiError;
import com.example.aichat.dto.response.RegisterResponse;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.JwtUtils;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.entities.HttpCommand;
import com.example.aichat.util.InputValidator;
import com.example.aichat.view.LoginActivity;
import com.example.aichat.view.PreferenceActivity;
import com.example.aichat.view.UserDataActivity;
import com.example.aichat.view.VerifyEmailActivity;
import com.example.aichat.view.main.MainActivity;
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
    private final ConnectionDispatcher dispatcher;

    private boolean isEmailValidFlag = false;
    private boolean isPasswordValidFlag = false;
    private boolean isLoginInProgress = false;

    private OnLoginSuccessListener onLoginSuccessListener;

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
        dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
        setupFieldListeners();
        setupLoginButton();
        setupGoogleSignIn(googleSignInButton);

        loginButton.setEnabled(false);
    }

    public void setOnLoginSuccessListener(OnLoginSuccessListener listener) {
        this.onLoginSuccessListener = listener;
    }

    // -------------------------
    // LOGIN VIA REST API
    // -------------------------

    public void login(String email, String password) {
        if (isLoginInProgress) return;
        isLoginInProgress = true;
        loginButton.setEnabled(false);

        LoginRequest request = new LoginRequest(email, password, "EMAIL_PASSWORD");
        dispatcher.sendHttpRequestAsync("/api/auth/login", HttpClient.HTTPMethod.POST, request, false)
                .thenAccept(this::handleResponse);
    }

    private void handleResponse(HttpCommand cmd) {
        if (cmd.isSuccess()) {
            try {
                RegisterResponse response = cmd.getData(RegisterResponse.class);

                if (response == null) {
                    onLoginError(activity.getString(R.string.try_again));
                    return;
                }

                handleRegisterResponse(response);

            } catch (RuntimeException e) {
                try {
                    String jwt = cmd.getData(String.class);

                    JSONObject jsonPayload = JwtUtils.decodePayload(jwt);

                    UUID userId =
                            UUID.fromString(jsonPayload.getString("sub"));

                    SecurePreferencesManager.saveUserId(activity, userId);
                    SecurePreferencesManager.saveAuthToken(activity, jwt);

                    dispatcher.setToken(jwt);

                    Intent intent =
                            new Intent(activity, MainActivity.class);

                    activity.startActivity(intent);
                    activity.finish();

                } catch (Exception ex) {
                    onLoginError(activity.getString(R.string.try_again));
                }
            }

        } else {

            ApiError error = cmd.getData(ApiError.class);

            String message =
                    activity.getString(R.string.try_again);

            if (error != null && error.getCode() != null) {

                switch (error.getCode()) {

                    case "USER_NOT_FOUND":
                        message =
                                activity.getString(R.string.user_not_found);
                        break;

                    case "INVALID_CREDENTIALS":
                        message =
                                activity.getString(R.string.invalid_credentials);
                        break;

                    case "USER_BANNED":
                        message =
                                activity.getString(R.string.user_banned);
                        break;
                }
            }

            onLoginError(message);
        }
    }

    private void handleRegisterResponse(RegisterResponse response) {
        activity.runOnUiThread(() -> {
            isLoginInProgress = false;
            enableLoginButton();
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
        });
    }

    private void onLoginError(String message) {
        activity.runOnUiThread(() -> {
            isLoginInProgress = false;
            enableLoginButton();
            if (loginErrorCallback != null) {
                loginErrorCallback.onLoginError(message);
            }
        });
    }

    // -------------------------
    // GOOGLE LOGIN
    // -------------------------

    private void sendGoogleTokenToServer(String idToken) {
        if (isLoginInProgress) return;
        isLoginInProgress = true;
        loginButton.setEnabled(false);

        GoogleTokenRequest req = new GoogleTokenRequest(idToken);

        dispatcher.sendHttpRequestAsync("/api/auth/oauth/google", HttpClient.HTTPMethod.POST, req, false)
                .thenAccept(this::handleResponse);
    }

    // -------------------------
    // UI VALIDATION
    // -------------------------

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
        loginButton.setEnabled(!isLoginInProgress && isEmailValidFlag && isPasswordValidFlag);
    }

    // -------------------------
    // GOOGLE SIGN-IN
    // -------------------------

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

    public void qrLogin(String token) {

        if (token == null || token.trim().isEmpty()) {
            onLoginError(activity.getString(R.string.try_again));
            return;
        }

        try {

            if (JwtUtils.decodePayload(token) == null) {
                onLoginError(activity.getString(R.string.invalid_qr_code));
                return;
            }

            dispatcher.setToken(token);

            dispatcher.sendHttpRequestAsync(
                            "/api/auth/code/verify",
                            HttpClient.HTTPMethod.POST,
                            null,
                            false
                    ).thenAccept(this::handleResponse)
                    .exceptionally(ex -> {

                        onLoginError(activity.getString(R.string.invalid_qr_code));

                        return null;
                    });

        } catch (Exception ex) {

            onLoginError(activity.getString(R.string.invalid_qr_code));
        }
    }
}
