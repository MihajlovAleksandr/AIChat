package com.example.aichat.model.utils.theme;

import android.graphics.Color;
import android.util.Log;
import com.example.aichat.R;

public class ThemeColors {

    public final int primary;

    public final int surface;

    public final int onSurface;

    public final int onSurfaceVariant;

    public final int secondary;

    public final int background;

    public final int myMessageColor;

    public final int otherMessageColor;

    public ThemeColors(
            ThemeModel theme
    ) {

        Log.d(
                "ThemeColors",
                "=== CREATE THEME COLORS FROM MODEL ==="
        );

        primary =
                resolveColor(
                        R.attr.colorPrimary,
                        theme.getColorPrimary(),
                        "primary"
                );

        surface =
                resolveColor(
                        R.attr.colorSurface,
                        theme.getColorSurface(),
                        "surface"
                );

        onSurface =
                resolveColor(
                        R.attr.colorOnSurface,
                        theme.getColorOnSurface(),
                        "onSurface"
                );

        onSurfaceVariant =
                resolveColor(
                        R.attr.colorOnSurfaceVariant,
                        theme.getColorOnSurfaceVariant(),
                        "onSurfaceVariant"
                );

        secondary =
                resolveColor(
                        R.attr.colorSecondary,
                        theme.getColorSecondary(),
                        "secondary"
                );

        background =
                resolveColor(
                        android.R.attr.colorBackground,
                        theme.getBackgroundColor(),
                        "background"
                );

        myMessageColor =
                resolveColor(
                        R.attr.myMessageColor,
                        theme.getMyMessageColor(),
                        "myMessageColor"
                );

        otherMessageColor =
                resolveColor(
                        R.attr.otherMessageColor,
                        theme.getOtherMessageColor(),
                        "otherMessageColor"
                );

        Log.d(
                "ThemeColors",
                "=== THEME COLORS CREATED ==="
        );
    }

    private int resolveColor(
            int attr,
            String fallback,
            String name
    ) {

        Integer runtimeColor =
                ThemeAttrResolver.getRuntimeColor(
                        attr
                );

        if (runtimeColor != null) {

            Log.d(
                    "ThemeColors",
                    name
                            + " from runtime = "
                            + runtimeColor
            );

            return runtimeColor;
        }

        int parsed =
                Color.parseColor(
                        fallback
                );

        Log.d(
                "ThemeColors",
                name
                        + " from theme model = "
                        + fallback
        );

        return parsed;
    }
}
