package com.example.aichat.view.main.chat.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.R;
import com.example.aichat.view.theme.binders.ThemeBackgroundBinder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Locale;

public final class ChatFragmentUiBinder {

    private ChatFragmentUiBinder() {
    }

    public static void applyScreenTheme(
            Context context,
            View root
    ) {
        if (context == null || root == null) {
            return;
        }

        View chatRoot =
                root.findViewById(
                        R.id.chat_root
                );

        ImageView backgroundImage =
                root.findViewById(
                        R.id.theme_background_image
                );

        View backgroundScrim =
                root.findViewById(
                        R.id.theme_background_scrim
                );

        View contentRoot =
                root.findViewById(
                        R.id.chat_content_root
                );

        boolean hasBackgroundImage =
                false;

        if (context instanceof Activity
                && backgroundImage != null
                && backgroundScrim != null
                && contentRoot != null) {

            hasBackgroundImage =
                    ThemeBackgroundBinder.applyToActivity(
                            (Activity) context,
                            backgroundImage,
                            backgroundScrim,
                            contentRoot
                    );
        }

        int primary =
                resolveColor(
                        context,
                        R.attr.colorPrimary
                );

        int onPrimary =
                resolveColor(
                        context,
                        R.attr.colorOnPrimary
                );

        int surface =
                resolveColor(
                        context,
                        R.attr.colorSurface
                );

        int onSurface =
                resolveColor(
                        context,
                        R.attr.colorOnSurface
                );

        int onSurfaceVariant =
                resolveColor(
                        context,
                        R.attr.colorOnSurfaceVariant
                );

        int secondary =
                resolveColor(
                        context,
                        R.attr.colorSecondary
                );

        if (!hasBackgroundImage) {
            if (backgroundImage != null) {
                backgroundImage.setVisibility(
                        View.GONE
                );
            }

            if (backgroundScrim != null) {
                backgroundScrim.setVisibility(
                        View.GONE
                );
            }

            if (chatRoot != null) {
                chatRoot.setBackground(
                        resolveActivityBackgroundDrawable(context)
                );
            }
        }

        setTransparent(
                contentRoot
        );

        setTransparent(
                find(root, R.id.rv_messages)
        );

        tintImage(
                root,
                R.id.btn_back,
                onSurface
        );

        tintImage(
                root,
                R.id.btn_options,
                onSurface
        );

        tintImage(
                root,
                R.id.btn_attach_file,
                secondary
        );

        setTextColor(
                root,
                R.id.tv_chat_title,
                onSurface
        );

        setTextColor(
                root,
                R.id.tv_search_status,
                onSurface
        );

        setTextColor(
                root,
                R.id.tv_chat_ended,
                onSurface
        );

        setTextColor(
                root,
                R.id.edit_original_text,
                onSurface
        );

        setTextColor(
                root,
                R.id.reply_original_text,
                onSurface
        );

        tintImage(
                root,
                R.id.edit_cancel,
                onSurfaceVariant
        );

        tintImage(
                root,
                R.id.reply_cancel,
                onSurfaceVariant
        );

        tintPanel(
                find(root, R.id.edit_panel),
                surface
        );

        tintPanel(
                find(root, R.id.reply_panel),
                surface
        );

        tintPanel(
                find(root, R.id.input_panel),
                surface
        );

        tintPanel(
                find(root, R.id.search_layout),
                primary
        );

        tintPanel(
                find(root, R.id.searchResult_layout),
                primary
        );

        tintImage(
                root,
                R.id.search_btn_back,
                onPrimary
        );

        tintImage(
                root,
                R.id.btn_search_action,
                onPrimary
        );

        tintImage(
                root,
                R.id.searchResult_btn_back,
                onPrimary
        );

        tintImage(
                root,
                R.id.btn_top,
                onPrimary
        );

        tintImage(
                root,
                R.id.btn_bottom,
                onPrimary
        );

        EditText search =
                root.findViewById(
                        R.id.et_search
                );

        if (search != null) {
            search.setTextColor(
                    onPrimary
            );

            search.setHintTextColor(
                    withAlpha(
                            onPrimary,
                            190
                    )
            );
        }

        setTextColor(
                root,
                R.id.tv_title,
                onPrimary
        );

        setTextColor(
                root,
                R.id.tv_searchResultCount,
                onPrimary
        );

        TextInputEditText input =
                root.findViewById(
                        R.id.ti_message
                );

        if (input != null) {
            input.setTextColor(
                    onSurface
            );

            input.setHintTextColor(
                    onSurfaceVariant
            );

            input.setBackgroundTintList(
                    ColorStateList.valueOf(primary)
            );
        }

        Button sendButton =
                root.findViewById(
                        R.id.b_send_message
                );

        if (sendButton != null) {
            sendButton.setTextColor(
                    onPrimary
            );

            sendButton.setBackgroundTintList(
                    ColorStateList.valueOf(primary)
            );
        }

        Button cancelSearch =
                root.findViewById(
                        R.id.btn_cancel_search
                );

        if (cancelSearch != null) {
            cancelSearch.setTextColor(
                    onPrimary
            );

            cancelSearch.setBackgroundTintList(
                    ColorStateList.valueOf(primary)
            );
        }

        Button exportChat =
                root.findViewById(
                        R.id.b_export_chat
                );

        if (exportChat != null) {
            exportChat.setTextColor(
                    onPrimary
            );

            exportChat.setBackgroundTintList(
                    ColorStateList.valueOf(primary)
            );
        }

        TextView fileCounter =
                root.findViewById(
                        R.id.file_counter
                );

        if (fileCounter != null) {
            fileCounter.setTextColor(
                    onPrimary
            );
        }

        View divider =
                root.findViewById(
                        R.id.divider_chat_ended
                );

        if (divider != null) {
            divider.setBackgroundColor(
                    withAlpha(
                            onSurfaceVariant,
                            120
                    )
            );
        }
    }

    private static View find(
            View root,
            int id
    ) {
        if (root == null) {
            return null;
        }

        return root.findViewById(
                id
        );
    }

    private static int resolveColor(
            Context context,
            int attr
    ) {
        return ThemeAttrResolver.resolveColor(
                context,
                attr
        );
    }

    private static void setTransparent(
            View view
    ) {
        if (view == null) {
            return;
        }

        view.setBackgroundColor(
                Color.TRANSPARENT
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

        if (textView != null) {
            textView.setTextColor(
                    color
            );
        }
    }

    private static void tintImage(
            View root,
            int id,
            int color
    ) {
        ImageView imageView =
                root.findViewById(
                        id
                );

        if (imageView != null) {
            ImageViewCompat.setImageTintList(
                    imageView,
                    ColorStateList.valueOf(color)
            );
        }
    }

    private static void tintPanel(
            View view,
            int color
    ) {
        if (view == null) {
            return;
        }

        Drawable background =
                view.getBackground();

        if (background != null) {
            Drawable mutated =
                    background.mutate();

            mutated.setTint(
                    color
            );

            view.setBackground(
                    mutated
            );

        } else {
            view.setBackgroundColor(
                    color
            );
        }

        view.invalidate();
    }

    private static Drawable resolveActivityBackgroundDrawable(
            Context context
    ) {
        Drawable windowBackground =
                resolveWindowBackgroundDrawable(
                        context
                );

        if (windowBackground != null
                && !isFlatWrongDarkBackground(
                context,
                windowBackground
        )) {

            return windowBackground;
        }

        int color =
                ThemeAttrResolver.resolveColor(
                        context,
                        android.R.attr.colorBackground
                );

        if (isDarkUiMode(context)
                && isVeryDarkColor(color)) {

            int primary =
                    ThemeAttrResolver.resolveColor(
                            context,
                            R.attr.colorPrimary
                    );

            color =
                    blendColors(
                            color,
                            primary,
                            0.18f
                    );
        }

        return new ColorDrawable(
                color
        );
    }

    private static Drawable resolveWindowBackgroundDrawable(
            Context context
    ) {
        TypedValue typedValue =
                new TypedValue();

        boolean resolved =
                context.getTheme()
                        .resolveAttribute(
                                android.R.attr.windowBackground,
                                typedValue,
                                true
                        );

        if (!resolved) {
            return null;
        }

        Drawable drawable =
                null;

        if (typedValue.resourceId != 0) {
            drawable =
                    ContextCompat.getDrawable(
                            context,
                            typedValue.resourceId
                    );

        } else if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {

            drawable =
                    new ColorDrawable(
                            typedValue.data
                    );
        }

        if (drawable == null) {
            return null;
        }

        if (drawable.getConstantState() != null) {
            return drawable.getConstantState()
                    .newDrawable(
                            context.getResources()
                    )
                    .mutate();
        }

        return drawable.mutate();
    }

    private static boolean isFlatWrongDarkBackground(
            Context context,
            Drawable drawable
    ) {
        if (!isDarkUiMode(context)) {
            return false;
        }

        if (!(drawable instanceof ColorDrawable)) {
            return false;
        }

        int color =
                ((ColorDrawable) drawable)
                        .getColor();

        String hex =
                toHex(color);

        return hex.equalsIgnoreCase("#141218")
                || hex.equalsIgnoreCase("#121212")
                || hex.equalsIgnoreCase("#0F0F0F");
    }

    private static boolean isDarkUiMode(
            Context context
    ) {
        int nightMode =
                context.getResources()
                        .getConfiguration()
                        .uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;

        return nightMode
                == Configuration.UI_MODE_NIGHT_YES;
    }

    private static boolean isVeryDarkColor(
            int color
    ) {
        double darkness =
                1.0 - (
                        0.299 * Color.red(color)
                                + 0.587 * Color.green(color)
                                + 0.114 * Color.blue(color)
                ) / 255.0;

        return darkness > 0.82;
    }

    private static int withAlpha(
            int color,
            int alpha
    ) {
        return Color.argb(
                alpha,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    private static int blendColors(
            int baseColor,
            int overlayColor,
            float ratio
    ) {
        float inverseRatio =
                1f - ratio;

        int red =
                Math.round(
                        Color.red(baseColor) * inverseRatio
                                + Color.red(overlayColor) * ratio
                );

        int green =
                Math.round(
                        Color.green(baseColor) * inverseRatio
                                + Color.green(overlayColor) * ratio
                );

        int blue =
                Math.round(
                        Color.blue(baseColor) * inverseRatio
                                + Color.blue(overlayColor) * ratio
                );

        return Color.rgb(
                red,
                green,
                blue
        );
    }

    private static String toHex(
            int color
    ) {
        return String.format(
                Locale.US,
                "#%06X",
                0xFFFFFF & color
        );
    }
}
