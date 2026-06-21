package com.example.aichat.view;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import com.example.aichat.controller.LoginController;
import com.example.aichat.model.LocaleManager;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.QRCodeActivity;
import com.example.aichat.R;
import com.example.aichat.view.helpers.HoneycombRevealView;
import com.example.aichat.view.main.BaseActivity;
import com.example.aichat.view.helpers.FullScreenHelper;
import com.example.aichat.view.main.chat.helpers.UiAnimations;
import com.example.aichat.view.main.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import java.util.UUID;

public class LoginActivity extends BaseActivity {

    private MaterialButton loginButton;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private EditText emailEditText;
    private EditText passwordEditText;
    private ActivityResultLauncher<Intent> qrLauncher;
    private LoginController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_AIChat);

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
        removeLegacySocialButtons();
        configureGoogleSignInButton();
        setupQRLoginLauncher();
        applyThemeColors();
        setupLanguageButton();
    }


    private void configureGoogleSignInButton() {
        ImageView googleButton =
                findViewById(R.id.sign_in_button);

        if (googleButton == null) {
            return;
        }

        int buttonSize = dpToPx(64);
        int topMargin = dpToPx(20);

        googleButton.setAdjustViewBounds(true);
        googleButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        googleButton.setMinimumWidth(buttonSize);
        googleButton.setMinimumHeight(buttonSize);

        android.view.ViewGroup.LayoutParams rawParams =
                googleButton.getLayoutParams();

        if (rawParams instanceof android.view.ViewGroup.MarginLayoutParams) {
            android.view.ViewGroup.MarginLayoutParams params =
                    (android.view.ViewGroup.MarginLayoutParams) rawParams;

            params.width = buttonSize;
            params.height = buttonSize;
            params.topMargin = Math.max(params.topMargin, topMargin);
            params.leftMargin = 0;
            params.rightMargin = 0;

            if (params instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) params).gravity = Gravity.CENTER_HORIZONTAL;
            } else if (params instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) params).gravity = Gravity.CENTER_HORIZONTAL;
            }

            googleButton.setLayoutParams(params);
        }

        View parent = getParentView(googleButton);
        centerContainerHorizontally(parent);
        centerContainerHorizontally(getParentView(parent));
    }

    private void centerContainerHorizontally(View view) {
        if (view == null) {
            return;
        }

        if (view instanceof LinearLayout) {
            ((LinearLayout) view).setGravity(Gravity.CENTER_HORIZONTAL);
        }

        android.view.ViewGroup.LayoutParams rawParams = view.getLayoutParams();

        if (rawParams instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) rawParams).gravity = Gravity.CENTER_HORIZONTAL;
            view.setLayoutParams(rawParams);
        } else if (rawParams instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) rawParams).gravity = Gravity.CENTER_HORIZONTAL;
            view.setLayoutParams(rawParams);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources().getDisplayMetrics().density
        );
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
                String currentLang = LocaleManager.normalizeLanguage(prefs.getString("app_language", "en"));
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
                    String selectedLang = LocaleManager.normalizeLanguage(codes[position]);

                    if (!selectedLang.equals(currentLang)) {
                        prefs.edit().putString("app_language", selectedLang).apply();
                        LocaleManager.setLocale(this, selectedLang);

                        HoneycombRevealView honeycombRevealView =
                                new HoneycombRevealView(this);

                        addContentView(
                                honeycombRevealView,
                                new FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT
                                )
                        );

                        honeycombRevealView.start(
                                false,
                                () -> restartAppTo(LoginActivity.class)
                        );
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

        startActivityClean(
                new Intent(this, MainActivity.class),
                true
        );
    }

    private void configureEditText(EditText editText, TextInputLayout layout) {
        if (editText == null || layout == null) return;

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

    private int getColorForTheme(int darkColorRes, int lightColorRes) {
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
        }

        ColorStateList defaultColors =
                layout.getDefaultHintTextColor();

        return defaultColors != null
                ? defaultColors.getDefaultColor()
                : getResources().getColor(
                lightColorRes,
                getTheme()
        );
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

                        if (qrValue != null && controller != null) {
                            controller.qrLogin(qrValue);
                        }
                    }
                }
        );

        View qrLogin =
                findViewById(R.id.qrLogin);

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

    private void removeLegacySocialButtons() {
        hideLegacySocialButton(
                findViewById(R.id.facebook_login)
        );

        hideLegacySocialButton(
                findViewById(R.id.instagram_login)
        );
    }

    private void hideLegacySocialButton(View view) {
        if (view == null) {
            return;
        }

        View firstParent = getParentView(view);
        View secondParent = getParentView(firstParent);

        view.setOnClickListener(null);
        view.setClickable(false);
        view.setFocusable(false);
        view.setVisibility(View.GONE);

        hideContainerIfEmpty(firstParent);
        hideContainerIfEmpty(secondParent);
    }

    private View getParentView(View view) {
        if (view == null || !(view.getParent() instanceof View)) {
            return null;
        }

        return (View) view.getParent();
    }

    private void hideContainerIfEmpty(View view) {
        if (!(view instanceof android.view.ViewGroup)
                || view.getId() == android.R.id.content) {
            return;
        }

        android.view.ViewGroup group =
                (android.view.ViewGroup) view;

        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);

            if (child != null && child.getVisibility() == View.VISIBLE) {
                return;
            }
        }

        group.setVisibility(View.GONE);
    }

    private void setupRegisterLink() {
        View registerTextView =
                findViewById(R.id.registerTextView);

        if (registerTextView != null) {
            registerTextView.setOnClickListener(v -> {
                startActivityClean(
                        new Intent(
                                LoginActivity.this,
                                RegistrationActivity.class
                        ),
                        true
                );
            });
        }
    }

    private void showLoginError(String message) {
        new Handler().postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;

            emailEditText.clearFocus();
            passwordEditText.clearFocus();

            View root =
                    findViewById(android.R.id.content);

            if (root != null) {
                root.requestFocus();
            }

            emailInputLayout.setError(null);
            emailInputLayout.setErrorEnabled(false);

            passwordInputLayout.setError(null);
            passwordInputLayout.setErrorEnabled(false);

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
        }, 300);
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

        if (controller != null) {
            controller.handleGoogleSignInResult(
                    requestCode,
                    data
            );
        }
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            controller.destroy();
            controller = null;
        }

        super.onDestroy();
    }
}
