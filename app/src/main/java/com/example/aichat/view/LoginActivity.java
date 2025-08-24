package com.example.aichat.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.aichat.R;
import com.example.aichat.controller.LoginController;
import com.example.aichat.model.database.DatabaseManager;
import com.google.android.gms.common.SignInButton;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends BaseActivity {
    private LoginController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        DatabaseManager.init(this);
        setupRegisterLink();

        TextInputLayout emailInputLayout = findViewById(R.id.emailInputLayout);
        TextInputLayout passwordInputLayout = findViewById(R.id.passwordInputLayout);
        EditText emailEditText = findViewById(R.id.email);
        EditText passwordEditText = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.login);
        ImageView imageView = findViewById(R.id.imageView);
        SignInButton googleSignInButton = findViewById(R.id.sign_in_button);

        controller = new LoginController(
                this,
                emailInputLayout,
                passwordInputLayout,
                emailEditText,
                passwordEditText,
                loginButton,
                imageView,
                googleSignInButton
        );
    }

    private void setupRegisterLink() {
        TextView registerTextView = findViewById(R.id.registerTextView);
        String fullText = getString(R.string.register_prompt);
        String linkText = getString(R.string.register_link_text);

        SpannableString spannableString = new SpannableString(fullText);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
                finish();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(ContextCompat.getColor(LoginActivity.this, R.color.link_color));
                ds.setUnderlineText(true);
            }
        };

        int startIndex = fullText.indexOf(linkText);
        spannableString.setSpan(clickableSpan, startIndex, startIndex + linkText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        registerTextView.setText(spannableString);
        registerTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        controller.handleGoogleSignInResult(requestCode, data);
    }
}