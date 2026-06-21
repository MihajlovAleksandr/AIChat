package com.example.aichat.view;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.example.aichat.controller.VerifyEmailController;
import com.example.aichat.databinding.ActivityVerifyEmailBinding;
import com.example.aichat.view.main.BaseActivity;
import com.example.aichat.view.helpers.FullScreenHelper;

public class VerifyEmailActivity extends BaseActivity {

    private EditText[] codeFields;
    private GradientDrawable[] backgrounds;
    private int[] strokeColors;
    private boolean wasLastActionInput = false;

    private int defaultStrokeColor;
    private int partialStrokeColor;
    private int successColor;
    private int errorColor;

    private TextView tvCodeError;
    private VerifyEmailController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityVerifyEmailBinding binding = ActivityVerifyEmailBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        FullScreenHelper.enableFullScreen(getWindow());

        initFields(binding);
        initColors();
        initBackgrounds();
        setupController();
        setupBackButton(binding);
    }

    private void initFields(ActivityVerifyEmailBinding binding) {
        codeFields = new EditText[]{
                binding.code1,
                binding.code2,
                binding.code3,
                binding.code4,
                binding.code5,
                binding.code6
        };

        tvCodeError = binding.tvCodeError;
        tvCodeError.setVisibility(View.GONE);
    }

    private void initColors() {
        boolean isDarkTheme = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        if (isDarkTheme) {
            defaultStrokeColor = 0xFFAAAAAA;
            partialStrokeColor = 0xFFFFFFFF;
        } else {
            defaultStrokeColor = 0xFFAAAAAA;
            partialStrokeColor = 0xFF555555;
        }

        successColor = 0xFF4CAF50;
        errorColor = 0xFFFF5252;
    }

    private void initBackgrounds() {
        backgrounds = new GradientDrawable[codeFields.length];
        strokeColors = new int[codeFields.length];

        for (int i = 0; i < codeFields.length; i++) {
            GradientDrawable bg = (GradientDrawable) codeFields[i].getBackground();

            backgrounds[i] = bg;
            strokeColors[i] = defaultStrokeColor;

            bg.setStroke(3, defaultStrokeColor);
        }
    }

    private void setupController() {
        controller = new VerifyEmailController(this, codeFields);

        for (int i = 0; i < codeFields.length; i++) {
            final int index = i;
            final EditText field = codeFields[i];

            field.setTextColor(defaultStrokeColor);
            field.setTypeface(Typeface.DEFAULT);

            field.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    if (s.length() == 0 && after > 0) {
                        field.setTextColor(partialStrokeColor);
                        field.setTypeface(Typeface.DEFAULT_BOLD);
                    }
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    wasLastActionInput = count > before;
                }

                @Override
                public void afterTextChanged(Editable s) {
                    updateFieldStyle(field, s);
                    handleFocusAndValidation(index, s);
                }
            });

            field.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_DEL) {

                    if (field.getText().length() == 0 && index > 0) {
                        codeFields[index - 1].requestFocus();
                        codeFields[index - 1].setText("");
                    }
                }

                return controller.handleKeyEvent(keyCode, index);
            });

            field.setOnLongClickListener(v -> {
                controller.handlePaste();
                return true;
            });
        }
    }

    private void updateFieldStyle(EditText field, Editable s) {
        if (s.length() == 1) {
            field.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            field.setTypeface(Typeface.DEFAULT);
        }
    }

    private void handleFocusAndValidation(int index, Editable s) {
        if (s.length() == 1 && index < codeFields.length - 1) {
            codeFields[index + 1].requestFocus();
        }

        boolean allFilled = isCodeFullyFilled();

        if (!allFilled) {
            resetCodeErrorState();
            return;
        }

        if (wasLastActionInput && index == codeFields.length - 1) {
            hideCodeErrorOnly();
            showCodeCheckingState();
            controller.verifyCode();
        }
    }

    private boolean isCodeFullyFilled() {
        for (EditText field : codeFields) {
            if (field.getText() == null || field.getText().length() == 0) {
                return false;
            }
        }

        return true;
    }

    private void resetCodeErrorState() {
        hideCodeErrorOnly();

        for (int i = 0; i < codeFields.length; i++) {
            int toColor = codeFields[i].getText().length() > 0
                    ? partialStrokeColor
                    : defaultStrokeColor;

            animateStrokeColor(i, strokeColors[i], toColor);
            strokeColors[i] = toColor;
        }
    }

    private void hideCodeErrorOnly() {
        if (tvCodeError != null) {
            tvCodeError.setVisibility(View.GONE);
        }
    }

    private void showCodeCheckingState() {
        for (int i = 0; i < codeFields.length; i++) {
            animateStrokeColor(i, strokeColors[i], partialStrokeColor);
            strokeColors[i] = partialStrokeColor;
        }
    }

    private void animateStrokeColor(int index, int fromColor, int toColor) {
        if (fromColor == toColor) {
            return;
        }

        ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);

        anim.setDuration(250);
        anim.addUpdateListener(animation ->
                backgrounds[index].setStroke(3, (int) animation.getAnimatedValue())
        );

        anim.start();
    }

    public void showCodeSuccessAnimation() {
        hideCodeErrorOnly();

        for (int i = 0; i < codeFields.length; i++) {
            animateStrokeColor(i, strokeColors[i], successColor);
            strokeColors[i] = successColor;
        }
    }

    public void showCodeErrorAnimation() {
        tvCodeError.setVisibility(View.VISIBLE);
        tvCodeError.setText(getString(com.example.aichat.R.string.verify_code_error));

        for (int i = 0; i < codeFields.length; i++) {
            animateStrokeColor(i, strokeColors[i], errorColor);
            strokeColors[i] = errorColor;
        }
    }

    public String getCurrentCodeFromFields() {
        StringBuilder code = new StringBuilder();

        for (EditText field : codeFields) {
            code.append(field.getText() != null ? field.getText().toString() : "");
        }

        return code.toString();
    }

    private void setupBackButton(ActivityVerifyEmailBinding binding) {
        binding.btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegistrationActivity.class);

            for (int i = 0; i < codeFields.length; i++) {
                intent.putExtra("code" + i, codeFields[i].getText().toString());
            }

            startActivityClean(intent, true);
        });
    }
}
