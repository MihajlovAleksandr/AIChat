package com.example.aichat.view;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.aichat.QRCodeActivity;
import com.example.aichat.R;
import com.example.aichat.controller.LoginController;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.view.main.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.UUID;

public class LoginActivity extends BaseActivity {

    private MaterialButton loginButton;
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private EditText emailEditText, passwordEditText;
    private ActivityResultLauncher<Intent> qrLauncher;
    private LoginController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_AIChat_Dark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        FullScreenHelper.enableFullScreen(getWindow());
        resetConnectionState();

        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);

        loginButton.setEnabled(false);

        configureEditText(emailEditText, emailInputLayout);
        configureEditText(passwordEditText, passwordInputLayout);

        ImageView googleSignInButton = findViewById(R.id.sign_in_button);

        controller = new LoginController(
                this,
                emailInputLayout,
                passwordInputLayout,
                emailEditText,
                passwordEditText,
                loginButton,
                googleSignInButton,
                this::showLoginError
        );

        controller.setOnLoginSuccessListener(this::onLoginSuccess);

        setupRegisterLink();
        setupSocialButtons();
        setupQRLoginLauncher();
        applyThemeColors();
    }

    private void resetConnectionState() {
        ConnectionSingleton singleton = ConnectionSingleton.getInstance();
        if (singleton.getConnectionManager() != null) {
            try {
                singleton.getConnectionManager().Close();
                singleton.getConnectionManager().dispose();
            } catch (Exception ignored) {}
        }
        singleton.setConnectionManager(null);
        singleton.setToken(null);

        SecurePreferencesManager.removeAuthToken(this);
        SecurePreferencesManager.removeUserId(this);
    }

    private void onLoginSuccess(UUID userId, String token) {
        SecurePreferencesManager.saveUserId(this, userId);
        SecurePreferencesManager.saveAuthToken(this, token);

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void configureEditText(EditText editText, TextInputLayout layout) {
        layout.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        editText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        editText.setAutofillHints("");
        editText.setHighlightColor(0x00000000);
        editText.setTextIsSelectable(true);
    }

    private void applyThemeColors() {
        int endIconColor = getColorForTheme(R.color.textinput_endicon_dark, R.color.textinput_endicon_light);
        emailInputLayout.setEndIconTintList(ColorStateList.valueOf(endIconColor));
        passwordInputLayout.setEndIconTintList(ColorStateList.valueOf(endIconColor));

        int hintColor = getColorForThemeSafe(R.color.primaryTextDark, emailInputLayout, R.color.primaryTextLight);
        emailInputLayout.setDefaultHintTextColor(ColorStateList.valueOf(hintColor));
        passwordInputLayout.setDefaultHintTextColor(ColorStateList.valueOf(hintColor));
    }

    private int getColorForTheme(int darkColorRes, int lightColorRes) {
        boolean isDarkTheme = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        return getResources().getColor(isDarkTheme ? darkColorRes : lightColorRes, getTheme());
    }

    private int getColorForThemeSafe(int darkColorRes, TextInputLayout layout, int lightColorRes) {
        boolean isDarkTheme = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        if (isDarkTheme) {
            return getResources().getColor(darkColorRes, getTheme());
        } else {
            ColorStateList defaultColors = layout.getDefaultHintTextColor();
            return defaultColors != null ? defaultColors.getDefaultColor()
                    : getResources().getColor(lightColorRes, getTheme());
        }
    }

    private void setupQRLoginLauncher() {
        qrLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String qrValue = result.getData().getStringExtra("QRCodeResult");
                        if (qrValue != null) {
                            Toast.makeText(this, getString(R.string.qr_login_result, qrValue), Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );

        View qrLogin = findViewById(R.id.qrLogin);
        if (qrLogin != null) {
            qrLogin.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, QRCodeActivity.class);
                qrLauncher.launch(intent);
            });
        }
    }

    private void setupSocialButtons() {
        ImageView facebook = findViewById(R.id.facebook_login);
        ImageView instagram = findViewById(R.id.instagram_login);

        if (facebook != null) {
            facebook.setOnClickListener(v ->
                    Snackbar.make(findViewById(android.R.id.content),
                            getString(R.string.facebook_login_not_implemented),
                            Snackbar.LENGTH_SHORT).show()
            );
        }
        if (instagram != null) {
            instagram.setOnClickListener(v ->
                    Snackbar.make(findViewById(android.R.id.content),
                            getString(R.string.instagram_login_not_implemented),
                            Snackbar.LENGTH_SHORT).show()
            );
        }
    }

    private void setupRegisterLink() {
        View registerTextView = findViewById(R.id.registerTextView);
        if (registerTextView != null) {
            registerTextView.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
                finish();
            });
        }
    }

    private void showLoginError(String message) {
        new Handler().postDelayed(() -> {
            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                    message,
                    Snackbar.LENGTH_SHORT);
            snackbar.show();

            emailEditText.setText("");
            passwordEditText.setText("");
            loginButton.setEnabled(false);
        }, 500);
    }
}