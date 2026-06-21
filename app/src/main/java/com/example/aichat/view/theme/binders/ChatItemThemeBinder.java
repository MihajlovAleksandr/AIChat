package com.example.aichat.view.theme.binders;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.R;
import com.google.android.material.card.MaterialCardView;

public final class ChatItemThemeBinder {

    private ChatItemThemeBinder() {
    }

    public static void apply(@Nullable View itemView) {
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

        int onPrimary =
                resolveColor(
                        context,
                        com.google.android.material.R.attr.colorOnPrimary,
                        Color.WHITE
                );

        int surface =
                resolveColor(
                        context,
                        com.google.android.material.R.attr.colorSurface,
                        Color.WHITE
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
                        R.id.tv_chat_name
                ),
                onSurface
        );

        setTextColor(
                itemView.findViewById(
                        R.id.tv_last_message
                ),
                onSurfaceVariant
        );

        setTextColor(
                itemView.findViewById(
                        R.id.tv_time
                ),
                onSurfaceVariant
        );

        MaterialCardView previewCard =
                itemView.findViewById(
                        R.id.chat_circle_preview_card
                );

        if (previewCard != null) {
            previewCard.setCardBackgroundColor(
                    primary
            );

            previewCard.setStrokeColor(
                    resolveColor(
                            context,
                            R.attr.chatCircleStroke,
                            adjustAlpha(
                                    onSurface,
                                    0.55f
                            )
                    )
            );

            previewCard.setStrokeWidth(
                    dp(
                            context,
                            2
                    )
            );

            previewCard.setCardElevation(
                    0f
            );
        }

        ImageView chatStatus =
                itemView.findViewById(
                        R.id.iv_chat_status
                );

        if (chatStatus != null) {
            chatStatus.setColorFilter(
                    primary,
                    PorterDuff.Mode.SRC_IN
            );
        }

        TextView circleInitials =
                itemView.findViewById(
                        R.id.tv_chat_circle_initials
                );

        if (circleInitials != null) {
            circleInitials.setTextColor(
                    onPrimary
            );
        }

        TextView typeLetter =
                itemView.findViewById(
                        R.id.tv_chat_type_letter
                );

        if (typeLetter != null) {
            typeLetter.setTextColor(
                    Color.WHITE
            );
        }

        ImageView play =
                itemView.findViewById(
                        R.id.chat_circle_preview_play
                );

        if (play != null) {
            play.setImageTintList(
                    ColorStateList.valueOf(
                            onPrimary
                    )
            );

            play.setBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );
        }

        ImageView messageStatus =
                itemView.findViewById(
                        R.id.iv_message_status
                );

        if (messageStatus != null) {
            messageStatus.setImageTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );
        }

        TextView unread =
                itemView.findViewById(
                        R.id.tv_unread_count
                );

        if (unread != null) {
            unread.setTextColor(
                    onPrimary
            );

            unread.setBackground(
                    createCircleDrawable(
                            primary
                    )
            );
        }

        View divider =
                findDividerView(
                        itemView
                );

        if (divider != null) {
            divider.setBackgroundColor(
                    adjustAlpha(
                            onSurfaceVariant,
                            0.30f
                    )
            );
        }
    }

    @Nullable
    private static View findDividerView(@NonNull View itemView) {
        if (!(itemView instanceof ViewGroup)) {
            return null;
        }

        ViewGroup root =
                (ViewGroup) itemView;

        for (int i = 0; i < root.getChildCount(); i++) {
            View child =
                    root.getChildAt(
                            i
                    );

            ViewGroup.LayoutParams params =
                    child.getLayoutParams();

            if (params != null && params.height == 1) {
                return child;
            }
        }

        return null;
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


    private static int dp(@NonNull Context context, int value) {
        return Math.round(
                value * context
                        .getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private static int adjustAlpha(
            int color,
            float factor
    ) {
        int alpha =
                Math.round(
                        Color.alpha(
                                color
                        ) * factor
                );

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
