package com.example.aichat.controller;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.example.aichat.dto.request.GoogleTokenRequest;
import com.example.aichat.dto.request.LoginRequest;
import com.example.aichat.dto.response.ApiError;
import com.example.aichat.dto.response.RegisterResponse;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.JwtUtils;
import com.example.aichat.model.connection.TokenStorage;
import com.example.aichat.model.entities.HttpCommand;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.R;
import com.example.aichat.view.helpers.InputValidator;
import com.example.aichat.view.LoginActivity;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.view.PreferenceActivity;
import com.example.aichat.view.UserDataActivity;
import com.example.aichat.view.VerifyEmailActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;
import org.json.JSONObject;

public class LoginController {

    public interface LoginErrorCallback {
        void onLoginError(String message);
    }

    public interface OnLoginSuccessListener {
        void onLoginSuccess(UUID userId, String token);
    }

    private static final String TAG = "LoginController";
    private static final int RC_SIGN_IN = 1001;

    private final LoginActivity activity;
    private final LoginErrorCallback loginErrorCallback;

    private final TextInputLayout emailInputLayout;
    private final TextInputLayout passwordInputLayout;
    private final EditText emailEditText;
    private final EditText passwordEditText;
    private final Button loginButton;
    private final ImageView googleSignInButton;
    private final LoadingDialogController loadingDialog;

    private GoogleSignInClient googleSignInClient;
    private final TokenStorage tokenStorage;
    private final ConnectionDispatcher dispatcher;

    private boolean isEmailValidFlag = false;
    private boolean isPasswordValidFlag = false;
    private boolean isLoginInProgress = false;
    private boolean destroyed = false;

    private boolean emailErrorWasShown = false;
    private boolean passwordErrorWasShown = false;

    private CompletableFuture<?> loginFuture;
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
        this.googleSignInButton = googleSignInButton;
        this.loginErrorCallback = loginErrorCallback;
        this.loadingDialog = new LoadingDialogController(activity);

        this.tokenStorage = new TokenStorage(activity);
        this.tokenStorage.saveToken(null);
        this.dispatcher = new ConnectionDispatcher(tokenStorage);

        setupFieldListeners();
        setupLoginButton();
        setupGoogleSignIn(googleSignInButton);

        loginButton.setEnabled(false);
    }

    public void setOnLoginSuccessListener(OnLoginSuccessListener listener) {
        this.onLoginSuccessListener = listener;
    }

    public void login(String email, String password) {
        if (isLoginInProgress) return;

        isLoginInProgress = true;
        updateButtonsState();

        loadingDialog.show(
                "Вход",
                "Пожалуйста, подождите. Выполняем вход..."
        );

        LoginRequest request = new LoginRequest(email, password, "EMAIL_PASSWORD");

        loginFuture = dispatcher.sendHttpRequestAsync(
                        "/api/auth/login",
                        HttpClient.HTTPMethod.POST,
                        request,
                        false
                )
                .thenAccept(this::handleResponse)
                .exceptionally(throwable -> {
                    Log.e(TAG, "Login request failed", throwable);
                    onLoginError(activity.getString(R.string.try_again));
                    return null;
                });
    }

    private void handleResponse(HttpCommand cmd) {
        if (cmd == null) {
            onLoginError(activity.getString(R.string.try_again));
            return;
        }

        if (cmd.isSuccess()) {
            try {
                RegisterResponse response = cmd.getData(RegisterResponse.class);

                if (response == null) {
                    onLoginError(activity.getString(R.string.try_again));
                    return;
                }

                handleRegisterResponse(response);

            } catch (RuntimeException e) {
                handleJwtLoginResponse(cmd);
            }

            return;
        }

        ApiError error = cmd.getData(ApiError.class);
        String message = activity.getString(R.string.try_again);

        if (error != null && error.getCode() != null) {
            switch (error.getCode()) {
                case "USER_NOT_FOUND":
                    message = activity.getString(R.string.user_not_found);
                    break;

                case "INVALID_CREDENTIALS":
                    message = activity.getString(R.string.invalid_credentials);
                    break;

                case "USER_BANNED":
                    message = activity.getString(R.string.user_banned);
                    break;
            }
        }

        onLoginError(message);
    }

    private void handleJwtLoginResponse(HttpCommand cmd) {
        try {
            String jwt = cmd.getData(String.class);
            JSONObject jsonPayload = JwtUtils.decodePayload(jwt);

            if (jwt == null || jsonPayload == null) {
                onLoginError(activity.getString(R.string.try_again));
                return;
            }

            UUID userId = UUID.fromString(jsonPayload.getString("sub"));

            activity.runOnUiThread(() -> {
                if (!isActivityUsable()) return;

                finishLoading();

                SecurePreferencesManager.saveUserId(activity, userId);
                SecurePreferencesManager.saveAuthToken(activity, jwt);
                tokenStorage.saveToken(jwt);

                Intent intent = new Intent(activity, MainActivity.class);
                activity.startActivityClean(intent, true);
            });

        } catch (Exception ex) {
            Log.e(TAG, "JWT login parse failed", ex);
            onLoginError(activity.getString(R.string.try_again));
        }
    }

    private void handleRegisterResponse(RegisterResponse response) {
        activity.runOnUiThread(() -> {
            if (!isActivityUsable()) return;

            finishLoading();

            if (response == null || response.state == null || response.token == null) {
                onLoginError(activity.getString(R.string.try_again));
                return;
            }

            Class<?> type;

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
                    onLoginError(activity.getString(R.string.try_again));
                    return;
            }

            SecurePreferencesManager.saveAuthToken(activity, response.token);
            tokenStorage.saveToken(response.token);

            Intent intent = new Intent(activity, type);
            activity.startActivityClean(intent, true);
        });
    }

    private void onLoginError(String message) {
        activity.runOnUiThread(() -> {
            if (!isActivityUsable()) return;

            finishLoading();

            if (loginErrorCallback != null) {
                loginErrorCallback.onLoginError(message);
            }
        });
    }

    private void sendGoogleTokenToServer(String idToken) {
        if (isLoginInProgress) return;

        isLoginInProgress = true;
        updateButtonsState();

        loadingDialog.show(
                "Вход через Google",
                "Пожалуйста, подождите. Проверяем аккаунт..."
        );

        GoogleTokenRequest request = new GoogleTokenRequest(idToken);

        loginFuture = dispatcher.sendHttpRequestAsync(
                        "/api/auth/oauth/google",
                        HttpClient.HTTPMethod.POST,
                        request,
                        false
                )
                .thenAccept(this::handleResponse)
                .exceptionally(throwable -> {
                    Log.e(TAG, "Google login failed", throwable);
                    onLoginError(activity.getString(R.string.try_again));
                    return null;
                });
    }

    private void setupFieldListeners() {
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && hasEmailText()) {
                emailErrorWasShown = true;
            }

            validateEmail(emailErrorWasShown);
            updateButtonsState();
        });

        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && hasPasswordText()) {
                passwordErrorWasShown = true;
            }

            validatePassword(passwordErrorWasShown);
            updateButtonsState();
        });

        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail(emailErrorWasShown);
                updateButtonsState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword(passwordErrorWasShown);
                updateButtonsState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupLoginButton() {
        loginButton.setOnClickListener(v -> {
            if (isLoginInProgress) return;

            emailErrorWasShown = true;
            passwordErrorWasShown = true;

            validateEmail(true);
            validatePassword(true);
            updateButtonsState();

            if (!isEmailValidFlag || !isPasswordValidFlag) return;

            String email = getEmail();
            String password = getPassword();

            login(email, password);
        });
    }

    private void validateEmail(boolean showError) {
        String text = getEmail();

        isEmailValidFlag = InputValidator.isEmailValid(text);

        if (text.isEmpty()) {
            emailInputLayout.setError(null);
            emailInputLayout.setErrorEnabled(false);
            emailErrorWasShown = false;
            return;
        }

        if (isEmailValidFlag) {
            emailInputLayout.setError(null);
            emailInputLayout.setErrorEnabled(false);
            return;
        }

        if (showError) {
            emailInputLayout.setError(activity.getString(R.string.invalid_email_error));
            emailInputLayout.setErrorEnabled(true);
        } else {
            emailInputLayout.setError(null);
            emailInputLayout.setErrorEnabled(false);
        }
    }

    private void validatePassword(boolean showError) {
        String text = getPassword();

        isPasswordValidFlag = InputValidator.isPasswordValid(text);

        if (text.isEmpty()) {
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
            passwordInputLayout.setError(activity.getString(R.string.invalid_password_error));
            passwordInputLayout.setErrorEnabled(true);
        } else {
            passwordInputLayout.setError(null);
            passwordInputLayout.setErrorEnabled(false);
        }
    }

    private void updateButtonsState() {
        boolean enabled = !isLoginInProgress
                && isEmailValidFlag
                && isPasswordValidFlag;

        loginButton.setEnabled(enabled);

        if (googleSignInButton != null) {
            googleSignInButton.setEnabled(!isLoginInProgress);
            googleSignInButton.setAlpha(isLoginInProgress ? 0.6f : 1f);
        }
    }

    private void finishLoading() {
        isLoginInProgress = false;
        loginFuture = null;
        loadingDialog.dismiss();
        updateButtonsState();
    }

    private boolean hasEmailText() {
        return !getEmail().isEmpty();
    }

    private boolean hasPasswordText() {
        return !getPassword().isEmpty();
    }

    private String getEmail() {
        return emailEditText.getText() != null
                ? emailEditText.getText().toString().trim()
                : "";
    }

    private String getPassword() {
        return passwordEditText.getText() != null
                ? passwordEditText.getText().toString().trim()
                : "";
    }

    private void setupGoogleSignIn(ImageView googleSignInButton) {
        if (googleSignInButton == null) return;

        String clientId = loadWebClientId();

        if (clientId == null) {
            Toast.makeText(
                    activity,
                    "Error configuring Google Sign-In",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        GoogleSignInOptions options =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(clientId)
                        .requestEmail()
                        .build();

        googleSignInClient = GoogleSignIn.getClient(activity, options);

        googleSignInButton.setOnClickListener(v -> startGoogleSignIn());
    }

    private void startGoogleSignIn() {
        if (isLoginInProgress || googleSignInClient == null) return;

        Intent signInIntent = googleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void handleGoogleSignInResult(int requestCode, @Nullable Intent data) {
        if (requestCode != RC_SIGN_IN) return;

        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(data);

        try {
            GoogleSignInAccount account =
                    task.getResult(ApiException.class);

            if (account != null && account.getIdToken() != null) {
                sendGoogleTokenToServer(account.getIdToken());
            }

        } catch (ApiException e) {
            Toast.makeText(
                    activity,
                    "Google Sign-In failed: " + e.getStatusCode(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private String loadWebClientId() {
        try (InputStream inputStream = activity.getAssets().open("google-secret.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            JSONObject root = new JSONObject(builder.toString());

            return root.getJSONObject("web").getString("client_id");

        } catch (Exception e) {
            Log.e(TAG, "Failed to load google-secret.json", e);
            return null;
        }
    }

    public void qrLogin(String token) {
        if (isLoginInProgress) return;

        if (token == null || token.trim().isEmpty()) {
            onLoginError(activity.getString(R.string.try_again));
            return;
        }

        try {
            if (JwtUtils.decodePayload(token) == null) {
                onLoginError(activity.getString(R.string.invalid_qr_code));
                return;
            }

            isLoginInProgress = true;
            updateButtonsState();

            loadingDialog.show(
                    "QR-вход",
                    "Пожалуйста, подождите. Проверяем QR-код..."
            );

            dispatcher.setToken(token);

            loginFuture = dispatcher.sendHttpRequestAsync(
                            "/api/auth/code/verify",
                            HttpClient.HTTPMethod.POST,
                            null,
                            false
                    )
                    .thenAccept(this::handleResponse)
                    .exceptionally(ex -> {
                        Log.e(TAG, "QR login failed", ex);
                        onLoginError(activity.getString(R.string.invalid_qr_code));
                        return null;
                    });

        } catch (Exception ex) {
            Log.e(TAG, "QR token parse failed", ex);
            onLoginError(activity.getString(R.string.invalid_qr_code));
        }
    }

    private boolean isActivityUsable() {
        return !destroyed
                && !activity.isFinishing()
                && !activity.isDestroyed();
    }

    public void destroy() {
        destroyed = true;

        if (loginFuture != null && !loginFuture.isDone()) {
            loginFuture.cancel(true);
        }

        loginFuture = null;
        loadingDialog.dismiss();
    }
}
