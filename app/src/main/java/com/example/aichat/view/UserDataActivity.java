package com.example.aichat.view;


import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aichat.R;
import com.example.aichat.model.entities.UserData;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class UserDataActivity extends BaseActivity {
    private TextInputLayout nameInputLayout;
    private TextInputLayout ageInputLayout;
    private RadioGroup genderGroup;
    private Button submitButton;
    private ImageView nameInfoIcon;
    private ImageView ageInfoIcon;

    private com.example.aichat.controller.UserDataController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_data);

        nameInputLayout = findViewById(R.id.nameInputLayout);
        ageInputLayout = findViewById(R.id.ageInputLayout);
        genderGroup = findViewById(R.id.genderGroup);
        submitButton = findViewById(R.id.submitButton);
        nameInfoIcon = findViewById(R.id.nameInfoIcon);
        ageInfoIcon = findViewById(R.id.ageInfoIcon);
        UserData userData = intent.getSerializableExtra("userData", UserData.class);
        if(userData==null) {
            controller = new com.example.aichat.controller.UserDataController(
                    this,
                    nameInputLayout,
                    ageInputLayout,
                    genderGroup,
                    submitButton
            );
        }
        else {
            controller = new com.example.aichat.controller.UserDataController(
                    this,
                    nameInputLayout,
                    ageInputLayout,
                    genderGroup,
                    submitButton,
                    userData
            );
            findViewById(R.id.progressDots).setVisibility(View.GONE);
        }
        // Используем строковые ресурсы на английском языке для всплывающих подсказок
        nameInfoIcon.setOnClickListener(v -> showPopup(v, getString(R.string.name_popup_info)));
        ageInfoIcon.setOnClickListener(v -> showPopup(v, getString(R.string.age_popup_info)));
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
