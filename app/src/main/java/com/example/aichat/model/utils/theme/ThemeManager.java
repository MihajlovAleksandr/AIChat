package com.example.aichat.model.utils.theme;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.core.widget.ImageViewCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.io.File;
import java.util.List;

public class ThemeManager {

    private final Activity activity;

    private final ThemeStorage storage;

    public ThemeManager(
            Activity activity
    ) {
        this.activity =
                activity;

        this.storage =
                new ThemeStorage(
                        activity
                );
    }

    public void apply(
            String themeId
    ) {
        ThemeModel theme =
                getTheme(
                        themeId
                );

        if (theme == null) {
            return;
        }

        ThemeAttrResolver.applyTheme(
                theme
        );

        View root =
                activity.findViewById(
                        android.R.id.content
                );

        if (root == null) {
            return;
        }

        boolean hasBackgroundImage =
                hasBackgroundImage(
                        theme
                );

        applyToView(
                root,
                theme,
                hasBackgroundImage
        );
    }

    private ThemeModel getTheme(
            String themeId
    ) {
        if (themeId == null) {
            return null;
        }

        List<ThemeModel> themes =
                storage.getThemes();

        for (ThemeModel theme : themes) {
            if (themeId.equals(theme.getId())) {
                return theme;
            }
        }

        return null;
    }

    private void applyToView(
            View view,
            ThemeModel theme,
            boolean hasBackgroundImage
    ) {
        if (view == null) {
            return;
        }

        ThemeColors colors =
                new ThemeColors(
                        theme
                );

        boolean handled =
                applySpecialViews(
                        view,
                        colors,
                        hasBackgroundImage
                );

        if (!handled) {
            applyGenericViewTheme(
                    view,
                    colors
            );
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                 i < group.getChildCount();
                 i++) {

                applyToView(
                        group.getChildAt(i),
                        theme,
                        hasBackgroundImage
                );
            }
        }
    }

    private void applyGenericViewTheme(
            View view,
            ThemeColors colors
    ) {
        if (view instanceof CardView) {
            ((CardView) view).setCardBackgroundColor(
                    colors.surface
            );
        }

        if (view instanceof TextView) {
            TextView textView =
                    (TextView) view;

            textView.setTextColor(
                    colors.onSurface
            );

            textView.setHintTextColor(
                    colors.onSurfaceVariant
            );
        }

        if (view instanceof EditText) {
            EditText editText =
                    (EditText) view;

            editText.setTextColor(
                    colors.onSurface
            );

            editText.setHintTextColor(
                    colors.onSurfaceVariant
            );

            editText.setBackgroundTintList(
                    ColorStateList.valueOf(
                            colors.primary
                    )
            );
        }

        if (view instanceof TextInputEditText) {
            TextInputEditText editText =
                    (TextInputEditText) view;

            editText.setTextColor(
                    colors.onSurface
            );

            editText.setHintTextColor(
                    colors.onSurfaceVariant
            );

            editText.setBackgroundTintList(
                    ColorStateList.valueOf(
                            colors.primary
                    )
            );
        }

        if (view instanceof MaterialButton) {
            MaterialButton button =
                    (MaterialButton) view;

            button.setBackgroundTintList(
                    ColorStateList.valueOf(
                            colors.primary
                    )
            );

            button.setTextColor(
                    colors.onSurface
            );

            button.invalidate();

        } else if (view instanceof Button) {
            Button button =
                    (Button) view;

            button.setTextColor(
                    colors.onSurface
            );

            Drawable background =
                    button.getBackground();

            if (background != null) {
                Drawable mutated =
                        background.mutate();

                mutated.setTint(
                        colors.primary
                );

                button.setBackground(
                        mutated
                );
            }
        }
    }

    private boolean applySpecialViews(
            View view,
            ThemeColors colors,
            boolean hasBackgroundImage
    ) {
        String idName =
                getIdName(
                        view
                );

        if (idName == null) {
            return false;
        }

        if (isId(
                idName,
                "theme_background_image",
                "theme_background_scrim"
        )) {
            return true;
        }

        if (isId(
                idName,
                "main_container",
                "view_pager",
                "view_pager_fragment_container",
                "root_constraint_layout",
                "chat_root",
                "chat_content_root",
                "themes_root",
                "themes_content_root",
                "editor_root",
                "editor_content_root",
                "nested_scroll",
                "rv_chats",
                "rv_messages",
                "item_my_message_root",
                "item_other_message_root"
        )) {
            if (hasBackgroundImage) {
                view.setBackgroundColor(
                        Color.TRANSPARENT
                );
            }

            return true;
        }

        if (isId(
                idName,
                "item_chat_root"
        )) {
            return true;
        }

        if (isId(
                idName,
                "top_bar"
        )) {
            view.setBackgroundColor(
                    Color.TRANSPARENT
            );

            return true;
        }

        if (isId(
                idName,
                "search_layout",
                "searchResult_layout",
                "premium_header"
        )) {
            tintViewBackground(
                    view,
                    colors.primary
            );

            return true;
        }

        if (isId(
                idName,
                "edit_panel",
                "reply_panel",
                "bottom_panel",
                "input_panel"
        )) {
            tintViewBackground(
                    view,
                    colors.surface
            );

            return true;
        }

        if (isId(
                idName,
                "productCard"
        )) {
            if (view instanceof CardView) {
                ((CardView) view).setCardBackgroundColor(
                        colors.primary
                );

                view.invalidate();
            }

            return true;
        }

        if (isId(
                idName,
                "card_create_theme",
                "card_theme_item"
        )) {
            if (view instanceof CardView) {
                ((CardView) view).setCardBackgroundColor(
                        colors.surface
                );

                view.invalidate();
            }

            return true;
        }

        if (isId(
                idName,
                "iconCard"
        )) {
            if (view instanceof CardView) {
                ((CardView) view).setCardBackgroundColor(
                        colors.secondary
                );

                view.invalidate();
            }

            return true;
        }

        if (isId(
                idName,
                "divider_chat_ended"
        )) {
            view.setBackgroundColor(
                    withAlpha(
                            colors.onSurfaceVariant,
                            120
                    )
            );

            return true;
        }

        if (isId(
                idName,
                "b_send_message",
                "b_export_chat",
                "btn_cancel_search",
                "payButton"
        )) {
            if (view instanceof Button) {
                Button button =
                        (Button) view;

                button.setBackgroundTintList(
                        ColorStateList.valueOf(
                                colors.primary
                        )
                );

                button.setTextColor(
                        colors.onSurface
                );

                button.invalidate();
            }

            return true;
        }

        if (isId(
                idName,
                "fab_add_chat"
        )) {
            if (view instanceof FloatingActionButton) {
                FloatingActionButton fab =
                        (FloatingActionButton) view;

                fab.setBackgroundTintList(
                        ColorStateList.valueOf(
                                colors.primary
                        )
                );

                fab.setImageTintList(
                        ColorStateList.valueOf(
                                Color.WHITE
                        )
                );

                fab.setRippleColor(
                        ColorStateList.valueOf(
                                colors.onSurface
                        )
                );

                fab.invalidate();
            }

            return true;
        }

        if (view instanceof TextInputLayout) {
            TextInputLayout layout =
                    (TextInputLayout) view;

            layout.setBoxBackgroundColor(
                    colors.surface
            );

            layout.setBoxStrokeColor(
                    colors.primary
            );

            layout.setHintTextColor(
                    ColorStateList.valueOf(
                            colors.onSurfaceVariant
                    )
            );

            return true;
        }

        if (isId(
                idName,
                "message_container"
        )) {
            String tagValue =
                    getTagValue(
                            view
                    );

            int bubbleColor =
                    "other_message".equals(tagValue)
                            ? colors.otherMessageColor
                            : colors.myMessageColor;

            applyBubbleColor(
                    view,
                    bubbleColor
            );

            return true;
        }

        if (isId(
                idName,
                "message_text"
        ) && view instanceof TextView) {
            String messageType =
                    getNearestMessageContainerTag(
                            view
                    );

            if ("my_message".equals(messageType)) {
                ((TextView) view).setTextColor(
                        getContrastColor(
                                colors.myMessageColor
                        )
                );

            } else {
                ((TextView) view).setTextColor(
                        getContrastColor(
                                colors.otherMessageColor
                        )
                );
            }

            return true;
        }

        if (isId(
                idName,
                "time_text"
        ) && view instanceof TextView) {
            String messageType =
                    getNearestMessageContainerTag(
                            view
                    );

            if ("my_message".equals(messageType)) {
                int contrast =
                        getContrastColor(
                                colors.myMessageColor
                        );

                ((TextView) view).setTextColor(
                        withAlpha(
                                contrast,
                                178
                        )
                );

            } else {
                int contrast =
                        getContrastColor(
                                colors.otherMessageColor
                        );

                ((TextView) view).setTextColor(
                        withAlpha(
                                contrast,
                                150
                        )
                );
            }

            return true;
        }

        if (isId(
                idName,
                "productBadgeText"
        )) {
            Drawable background =
                    view.getBackground();

            if (background != null) {
                Drawable mutated =
                        background.mutate();

                mutated.setTint(
                        colors.secondary
                );

                view.setBackground(
                        mutated
                );
            }

            if (view instanceof TextView) {
                ((TextView) view).setTextColor(
                        colors.onSurface
                );
            }

            return true;
        }

        if (isId(
                idName,
                "btn_back",
                "btn_options",
                "btn_attach_file",
                "search_btn_back",
                "btn_search_action",
                "searchResult_btn_back",
                "btn_top",
                "btn_bottom",
                "btn_history",
                "btn_themes",
                "btn_marketplace",
                "btn_settings",
                "btn_subscribe",
                "btn_theme_menu",
                "edit_cancel",
                "reply_cancel"
        )) {
            if (view instanceof ImageView) {
                ImageViewCompat.setImageTintList(
                        (ImageView) view,
                        ColorStateList.valueOf(
                                colors.secondary
                        )
                );
            }

            return true;
        }

        if (isId(
                idName,
                "status_icon"
        )) {
            if (view instanceof ImageView) {
                ImageViewCompat.setImageTintList(
                        (ImageView) view,
                        ColorStateList.valueOf(
                                colors.onSurfaceVariant
                        )
                );
            }

            return true;
        }

        if (isId(
                idName,
                "tv_chat_title",
                "tv_title",
                "tv_my_themes",
                "tv_library",
                "tv_subscriptions_title",
                "tv_tokens_title",
                "titleText",
                "productNameText",
                "productCurrencyText",
                "productPriceText",
                "tv_premium_title",
                "tv_premium_description",
                "tv_chat_name",
                "tv_chat_ended",
                "edit_original_text",
                "reply_original_text"
        )) {
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(
                        colors.onSurface
                );
            }

            return true;
        }

        if (isId(
                idName,
                "productDescriptionText",
                "tv_theme_subtitle",
                "tv_last_message",
                "tv_time"
        )) {
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(
                        colors.onSurfaceVariant
                );
            }

            return true;
        }

        return false;
    }

    private void tintViewBackground(
            View view,
            int color
    ) {
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

    private void applyBubbleColor(
            View view,
            int color
    ) {
        Drawable background =
                view.getBackground();

        if (background == null) {
            view.setBackgroundColor(
                    color
            );

            return;
        }

        Drawable mutated =
                background.mutate();

        mutated.setTint(
                color
        );

        view.setBackground(
                mutated
        );

        view.invalidate();
    }

    private String getNearestMessageContainerTag(
            View view
    ) {
        ViewParent parent =
                view.getParent();

        while (parent instanceof View) {
            View parentView =
                    (View) parent;

            String idName =
                    getIdName(
                            parentView
                    );

            if ("message_container".equals(idName)) {
                return getTagValue(
                        parentView
                );
            }

            parent =
                    parentView.getParent();
        }

        return null;
    }

    private String getTagValue(
            View view
    ) {
        Object tag =
                view.getTag();

        if (tag == null) {
            return null;
        }

        return tag.toString();
    }

    private int getContrastColor(
            int backgroundColor
    ) {
        int red =
                Color.red(backgroundColor);

        int green =
                Color.green(backgroundColor);

        int blue =
                Color.blue(backgroundColor);

        double luminance =
                0.299 * red
                        + 0.587 * green
                        + 0.114 * blue;

        return luminance > 186
                ? Color.parseColor("#1A1A1A")
                : Color.WHITE;
    }

    private int withAlpha(
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

    private String getIdName(
            View view
    ) {
        int id =
                view.getId();

        if (id == View.NO_ID) {
            return null;
        }

        try {
            return view.getResources()
                    .getResourceEntryName(id);

        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isId(
            String currentId,
            String... ids
    ) {
        if (currentId == null
                || ids == null) {

            return false;
        }

        for (String id : ids) {
            if (currentId.equals(id)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasBackgroundImage(
            ThemeModel theme
    ) {
        if (theme == null) {
            return false;
        }

        String path =
                theme.getBackgroundImagePath();

        if (path == null
                || path.trim().isEmpty()) {

            return false;
        }

        File file =
                new File(path);

        return file.exists();
    }
}
