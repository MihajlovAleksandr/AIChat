package com.example.aichat.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aichat.R;
import com.example.aichat.controller.PreferenceController;
import com.example.aichat.model.entities.Preference;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.RadioGroup;

public class PreferenceActivity extends BaseActivity {

    private PreferenceController controller;
    private TextInputLayout minAgeInputLayout;
    private TextInputLayout maxAgeInputLayout;
    private RadioGroup genderGroup;
    private Button submitButton;
    private Button skipButton;
    private ImageView minAgeInfoIcon;
    private ImageView maxAgeInfoIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preference);

        minAgeInputLayout = findViewById(R.id.minAgeInputLayout);
        maxAgeInputLayout = findViewById(R.id.maxAgeInputLayout);
        genderGroup = findViewById(R.id.genderGroup);
        submitButton = findViewById(R.id.submitButton);
        skipButton = findViewById(R.id.skipButton);
        minAgeInfoIcon = findViewById(R.id.minAgeInfoIcon);
        maxAgeInfoIcon = findViewById(R.id.maxAgeInfoIcon);

        minAgeInfoIcon.setOnClickListener(v ->
                showPopup(v, getString(R.string.min_age_info)));
        maxAgeInfoIcon.setOnClickListener(v ->
                showPopup(v, getString(R.string.max_age_info)));
        Intent intent = getIntent();
        Preference preference = intent.getSerializableExtra("preference",Preference.class);
        if(preference!=null){
            controller = new PreferenceController(
                    this,
                    minAgeInputLayout,
                    maxAgeInputLayout,
                    genderGroup,
                    submitButton,
                    skipButton,
                    preference
            );
            findViewById(R.id.progressDots).setVisibility(View.GONE);
        }
        else{
            controller = new PreferenceController(
                this,
                minAgeInputLayout,
                maxAgeInputLayout,
                genderGroup,
                submitButton,
                skipButton
            );
        }
        controller.setupValidation();
    }

    public void showPopup(View anchorView, String message) {
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

        popupWindow.showAsDropDown(anchorView, 0, 0, Gravity.START);
    }
}