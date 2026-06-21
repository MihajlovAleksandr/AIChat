package com.example.aichat.view.theme.binders;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.widget.ImageViewCompat;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.model.utils.theme.ThemeModel;
import com.example.aichat.model.utils.theme.ThemeStorage;
import com.example.aichat.R;

public final class ThemeMessageBinder {

    private static final String TAG = "ThemeMessageBinder";
    private static final String TAG_MY_MESSAGE = "my_message";
    private static final String TAG_OTHER_MESSAGE = "other_message";

    private static final String DEFAULT_MY_MESSAGE = "#20A39A";
    private static final String DEFAULT_OTHER_LIGHT = "#F1F1F1";
    private static final String DEFAULT_OTHER_DARK = "#243D3E";
    private static final String DEFAULT_SECONDARY_TEXT = "#666666";
    private static final String DEFAULT_ON_SURFACE_LIGHT = "#1A1A1A";
    private static final String DEFAULT_ON_SURFACE_DARK = "#FFFFFF";

    private ThemeMessageBinder() {
    }

    public static void bind(View itemView) {
        if (itemView == null) {
            return;
        }

        Context context = itemView.getContext();

        if (context == null) {
            return;
        }

        ThemeModel theme = getActiveTheme(context);

        int myMessageColor = resolveColor(
                context,
                R.attr.myMessageColor,
                theme != null ? theme.getMyMessageColor() : null,
                DEFAULT_MY_MESSAGE
        );

        int otherMessageColor = resolveOtherMessageColor(context, theme);

        int onSurface = resolveColor(
                context,
                R.attr.colorOnSurface,
                theme != null ? theme.getColorOnSurface() : null,
                isDarkTheme(context) ? DEFAULT_ON_SURFACE_DARK : DEFAULT_ON_SURFACE_LIGHT
        );

        int onSurfaceVariant = resolveColor(
                context,
                R.attr.colorOnSurfaceVariant,
                theme != null ? theme.getColorOnSurfaceVariant() : null,
                DEFAULT_SECONDARY_TEXT
        );

        View messageContainer = itemView.findViewById(R.id.message_container);
        TextView messageText = itemView.findViewById(R.id.message_text);
        TextView timeText = itemView.findViewById(R.id.time_text);
        ImageView statusIcon = itemView.findViewById(R.id.status_icon);

        if (messageContainer == null) {
            return;
        }

        String messageTag = getTagValue(messageContainer);

        boolean isMyMessage = TAG_MY_MESSAGE.equals(messageTag);
        boolean isOtherMessage = TAG_OTHER_MESSAGE.equals(messageTag);

        if (!isMyMessage && !isOtherMessage) {
            isMyMessage = statusIcon != null;
        }

        boolean hasMediaOrFiles = hasVisibleFiles(itemView);
        boolean hasText = hasText(messageText);
        boolean mediaOnly = hasMediaOrFiles && !hasText;

        int bubbleColor = isMyMessage ? myMessageColor : otherMessageColor;
        int textColor = getContrastColor(bubbleColor);

        restoreMessageContainerBaseState(messageContainer, isMyMessage);

        if (mediaOnly) {
            makeMediaOnlyContainerTransparent(messageContainer);
        } else {
            applyBubbleColor(messageContainer, bubbleColor);
        }

        if (messageText != null) {
            messageText.setTextColor(textColor);
            messageText.setVisibility(hasText ? View.VISIBLE : View.GONE);
        }

        if (timeText != null) {
            timeText.setTextColor(withAlpha(onSurface, 184));
            timeText.setAlpha(1f);
        }

        if (statusIcon != null) {
            ImageViewCompat.setImageTintList(
                    statusIcon,
                    ColorStateList.valueOf(onSurfaceVariant)
            );
        }

        Log.d(TAG, "bind message"
                + ", tag=" + messageTag
                + ", media=" + hasMediaOrFiles
                + ", text=" + hasText
                + ", mediaOnly=" + mediaOnly
                + ", bubble=" + colorToHex(bubbleColor));

        Log.d(TAG, "after bind"
                + ", tag=" + messageTag
                + ", bgClass=" + getBackgroundClassName(messageContainer)
                + ", padding="
                + messageContainer.getPaddingLeft() + ","
                + messageContainer.getPaddingTop() + ","
                + messageContainer.getPaddingRight() + ","
                + messageContainer.getPaddingBottom());
    }

    private static boolean hasVisibleFiles(View itemView) {
        HorizontalScrollView filesScroll = itemView.findViewById(R.id.files_scroll);

        return filesScroll != null
                && filesScroll.getVisibility() == View.VISIBLE;
    }

    private static boolean hasText(TextView textView) {
        return textView != null
                && textView.getText() != null
                && !textView.getText().toString().trim().isEmpty();
    }

    private static ThemeModel getActiveTheme(Context context) {
        ThemeModel currentTheme = ThemeAttrResolver.getCurrentTheme();

        if (currentTheme != null) {
            return currentTheme;
        }

        ThemeStorage storage = new ThemeStorage(context);
        ThemeModel selectedTheme = storage.getSelectedTheme();

        if (selectedTheme != null) {
            ThemeAttrResolver.applyTheme(selectedTheme);
        }

        return selectedTheme;
    }

    private static int resolveOtherMessageColor(Context context, ThemeModel theme) {
        if (theme != null) {
            return resolveColor(
                    context,
                    R.attr.otherMessageColor,
                    theme.getOtherMessageColor(),
                    isDarkTheme(context) ? DEFAULT_OTHER_DARK : DEFAULT_OTHER_LIGHT
            );
        }

        int attrColor = resolveColor(
                context,
                R.attr.otherMessageColor,
                null,
                isDarkTheme(context) ? DEFAULT_OTHER_DARK : DEFAULT_OTHER_LIGHT
        );

        if (isDarkTheme(context) && isLightColor(attrColor)) {
            return Color.parseColor(DEFAULT_OTHER_DARK);
        }

        return attrColor;
    }

    private static int resolveColor(Context context, int attr, String modelColor, String fallback) {
        if (modelColor != null && !modelColor.trim().isEmpty()) {
            try {
                return Color.parseColor(modelColor);
            } catch (Exception ignored) {
            }
        }

        try {
            return ThemeAttrResolver.resolveColor(context, attr);
        } catch (Exception ignored) {
        }

        try {
            return Color.parseColor(fallback);
        } catch (Exception ignored) {
            return Color.WHITE;
        }
    }

    private static void restoreMessageContainerBaseState(View view, boolean isMyMessage) {
        if (view == null) {
            return;
        }

        view.setBackgroundResource(isMyMessage ? R.drawable.bg_my_message : R.drawable.bg_other_message);

        int horizontal = dp(view.getContext(), 10);
        int vertical = dp(view.getContext(), 8);

        view.setPadding(horizontal, vertical, horizontal, vertical);
    }

    private static void makeMediaOnlyContainerTransparent(View view) {
        if (view == null) {
            return;
        }

        view.setBackground(new ColorDrawable(Color.TRANSPARENT));
        view.setPadding(0, 0, 0, 0);
        view.invalidate();
    }

    private static void applyBubbleColor(View view, int color) {
        if (view == null) {
            return;
        }

        Drawable background = view.getBackground();

        if (background == null) {
            view.setBackgroundColor(color);
            view.invalidate();
            return;
        }

        Drawable mutated = background.mutate();
        mutated.setTint(color);
        view.setBackground(mutated);
        view.invalidate();
    }

    private static int dp(Context context, int value) {
        if (context == null) {
            return value;
        }

        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static String getTagValue(View view) {
        if (view == null) {
            return null;
        }

        Object tag = view.getTag();

        return tag == null ? null : tag.toString();
    }

    private static boolean isDarkTheme(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;

        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static boolean isLightColor(int color) {
        double darkness = 1.0 - (
                0.299 * Color.red(color)
                        + 0.587 * Color.green(color)
                        + 0.114 * Color.blue(color)
        ) / 255.0;

        return darkness < 0.35;
    }

    private static int getContrastColor(int backgroundColor) {
        double luminance = 0.299 * Color.red(backgroundColor)
                + 0.587 * Color.green(backgroundColor)
                + 0.114 * Color.blue(backgroundColor);

        return luminance > 186 ? Color.parseColor("#1A1A1A") : Color.WHITE;
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(
                alpha,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    private static String colorToHex(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    private static String getBackgroundClassName(View view) {
        if (view == null || view.getBackground() == null) {
            return "null";
        }

        return view.getBackground().getClass().getSimpleName();
    }
}
