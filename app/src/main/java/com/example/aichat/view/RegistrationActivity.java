package com.example.aichat.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.aichat.R;
import com.example.aichat.controller.RegistrationController;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegistrationActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        FullScreenHelper.enableFullScreen(getWindow());

        TextInputLayout emailInputLayout = findViewById(R.id.emailInputLayout);
        TextInputLayout passwordInputLayout = findViewById(R.id.passwordInputLayout);
        TextInputLayout confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        TextInputEditText emailEditText = findViewById(R.id.email);
        TextInputEditText passwordEditText = findViewById(R.id.password);
        TextInputEditText confirmPasswordEditText = findViewById(R.id.confirmPassword);
        MaterialButton registrationButton = findViewById(R.id.registration);
        TextView loginTextView = findViewById(R.id.loginTextView);

        if (loginTextView != null) {
            loginTextView.setOnClickListener(v -> {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });
        }

        LanguageHandler languageHandler = new LanguageHandler(this);
        LanguageMenuHelper languageMenuHelper = new LanguageMenuHelper(languageHandler);
        TextView btnLanguage = findViewById(R.id.btnLanguage);
        languageMenuHelper.attachToButton(btnLanguage);

        applyThemeColors(emailInputLayout, passwordInputLayout, confirmPasswordInputLayout);

        new RegistrationController(
                this,
                emailInputLayout,
                passwordInputLayout,
                confirmPasswordInputLayout,
                emailEditText,
                passwordEditText,
                confirmPasswordEditText,
                registrationButton
        );
    }

    private void applyThemeColors(TextInputLayout emailLayout, TextInputLayout passwordLayout, TextInputLayout confirmLayout) {
        boolean isDarkTheme = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        int endIconColor = isDarkTheme
                ? getResources().getColor(R.color.textinput_endicon_dark, getTheme())
                : getResources().getColor(R.color.textinput_endicon_light, getTheme());

        emailLayout.setEndIconTintList(android.content.res.ColorStateList.valueOf(endIconColor));
        passwordLayout.setEndIconTintList(android.content.res.ColorStateList.valueOf(endIconColor));
        confirmLayout.setEndIconTintList(android.content.res.ColorStateList.valueOf(endIconColor));

        int hintColor;
        if (!isDarkTheme && emailLayout.getDefaultHintTextColor() != null) {
            hintColor = emailLayout.getDefaultHintTextColor().getDefaultColor();
        } else {
            hintColor = isDarkTheme
                    ? getResources().getColor(R.color.primaryTextDark, getTheme())
                    : 0xFF000000; // fallback
        }

        emailLayout.setDefaultHintTextColor(android.content.res.ColorStateList.valueOf(hintColor));
        passwordLayout.setDefaultHintTextColor(android.content.res.ColorStateList.valueOf(hintColor));
        confirmLayout.setDefaultHintTextColor(android.content.res.ColorStateList.valueOf(hintColor));
    }
}