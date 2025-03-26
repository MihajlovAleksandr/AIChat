package com.example.aichat.view;

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
import com.google.android.material.textfield.TextInputLayout;
import android.widget.RadioGroup;

public class PreferenceActivity extends AppCompatActivity {

    private PreferenceController controller;
    private TextInputLayout minAgeInputLayout;
    private TextInputLayout maxAgeInputLayout;
    private RadioGroup genderGroup;
    private Button submitButton;
    private Button skippButton;
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
        skippButton = findViewById(R.id.skippButton);
        minAgeInfoIcon = findViewById(R.id.minAgeInfoIcon);
        maxAgeInfoIcon = findViewById(R.id.maxAgeInfoIcon);

        minAgeInfoIcon.setOnClickListener(v ->
                showPopup(v, "Какой минимальный возраст собеседника вам подходит? Укажите просто цифру, например, 18.\nЭто поможет нам найти людей, с которыми вам будет комфортно общаться."));
        maxAgeInfoIcon.setOnClickListener(v ->
                showPopup(v, "Какой максимальный возраст собеседника вам подходит? Укажите просто цифру, например, 23.\nЭто поможет нам найти людей, с которыми вам будет комфортно общаться."));

        controller = new PreferenceController(
                this,
                minAgeInputLayout,
                maxAgeInputLayout,
                genderGroup,
                submitButton,
                skippButton
        );
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
