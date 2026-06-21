package com.example.aichat.model.utils.mappers;

import android.content.res.Resources;
import com.example.aichat.dto.response.ThemeContentResponse;
import com.example.aichat.dto.response.ThemeResponse;
import com.example.aichat.model.utils.JsonHelper;
import com.example.aichat.model.utils.theme.ThemeModel;

public class ThemeMapper implements MapperRequest<ThemeModel, ThemeResponse> {
    @Override
    public ThemeModel ToDTO(ThemeResponse themeResponse) {
        ThemeContentResponse response = JsonHelper.Deserialize(themeResponse.content, ThemeContentResponse.class);
        assert response != null;
        return new ThemeModel(
                themeResponse.id,
                themeResponse.name,
                themeResponse.type,
                response.colorPrimary,
                response.colorSurface,
                response.colorOnSurface,
                response.colorOnSurfaceVariant,
                response.colorSecondary,
                response.myMessageColor,
                response.otherMessageColor,
                response.backgroundColor,
                response.backgroundImagePath,
                response.backgroundImageName,
                response.messageAnimation
        );
    }
}
