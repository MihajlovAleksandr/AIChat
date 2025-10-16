package com.example.aichat.model.utils.mappers;

import com.example.aichat.dto.response.PreferenceResponse;
import com.example.aichat.model.entities.Preference;

public class PreferenceMapper implements  MapperResponse<Preference, PreferenceResponse> {
    @Override
    public Preference ToModel(PreferenceResponse preferenceResponse) {
        return new Preference(preferenceResponse.minAge,preferenceResponse.maxAge, preferenceResponse.gender);
    }
}
