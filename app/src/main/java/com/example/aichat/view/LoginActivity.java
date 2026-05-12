package com.example.aichat.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.aichat.QRCodeActivity;
import com.example.aichat.R;
import com.example.aichat.controller.LoginController;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.util.HoneycombRevealView;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.view.main.chat.ui.UiAnimations;
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

        ImageView googleSignInButton =
                findViewById(R.id.sign_in_button);

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

        setupLanguageButton();
    }

    private void setupLanguageButton() {
        View languageButton = findViewById(R.id.btnLanguage);
        if (languageButton == null) return;

        languageButton.setOnClickListener(v -> {
            UiAnimations.animatePress(v, true);

            new Handler().postDelayed(() -> {
                String[] languages = getResources().getStringArray(R.array.languages);
                String[] codes = getResources().getStringArray(R.array.language_codes);
                SharedPreferences prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE);
                String currentLang = prefs.getString("app_language", "en");
                int currentIndex = 0;

                for (int i = 0; i < codes.length; i++) {
                    if (codes[i].equals(currentLang)) {
                        currentIndex = i;
                        break;
                    }
                }

                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.select_language)
                        .setSingleChoiceItems(languages, currentIndex, null)
                        .create();

                UiAnimations.setupLanguageDialogAnimations(dialog, position -> {
                    String selectedLang = codes[position];
                    if (!selectedLang.equals(currentLang)) {
                        prefs.edit().putString("app_language", selectedLang).apply();
                        HoneycombRevealView honey = new HoneycombRevealView(this);
                        addContentView(honey, new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                        ));
                        honey.start(false, () -> restartAppTo(LoginActivity.class));
                    }
                });

                dialog.show();
            }, 120);
        });
    }

    private void resetConnectionState() {
        SecurePreferencesManager.removeAuthToken(this);
        SecurePreferencesManager.removeUserId(this);
    }

    private void onLoginSuccess(UUID userId, String token) {
        SecurePreferencesManager.saveUserId(this, userId);
        SecurePreferencesManager.saveAuthToken(this, token);

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void configureEditText(
            EditText editText,
            TextInputLayout layout
    ) {
        layout.setImportantForAutofill(
                View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        );

        editText.setImportantForAutofill(
                View.IMPORTANT_FOR_AUTOFILL_NO
        );

        editText.setAutofillHints("");

        editText.setHighlightColor(0x00000000);

        editText.setTextIsSelectable(true);
    }

    private void applyThemeColors() {

        int endIconColor =
                getColorForTheme(
                        R.color.textinput_endicon_dark,
                        R.color.textinput_endicon_light
                );

        emailInputLayout.setEndIconTintList(
                ColorStateList.valueOf(endIconColor)
        );

        passwordInputLayout.setEndIconTintList(
                ColorStateList.valueOf(endIconColor)
        );

        int hintColor =
                getColorForThemeSafe(
                        R.color.primaryTextDark,
                        emailInputLayout,
                        R.color.primaryTextLight
                );

        emailInputLayout.setDefaultHintTextColor(
                ColorStateList.valueOf(hintColor)
        );

        passwordInputLayout.setDefaultHintTextColor(
                ColorStateList.valueOf(hintColor)
        );
    }

    private int getColorForTheme(
            int darkColorRes,
            int lightColorRes
    ) {

        boolean isDarkTheme =
                (getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        return getResources().getColor(
                isDarkTheme ? darkColorRes : lightColorRes,
                getTheme()
        );
    }

    private int getColorForThemeSafe(
            int darkColorRes,
            TextInputLayout layout,
            int lightColorRes
    ) {

        boolean isDarkTheme =
                (getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        if (isDarkTheme) {
            return getResources().getColor(
                    darkColorRes,
                    getTheme()
            );
        } else {

            ColorStateList defaultColors =
                    layout.getDefaultHintTextColor();

            return defaultColors != null
                    ? defaultColors.getDefaultColor()
                    : getResources().getColor(
                    lightColorRes,
                    getTheme()
            );
        }
    }

    private void setupQRLoginLauncher() {

        qrLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    if (result.getResultCode() == RESULT_OK
                            && result.getData() != null) {

                        String qrValue =
                                result.getData()
                                        .getStringExtra("QRCodeResult");

                        if (qrValue != null) {
                            controller.qrLogin(qrValue);
                        }
                    }
                }
        );

        View qrLogin = findViewById(R.id.qrLogin);

        if (qrLogin != null) {

            qrLogin.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                LoginActivity.this,
                                QRCodeActivity.class
                        );

                qrLauncher.launch(intent);
            });
        }
    }

    private void setupSocialButtons() {

        ImageView facebook =
                findViewById(R.id.facebook_login);

        ImageView instagram =
                findViewById(R.id.instagram_login);

        if (facebook != null) {

            facebook.setOnClickListener(v ->
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            getString(R.string.facebook_login_not_implemented),
                            Snackbar.LENGTH_SHORT
                    ).show()
            );
        }

        if (instagram != null) {

            instagram.setOnClickListener(v ->
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            getString(R.string.instagram_login_not_implemented),
                            Snackbar.LENGTH_SHORT
                    ).show()
            );
        }
    }

    private void setupRegisterLink() {

        View registerTextView =
                findViewById(R.id.registerTextView);

        if (registerTextView != null) {

            registerTextView.setOnClickListener(v -> {

                startActivity(
                        new Intent(
                                LoginActivity.this,
                                RegistrationActivity.class
                        )
                );

                finish();
            });
        }
    }

    private void showLoginError(String message) {

        new Handler().postDelayed(() -> {

            emailEditText.clearFocus();
            passwordEditText.clearFocus();

            View root =
                    findViewById(android.R.id.content);

            if (root != null) {
                root.requestFocus();
            }

            emailInputLayout.setError(null);
            passwordInputLayout.setError(null);

            emailEditText.setText("");
            passwordEditText.setText("");

            loginButton.setEnabled(false);

            Snackbar snackbar =
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            message,
                            Snackbar.LENGTH_SHORT
                    );

            snackbar.show();

        }, 500);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        controller.handleGoogleSignInResult(
                requestCode,
                data
        );
    }
}