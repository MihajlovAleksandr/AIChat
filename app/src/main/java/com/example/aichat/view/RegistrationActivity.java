package com.example.aichat.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.aichat.R;
import com.example.aichat.controller.RegistrationController;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.EditText;
import android.widget.Button;

public class RegistrationActivity extends BaseActivity {

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button registrationButton;
    private TextView loginTextView;

    private RegistrationController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        loginTextView = findViewById(R.id.loginTextView);

        // Получаем текст полностью без префикса
        String fullText = getString(R.string.login_prompt);
        String linkText = getString(R.string.login_link_text);
        SpannableString spannableString = new SpannableString(fullText);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(ContextCompat.getColor(RegistrationActivity.this, R.color.link_color));
                ds.setUnderlineText(true);
            }
        };

        // Устанавливаем span только на кликабельную часть текста
        int startIndex = fullText.indexOf(linkText);
        int endIndex = startIndex + linkText.length();
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        loginTextView.setText(spannableString);
        loginTextView.setMovementMethod(LinkMovementMethod.getInstance());

        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.confirmPassword);
        registrationButton = findViewById(R.id.registration);

        controller = new RegistrationController(this,
                emailInputLayout,
                passwordInputLayout,
                confirmPasswordInputLayout,
                emailEditText,
                passwordEditText,
                confirmPasswordEditText,
                registrationButton);
    }

    public void showPopup(String message) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_info, null);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        TextView popupText = popupView.findViewById(R.id.popupText);
        popupText.setText(message);

        popupWindow.showAsDropDown(findViewById(android.R.id.content), 0, 0);
    }
}
