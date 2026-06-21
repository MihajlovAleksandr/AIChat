package com.example.aichat.view;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.example.aichat.controller.PreferenceController;
import com.example.aichat.model.entities.Preference;
import com.example.aichat.model.utils.JsonHelper;
import com.example.aichat.R;
import com.example.aichat.view.main.BaseActivity;
import com.example.aichat.view.helpers.DialogHelper;
import com.example.aichat.view.helpers.FullScreenHelper;
import com.example.aichat.view.theme.binders.PreferenceActivityThemeBinder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

public class PreferenceActivity extends BaseActivity {

    private PreferenceController controller;
    private boolean customThemeAllowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preference);

        FullScreenHelper.enableFullScreen(getWindow());

        TextInputLayout minAgeInputLayout =
                findViewById(R.id.minAgeInputLayout);

        TextInputLayout maxAgeInputLayout =
                findViewById(R.id.maxAgeInputLayout);

        RadioGroup genderGroup =
                findViewById(R.id.genderGroup);

        Button submitButton =
                findViewById(R.id.submitButton);

        Button skipButton =
                findViewById(R.id.skipButton);

        ImageView minAgeInfoIcon =
                findViewById(R.id.minAgeInfoIcon);

        ImageView maxAgeInfoIcon =
                findViewById(R.id.maxAgeInfoIcon);

        FloatingActionButton btnBack =
                findViewById(R.id.btnBack);

        if (minAgeInfoIcon != null) {
            minAgeInfoIcon.setOnClickListener(v ->
                    showPopup(v, getString(R.string.min_age_info)));
        }

        if (maxAgeInfoIcon != null) {
            maxAgeInfoIcon.setOnClickListener(v ->
                    showPopup(v, getString(R.string.max_age_info)));
        }

        Preference preference = null;

        String strPreference =
                getIntent().getStringExtra("preference");

        if (strPreference != null) {
            preference = JsonHelper.Deserialize(
                    strPreference,
                    Preference.class
            );
        }

        customThemeAllowed =
                preference != null;

        if (preference != null) {

            controller = new PreferenceController(
                    this,
                    minAgeInputLayout,
                    maxAgeInputLayout,
                    genderGroup,
                    submitButton,
                    skipButton,
                    preference
            );

            View progressDots =
                    findViewById(R.id.progressDots);

            if (progressDots != null) {
                progressDots.setVisibility(View.GONE);
            }

            if (btnBack != null) {

                btnBack.setVisibility(View.VISIBLE);

                btnBack.setOnClickListener(v ->
                        getOnBackPressedDispatcher().onBackPressed()
                );
            }

        } else {

            controller = new PreferenceController(
                    this,
                    minAgeInputLayout,
                    maxAgeInputLayout,
                    genderGroup,
                    submitButton,
                    skipButton
            );

            if (btnBack != null) {
                btnBack.setVisibility(View.GONE);
            }

            if (skipButton != null) {

                skipButton.setOnClickListener(v ->
                        DialogHelper.showBottomDialog(
                                this,
                                getString(R.string.preference_skip_title),
                                getString(R.string.preference_skip_message),
                                getString(R.string.preference_skip_ok),
                                () -> controller.sendSkipCommand()
                        )
                );
            }
        }

        if (controller != null) {
            controller.setupValidation();
        }

        if (customThemeAllowed) {
            PreferenceActivityThemeBinder.applyForAccountEditOnly(
                    this
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (customThemeAllowed) {
            PreferenceActivityThemeBinder.applyForAccountEditOnly(
                    this
            );
        }
    }

    public void showPopup(View anchorView, String message) {

        if (anchorView == null || isFinishing()) {
            return;
        }

        LayoutInflater inflater =
                (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        if (inflater == null) {
            return;
        }

        View popupView =
                inflater.inflate(R.layout.popup_info, null);

        PopupWindow popupWindow =
                new PopupWindow(
                        popupView,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        true
                );

        TextView popupText =
                popupView.findViewById(R.id.popupText);

        if (popupText != null) {
            popupText.setText(message);
        }

        popupWindow.showAsDropDown(
                anchorView,
                0,
                0,
                Gravity.START
        );
    }
}
