package com.example.aichat.view.theme.binders;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.R;
import com.google.android.material.materialswitch.MaterialSwitch;

public final class SettingsActivityThemeBinder {

    private SettingsActivityThemeBinder() {
    }

    public static void apply(@Nullable Activity activity) {
        if (activity == null) {
            return;
        }

        int primary =
                resolveColor(
                        activity,
                        com.google.android.material.R.attr.colorPrimary,
                        0xFF20A39A
                );

        int onPrimary =
                resolveColor(
                        activity,
                        com.google.android.material.R.attr.colorOnPrimary,
                        Color.WHITE
                );

        int surface =
                resolveColor(
                        activity,
                        com.google.android.material.R.attr.colorSurface,
                        Color.WHITE
                );

        int onSurface =
                resolveColor(
                        activity,
                        com.google.android.material.R.attr.colorOnSurface,
                        0xFF1A1A1A
                );

        int onSurfaceVariant =
                resolveColor(
                        activity,
                        com.google.android.material.R.attr.colorOnSurfaceVariant,
                        0xFF666666
                );

        View root =
                activity.findViewById(
                        android.R.id.content
                );

        tintTextRecursively(
                root,
                onSurface,
                onSurfaceVariant
        );

        tintIcon(
                activity.findViewById(
                        R.id.back_button
                ),
                primary
        );

        tintIcon(
                activity.findViewById(
                        R.id.theme_chevron
                ),
                primary
        );

        tintIcon(
                activity.findViewById(
                        R.id.ai_model_chevron
                ),
                primary
        );

        tintIcon(
                activity.findViewById(
                        R.id.language_chevron
                ),
                primary
        );

        tintIcon(
                activity.findViewById(
                        R.id.faq_chevron
                ),
                primary
        );

        tintIcon(
                activity.findViewById(
                        R.id.policy_chevron
                ),
                primary
        );

        tintIcon(
                activity.findViewById(
                        R.id.support_chevron
                ),
                primary
        );

        tintSwitch(
                activity.findViewById(
                        R.id.email_notifications_switch
                ),
                primary,
                onPrimary,
                surface,
                onSurfaceVariant
        );

        tintSwitch(
                activity.findViewById(
                        R.id.show_notifications_switch
                ),
                primary,
                onPrimary,
                surface,
                onSurfaceVariant
        );

        tintSwitch(
                activity.findViewById(
                        R.id.background_notifications_switch
                ),
                primary,
                onPrimary,
                surface,
                onSurfaceVariant
        );

        tintSwitch(
                activity.findViewById(
                        R.id.in_app_notifications_switch
                ),
                primary,
                onPrimary,
                surface,
                onSurfaceVariant
        );

        tintSwitch(
                activity.findViewById(
                        R.id.vibration_switch
                ),
                primary,
                onPrimary,
                surface,
                onSurfaceVariant
        );

        tintSwitch(
                activity.findViewById(
                        R.id.fullscreen_switch
                ),
                primary,
                onPrimary,
                surface,
                onSurfaceVariant
        );

        restoreSelectableBackground(
                activity.findViewById(
                        R.id.email_notifications_item
                )
        );

        restoreSelectableBackground(
                activity.findViewById(
                        R.id.show_notifications_item
                )
        );

        restoreSelectableBackground(
                activity.findViewById(
                        R.id.background_notifications_item
                )
        );

        restoreSelectableBackground(
                activity.findViewById(
                        R.id.in_app_notifications_item
                )
        );

        restoreSelectableBackground(
                activity.findViewById(
                        R.id.vibration_item
                )
        );

        restoreSelectableBackground(
                activity.findViewById(
                        R.id.fullscreen_item
                )
        );

        restoreSelectableBackground(
                activity.findViewById(
                        R.id.theme_item
                )
        );

        restoreSelectableBackground(
                activity.findViewById(
                        R.id.ai_model_item
                )
        );

        restoreSelectableBackground(
                activity.findViewById(
                        R.id.language_item
                )
        );

        restoreSelectableBackground(
                activity.findViewById(
                        R.id.reference_faq_header
                )
        );

        restoreSelectableBackground(
                activity.findViewById(
                        R.id.reference_policy_header
                )
        );

        restoreSelectableBackground(
                activity.findViewById(
                        R.id.reference_support_header
                )
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.email_text
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.userData_text
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.preference_text
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.devices_text
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.current_theme_text
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.current_ai_model_text
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.current_language_text
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.reference_intro
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.faq_summary_text
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.policy_summary_text
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.support_summary_text
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.faq_content_text
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.policy_content_text
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.support_content_text
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.storage_summary
                ),
                onSurfaceVariant
        );

        tintSecondaryText(
                activity.findViewById(
                        R.id.version_text
                ),
                onSurfaceVariant
        );

        ProgressBar storageProgress =
                activity.findViewById(
                        R.id.storage_progress
                );

        if (storageProgress != null) {
            storageProgress.setProgressTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            storageProgress.setProgressBackgroundTintList(
                    ColorStateList.valueOf(
                            withAlpha(
                                    onSurfaceVariant,
                                    54
                            )
                    )
            );
        }
    }

    private static void tintTextRecursively(
            @Nullable View view,
            int onSurface,
            int onSurfaceVariant
    ) {
        if (view == null) {
            return;
        }

        if (isInsideLogoutItem(view)) {
            return;
        }

        if (view instanceof TextView) {
            TextView textView =
                    (TextView) view;

            int current =
                    textView.getCurrentTextColor();

            if (isRedLike(current)) {
                return;
            }

            float sp =
                    textView.getTextSize()
                            / view
                            .getResources()
                            .getDisplayMetrics()
                            .scaledDensity;

            boolean bold =
                    textView.getTypeface() != null
                            && textView.getTypeface().getStyle() == Typeface.BOLD;

            textView.setTextColor(
                    sp >= 16f || bold
                            ? onSurface
                            : onSurfaceVariant
            );
        }

        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group =
                    (android.view.ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                tintTextRecursively(
                        group.getChildAt(
                                i
                        ),
                        onSurface,
                        onSurfaceVariant
                );
            }
        }
    }

    private static boolean isInsideLogoutItem(@NonNull View view) {
        View current =
                view;

        while (current != null) {
            if (current.getId() == R.id.logout_item) {
                return true;
            }

            Object parent =
                    current.getParent();

            if (!(parent instanceof View)) {
                return false;
            }

            current =
                    (View) parent;
        }

        return false;
    }

    private static boolean isRedLike(int color) {
        return Color.red(color) > 180
                && Color.green(color) < 90
                && Color.blue(color) < 90;
    }

    private static void tintSecondaryText(
            @Nullable TextView textView,
            int color
    ) {
        if (textView != null) {
            textView.setTextColor(
                    color
            );
        }
    }

    private static void tintIcon(
            @Nullable View view,
            int color
    ) {
        if (view instanceof ImageButton) {
            ((ImageButton) view).setImageTintList(
                    ColorStateList.valueOf(
                            color
                    )
            );
        } else if (view instanceof ImageView) {
            ((ImageView) view).setImageTintList(
                    ColorStateList.valueOf(
                            color
                    )
            );
        }
    }

    private static void tintSwitch(
            @Nullable MaterialSwitch switchView,
            int primary,
            int onPrimary,
            int surface,
            int onSurfaceVariant
    ) {
        if (switchView == null) {
            return;
        }

        int uncheckedTrack =
                withAlpha(
                        onSurfaceVariant,
                        80
                );

        int disabledTrack =
                withAlpha(
                        onSurfaceVariant,
                        42
                );

        int uncheckedThumb =
                surface;

        int disabledThumb =
                blend(
                        surface,
                        onSurfaceVariant,
                        0.22f
                );

        switchView.setThumbTintList(
                new ColorStateList(
                        new int[][]{
                                new int[]{
                                        android.R.attr.state_checked,
                                        android.R.attr.state_enabled
                                },
                                new int[]{
                                        -android.R.attr.state_checked,
                                        android.R.attr.state_enabled
                                },
                                new int[]{
                                        -android.R.attr.state_enabled
                                }
                        },
                        new int[]{
                                onPrimary,
                                uncheckedThumb,
                                disabledThumb
                        }
                )
        );

        switchView.setTrackTintList(
                new ColorStateList(
                        new int[][]{
                                new int[]{
                                        android.R.attr.state_checked,
                                        android.R.attr.state_enabled
                                },
                                new int[]{
                                        -android.R.attr.state_checked,
                                        android.R.attr.state_enabled
                                },
                                new int[]{
                                        -android.R.attr.state_enabled
                                }
                        },
                        new int[]{
                                primary,
                                uncheckedTrack,
                                disabledTrack
                        }
                )
        );

        switchView.setTextColor(
                onSurfaceVariant
        );
    }

    private static void restoreSelectableBackground(@Nullable View view) {
        if (view == null) {
            return;
        }

        Context context =
                view.getContext();

        TypedValue typedValue =
                new TypedValue();

        boolean resolved =
                context
                        .getTheme()
                        .resolveAttribute(
                                android.R.attr.selectableItemBackground,
                                typedValue,
                                true
                        );

        if (resolved && typedValue.resourceId != 0) {
            view.setBackgroundResource(
                    typedValue.resourceId
            );
        }
    }

    private static int resolveColor(
            @NonNull Context context,
            @AttrRes int attr,
            int fallback
    ) {
        try {
            return ThemeAttrResolver.resolveColor(
                    context,
                    attr
            );
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int withAlpha(
            int color,
            int alpha
    ) {
        int safeAlpha =
                Math.max(
                        0,
                        Math.min(
                                255,
                                alpha
                        )
                );

        return (color & 0x00FFFFFF)
                | (safeAlpha << 24);
    }

    private static int blend(
            int base,
            int overlay,
            float ratio
    ) {
        float inverse =
                1f - ratio;

        return Color.rgb(
                Math.round(
                        Color.red(base) * inverse
                                + Color.red(overlay) * ratio
                ),
                Math.round(
                        Color.green(base) * inverse
                                + Color.green(overlay) * ratio
                ),
                Math.round(
                        Color.blue(base) * inverse
                                + Color.blue(overlay) * ratio
                )
        );
    }
}
