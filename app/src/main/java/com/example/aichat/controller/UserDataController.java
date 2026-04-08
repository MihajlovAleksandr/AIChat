package com.example.aichat.controller;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Button;

import com.example.aichat.R;
import com.example.aichat.dto.request.UserDataRequest;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Gender;
import com.example.aichat.model.entities.UserData;
import com.example.aichat.view.PreferenceActivity;
import com.example.aichat.view.UserDataActivity;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class UserDataController {

    private final UserDataActivity activity;
    private final TextInputLayout nameInputLayout;
    private final TextInputLayout ageInputLayout;
    private final RadioGroup genderGroup;
    private final Button submitButton;
    private final ConnectionManager connectionManager;
    private boolean isEditMode = false;
    private final UserData userDataToEdit;
    private String originalName = null;
    private int originalAge = -1;
    private Gender originalGender = null;

    public UserDataController(UserDataActivity activity,
                              TextInputLayout nameInputLayout,
                              TextInputLayout ageInputLayout,
                              RadioGroup genderGroup,
                              Button submitButton) {
        this(activity, nameInputLayout, ageInputLayout, genderGroup, submitButton, null);
    }

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

        ConnectionManager manager = ConnectionSingleton.getInstance().getConnectionManager();
        if (manager == null) {
            ConnectionSingleton.getInstance().setConnectionManager(new ConnectionManager(""));
            manager = ConnectionSingleton.getInstance().getConnectionManager();
        }
        connectionManager = manager;

        if (connectionManager != null) {
            connectionManager.addConnectionEvent(new OnConnectionEvents() {
                @Override
                public void OnCommandGot(WSSCommand WSSCommand) {
                    if (Objects.equals(WSSCommand.getOperation(), "UserDataAdded")) {
                        ConnectionSingleton.getInstance().setConnectionManager(connectionManager);
                        Intent intent = new Intent(activity, PreferenceActivity.class);
                        activity.startActivity(intent);
                        activity.finish();
                    } else if (Objects.equals(WSSCommand.getOperation(), "UserDataUpdated")) {
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
        }

        setupValidation();
        setupSubmitButton();
        updateSubmitButtonState();
    }

    private void populateFormWithUserData() {
        if (userDataToEdit != null) {

            originalName = userDataToEdit.getName();
            originalAge = userDataToEdit.getAge();
            originalGender = userDataToEdit.getGender();

            if (nameInputLayout.getEditText() != null)
                nameInputLayout.getEditText().setText(originalName);

            if (ageInputLayout.getEditText() != null)
                ageInputLayout.getEditText().setText(String.valueOf(originalAge));

            if (genderGroup != null) {
                int radioButtonId = -1;

                for (int i = 0; i < genderGroup.getChildCount(); i++) {
                    RadioButton rb = (RadioButton) genderGroup.getChildAt(i);
                    if (rb != null && Gender.valueOf(rb.getTag().toString()) == originalGender) {
                        radioButtonId = rb.getId();
                        break;
                    }
                }

                if (radioButtonId != -1) genderGroup.check(radioButtonId);
            }

            if (submitButton != null)
                submitButton.setText(R.string.update_button);
        }

        updateSubmitButtonState();
    }

    private void setupValidation() {

        if (nameInputLayout.getEditText() != null) {
            nameInputLayout.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateAndUpdateName();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (ageInputLayout.getEditText() != null) {
            ageInputLayout.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateAndUpdateAge();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (genderGroup != null) {
            genderGroup.setOnCheckedChangeListener((group, checkedId) -> updateSubmitButtonState());
        }
    }

    private void setupSubmitButton() {
        if (submitButton != null) {
            submitButton.setOnClickListener(v -> submitUserData());
        }
    }

    private void submitUserData() {
        if (nameInputLayout.getEditText() == null ||
                ageInputLayout.getEditText() == null ||
                genderGroup == null) return;

        String name = nameInputLayout.getEditText().getText().toString().trim();
        String ageText = ageInputLayout.getEditText().getText().toString().trim();
        if (ageText.isEmpty()) return;

        int age = Integer.parseInt(ageText);
        int selectedGenderId = genderGroup.getCheckedRadioButtonId();
        RadioButton selectedGender = activity.findViewById(selectedGenderId);
        if (selectedGender == null) return;

        String gender = selectedGender.getTag().toString();

        WSSCommand command = new WSSCommand(
                isEditMode ? "UpdateUserData" : "AddUserData",
                new UserDataRequest(name, age, Gender.valueOf(gender))
        );

        if (connectionManager != null)
            connectionManager.SendCommand(command);
    }

    private void validateAndUpdateName() {
        if (nameInputLayout.getEditText() == null) return;

        String text = nameInputLayout.getEditText().getText().toString().trim();
        boolean isValid = !text.isEmpty() && text.length() <= 50;

        nameInputLayout.setError(isValid ? null : activity.getString(R.string.name_validation_error));

        updateSubmitButtonState();
    }

    private void validateAndUpdateAge() {
        if (ageInputLayout.getEditText() == null) return;

        String text = ageInputLayout.getEditText().getText().toString().trim();
        boolean isValid = false;

        if (!text.isEmpty()) {
            try {
                int age = Integer.parseInt(text);
                isValid = age >= 18 && age <= 120;
            } catch (NumberFormatException ignored) {}
        }

        ageInputLayout.setError(isValid ? null : activity.getString(R.string.age_range_error));

        updateSubmitButtonState();
    }

    private boolean isDataChanged() {
        if (!isEditMode) return true;

        String currentName = nameInputLayout.getEditText().getText().toString().trim();
        String ageText = ageInputLayout.getEditText().getText().toString().trim();
        int currentAge = ageText.isEmpty() ? -1 : Integer.parseInt(ageText);

        int selectedGenderId = genderGroup.getCheckedRadioButtonId();
        RadioButton selectedGender = activity.findViewById(selectedGenderId);
        Gender currentGender = selectedGender != null
                ? Gender.valueOf(selectedGender.getTag().toString())
                : null;

        return !Objects.equals(currentName, originalName)
                || currentAge != originalAge
                || currentGender != originalGender;
    }

    private boolean validateGender() {
        return genderGroup != null && genderGroup.getCheckedRadioButtonId() != -1;
    }

    private void updateSubmitButtonState() {

        boolean isNameValid = nameInputLayout.getEditText() != null &&
                !nameInputLayout.getEditText().getText().toString().trim().isEmpty() &&
                nameInputLayout.getEditText().getText().toString().trim().length() <= 50;

        boolean isAgeValid = false;
        if (ageInputLayout.getEditText() != null) {
            String ageText = ageInputLayout.getEditText().getText().toString().trim();
            if (!ageText.isEmpty()) {
                try {
                    int age = Integer.parseInt(ageText);
                    isAgeValid = age >= 18 && age <= 120;
                } catch (NumberFormatException ignored) {}
            }
        }

        boolean isEnabled =
                isNameValid &&
                        isAgeValid &&
                        validateGender() &&
                        isDataChanged();
        if (submitButton != null)
            submitButton.setEnabled(isEnabled);
    }
}
