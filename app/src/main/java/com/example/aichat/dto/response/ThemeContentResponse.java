package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ThemeContentResponse {
    public final String colorPrimary;
    public final String colorSurface;
    public final String colorOnSurface;
    public final String colorOnSurfaceVariant;
    public final String colorSecondary;
    public final String myMessageColor;
    public final String otherMessageColor;
    public final String backgroundColor;
    public final String backgroundImagePath;
    public final String backgroundImageName;
    public final String messageAnimation;

    @JsonCreator
    public ThemeContentResponse(
            @JsonProperty("colorPrimary") String colorPrimary,
            @JsonProperty("colorSurface") String colorSurface,
            @JsonProperty("colorOnSurface") String colorOnSurface,
            @JsonProperty("colorOnSurfaceVariant") String colorOnSurfaceVariant,
            @JsonProperty("colorSecondary") String colorSecondary,
            @JsonProperty("myMessageColor") String myMessageColor,
            @JsonProperty("otherMessageColor") String otherMessageColor,
            @JsonProperty("backgroundColor") String backgroundColor,
            @JsonProperty("backgroundImagePath") String backgroundImagePath,
            @JsonProperty("backgroundImageName") String backgroundImageName,
            @JsonProperty("messageAnimation") String messageAnimation) {
        this.colorPrimary = colorPrimary;
        this.colorSurface = colorSurface;
        this.colorOnSurface = colorOnSurface;
        this.colorOnSurfaceVariant = colorOnSurfaceVariant;
        this.colorSecondary = colorSecondary;
        this.myMessageColor = myMessageColor;
        this.otherMessageColor = otherMessageColor;
        this.backgroundColor = backgroundColor;
        this.backgroundImagePath = backgroundImagePath;
        this.backgroundImageName = backgroundImageName;
        this.messageAnimation = messageAnimation;
    }
}
