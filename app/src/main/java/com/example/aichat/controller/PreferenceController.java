package com.example.aichat.controller;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.aichat.R;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.view.PreferenceActivity;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Preference;
import com.example.aichat.model.SecurePreferencesManager;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class PreferenceController {

    private PreferenceActivity activity;
    private ConnectionManager connectionManager;

    private TextInputLayout minAgeInputLayout;
    private TextInputLayout maxAgeInputLayout;
    private RadioGroup genderGroup;
    private Button submitButton;
    private Button skipButton;
    private boolean isEditMode = false;
    private Preference preferenceToEdit;

    // Первый конструктор (оригинальный)
    public PreferenceController(PreferenceActivity activity,
                                TextInputLayout minAgeInputLayout,
                                TextInputLayout maxAgeInputLayout,
                                RadioGroup genderGroup,
                                Button submitButton,
                                Button skipButton) {
        this(activity, minAgeInputLayout, maxAgeInputLayout, genderGroup, submitButton, skipButton, null);
    }

    // Второй конструктор для редактирования данных
    public PreferenceController(PreferenceActivity activity,
                                TextInputLayout minAgeInputLayout,
                                TextInputLayout maxAgeInputLayout,
                                RadioGroup genderGroup,
                                Button submitButton,
                                Button skipButton,
                                Preference preferenceToEdit) {
        this.activity = activity;
        this.minAgeInputLayout = minAgeInputLayout;
        this.maxAgeInputLayout = maxAgeInputLayout;
        this.genderGroup = genderGroup;
        this.submitButton = submitButton;
        this.skipButton = skipButton;
        this.preferenceToEdit = preferenceToEdit;

        if (preferenceToEdit != null) {
            this.isEditMode = true;
            populateFormWithPreference();
            skipButton.setVisibility(Button.GONE);
        }

        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        if (connectionManager == null) {
            ConnectionSingleton.getInstance().setConnectionManager(new ConnectionManager(""));
            connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        }

        setupConnectionCallbacks();
        setupButtons();
        setupValidation();
    }

    private void populateFormWithPreference() {
        if (preferenceToEdit != null) {
            minAgeInputLayout.getEditText().setText(String.valueOf(preferenceToEdit.getMinAge()));
            maxAgeInputLayout.getEditText().setText(String.valueOf(preferenceToEdit.getMaxAge()));

            String gender = preferenceToEdit.getGender();
            int radioButtonId = -1;
            for (int i = 0; i < genderGroup.getChildCount(); i++) {
                RadioButton radioButton = (RadioButton) genderGroup.getChildAt(i);
                if (radioButton.getTag().toString().equals(gender)) {
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

    private void setupConnectionCallbacks() {
        if (isEditMode) {
            connectionManager.addConnectionEvent(new OnConnectionEvents() {
                @Override
                public void OnCommandGot(Command command) {
                    if (Objects.equals(command.getOperation(), "PreferenceUpdated")) {
                        connectionManager.removeConnectionEvent(this);
                        activity.finish();
                    }
                }

                @Override
                public void OnConnectionFailed() {
                    Log.e("PreferenceController", "Connection failed");
                }

                @Override
                public void OnOpen() {
                    Log.d("PreferenceController", "Connection opened");
                }
            });
        }
        else {
            connectionManager.setConnectionEvent(new OnConnectionEvents() {
                @Override
                public void OnCommandGot(Command command) {
                    switch (command.getOperation()) {
                        case "CreateToken":
                            String token = command.getData("token", String.class);
                            SecurePreferencesManager.saveAuthToken(activity, token);
                            break;
                        case "LoginIn":
                            ConnectionSingleton.getInstance().setConnectionManager(connectionManager);
                            Intent intent = new Intent(activity, MainActivity.class);
                            int userId = command.getData("userId", int.class);
                            SecurePreferencesManager.saveUserId(activity, userId);
                            intent.putExtra("userId", userId);
                            activity.startActivity(intent);
                            activity.finish();
                            break;
                    }
                }

                @Override
                public void OnConnectionFailed() {
                    Log.e("PreferenceController", "Connection failed");
                }

                @Override
                public void OnOpen() {
                    Log.d("PreferenceController", "Connection opened");
                }
            });
        }
    }

    private void setupButtons() {
        submitButton.setOnClickListener(v -> {
            int maxAge = Integer.parseInt(maxAgeInputLayout.getEditText().getText().toString().trim());
            int minAge = Integer.parseInt(minAgeInputLayout.getEditText().getText().toString().trim());
            int selectedGenderId = genderGroup.getCheckedRadioButtonId();
            RadioButton selectedGender = activity.findViewById(selectedGenderId);
            String gender = selectedGender.getTag().toString();

            Preference preference = new Preference(minAge, maxAge, gender);
            Command command = new Command(isEditMode ? "UpdatePreference" : "AddPreference");
            command.addData("preference", preference);
            connectionManager.SendCommand(command);
        });

        skipButton.setOnClickListener(v ->
                connectionManager.SendCommand(new Command("AddPreference"))
        );
    }

    public void setupValidation() {
        maxAgeInputLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateAndUpdateAge(maxAgeInputLayout, minAgeInputLayout);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        minAgeInputLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateAndUpdateAge(minAgeInputLayout, maxAgeInputLayout);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        genderGroup.setOnCheckedChangeListener((group, checkedId) -> {
            validateAndUpdateGender();
        });
    }

    private void validateAndUpdateAge(TextInputLayout thisLayout, TextInputLayout otherLayout) {
        int thisAge = validateAge(thisLayout);
        if (thisAge > 0) {
            int otherAge = validateAge(otherLayout);
            if (otherAge > 0) {
                otherLayout.setError(null);
                if (thisLayout == maxAgeInputLayout) {
                    thisLayout.setError(thisAge > otherAge ? null : activity.getString(R.string.max_age_error));
                } else {
                    thisLayout.setError(thisAge < otherAge ? null : activity.getString(R.string.min_age_error));
                }
            } else {
                thisLayout.setError(null);
            }
        } else {
            thisLayout.setError(activity.getString(R.string.age_range_error));
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
            return (age >= 18 && age <= 120) ? age : -1;
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
        boolean isAgeValid = (minAge > 0) && (maxAge > minAge);
        boolean isGenderValid = validateGender();

        submitButton.setEnabled(isAgeValid && isGenderValid);
    }
}