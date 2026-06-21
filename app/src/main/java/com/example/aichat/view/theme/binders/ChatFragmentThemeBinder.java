package com.example.aichat.view.theme.binders;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.R;
import com.google.android.material.button.MaterialButton;

public final class ChatFragmentThemeBinder {

    private ChatFragmentThemeBinder() {
    }

    public static void apply(
            @NonNull Fragment fragment,
            @Nullable View root
    ) {
        if (root == null || fragment.getActivity() == null) {
            return;
        }

        apply(
                fragment.requireActivity(),
                root
        );
    }

    public static void apply(
            @NonNull Activity activity,
            @Nullable View root
    ) {
        if (root == null) {
            return;
        }

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

        ThemeBackgroundBinder.applyToActivity(
                activity,
                backgroundImage,
                backgroundScrim,
                contentRoot
        );

        ThemeBackgroundBinder.makeTransparent(
                root
        );

        ThemeBackgroundBinder.makeTransparent(
                contentRoot
        );

        ThemeBackgroundBinder.makeTransparent(
                root.findViewById(
                        R.id.rv_messages
                )
        );

        int surface =
                resolveColor(
                        activity,
                        com.google.android.material.R.attr.colorSurface,
                        Color.WHITE
                );

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

        tintImageButton(
                root.findViewById(
                        R.id.btn_back
                ),
                primary
        );

        tintImageButton(
                root.findViewById(
                        R.id.btn_options
                ),
                primary
        );

        tintImageButton(
                root.findViewById(
                        R.id.btn_attach_file
                ),
                primary
        );

        tintImageButton(
                root.findViewById(
                        R.id.btn_record_voice
                ),
                primary
        );

        tintImageButton(
                root.findViewById(
                        R.id.btn_record_video_circle
                ),
                primary
        );

        tintImageButton(
                root.findViewById(
                        R.id.edit_cancel
                ),
                primary
        );

        tintImageButton(
                root.findViewById(
                        R.id.reply_cancel
                ),
                primary
        );

        tintImageButton(
                root.findViewById(
                        R.id.search_btn_back
                ),
                onPrimary
        );

        tintImageButton(
                root.findViewById(
                        R.id.searchResult_btn_back
                ),
                onPrimary
        );

        tintImageButton(
                root.findViewById(
                        R.id.btn_search_action
                ),
                onPrimary
        );

        tintImageButton(
                root.findViewById(
                        R.id.btn_top
                ),
                onPrimary
        );

        tintImageButton(
                root.findViewById(
                        R.id.btn_bottom
                ),
                onPrimary
        );

        setTextColor(
                root.findViewById(
                        R.id.tv_chat_title
                ),
                onSurface
        );

        setTextColor(
                root.findViewById(
                        R.id.tv_search_status
                ),
                onSurface
        );

        setTextColor(
                root.findViewById(
                        R.id.tv_typing_status
                ),
                onSurfaceVariant
        );

        setTextColor(
                root.findViewById(
                        R.id.typing_dot_1
                ),
                primary
        );

        setTextColor(
                root.findViewById(
                        R.id.typing_dot_2
                ),
                primary
        );

        setTextColor(
                root.findViewById(
                        R.id.typing_dot_3
                ),
                primary
        );

        setTextColor(
                root.findViewById(
                        R.id.edit_original_text
                ),
                onSurface
        );

        setTextColor(
                root.findViewById(
                        R.id.reply_original_text
                ),
                onSurface
        );

        setTextColor(
                root.findViewById(
                        R.id.tv_chat_ended
                ),
                onSurface
        );

        View inputPanel =
                root.findViewById(
                        R.id.input_panel
                );

        if (inputPanel != null) {
            inputPanel.setBackground(
                    createRoundedSurface(
                            surface,
                            dp(
                                    activity,
                                    28
                            ),
                            0
                    )
            );
        }

        View editPanel =
                root.findViewById(
                        R.id.edit_panel
                );

        if (editPanel != null) {
            editPanel.setBackground(
                    createPanelSurface(
                            surface
                    )
            );
        }

        View replyPanel =
                root.findViewById(
                        R.id.reply_panel
                );

        if (replyPanel != null) {
            replyPanel.setBackground(
                    createPanelSurface(
                            surface
                    )
            );
        }

        View searchLayout =
                root.findViewById(
                        R.id.search_layout
                );

        if (searchLayout != null) {
            searchLayout.setBackgroundColor(
                    primary
            );
        }

        View searchResultLayout =
                root.findViewById(
                        R.id.searchResult_layout
                );

        if (searchResultLayout != null) {
            searchResultLayout.setBackgroundColor(
                    primary
            );
        }

        MaterialButton unreadJump =
                root.findViewById(
                        R.id.btn_unread_jump
                );

        if (unreadJump != null) {
            unreadJump.setBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            unreadJump.setTextColor(
                    onPrimary
            );

            unreadJump.setIconTint(
                    ColorStateList.valueOf(
                            onPrimary
                    )
            );
        }

        View exportButton =
                root.findViewById(
                        R.id.b_export_chat
                );

        if (exportButton instanceof TextView) {
            exportButton.setBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            ((TextView) exportButton).setTextColor(
                    onPrimary
            );
        }

        View sendButton =
                root.findViewById(
                        R.id.b_send_message
                );

        if (sendButton instanceof TextView) {
            sendButton.setBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            ((TextView) sendButton).setTextColor(
                    onPrimary
            );
        }

        TextView input =
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
        }

        TextView fileCounter =
                root.findViewById(
                        R.id.file_counter
                );

        if (fileCounter != null) {
            fileCounter.setTextColor(
                    onPrimary
            );

            fileCounter.setBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );
        }

        RecyclerView recyclerView =
                root.findViewById(
                        R.id.rv_messages
                );

        if (recyclerView != null) {
            recyclerView.setBackgroundColor(
                    Color.TRANSPARENT
            );
        }

        applyMembersPanelTheme(
                root.findViewById(
                        R.id.membersPanel
                ),
                primary,
                onPrimary,
                surface,
                onSurface,
                onSurfaceVariant
        );

        if (contentRoot != null
                && backgroundImage != null
                && backgroundImage.getVisibility() != View.VISIBLE) {

            Drawable backgroundDrawable =
                    ThemeBackgroundBinder.resolveBaseBackground(
                            activity,
                            ThemeAttrResolver.getCurrentTheme()
                    );

            contentRoot.setBackground(
                    backgroundDrawable
            );

        } else if (contentRoot != null) {

            contentRoot.setBackgroundColor(
                    Color.TRANSPARENT
            );
        }
    }

    private static void applyMembersPanelTheme(
            @Nullable View membersPanel,
            int primary,
            int onPrimary,
            int surface,
            int onSurface,
            int onSurfaceVariant
    ) {
        if (membersPanel == null) {
            return;
        }

        membersPanel.setBackgroundColor(
                surface
        );

        if (membersPanel instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) membersPanel;

            View header =
                    findLikelyMembersHeader(
                            group
                    );

            if (header != null) {
                header.setBackgroundColor(
                        primary
                );

                tintMembersHeaderRecursively(
                        header,
                        primary,
                        onPrimary
                );
            }

            tintMembersBodyRecursively(
                    group,
                    header,
                    primary,
                    onSurface,
                    onSurfaceVariant
            );

            return;
        }

        tintSingleMembersView(
                membersPanel,
                primary,
                onSurface
        );
    }

    @Nullable
    private static View findLikelyMembersHeader(@NonNull ViewGroup group) {
        if (group.getChildCount() == 0) {
            return null;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            View child =
                    group.getChildAt(
                            i
                    );

            if (child.getVisibility() == View.GONE) {
                continue;
            }

            return child;
        }

        return null;
    }

    private static void tintMembersHeaderRecursively(
            @Nullable View view,
            int primary,
            int onPrimary
    ) {
        if (view == null) {
            return;
        }

        view.setBackgroundColor(
                primary
        );

        if (view instanceof TextView) {
            ((TextView) view).setTextColor(
                    onPrimary
            );
        }

        if (view instanceof ImageButton) {
            ImageButton button =
                    (ImageButton) view;

            button.setImageTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            button.setBackground(
                    createCircleDrawable(
                            withAlpha(
                                    onPrimary,
                                    245
                            )
                    )
            );
        } else if (view instanceof ImageView) {
            ((ImageView) view).setImageTintList(
                    ColorStateList.valueOf(
                            onPrimary
                    )
            );
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                tintMembersHeaderRecursively(
                        group.getChildAt(
                                i
                        ),
                        primary,
                        onPrimary
                );
            }
        }
    }

    private static void tintMembersBodyRecursively(
            @Nullable View view,
            @Nullable View header,
            int primary,
            int onSurface,
            int onSurfaceVariant
    ) {
        if (view == null || view == header) {
            return;
        }

        if (view instanceof TextView) {
            TextView textView =
                    (TextView) view;

            int currentColor =
                    textView.getCurrentTextColor();

            boolean looksSecondary =
                    Color.alpha(
                            currentColor
                    ) < 230;

            textView.setTextColor(
                    looksSecondary
                            ? onSurfaceVariant
                            : onSurface
            );
        }

        if (view instanceof ImageButton) {
            ((ImageButton) view).setImageTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );
        } else if (view instanceof ImageView) {
            ((ImageView) view).setImageTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );
        }

        if (view instanceof ViewGroup) {
            ViewGroup group =
                    (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                tintMembersBodyRecursively(
                        group.getChildAt(
                                i
                        ),
                        header,
                        primary,
                        onSurface,
                        onSurfaceVariant
                );
            }
        }
    }

    private static void tintSingleMembersView(
            @NonNull View view,
            int primary,
            int onSurface
    ) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(
                    onSurface
            );
        }

        if (view instanceof ImageButton) {
            ((ImageButton) view).setImageTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );
        } else if (view instanceof ImageView) {
            ((ImageView) view).setImageTintList(
                    ColorStateList.valueOf(
                            primary
                    )
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

    private static void setTextColor(
            @Nullable TextView view,
            int color
    ) {
        if (view != null) {
            view.setTextColor(
                    color
            );
        }
    }

    private static void tintImageButton(
            @Nullable ImageButton view,
            int color
    ) {
        if (view != null) {
            view.setImageTintList(
                    ColorStateList.valueOf(
                            color
                    )
            );
        }
    }

    private static GradientDrawable createPanelSurface(int color) {
        return createRoundedSurface(
                color,
                0,
                0
        );
    }

    private static GradientDrawable createRoundedSurface(
            int color,
            int cornerRadiusPx,
            int strokeColor
    ) {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                color
        );

        drawable.setCornerRadius(
                cornerRadiusPx
        );

        if (strokeColor != 0) {
            drawable.setStroke(
                    1,
                    strokeColor
            );
        }

        return drawable;
    }

    private static GradientDrawable createCircleDrawable(int color) {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setShape(
                GradientDrawable.OVAL
        );

        drawable.setColor(
                color
        );

        return drawable;
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

    private static int dp(
            @NonNull Context context,
            int value
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
