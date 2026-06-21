package com.example.aichat.view.theme.binders;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.ColorUtils;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;

public final class ScreenThemeBinder {

    private ScreenThemeBinder() {
    }

    @ColorInt
    public static int primary(@NonNull Context context) {
        return resolveColor(
                context,
                com.google.android.material.R.attr.colorPrimary,
                0xFF20A39A
        );
    }

    @ColorInt
    public static int onPrimary(@NonNull Context context) {
        return resolveColor(
                context,
                com.google.android.material.R.attr.colorOnPrimary,
                Color.WHITE
        );
    }

    @ColorInt
    public static int surface(@NonNull Context context) {
        return resolveColor(
                context,
                com.google.android.material.R.attr.colorSurface,
                Color.WHITE
        );
    }

    @ColorInt
    public static int surfaceVariant(@NonNull Context context) {
        return resolveColor(
                context,
                com.google.android.material.R.attr.colorSurfaceVariant,
                ColorUtils.blendARGB(
                        surface(context),
                        Color.BLACK,
                        0.05f
                )
        );
    }

    @ColorInt
    public static int background(@NonNull Context context) {
        return resolveColor(
                context,
                android.R.attr.colorBackground,
                Color.WHITE
        );
    }

    @ColorInt
    public static int onSurface(@NonNull Context context) {
        return resolveColor(
                context,
                com.google.android.material.R.attr.colorOnSurface,
                0xFF1A1A1A
        );
    }

    @ColorInt
    public static int onSurfaceVariant(@NonNull Context context) {
        return resolveColor(
                context,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                0xFF666666
        );
    }

    @ColorInt
    public static int outline(@NonNull Context context) {
        return resolveColor(
                context,
                com.google.android.material.R.attr.colorOutline,
                withAlpha(
                        onSurfaceVariant(context),
                        82
                )
        );
    }

    public static void applyWindow(
            @Nullable Activity activity
    ) {
    }

    public static void tintIconPrimary(
            @Nullable View view
    ) {
        if (view == null) {
            return;
        }

        tintIcon(
                view,
                primary(
                        view.getContext()
                )
        );
    }

    public static void tintIcon(
            @Nullable View view,
            int color
    ) {
        if (view instanceof ImageButton) {
            ((ImageButton) view).setImageTintList(
                    ColorStateList.valueOf(
                            color
                    )
            );
            return;
        }

        if (view instanceof ImageView) {
            ((ImageView) view).setImageTintList(
                    ColorStateList.valueOf(
                            color
                    )
            );
        }
    }

    public static void textColor(
            @Nullable TextView view,
            int color
    ) {
        if (view != null) {
            view.setTextColor(
                    color
            );
        }
    }

    public static void primaryText(
            @Nullable TextView view
    ) {
        if (view != null) {
            view.setTextColor(
                    primary(
                            view.getContext()
                    )
            );
        }
    }

    public static void onPrimaryText(
            @Nullable TextView view
    ) {
        if (view != null) {
            view.setTextColor(
                    onPrimary(
                            view.getContext()
                    )
            );
        }
    }

    public static void whiteText(
            @Nullable TextView view
    ) {
        if (view != null) {
            view.setTextColor(
                    Color.WHITE
            );
        }
    }

    public static void surfaceText(
            @Nullable TextView view
    ) {
        if (view != null) {
            view.setTextColor(
                    onSurface(
                            view.getContext()
                    )
            );
        }
    }

    public static void variantText(
            @Nullable TextView view
    ) {
        if (view != null) {
            view.setTextColor(
                    onSurfaceVariant(
                            view.getContext()
                    )
            );
        }
    }

    public static void tintTextCompoundDrawables(
            @Nullable TextView view,
            int color
    ) {
        if (view == null) {
            return;
        }

        android.graphics.drawable.Drawable[] drawables =
                view.getCompoundDrawablesRelative();

        for (android.graphics.drawable.Drawable drawable : drawables) {
            if (drawable == null) {
                continue;
            }

            drawable.mutate();
            drawable.setTint(
                    color
            );
        }

        view.setCompoundDrawablesRelativeWithIntrinsicBounds(
                drawables[0],
                drawables[1],
                drawables[2],
                drawables[3]
        );
    }

    public static void primaryButton(
            @Nullable View view
    ) {
        if (view == null) {
            return;
        }

        int primary =
                primary(
                        view.getContext()
                );

        int onPrimary =
                onPrimary(
                        view.getContext()
                );

        if (view instanceof MaterialButton) {
            MaterialButton button =
                    (MaterialButton) view;

            button.setBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            button.setTextColor(
                    onPrimary
            );

            button.setIconTint(
                    ColorStateList.valueOf(
                            onPrimary
                    )
            );

            button.setRippleColor(
                    ColorStateList.valueOf(
                            withAlpha(
                                    onPrimary,
                                    45
                            )
                    )
            );

            return;
        }

        if (view instanceof Button) {
            Button button =
                    (Button) view;

            button.setBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            button.setTextColor(
                    onPrimary
            );
        }
    }

    public static void primarySwitch(
            @Nullable MaterialSwitch switchView
    ) {
        if (switchView == null) {
            return;
        }

        Context context =
                switchView.getContext();

        int primary =
                primary(
                        context
                );

        int onPrimary =
                onPrimary(
                        context
                );

        int variant =
                onSurfaceVariant(
                        context
                );

        switchView.setThumbTintList(
                new ColorStateList(
                        new int[][]{
                                new int[]{
                                        android.R.attr.state_checked
                                },
                                new int[]{}
                        },
                        new int[]{
                                onPrimary,
                                surface(
                                        context
                                )
                        }
                )
        );

        switchView.setTrackTintList(
                new ColorStateList(
                        new int[][]{
                                new int[]{
                                        android.R.attr.state_checked
                                },
                                new int[]{}
                        },
                        new int[]{
                                primary,
                                withAlpha(
                                        variant,
                                        72
                                )
                        }
                )
        );
    }

    public static void primarySeekBar(
            @Nullable SeekBar seekBar
    ) {
        if (seekBar == null) {
            return;
        }

        int primary =
                primary(
                        seekBar.getContext()
                );

        int variant =
                onSurfaceVariant(
                        seekBar.getContext()
                );

        seekBar.setProgressTintList(
                ColorStateList.valueOf(
                        primary
                )
        );

        seekBar.setThumbTintList(
                ColorStateList.valueOf(
                        primary
                )
        );

        seekBar.setProgressBackgroundTintList(
                ColorStateList.valueOf(
                        withAlpha(
                                variant,
                                52
                        )
                )
        );
    }

    public static void textInputLayout(
            @Nullable TextInputLayout layout
    ) {
        if (layout == null) {
            return;
        }

        Context context =
                layout.getContext();

        int primary =
                primary(
                        context
                );

        int onSurface =
                onSurface(
                        context
                );

        int variant =
                onSurfaceVariant(
                        context
                );

        layout.setBoxStrokeColorStateList(
                new ColorStateList(
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
                                withAlpha(
                                        variant,
                                        150
                                ),
                                withAlpha(
                                        variant,
                                        100
                                )
                        }
                )
        );

        layout.setHintTextColor(
                ColorStateList.valueOf(
                        variant
                )
        );

        layout.setDefaultHintTextColor(
                ColorStateList.valueOf(
                        variant
                )
        );

        if (layout.getEditText() != null) {
            layout.getEditText().setTextColor(
                    onSurface
            );

            layout.getEditText().setHintTextColor(
                    variant
            );
        }
    }

    public static void surfaceCard(
            @Nullable View view,
            float radiusDp
    ) {
        if (view == null) {
            return;
        }

        Context context =
                view.getContext();

        int surface =
                surface(
                        context
                );

        if (view instanceof MaterialCardView) {
            MaterialCardView card =
                    (MaterialCardView) view;

            card.setCardBackgroundColor(
                    surface
            );

            card.setStrokeColor(
                    outline(
                            context
                    )
            );

            card.setStrokeWidth(
                    dp(
                            context,
                            1
                    )
            );

            card.setRadius(
                    dp(
                            context,
                            radiusDp
                    )
            );

            return;
        }

        if (view instanceof CardView) {
            CardView card =
                    (CardView) view;

            card.setCardBackgroundColor(
                    surface
            );

            card.setRadius(
                    dp(
                            context,
                            radiusDp
                    )
            );

            return;
        }

        view.setBackground(
                rounded(
                        context,
                        surface,
                        radiusDp
                )
        );
    }

    public static void surfaceRoundedBackground(
            @Nullable View view,
            float radiusDp
    ) {
        if (view == null) {
            return;
        }

        view.setBackground(
                rounded(
                        view.getContext(),
                        surface(
                                view.getContext()
                        ),
                        radiusDp
                )
        );
    }

    public static void primaryRoundedBackground(
            @Nullable View view,
            float radiusDp
    ) {
        if (view == null) {
            return;
        }

        view.setBackground(
                rounded(
                        view.getContext(),
                        primary(
                                view.getContext()
                        ),
                        radiusDp
                )
        );
    }

    public static void headerGradientBackground(
            @Nullable View view,
            float radiusDp
    ) {
        if (view == null) {
            return;
        }

        Context context =
                view.getContext();

        int primary =
                primary(
                        context
                );

        int endColor =
                ColorUtils.blendARGB(
                        primary,
                        Color.WHITE,
                        0.22f
                );

        GradientDrawable drawable =
                new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{
                                primary,
                                endColor
                        }
                );

        drawable.setCornerRadius(
                dp(
                        context,
                        radiusDp
                )
        );

        view.setBackground(
                drawable
        );
    }

    public static void primarySoftIconBackground(
            @Nullable View view,
            float radiusDp
    ) {
        if (view == null) {
            return;
        }

        view.setBackground(
                rounded(
                        view.getContext(),
                        withAlpha(
                                primary(
                                        view.getContext()
                                ),
                                28
                        ),
                        radiusDp
                )
        );
    }

    public static GradientDrawable rounded(
            @NonNull Context context,
            int color,
            float radiusDp
    ) {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                color
        );

        drawable.setCornerRadius(
                dp(
                        context,
                        radiusDp
                )
        );

        return drawable;
    }

    public static int resolveColor(
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

    public static int withAlpha(
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

    public static int dp(
            @NonNull Context context,
            float value
    ) {
        return Math.round(
                value
                        * context
                        .getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
