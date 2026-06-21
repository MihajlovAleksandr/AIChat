package com.example.aichat.model.utils.theme;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import androidx.annotation.AttrRes;
import androidx.core.content.ContextCompat;
import com.example.aichat.R;
import java.util.HashMap;
import java.util.Map;

public class ThemeAttrResolver {

    private static ThemeModel currentTheme;

    private static final Map<Integer, Integer> runtimeColors =
            new HashMap<>();

    private ThemeAttrResolver() {
    }
    public static void applyTheme(
            ThemeModel theme
    ) {

        if (theme == null) {

            clear();

            return;
        }

        currentTheme =
                theme;

        runtimeColors.clear();

        putColor(
                android.R.attr.colorBackground,
                theme.getBackgroundColor(),
                "#FFFFFF"
        );

        putColor(
                R.attr.colorPrimary,
                theme.getColorPrimary(),
                "#20A39A"
        );

        putColor(
                R.attr.colorSurface,
                theme.getColorSurface(),
                "#FFFFFF"
        );

        putColor(
                R.attr.colorOnSurface,
                theme.getColorOnSurface(),
                "#1A1A1A"
        );

        putColor(
                R.attr.colorOnSurfaceVariant,
                theme.getColorOnSurfaceVariant(),
                "#666666"
        );

        putColor(
                R.attr.colorSecondary,
                theme.getColorSecondary(),
                theme.getColorPrimary()
        );

        putColor(
                R.attr.myMessageColor,
                theme.getMyMessageColor(),
                theme.getColorPrimary()
        );

        putColor(
                R.attr.otherMessageColor,
                theme.getOtherMessageColor(),
                "#F1F1F1"
        );

        Log.d(
                "RuntimeTheme",
                "Theme applied: " + theme.getName()
        );

        Log.d(
                "RuntimeTheme",
                "My message color = " + theme.getMyMessageColor()
        );

        Log.d(
                "RuntimeTheme",
                "Other message color = " + theme.getOtherMessageColor()
        );

        Log.d(
                "RuntimeTheme",
                "Message animation = " + theme.getMessageAnimation()
        );
    }

    public static Integer getRuntimeColor(
            int attr
    ) {

        return runtimeColors.get(
                attr
        );
    }

    public static ThemeModel getCurrentTheme() {

        return currentTheme;
    }

    public static int resolveColor(
            Context context,
            @AttrRes int attr
    ) {

        Integer runtimeColor =
                runtimeColors.get(
                        attr
                );

        if (runtimeColor != null) {

            return runtimeColor;
        }

        TypedValue typedValue =
                new TypedValue();

        boolean resolved =
                context.getTheme()
                        .resolveAttribute(
                                attr,
                                typedValue,
                                true
                        );

        if (!resolved) {

            return Color.WHITE;
        }

        if (typedValue.resourceId != 0) {

            try {

                return ContextCompat.getColor(
                        context,
                        typedValue.resourceId
                );

            } catch (Exception ignored) {
            }
        }

        return typedValue.data;
    }

    public static void clear() {

        currentTheme =
                null;

        runtimeColors.clear();
    }

    public static boolean hasRuntimeTheme() {

        return !runtimeColors.isEmpty();
    }

    private static void putColor(
            int attr,
            String color,
            String fallback
    ) {

        runtimeColors.put(
                attr,
                parseColorOrFallback(
                        color,
                        fallback
                )
        );
    }

    private static int parseColorOrFallback(
            String color,
            String fallback
    ) {

        try {

            if (color != null
                    && !color.trim().isEmpty()) {

                return Color.parseColor(
                        color
                );
            }

        } catch (Exception ignored) {
        }

        try {

            if (fallback != null
                    && !fallback.trim().isEmpty()) {

                return Color.parseColor(
                        fallback
                );
            }

        } catch (Exception ignored) {
        }

        return Color.WHITE;
    }
}
