package com.example.aichat.model.utils.theme;

import android.graphics.Color;

public class ThemeUtils {

    private ThemeUtils() {
    }

    public static int parseColor(
            String color
    ) {

        try {

            return Color.parseColor(
                    color
            );

        } catch (Exception ex) {

            return Color.WHITE;
        }
    }
}
