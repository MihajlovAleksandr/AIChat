package com.example.aichat.controller;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.aichat.R;
import com.example.aichat.dto.request.PreferenceRequest;
import com.example.aichat.dto.response.LoginInResponse;
import com.example.aichat.dto.response.TokenResponse;
import com.example.aichat.model.entities.PreferenceGender;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.view.PreferenceActivity;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Preference;
import com.example.aichat.model.SecurePreferencesManager;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class PreferenceController {

    private final PreferenceActivity activity;
    private final TextInputLayout minAgeInputLayout;
    private final TextInputLayout maxAgeInputLayout;
    private final RadioGroup genderGroup;
    private final Button submitButton;
    private final Button skipButton;
    private final Preference preferenceToEdit;

    private ConnectionManager connectionManager;
    private boolean isEditMode = false;

    private int originalMinAge = -1;
    private int originalMaxAge = -1;
    private PreferenceGender originalGender = null;

    public PreferenceController(PreferenceActivity activity,
                                TextInputLayout minAgeInputLayout,
                                TextInputLayout maxAgeInputLayout,
                                RadioGroup genderGroup,
                                Button submitButton,
                                Button skipButton) {
        this(activity, minAgeInputLayout, maxAgeInputLayout, genderGroup, submitButton, skipButton, null);
    }

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
            connectionManager = new ConnectionManager("");
            ConnectionSingleton.getInstance().setConnectionManager(connectionManager);
        }

        setupConnectionCallbacks();
        setupButtons();
        setupValidation();
        updateSubmitButtonState();
    }

    private void populateFormWithPreference() {
        if (preferenceToEdit != null) {
            originalMinAge = preferenceToEdit.getMinAge();
            originalMaxAge = preferenceToEdit.getMaxAge();
            originalGender = preferenceToEdit.getGender();

            if (minAgeInputLayout.getEditText() != null)
                minAgeInputLayout.getEditText().setText(String.valueOf(originalMinAge));

            if (maxAgeInputLayout.getEditText() != null)
                maxAgeInputLayout.getEditText().setText(String.valueOf(originalMaxAge));

            PreferenceGender gender = preferenceToEdit.getGender();
            int radioButtonId = -1;

            for (int i = 0; i < genderGroup.getChildCount(); i++) {
                RadioButton radioButton = (RadioButton) genderGroup.getChildAt(i);
                if (radioButton.getTag() != null && radioButton.getTag().toString().equals(gender.toString())) {
                    radioButtonId = radioButton.getId();
                    break;
                }
            }

            if (radioButtonId != -1) {
                genderGroup.check(radioButtonId);
            }

            submitButton.setText(R.string.update_button);
        }

        updateSubmitButtonState();
    }

    private void setupConnectionCallbacks() {

        if (isEditMode) {
            connectionManager.addConnectionEvent(new OnConnectionEvents() {
                @Override
                public void OnCommandGot(WSSCommand WSSCommand) {
                    if (Objects.equals(WSSCommand.getOperation(), "PreferenceUpdated")) {
                        connectionManager.removeConnectionEvent(this);
                        activity.finish();
                    }
                }

                @Override public void OnConnectionFailed() {}
                @Override public void OnOpen() {}
            });

        } else {
            connectionManager.clearConnectionEvents();
            connectionManager.addConnectionEvent(new OnConnectionEvents() {

                @Override
                public void OnCommandGot(WSSCommand WSSCommand) {

                    switch (WSSCommand.getOperation()) {

                        case "CreateToken":
                            TokenResponse tokenResponse = WSSCommand.getData(TokenResponse.class);
                            SecurePreferencesManager.saveAuthToken(activity, tokenResponse.token);
                            connectionManager.setToken(tokenResponse.token);
                            break;

                        case "LoginIn":
                            ConnectionSingleton.getInstance().setConnectionManager(connectionManager);
                            Intent intent = new Intent(activity, MainActivity.class);
                            LoginInResponse loginInResponse = WSSCommand.getData(LoginInResponse.class);
                            SecurePreferencesManager.saveUserId(activity, loginInResponse.userId);
                            intent.putExtra("userId", loginInResponse.userId);
                            activity.startActivity(intent);
                            activity.finish();
                            break;
                    }
                }

                @Override public void OnConnectionFailed() {}
                @Override public void OnOpen() {}
            });
        }
    }

    private void setupButtons() {
        submitButton.setOnClickListener(v -> {
            int maxAge = validateAge(maxAgeInputLayout);
            int minAge = validateAge(minAgeInputLayout);

            int selectedGenderId = genderGroup.getCheckedRadioButtonId();
            RadioButton selectedGender = activity.findViewById(selectedGenderId);
            PreferenceGender gender = selectedGender != null
                    ? PreferenceGender.valueOf(selectedGender.getTag().toString())
                    : null;

            WSSCommand WSSCommand = new WSSCommand(
                    isEditMode ? "UpdatePreference" : "AddPreference",
                    new PreferenceRequest(minAge, maxAge, gender)
            );

            connectionManager.SendCommand(WSSCommand);
        });

        skipButton.setOnClickListener(v ->
                connectionManager.SendCommand(new WSSCommand("AddPreference"))
        );
    }

    public void sendSkipCommand() {
        connectionManager.SendCommand(new WSSCommand("AddPreference"));
    }

    public void setupValidation() {

        if (maxAgeInputLayout.getEditText() != null) {
            maxAgeInputLayout.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateAndUpdateAge(maxAgeInputLayout, minAgeInputLayout);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (minAgeInputLayout.getEditText() != null) {
            minAgeInputLayout.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateAndUpdateAge(minAgeInputLayout, maxAgeInputLayout);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        genderGroup.setOnCheckedChangeListener((group, checkedId) -> updateSubmitButtonState());
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

    private int validateAge(TextInputLayout layout) {
        if (layout.getEditText() == null) return -1;

        String ageText = layout.getEditText().getText().toString().trim();
        if (ageText.isEmpty()) return -1;

        try {
            int age = Integer.parseInt(ageText);
            return (age >= 18 && age <= 120) ? age : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean validateGender() {
        int selectedGenderId = genderGroup.getCheckedRadioButtonId();
        return selectedGenderId != -1 && activity.findViewById(selectedGenderId) != null;
    }

    private boolean isDataChanged() {
        if (!isEditMode) return true;

        int minAge = validateAge(minAgeInputLayout);
        int maxAge = validateAge(maxAgeInputLayout);

        int selectedGenderId = genderGroup.getCheckedRadioButtonId();
        RadioButton selectedGender = activity.findViewById(selectedGenderId);
        PreferenceGender gender = selectedGender != null
                ? PreferenceGender.valueOf(selectedGender.getTag().toString())
                : null;

        return minAge != originalMinAge
                || maxAge != originalMaxAge
                || gender != originalGender;
    }

    private void updateSubmitButtonState() {
        int minAge = validateAge(minAgeInputLayout);
        int maxAge = validateAge(maxAgeInputLayout);

        boolean isAgeValid = (minAge > 0) && (maxAge > minAge);
        boolean isGenderValid = validateGender();

        submitButton.setEnabled(isAgeValid && isGenderValid && isDataChanged());
    }
}
