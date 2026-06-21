package com.example.aichat.controller;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.example.aichat.dto.request.PreferenceRequest;
import com.example.aichat.dto.response.ApiError;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.JwtUtils;
import com.example.aichat.model.entities.Preference;
import com.example.aichat.model.entities.PreferenceGender;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.R;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.view.PreferenceActivity;
import com.google.android.material.textfield.TextInputLayout;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

public class PreferenceController {

    private final PreferenceActivity activity;
    private final TextInputLayout minAgeInputLayout;
    private final TextInputLayout maxAgeInputLayout;
    private final RadioGroup genderGroup;
    private final Button submitButton;
    private final Preference preferenceToEdit;
    private final PreferenceRequest defaultPreference = new PreferenceRequest(18, 120, PreferenceGender.ANY);

    private boolean isEditMode = false;

    private int originalMinAge = -1;
    private int originalMaxAge = -1;
    private PreferenceGender originalGender = null;
    private final ConnectionDispatcher dispatcher;

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
        this.preferenceToEdit = preferenceToEdit;

        if (preferenceToEdit != null) {
            this.isEditMode = true;
            populateFormWithPreference();
            skipButton.setVisibility(Button.GONE);
        }
        dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
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

    private void setupButtons() {
        submitButton.setOnClickListener(v -> {
            int maxAge = validateAge(maxAgeInputLayout);
            int minAge = validateAge(minAgeInputLayout);

            int selectedGenderId = genderGroup.getCheckedRadioButtonId();
            RadioButton selectedGender = activity.findViewById(selectedGenderId);
            PreferenceGender gender = selectedGender != null
                    ? PreferenceGender.valueOf(selectedGender.getTag().toString())
                    : null;

            fetch(new PreferenceRequest(minAge, maxAge, gender));
        });
    }

    private void fetch(PreferenceRequest request){
        String url = isEditMode ? "/api/user/preference" : "/api/auth/register/preference";
        HttpClient.HTTPMethod method = isEditMode ? HttpClient.HTTPMethod.PUT : HttpClient.HTTPMethod.POST;

        dispatcher.sendHttpRequestAsync(url, method, request, false)
                .thenAccept((cmd)->{
                    if(cmd.isSuccess()){
                        if(isEditMode){
                            activity.finish();
                        }
                        else{
                            String jwt = cmd.getData(String.class);
                            JSONObject jsonPayload = JwtUtils.decodePayload(jwt);
                            try{
                                UUID userId = UUID.fromString(jsonPayload.getString("sub"));
                                SecurePreferencesManager.saveUserId(activity, userId);
                                SecurePreferencesManager.saveAuthToken(activity, jwt);
                                dispatcher.setToken(jwt);
                                Intent intent = new Intent(activity, MainActivity.class);
                                activity.startActivity(intent);
                                activity.finish();

                            } catch (JSONException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                    else{
                        Log.e("UserDataChange", cmd.getData(ApiError.class).toString());
                    }
                });
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

    public void sendSkipCommand() {
        fetch(defaultPreference);
    }
}
