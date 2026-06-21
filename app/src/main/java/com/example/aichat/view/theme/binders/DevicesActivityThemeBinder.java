package com.example.aichat.view.theme.binders;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

public final class DevicesActivityThemeBinder {

    private DevicesActivityThemeBinder() {
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

        tintImageButton(
                activity.findViewById(
                        R.id.btn_back
                ),
                primary
        );

        tintAllTextRecursively(
                root,
                primary,
                onSurface,
                onSurfaceVariant
        );

        TabLayout tabLayout =
                activity.findViewById(
                        R.id.tabLayout
                );

        if (tabLayout != null) {
            tabLayout.setSelectedTabIndicatorColor(
                    primary
            );

            tabLayout.setTabTextColors(
                    onSurfaceVariant,
                    primary
            );
        }

        MaterialButton terminateButton =
                activity.findViewById(
                        R.id.terminateButton
                );

        if (terminateButton != null) {
            terminateButton.setBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            terminateButton.setTextColor(
                    onPrimary
            );

            terminateButton.setRippleColor(
                    ColorStateList.valueOf(
                            withAlpha(
                                    onPrimary,
                                    40
                            )
                    )
            );
        }

        RecyclerView recyclerView =
                activity.findViewById(
                        R.id.devicesRecyclerView
                );

        if (recyclerView != null) {
            recyclerView.setBackgroundColor(
                    Color.TRANSPARENT
            );
        }
    }

    public static void applyDeviceItem(@Nullable View itemView) {
        if (itemView == null) {
            return;
        }

        Context context =
                itemView.getContext();

        int primary =
                resolveColor(
                        context,
                        com.google.android.material.R.attr.colorPrimary,
                        0xFF20A39A
                );

        int onSurface =
                resolveColor(
                        context,
                        com.google.android.material.R.attr.colorOnSurface,
                        0xFF1A1A1A
                );

        int onSurfaceVariant =
                resolveColor(
                        context,
                        com.google.android.material.R.attr.colorOnSurfaceVariant,
                        0xFF666666
                );

        itemView.setBackgroundColor(
                Color.TRANSPARENT
        );

        setTextColor(
                itemView.findViewById(
                        R.id.deviceName
                ),
                onSurface
        );

        setTextColor(
                itemView.findViewById(
                        R.id.lastActivity
                ),
                onSurfaceVariant
        );

        tintImageButton(
                itemView.findViewById(
                        R.id.btn_logout
                ),
                primary
        );
    }

    public static void applyQrBottomSheet(@Nullable View view) {
        if (view == null) {
            return;
        }

        Context context =
                view.getContext();

        int primary =
                resolveColor(
                        context,
                        com.google.android.material.R.attr.colorPrimary,
                        0xFF20A39A
                );

        int onSurface =
                resolveColor(
                        context,
                        com.google.android.material.R.attr.colorOnSurface,
                        0xFF1A1A1A
                );

        int onSurfaceVariant =
                resolveColor(
                        context,
                        com.google.android.material.R.attr.colorOnSurfaceVariant,
                        0xFF666666
                );

        tintAllTextRecursively(
                view,
                primary,
                onSurface,
                onSurfaceVariant
        );

        tintImage(
                view.findViewById(
                        R.id.iv_tap_hint
                ),
                primary
        );

        tintImageButton(
                view.findViewById(
                        R.id.btn_close_qr
                ),
                primary
        );
    }

    private static void tintAllTextRecursively(
            @Nullable View view,
            int primary,
            int onSurface,
            int onSurfaceVariant
    ) {
        if (view == null) {
            return;
        }

        if (view instanceof TextView) {
            TextView textView =
                    (TextView) view;

            float sp =
                    textView.getTextSize()
                            / view.getResources()
                            .getDisplayMetrics()
                            .scaledDensity;

            textView.setTextColor(
                    sp >= 19f
                            ? primary
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
                        primary,
                        onSurface,
                        onSurfaceVariant
                );
            }
        }
    }

    private static void setTextColor(
            @Nullable TextView textView,
            int color
    ) {
        if (textView != null) {
            textView.setTextColor(
                    color
            );
        }
    }

    private static void tintImageButton(
            @Nullable ImageButton imageButton,
            int color
    ) {
        if (imageButton != null) {
            imageButton.setImageTintList(
                    ColorStateList.valueOf(
                            color
                    )
            );
        }
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

    private static int resolveColor(
            @NonNull Context context,
            int attr,
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
}
