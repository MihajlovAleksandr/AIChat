package com.example.aichat.model.utils.theme;

import androidx.annotation.Nullable;
import com.example.aichat.model.entities.ThemeType;
import com.example.aichat.view.theme.binders.ThemeMessageAnimationBinder;
import java.util.UUID;

public class ThemeModel {
    public static final String DEFAULT_MESSAGE_ANIMATION = "default";

    private UUID id;
    private String name;

    private String colorPrimary;
    private String colorSurface;
    private String colorOnSurface;
    private String colorOnSurfaceVariant;
    private String colorSecondary;

    private String myMessageColor;
    private String otherMessageColor;
    private String backgroundColor;

    private String backgroundImagePath;
    private String backgroundImageName;

    private String messageAnimation;
    @Nullable
    private UUID fileId;
    private ThemeType type;

    public ThemeModel() {
    }

    public ThemeModel(
            UUID id,
            String name,
            ThemeType type,
            String colorPrimary,
            String colorSurface,
            String colorOnSurface,
            String colorOnSurfaceVariant,
            String colorSecondary,
            String myMessageColor,
            String otherMessageColor,
            String backgroundColor
    ) {
        this.id = id;
        this.name = name;
        this.colorPrimary = colorPrimary;
        this.colorSurface = colorSurface;
        this.colorOnSurface = colorOnSurface;
        this.colorOnSurfaceVariant = colorOnSurfaceVariant;
        this.colorSecondary = colorSecondary;
        this.myMessageColor = myMessageColor;
        this.otherMessageColor = otherMessageColor;
        this.backgroundColor = backgroundColor;
        this.messageAnimation = DEFAULT_MESSAGE_ANIMATION;
        this.type = type;
    }

    public ThemeModel(
            UUID id,
            String name,
            ThemeType type,
            String colorPrimary,
            String colorSurface,
            String colorOnSurface,
            String colorOnSurfaceVariant,
            String colorSecondary,
            String myMessageColor,
            String otherMessageColor,
            String backgroundColor,
            String backgroundImagePath,
            String backgroundImageName
    ) {
        this(
                id,
                name,
                type,
                colorPrimary,
                colorSurface,
                colorOnSurface,
                colorOnSurfaceVariant,
                colorSecondary,
                myMessageColor,
                otherMessageColor,
                backgroundColor
        );

        this.backgroundImagePath = backgroundImagePath;
        this.backgroundImageName = backgroundImageName;
    }

    public ThemeModel(
            UUID id,
            String name,
            ThemeType type,
            String colorPrimary,
            String colorSurface,
            String colorOnSurface,
            String colorOnSurfaceVariant,
            String colorSecondary,
            String myMessageColor,
            String otherMessageColor,
            String backgroundColor,
            String backgroundImagePath,
            String backgroundImageName,
            String messageAnimation
    ) {
        this(
                id,
                name,
                type,
                colorPrimary,
                colorSurface,
                colorOnSurface,
                colorOnSurfaceVariant,
                colorSecondary,
                myMessageColor,
                otherMessageColor,
                backgroundColor,
                backgroundImagePath,
                backgroundImageName
        );

        setMessageAnimation(messageAnimation);
    }

    public ThemeModel copyAsUserTheme(String suffix) {
        String copiedName = name;

        if (suffix != null && !suffix.trim().isEmpty()) {
            copiedName = copiedName + " " + suffix;
        }

        ThemeModel copy = new ThemeModel(
                UUID.randomUUID(),
                copiedName,
                ThemeType.Custom,
                colorPrimary,
                colorSurface,
                colorOnSurface,
                colorOnSurfaceVariant,
                colorSecondary,
                myMessageColor,
                otherMessageColor,
                backgroundColor,
                backgroundImagePath,
                backgroundImageName,
                getMessageAnimation()
        );

        return copy;
    }

    public boolean hasBackgroundImage() {
        return backgroundImagePath != null
                && !backgroundImagePath.trim().isEmpty();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColorPrimary() {
        return colorPrimary;
    }

    public void setColorPrimary(String colorPrimary) {
        this.colorPrimary = colorPrimary;
    }

    public String getColorSurface() {
        return colorSurface;
    }

    public void setColorSurface(String colorSurface) {
        this.colorSurface = colorSurface;
    }

    public String getColorOnSurface() {
        return colorOnSurface;
    }

    public void setColorOnSurface(String colorOnSurface) {
        this.colorOnSurface = colorOnSurface;
    }

    public String getColorOnSurfaceVariant() {
        return colorOnSurfaceVariant;
    }

    public void setColorOnSurfaceVariant(String colorOnSurfaceVariant) {
        this.colorOnSurfaceVariant = colorOnSurfaceVariant;
    }

    public String getColorSecondary() {
        return colorSecondary;
    }

    public void setColorSecondary(String colorSecondary) {
        this.colorSecondary = colorSecondary;
    }

    public String getMyMessageColor() {
        return myMessageColor;
    }

    public void setMyMessageColor(String myMessageColor) {
        this.myMessageColor = myMessageColor;
    }

    public String getOtherMessageColor() {
        return otherMessageColor;
    }

    public void setOtherMessageColor(String otherMessageColor) {
        this.otherMessageColor = otherMessageColor;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getBackgroundImagePath() {
        return backgroundImagePath;
    }

    public void setBackgroundImagePath(String backgroundImagePath) {
        this.backgroundImagePath = backgroundImagePath;
    }

    public String getBackgroundImageName() {
        return backgroundImageName;
    }

    public void setBackgroundImageName(String backgroundImageName) {
        this.backgroundImageName = backgroundImageName;
    }

    public String getMessageAnimation() {
        if (messageAnimation == null || messageAnimation.trim().isEmpty()) {
            return DEFAULT_MESSAGE_ANIMATION;
        }

        return ThemeMessageAnimationBinder.normalize(messageAnimation);
    }

    public void setMessageAnimation(String messageAnimation) {
        this.messageAnimation =
                ThemeMessageAnimationBinder.normalize(messageAnimation);
    }

    @Nullable
    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(@Nullable UUID fileId) {
        this.fileId = fileId;
    }

    public String getRawMessageAnimation() {
        return messageAnimation;
    }

    public ThemeType getSource() {
        return type;
    }

    public void setSource(ThemeType source) {
        this.type = source;
    }
}
