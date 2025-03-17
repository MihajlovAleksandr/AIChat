package com.example.aichat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class UserDataActivity extends AppCompatActivity {

    private TextInputLayout nameInputLayout;
    private TextInputLayout ageInputLayout;
    private RadioGroup genderGroup;
    private Button submitButton;
    private ImageView nameInfoIcon;
    private ImageView ageInfoIcon;
    private ConnectionManager connectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectionManager = Singleton.getInstance().getConnectionManager();
        if (connectionManager == null) {
            connectionManager = new ConnectionManager(TokenManager.getToken(this));
        }

        connectionManager.SetCommandGot(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                if (Objects.equals(command.getOperation(), "UserDataAdded")) {
                    Singleton.getInstance().setConnectionManager(connectionManager);
                    Intent intent = new Intent(UserDataActivity.this, PreferenceActivity.class);
                    startActivity(intent);
                }
            }

            @Override
            public void OnConnectionFailed() {
                Log.e("VerifyEmailActivity", "Connection failed");
            }

            @Override
            public void OnOpen() {
                Log.d("VerifyEmailActivity", "Connection opened");
            }
        });
        setContentView(R.layout.activity_user_data);

        nameInputLayout = findViewById(R.id.nameInputLayout);
        ageInputLayout = findViewById(R.id.ageInputLayout);
        genderGroup = findViewById(R.id.genderGroup);
        submitButton = findViewById(R.id.submitButton);
        nameInfoIcon = findViewById(R.id.nameInfoIcon);
        ageInfoIcon = findViewById(R.id.ageInfoIcon);

        nameInfoIcon.setOnClickListener(v -> showPopup(v, "Как бы вы хотели, чтобы вас называли? Напишите своё имя, ник или что угодно, что вам нравится.\nГлавное, чтобы оно было удобно и вы чувствовали себя комфортно."));

        ageInfoIcon.setOnClickListener(v -> showPopup(v, "Сколько вам лет? Просто укажите цифру, например, 19.\nСовсем не обязательно указывать правду, нам всем всегда 18)\nНо помните, это поможет нам подобрать людей, которые будут вам интересны."));

        submitButton.setOnClickListener(v -> {
            String name = nameInputLayout.getEditText().getText().toString().trim();
            int age = Integer.parseInt(ageInputLayout.getEditText().getText().toString().trim());
            int selectedGenderId = genderGroup.getCheckedRadioButtonId();
            RadioButton selectedGender = findViewById(selectedGenderId);
            String gender = selectedGender.getTag().toString();
            UserData userData = new UserData(name, age, gender.charAt(0));
            Command command = new Command("AddUserData");
            command.addData("userData", userData);
            connectionManager.SendCommand(command);
        });

        setupValidation();
    }

    private void showPopup(View anchorView, String message) {
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

    private void setupValidation() {
        nameInputLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateAndUpdateName();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ageInputLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateAndUpdateAge();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        genderGroup.setOnCheckedChangeListener((group, checkedId) -> {
            validateAndUpdateGender();
        });
    }

    private void validateAndUpdateName() {
        boolean isNameValid = validateName();
        updateNameError(isNameValid);
        updateSubmitButtonState();
    }

    private void validateAndUpdateAge() {
        boolean isAgeValid = validateAge();
        updateAgeError(isAgeValid);
        updateSubmitButtonState();
    }

    private void validateAndUpdateGender() {
        boolean isGenderValid = validateGender();
        updateSubmitButtonState();
    }

    private boolean validateName() {
        String name = nameInputLayout.getEditText().getText().toString().trim();
        return !name.isEmpty() && name.length() <= 50;
    }

    private boolean validateAge() {
        String ageText = ageInputLayout.getEditText().getText().toString().trim();
        if (ageText.isEmpty()) {
            return false;
        }
        try {
            int age = Integer.parseInt(ageText);
            return age >= 18 && age <= 120;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean validateGender() {
        return genderGroup.getCheckedRadioButtonId() != -1;
    }

    private void updateNameError(boolean isNameValid) {
        if (isNameValid) {
            nameInputLayout.setError(null);
        } else {
            nameInputLayout.setError("Имя должно содержать от 1 до 50 символов");
        }
    }

    private void updateAgeError(boolean isAgeValid) {
        if (isAgeValid) {
            ageInputLayout.setError(null);
        } else {
            ageInputLayout.setError("Возраст должен быть от 1 до 120 лет");
        }
    }

    private void updateSubmitButtonState() {
        boolean isNameValid = validateName();
        boolean isAgeValid = validateAge();
        boolean isGenderValid = validateGender();

        submitButton.setEnabled(isNameValid && isAgeValid && isGenderValid);
    }
}