package com.example.aichat.view.theme;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import java.util.Locale;

public class ThemeEditorDrawableUtils {

    private ThemeEditorDrawableUtils() {
    }

    public static void setCircleColor(
            View view,
            String color
    ) {

        view.setBackground(
                createCircleDrawable(
                        view.getContext(),
                        color
                )
        );
    }

    public static GradientDrawable createCircleDrawable(
            Context context,
            String color
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setShape(
                GradientDrawable.OVAL
        );

        drawable.setColor(
                parseColor(
                        color
                )
        );

        if ("#FFFFFF".equalsIgnoreCase(
                color
        )
                || "#F1F1F1".equalsIgnoreCase(
                color
        )) {

            drawable.setStroke(
                    dp(
                            context,
                            1
                    ),
                    Color.parseColor(
                            "#22000000"
                    )
            );
        }

        return drawable;
    }

    public static GradientDrawable createBubbleDrawable(
            Context context,
            String color,
            boolean isOtherMessage
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                parseColor(
                        color
                )
        );

        float radius =
                dp(
                        context,
                        14
                );

        if (isOtherMessage) {

            drawable.setCornerRadii(
                    new float[]{
                            radius, radius,
                            radius, radius,
                            dp(context, 4), dp(context, 4),
                            radius, radius
                    }
            );

        } else {

            drawable.setCornerRadii(
                    new float[]{
                            radius, radius,
                            radius, radius,
                            radius, radius,
                            dp(context, 4), dp(context, 4)
                    }
            );
        }

        if ("#FFFFFF".equalsIgnoreCase(
                color
        )
                || "#F1F1F1".equalsIgnoreCase(
                color
        )) {

            drawable.setStroke(
                    dp(
                            context,
                            1
                    ),
                    Color.parseColor(
                            "#12000000"
                    )
            );
        }

        return drawable;
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

    public static String normalizeColor(
            String color
    ) {

        int parsed =
                parseColor(
                        color
                );

        return String.format(
                Locale.US,
                "#%06X",
                0xFFFFFF & parsed
        );
    }

    public static int getContrastColor(
            String color
    ) {

        int parsed =
                parseColor(
                        color
                );

        double darkness =
                1
                        - (
                        0.299 * Color.red(
                                parsed
                        )
                                + 0.587 * Color.green(
                                parsed
                        )
                                + 0.114 * Color.blue(
                                parsed
                        )
                ) / 255;

        if (darkness < 0.5) {
            return Color.BLACK;
        }

        return Color.WHITE;
    }

    public static int dp(
            Context context,
            int value
    ) {

        return (int) (
                value
                        * context.getResources()
                        .getDisplayMetrics()
                        .density
                        + 0.5f
        );
    }
}
