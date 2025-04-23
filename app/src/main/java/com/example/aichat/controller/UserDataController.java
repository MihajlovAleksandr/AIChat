package com.example.aichat.controller;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Button;

import com.example.aichat.R;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.UserData;
import com.example.aichat.view.PreferenceActivity;
import com.example.aichat.view.UserDataActivity;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class UserDataController {

    private UserDataActivity activity;
    private TextInputLayout nameInputLayout;
    private TextInputLayout ageInputLayout;
    private RadioGroup genderGroup;
    private Button submitButton;
    private ConnectionManager connectionManager;
    private boolean isEditMode = false;
    private UserData userDataToEdit;

    // Первый конструктор (оригинальный)
    public UserDataController(UserDataActivity activity,
                              TextInputLayout nameInputLayout,
                              TextInputLayout ageInputLayout,
                              RadioGroup genderGroup,
                              Button submitButton) {
        this(activity, nameInputLayout, ageInputLayout, genderGroup, submitButton, null);
    }

    // Второй конструктор для редактирования данных
    public UserDataController(UserDataActivity activity,
                              TextInputLayout nameInputLayout,
                              TextInputLayout ageInputLayout,
                              RadioGroup genderGroup,
                              Button submitButton,
                              UserData userDataToEdit) {
        this.activity = activity;
        this.nameInputLayout = nameInputLayout;
        this.ageInputLayout = ageInputLayout;
        this.genderGroup = genderGroup;
        this.submitButton = submitButton;
        this.userDataToEdit = userDataToEdit;

        if (userDataToEdit != null) {
            this.isEditMode = true;
            populateFormWithUserData();
        }

        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        if (connectionManager == null) {
            ConnectionSingleton.getInstance().setConnectionManager(new ConnectionManager(""));
            connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        }

        connectionManager.addConnectionEvent(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                if (Objects.equals(command.getOperation(), "UserDataAdded")) {
                    ConnectionSingleton.getInstance().setConnectionManager(connectionManager);
                    Intent intent = new Intent(activity, PreferenceActivity.class);
                    activity.startActivity(intent);
                    activity.finish();
                }
                else if(Objects.equals(command.getOperation(), "UserDataUpdated")){
                    connectionManager.removeConnectionEvent(this);
                    activity.finish();
                }
            }

            @Override
            public void OnConnectionFailed() {
                Log.e("UserDataController", "Connection failed");
            }

            @Override
            public void OnOpen() {
                Log.d("UserDataController", "Connection opened");
            }
        });
        submitButton.setEnabled(true);
        setupValidation();
        setupSubmitButton();
    }

    private void populateFormWithUserData() {
        if (userDataToEdit != null) {
            nameInputLayout.getEditText().setText(userDataToEdit.getName());
            ageInputLayout.getEditText().setText(String.valueOf(userDataToEdit.getAge()));
            char gender = userDataToEdit.getGender();
            int radioButtonId = -1;
            for (int i = 0; i < genderGroup.getChildCount(); i++) {
                RadioButton radioButton = (RadioButton) genderGroup.getChildAt(i);
                if (radioButton.getTag().toString().charAt(0) == gender) {
                    radioButtonId = radioButton.getId();
                    break;
                }
            }
            if (radioButtonId != -1) {
                genderGroup.check(radioButtonId);
            }
            submitButton.setText(R.string.update_button);
        }
    }

    private void setupValidation() {
        nameInputLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* не используется */ }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateAndUpdateName();
            }
            @Override
            public void afterTextChanged(Editable s) { /* не используется */ }
        });

        ageInputLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* не используется */ }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateAndUpdateAge();
            }
            @Override
            public void afterTextChanged(Editable s) { /* не используется */ }
        });

        genderGroup.setOnCheckedChangeListener((group, checkedId) -> {
            validateAndUpdateGender();
        });
    }

    private void setupSubmitButton() {
        submitButton.setOnClickListener(v -> submitUserData());
    }

    private void submitUserData() {
        String name = nameInputLayout.getEditText().getText().toString().trim();
        int age = Integer.parseInt(ageInputLayout.getEditText().getText().toString().trim());
        int selectedGenderId = genderGroup.getCheckedRadioButtonId();
        RadioButton selectedGender = activity.findViewById(selectedGenderId);
        String gender = selectedGender.getTag().toString();

        UserData userData = new UserData(name, age, gender.charAt(0));
        Command command = new Command(isEditMode ? "UpdateUserData" : "AddUserData");
        command.addData("userData", userData);
        connectionManager.SendCommand(command);
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

    private void updateNameError(boolean isValid) {
        if (isValid) {
            nameInputLayout.setError(null);
        } else {
            nameInputLayout.setError(activity.getString(R.string.name_validation_error));
        }
    }

    private void updateAgeError(boolean isValid) {
        if (isValid) {
            ageInputLayout.setError(null);
        } else {
            ageInputLayout.setError(activity.getString(R.string.age_range_error));
        }
    }

    private void updateSubmitButtonState() {
        boolean isEnabled = validateName() && validateAge() && validateGender();
        submitButton.setEnabled(isEnabled);
    }
}