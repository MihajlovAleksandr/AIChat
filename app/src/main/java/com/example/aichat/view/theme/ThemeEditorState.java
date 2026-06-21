package com.example.aichat.view.theme;

import com.example.aichat.view.theme.binders.ThemeMessageAnimationBinder;

public class ThemeEditorState {

    public String colorPrimary =
            "#20A39A";

    public String colorSurface =
            "#FFFFFF";

    public String colorOnSurface =
            "#1A1A1A";

    public String colorOnSurfaceVariant =
            "#666666";

    public String colorSecondary =
            "#20A39A";

    public String myMessageColor =
            "#20A39A";

    public String otherMessageColor =
            "#F1F1F1";

    public String backgroundColor =
            "#FFFFFF";

    public String backgroundImagePath;

    public String backgroundImageName;

    public String messageAnimation =
            ThemeMessageAnimationBinder.ANIMATION_DEFAULT;

    public String messageAnimationDisplayName =
            ThemeMessageAnimationBinder.ANIMATION_DEFAULT;

    public ThemeEditorState() {
    }
}
