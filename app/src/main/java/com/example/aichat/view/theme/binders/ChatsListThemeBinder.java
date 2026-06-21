package com.example.aichat.view.theme.binders;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public final class ChatsListThemeBinder {

    private ChatsListThemeBinder() {
    }

    public static void bind(
            View root
    ) {

        if (root == null) {
            return;
        }

        int primary =
                ThemeAttrResolver.resolveColor(
                        root.getContext(),
                        R.attr.colorPrimary
                );

        int surface =
                ThemeAttrResolver.resolveColor(
                        root.getContext(),
                        R.attr.colorSurface
                );

        int onSurface =
                ThemeAttrResolver.resolveColor(
                        root.getContext(),
                        R.attr.colorOnSurface
                );

        int onSurfaceVariant =
                ThemeAttrResolver.resolveColor(
                        root.getContext(),
                        R.attr.colorOnSurfaceVariant
                );

        root.setBackgroundColor(
                Color.TRANSPARENT
        );

        RecyclerView recyclerView =
                root.findViewById(
                        R.id.rv_chats
                );

        if (recyclerView != null) {

            recyclerView.setBackgroundColor(
                    Color.TRANSPARENT
            );
        }

        View bottomPanel =
                root.findViewById(
                        R.id.bottom_panel
                );

        if (bottomPanel != null) {

            bottomPanel.setBackgroundTintList(
                    ColorStateList.valueOf(
                            surface
                    )
            );
        }

        FloatingActionButton fab =
                root.findViewById(
                        R.id.fab_add_chat
                );

        if (fab != null) {

            fab.setBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            fab.setSupportBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            fab.setImageTintList(
                    ColorStateList.valueOf(
                            Color.WHITE
                    )
            );

            fab.setRippleColor(
                    withAlpha(
                            onSurface,
                            48
                    )
            );

            fab.clearColorFilter();

            fab.invalidate();
        }

        tintImageButton(
                root,
                R.id.btn_themes,
                onSurface
        );

        tintImageButton(
                root,
                R.id.btn_leaderboard,
                onSurface
        );

        tintImageButton(
                root,
                R.id.btn_settings,
                onSurface
        );

        tintImageButton(
                root,
                R.id.btn_subscribe,
                onSurface
        );

        setTextColor(
                root,
                R.id.tv_empty_chats_subtitle,
                onSurfaceVariant
        );
    }

    private static void tintImageButton(
            View root,
            int id,
            int color
    ) {

        ImageButton button =
                root.findViewById(
                        id
                );

        if (button == null) {
            return;
        }

        button.setImageTintList(
                ColorStateList.valueOf(
                        color
                )
        );
    }

    private static void setTextColor(
            View root,
            int id,
            int color
    ) {

        TextView textView =
                root.findViewById(
                        id
                );

        if (textView == null) {
            return;
        }

        textView.setTextColor(
                color
        );
    }

    private static int withAlpha(
            int color,
            int alpha
    ) {

        return Color.argb(
                alpha,
                Color.red(
                        color
                ),
                Color.green(
                        color
                ),
                Color.blue(
                        color
                )
        );
    }
}
