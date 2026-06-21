
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
import com.example.aichat.model.entities.UserData;
import com.example.aichat.model.utils.JsonHelper;
import com.example.aichat.R;
import com.example.aichat.view.main.BaseActivity;
import com.example.aichat.view.helpers.FullScreenHelper;
import com.example.aichat.view.theme.binders.UserDataActivityThemeBinder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

public class UserDataActivity extends BaseActivity {

    private boolean customThemeAllowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_user_data
        );

        FullScreenHelper.enableFullScreen(
                getWindow()
        );

        TextInputLayout nameInputLayout =
                findViewById(
                        R.id.nameInputLayout
                );

        TextInputLayout ageInputLayout =
                findViewById(
                        R.id.ageInputLayout
                );

        RadioGroup genderGroup =
                findViewById(
                        R.id.genderGroup
                );

        Button submitButton =
                findViewById(
                        R.id.submitButton
                );

        ImageView nameInfoIcon =
                findViewById(
                        R.id.nameInfoIcon
                );

        ImageView ageInfoIcon =
                findViewById(
                        R.id.ageInfoIcon
                );

        String strUserData =
                getIntent().getStringExtra(
                        "userData"
                );

        UserData userData =
                null;

        if (strUserData != null) {
            userData =
                    JsonHelper.Deserialize(
                            strUserData,
                            UserData.class
                    );
        }


        customThemeAllowed =
                userData != null;

        com.example.aichat.controller.UserDataController controller;

        if (userData == null) {
            controller =
                    new com.example.aichat.controller.UserDataController(
                            this,
                            nameInputLayout,
                            ageInputLayout,
                            genderGroup,
                            submitButton
                    );

        } else {

            controller =
                    new com.example.aichat.controller.UserDataController(
                            this,
                            nameInputLayout,
                            ageInputLayout,
                            genderGroup,
                            submitButton,
                            userData
                    );

            View progressDots =
                    findViewById(
                            R.id.progressDots
                    );

            if (progressDots != null) {
                progressDots.setVisibility(
                        View.GONE
                );
            }
        }

        if (customThemeAllowed) {
            UserDataActivityThemeBinder.applyForAccountEditOnly(
                    this
            );
        }

        if (nameInfoIcon != null) {
            nameInfoIcon.setOnClickListener(v ->
                    showPopup(
                            v,
                            getString(
                                    R.string.name_popup_info
                            )
                    )
            );
        }

        if (ageInfoIcon != null) {
            ageInfoIcon.setOnClickListener(v ->
                    showPopup(
                            v,
                            getString(
                                    R.string.age_popup_info
                            )
                    )
            );
        }

        FloatingActionButton btnBack =
                findViewById(
                        R.id.btnBack
                );

        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    getOnBackPressedDispatcher()
                            .onBackPressed()
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (customThemeAllowed) {
            UserDataActivityThemeBinder.applyForAccountEditOnly(
                    this
            );
        }
    }

    public void showPopup(View anchorView, String message) {
        if (anchorView == null || isFinishing()) {
            return;
        }

        LayoutInflater inflater =
                (LayoutInflater) getSystemService(
                        LAYOUT_INFLATER_SERVICE
                );

        if (inflater == null) {
            return;
        }

        View popupView =
                inflater.inflate(
                        R.layout.popup_info,
                        null
                );

        PopupWindow popupWindow =
                new PopupWindow(
                        popupView,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        true
                );

        TextView popupText =
                popupView.findViewById(
                        R.id.popupText
                );

        if (popupText != null) {
            popupText.setText(
                    message
            );
        }

        popupWindow.showAsDropDown(
                anchorView,
                0,
                0,
                Gravity.START
        );
    }
}
