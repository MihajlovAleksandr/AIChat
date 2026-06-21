package com.example.aichat.view.theme.binders;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.model.utils.theme.ThemeModel;
import com.example.aichat.model.utils.theme.ThemeStorage;
import com.example.aichat.model.utils.theme.ThemeUtils;
import java.io.File;

public final class ThemeBackgroundBinder {

    private ThemeBackgroundBinder() {
    }

    public static boolean applyToActivity(
            Activity activity,
            ImageView backgroundImageView,
            View scrimView,
            View contentRoot
    ) {

        if (activity == null) {
            return false;
        }

        ThemeStorage themeStorage =
                new ThemeStorage(
                        activity
                );

        ThemeModel selectedTheme =
                themeStorage.getSelectedTheme();

        if (selectedTheme != null) {

            ThemeAttrResolver.applyTheme(
                    selectedTheme
            );

        } else {

            ThemeAttrResolver.clear();
        }

        boolean hasBackgroundImage =
                hasExistingBackgroundImage(
                        selectedTheme
                );

        Drawable baseBackground =
                resolveBaseBackground(
                        activity,
                        selectedTheme
                );

        View decorContent =
                activity.findViewById(
                        android.R.id.content
                );

        if (decorContent != null) {

            decorContent.setBackground(
                    cloneDrawable(
                            baseBackground
                    )
            );
        }

        if (hasBackgroundImage) {

            if (contentRoot != null) {

                contentRoot.setBackgroundColor(
                        Color.TRANSPARENT
                );
            }

            if (backgroundImageView != null) {

                File imageFile =
                        new File(
                                selectedTheme.getBackgroundImagePath()
                        );

                backgroundImageView.setVisibility(
                        View.VISIBLE
                );

                backgroundImageView.setAlpha(
                        1f
                );

                backgroundImageView.setBackgroundColor(
                        Color.TRANSPARENT
                );

                Glide.with(
                                activity
                        )
                        .load(
                                imageFile
                        )
                        .centerCrop()
                        .into(
                                backgroundImageView
                        );
            }

            if (scrimView != null) {

                scrimView.setVisibility(
                        View.VISIBLE
                );
            }

            return true;
        }

        hideBackgroundImage(
                backgroundImageView,
                scrimView
        );

        if (contentRoot != null) {

            contentRoot.setBackground(
                    cloneDrawable(
                            baseBackground
                    )
            );
        }

        return false;
    }

    public static boolean hasSelectedBackgroundImage(
            Context context
    ) {

        if (context == null) {
            return false;
        }

        ThemeStorage themeStorage =
                new ThemeStorage(
                        context
                );

        return hasExistingBackgroundImage(
                themeStorage.getSelectedTheme()
        );
    }

    public static void makeTransparent(
            View view
    ) {

        if (view == null) {
            return;
        }

        if (shouldKeepRippleBackground(view)) {
            return;
        }

        view.setBackgroundColor(
                Color.TRANSPARENT
        );
    }

    public static void makeViewPagerTransparent(
            ViewPager2 viewPager
    ) {

        if (viewPager == null) {
            return;
        }

        viewPager.setBackgroundColor(
                Color.TRANSPARENT
        );

        viewPager.post(() -> {

            viewPager.setBackgroundColor(
                    Color.TRANSPARENT
            );

            for (int i = 0;
                 i < viewPager.getChildCount();
                 i++) {

                View child =
                        viewPager.getChildAt(
                                i
                        );

                makeOnlyContainerTransparent(
                        child
                );
            }
        });
    }

    public static void makeOnlyContainerTransparent(
            View view
    ) {

        if (view == null) {
            return;
        }

        if (shouldKeepRippleBackground(view)) {
            return;
        }

        if (isSafeTransparentContainer(view)) {

            view.setBackgroundColor(
                    Color.TRANSPARENT
            );
        }

        if (view instanceof ViewGroup) {

            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0;
                 i < group.getChildCount();
                 i++) {

                View child =
                        group.getChildAt(
                                i
                        );

                String childIdName =
                        getIdName(
                                child
                        );

                if (isStructuralContainerId(
                        childIdName
                ) || child instanceof RecyclerView) {

                    makeOnlyContainerTransparent(
                            child
                    );
                }
            }
        }
    }

    public static Drawable resolveBaseBackground(
            Activity activity,
            ThemeModel selectedTheme
    ) {

        if (selectedTheme != null) {

            return new ColorDrawable(
                    ThemeUtils.parseColor(
                            selectedTheme.getBackgroundColor()
                    )
            );
        }

        return resolveWindowBackground(
                activity
        );
    }

    private static Drawable resolveWindowBackground(
            Activity activity
    ) {

        TypedValue typedValue =
                new TypedValue();

        boolean resolved =
                activity.getTheme()
                        .resolveAttribute(
                                android.R.attr.windowBackground,
                                typedValue,
                                true
                        );

        if (!resolved) {
            return new ColorDrawable(
                    Color.TRANSPARENT
            );
        }

        if (typedValue.resourceId != 0) {

            Drawable drawable =
                    ResourcesCompat.getDrawable(
                            activity.getResources(),
                            typedValue.resourceId,
                            activity.getTheme()
                    );

            if (drawable != null) {
                return drawable;
            }
        }

        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {

            return new ColorDrawable(
                    typedValue.data
            );
        }

        return new ColorDrawable(
                Color.TRANSPARENT
        );
    }

    private static Drawable cloneDrawable(
            Drawable drawable
    ) {

        if (drawable == null) {
            return new ColorDrawable(
                    Color.TRANSPARENT
            );
        }

        Drawable.ConstantState constantState =
                drawable.getConstantState();

        if (constantState != null) {

            return constantState.newDrawable()
                    .mutate();
        }

        return drawable.mutate();
    }

    private static boolean hasExistingBackgroundImage(
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
                new File(
                        path
                );

        return file.exists();
    }

    private static void hideBackgroundImage(
            ImageView backgroundImageView,
            View scrimView
    ) {

        if (backgroundImageView != null) {

            backgroundImageView.setImageDrawable(
                    null
            );

            backgroundImageView.setVisibility(
                    View.GONE
            );
        }

        if (scrimView != null) {

            scrimView.setVisibility(
                    View.GONE
            );
        }
    }

    private static boolean isSafeTransparentContainer(
            View view
    ) {

        if (view instanceof ViewPager2) {
            return true;
        }

        if (view instanceof RecyclerView) {
            return true;
        }

        String idName =
                getIdName(
                        view
                );

        return isStructuralContainerId(
                idName
        );
    }

    private static boolean isStructuralContainerId(
            String idName
    ) {

        if (idName == null) {
            return false;
        }

        return idName.equals(
                "main_container"
        )
                || idName.equals(
                "view_pager"
        )
                || idName.equals(
                "view_pager_fragment_container"
        )
                || idName.equals(
                "root_constraint_layout"
        )
                || idName.equals(
                "chat_root"
        )
                || idName.equals(
                "themes_root"
        )
                || idName.equals(
                "themes_content_root"
        )
                || idName.equals(
                "editor_root"
        )
                || idName.equals(
                "editor_content_root"
        )
                || idName.equals(
                "nested_scroll"
        )
                || idName.equals(
                "rv_chats"
        )
                || idName.equals(
                "rv_messages"
        );
    }

    private static boolean shouldKeepRippleBackground(
            View view
    ) {

        if (view == null) {
            return true;
        }

        String idName =
                getIdName(
                        view
                );

        if (idName == null) {
            return false;
        }

        return idName.equals(
                "btn_back"
        )
                || idName.equals(
                "btn_options"
        )
                || idName.equals(
                "btn_attach_file"
        )
                || idName.equals(
                "btn_search_action"
        )
                || idName.equals(
                "search_btn_back"
        )
                || idName.equals(
                "searchResult_btn_back"
        )
                || idName.equals(
                "btn_top"
        )
                || idName.equals(
                "btn_bottom"
        )
                || idName.equals(
                "btn_history"
        )
                || idName.equals(
                "card_create_theme"
        )
                || idName.equals(
                "row_primary"
        )
                || idName.equals(
                "row_surface"
        )
                || idName.equals(
                "row_text"
        )
                || idName.equals(
                "row_text_secondary"
        )
                || idName.equals(
                "row_my_message"
        )
                || idName.equals(
                "row_other_message"
        )
                || idName.equals(
                "row_chat_background"
        )
                || idName.equals(
                "row_message_animation"
        )
                || idName.equals(
                "bottom_panel"
        );
    }

    private static String getIdName(
            View view
    ) {

        int id =
                view.getId();

        if (id == View.NO_ID) {
            return null;
        }

        try {

            return view.getResources()
                    .getResourceEntryName(
                            id
                    );

        } catch (Exception ex) {

            return null;
        }
    }
}
