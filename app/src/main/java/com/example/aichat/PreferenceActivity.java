package com.example.aichat;

import android.content.Intent;
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

public class PreferenceActivity extends AppCompatActivity {

    private TextInputLayout minAgeInputLayout;
    private TextInputLayout maxAgeInputLayout;
    private RadioGroup genderGroup;
    private Button submitButton;
    private Button skippButton;
    private ImageView minAgeInfoIcon;
    private ImageView maxAgeInfoIcon;
    private ConnectionManager connectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        if (connectionManager == null) {
            connectionManager = new ConnectionManager(TokenManager.getToken(this));
        }

        connectionManager.SetCommandGot(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                switch (command.getOperation()) {
                    case "CreateToken":
                        String token = command.getData("token", String.class);
                        TokenManager.saveToken(PreferenceActivity.this, token);
                        break;
                    case "LoginIn":
                        ConnectionSingleton.getInstance().setConnectionManager(connectionManager);
                        Intent intent = new Intent(PreferenceActivity.this, MainActivity.class);
                        intent.putExtra("userId", command.getData("userId", int.class));
                        startActivity(intent);
                        finish();
                        break;
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
        submitButton.setOnClickListener(v -> {
            int maxAge = Integer.parseInt(maxAgeInputLayout.getEditText().getText().toString().trim());
            int minAge = Integer.parseInt(minAgeInputLayout.getEditText().getText().toString().trim());
            int selectedGenderId = genderGroup.getCheckedRadioButtonId();
            RadioButton selectedGender = findViewById(selectedGenderId);
            String gender = selectedGender.getTag().toString();
            Preference preference = new Preference(minAge,maxAge, gender);
            Command command = new Command("AddPreference");
            command.addData("preference", preference);
            connectionManager.SendCommand(command);
        });
        skippButton.setOnClickListener(v->{
            connectionManager.SendCommand(new Command("AddPreference"));
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


        maxAgeInputLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateAndUpdateAge(maxAgeInputLayout, minAgeInputLayout);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        minAgeInputLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateAndUpdateAge(minAgeInputLayout, maxAgeInputLayout);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        genderGroup.setOnCheckedChangeListener((group, checkedId) -> {
            validateAndUpdateGender();
        });
    }

    private void validateAndUpdateAge(TextInputLayout thisLayout, TextInputLayout otherLayout) {
        int thisAge = validateAge(thisLayout);
        if(thisAge>0) {
            int otherAge = validateAge(otherLayout);
            if (otherAge > 0) {
                otherLayout.setError(null);
                if (thisLayout == maxAgeInputLayout) {
                    thisLayout.setError(thisAge > otherAge ? null : "Максимальный возраст должен быть больше минимального");
                } else {
                    thisLayout.setError(thisAge < otherAge ? null : "Минимальный возраст должен быть меньше максимального");
                }
            }
            else{
                thisLayout.setError(null);

            }
        }
        else{

            thisLayout.setError("Возраст должен быть от 1 до 120 лет");
        }
        updateSubmitButtonState();
    }

    private void validateAndUpdateGender() {
        updateSubmitButtonState();
    }


    private int validateAge(TextInputLayout layout) {
        String ageText = layout.getEditText().getText().toString().trim();
        if (ageText.isEmpty()) {
            return -1;
        }
        try {
            int age = Integer.parseInt(ageText);
            return age >= 18 && age <= 120? age: -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean validateGender() {
        return genderGroup.getCheckedRadioButtonId() != -1;
    }



    private void updateSubmitButtonState() {
        int minAge = validateAge(minAgeInputLayout);
        int maxAge = validateAge(maxAgeInputLayout);
        boolean isAgeValid;
        if(minAge>0){
            isAgeValid = maxAge>minAge;
        }
        else{
            isAgeValid=false;
        }
        boolean isGenderValid = validateGender();

        submitButton.setEnabled(isAgeValid && isGenderValid);
    }
}