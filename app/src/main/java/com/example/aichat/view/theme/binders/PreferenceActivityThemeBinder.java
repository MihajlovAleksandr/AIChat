package com.example.aichat.view.theme.binders;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.R;
import com.example.aichat.view.theme.ActivityThemeGuard;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

public final class PreferenceActivityThemeBinder {

    private PreferenceActivityThemeBinder() {
    }

    public static void applyForAccountEditOnly(@NonNull Activity activity) {
        if (!ActivityThemeGuard.shouldApplyAccountThemeForEntityEdit(
                activity,
                "preference"
        )) {
            return;
        }

        apply(
                activity
        );
    }

    public static void apply(@NonNull Activity activity) {
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

        if (root != null) {
            root.setBackgroundColor(
                    resolveColor(
                            activity,
                            android.R.attr.colorBackground,
                            Color.WHITE
                    )
            );
        }

        tintAllTextRecursively(
                root,
                onSurface,
                onSurfaceVariant
        );

        tintTextInputLayout(
                activity.findViewById(
                        R.id.minAgeInputLayout
                ),
                primary,
                onSurface,
                onSurfaceVariant
        );

        tintTextInputLayout(
                activity.findViewById(
                        R.id.maxAgeInputLayout
                ),
                primary,
                onSurface,
                onSurfaceVariant
        );

        tintImage(
                activity.findViewById(
                        R.id.minAgeInfoIcon
                ),
                primary
        );

        tintImage(
                activity.findViewById(
                        R.id.maxAgeInfoIcon
                ),
                primary
        );

        tintRadio(
                activity.findViewById(
                        R.id.maleRadio
                ),
                primary,
                onSurface
        );

        tintRadio(
                activity.findViewById(
                        R.id.femaleRadio
                ),
                primary,
                onSurface
        );

        tintRadio(
                activity.findViewById(
                        R.id.anyRadio
                ),
                primary,
                onSurface
        );

        MaterialButton submitButton =
                activity.findViewById(
                        R.id.submitButton
                );

        if (submitButton != null) {
            submitButton.setBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            submitButton.setTextColor(
                    onPrimary
            );

            submitButton.setRippleColor(
                    ColorStateList.valueOf(
                            withAlpha(
                                    onPrimary,
                                    40
                            )
                    )
            );
        }

        MaterialButton skipButton =
                activity.findViewById(
                        R.id.skipButton
                );

        if (skipButton != null) {
            skipButton.setBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            skipButton.setTextColor(
                    onPrimary
            );

            skipButton.setRippleColor(
                    ColorStateList.valueOf(
                            withAlpha(
                                    onPrimary,
                                    40
                            )
                    )
            );
        }

        FloatingActionButton backButton =
                activity.findViewById(
                        R.id.btnBack
                );

        if (backButton != null) {
            backButton.setBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            backButton.setImageTintList(
                    ColorStateList.valueOf(
                            onPrimary
                    )
            );

            backButton.setRippleColor(
                    withAlpha(
                            onPrimary,
                            55
                    )
            );
        }

        tintProgressDots(
                activity,
                primary,
                onSurfaceVariant
        );
    }

    private static void tintTextInputLayout(
            @Nullable TextInputLayout layout,
            int primary,
            int onSurface,
            int onSurfaceVariant
    ) {
        if (layout == null) {
            return;
        }

        layout.setBoxStrokeColorStateList(
                createStrokeStateList(
                        primary,
                        onSurfaceVariant
                )
        );

        layout.setHintTextColor(
                ColorStateList.valueOf(
                        onSurfaceVariant
                )
        );

        layout.setDefaultHintTextColor(
                ColorStateList.valueOf(
                        onSurfaceVariant
                )
        );

        if (layout.getEditText() != null) {
            layout.getEditText().setTextColor(
                    onSurface
            );

            layout.getEditText().setHintTextColor(
                    onSurfaceVariant
            );
        }

        layout.setEndIconTintList(
                ColorStateList.valueOf(
                        primary
                )
        );
    }

    private static ColorStateList createStrokeStateList(
            int primary,
            int secondary
    ) {
        return new ColorStateList(
                new int[][]{
                        new int[]{
                                android.R.attr.state_focused
                        },
                        new int[]{
                                android.R.attr.state_enabled
                        },
                        new int[]{}
                },
                new int[]{
                        primary,
                        secondary,
                        secondary
                }
        );
    }

    private static void tintImage(
            @Nullable ImageView imageView,
            int color
    ) {
        if (imageView != null) {
            imageView.setImageTintList(
                    ColorStateList.valueOf(
                            color
                    )
            );
        }
    }

    private static void tintRadio(
            @Nullable RadioButton radioButton,
            int primary,
            int onSurface
    ) {
        if (radioButton == null) {
            return;
        }

        radioButton.setTextColor(
                onSurface
        );

        radioButton.setButtonTintList(
                new ColorStateList(
                        new int[][]{
                                new int[]{
                                        android.R.attr.state_checked
                                },
                                new int[]{
                                        android.R.attr.state_enabled
                                },
                                new int[]{}
                        },
                        new int[]{
                                primary,
                                withAlpha(
                                        onSurface,
                                        140
                                ),
                                withAlpha(
                                        onSurface,
                                        90
                                )
                        }
                )
        );
    }

    private static void tintProgressDots(
            @NonNull Activity activity,
            int primary,
            int inactive
    ) {
        tintDot(
                activity.findViewById(
                        R.id.dot1
                ),
                primary
        );

        tintDot(
                activity.findViewById(
                        R.id.dot2
                ),
                primary
        );

        tintDot(
                activity.findViewById(
                        R.id.dot3
                ),
                primary
        );

        tintDot(
                activity.findViewById(
                        R.id.dot4
                ),
                inactive
        );
    }

    private static void tintDot(
            @Nullable View view,
            int color
    ) {
        if (view != null) {
            view.setBackgroundTintList(
                    ColorStateList.valueOf(
                            color
                    )
            );
        }
    }

    private static void tintAllTextRecursively(
            @Nullable View view,
            int onSurface,
            int onSurfaceVariant
    ) {
        if (view == null) {
            return;
        }

        if (view instanceof TextView) {
            TextView textView =
                    (TextView) view;

            float size =
                    textView.getTextSize()
                            / view.getResources()
                            .getDisplayMetrics()
                            .scaledDensity;

            textView.setTextColor(
                    size >= 20f
                            ? onSurface
                            : onSurfaceVariant
            );
        }

        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group =
                    (android.view.ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                tintAllTextRecursively(
                        group.getChildAt(
                                i
                        ),
                        onSurface,
                        onSurfaceVariant
                );
            }
        }
    }

    private static int resolveColor(
            @NonNull Activity activity,
            int attr,
            int fallback
    ) {
        try {
            return ThemeAttrResolver.resolveColor(
                    activity,
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
}
