package com.example.aichat.view.cache;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import com.example.aichat.model.utils.files.CacheCategory;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.R;

public final class CacheUi {

    private CacheUi() {
    }

    public static final long MB = 1024L * 1024L;
    public static final long GB = 1024L * 1024L * 1024L;
    public static final long MIN_CACHE_LIMIT = 100L * MB;
    public static final long MAX_CACHE_LIMIT = 5L * GB;
    public static final long CACHE_LIMIT_STEP = 100L * MB;

    public static final int[] CHART_COLORS = {
            0xFF5DAEFF,
            0xFF31C553,
            0xFFFFB321,
            0xFF26A69A,
            0xFFFF5365,
            0xFF35C4D5
    };

    @ColorInt
    public static int colorPrimary(Context context) {
        return resolveColor(
                context,
                com.google.android.material.R.attr.colorPrimary,
                0xFF20A39A
        );
    }

    @ColorInt
    public static int colorBackground(Context context) {
        /*
         * Cache screens are service/settings screens, not chat screens.
         * They must not inherit dynamic chat backgrounds or selected theme images,
         * otherwise custom chat themes can leak as side stripes around the content.
         */
        if (isDark(context)) {
            return 0xFF0B141A;
        }

        return 0xFFF7FAFC;
    }

    @ColorInt
    public static int colorSurface(Context context) {
        if (isDark(context)) {
            return 0xFF17242D;
        }

        return 0xFFFFFFFF;
    }

    @ColorInt
    public static int colorOnSurface(Context context) {
        return resolveColor(
                context,
                com.google.android.material.R.attr.colorOnSurface,
                isDark(context)
                        ? 0xFFFFFFFF
                        : 0xFF121212
        );
    }

    @ColorInt
    public static int colorOnSurfaceVariant(Context context) {
        return resolveColor(
                context,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                isDark(context)
                        ? 0x99FFFFFF
                        : 0x99000000
        );
    }

    @ColorInt
    public static int colorOnPrimary(Context context) {
        return resolveColor(
                context,
                com.google.android.material.R.attr.colorOnPrimary,
                0xFFFFFFFF
        );
    }

    @ColorInt
    public static int cardColor(Context context) {
        if (isDark(context)) {
            return 0xFF17242D;
        }

        return ColorUtils.blendARGB(colorSurface(context), Color.BLACK, 0.018f);
    }

    public static boolean isDark(Context context) {
        int flags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return flags == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void applyWindowBackground(@NonNull AppCompatActivity activity) {
        int background = colorBackground(activity);
        Window window = activity.getWindow();

        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(background));
            window.setStatusBarColor(background);
            window.setNavigationBarColor(background);
        }

        View decorView = window != null ? window.getDecorView() : null;
        if (decorView != null) {
            decorView.setBackgroundColor(background);
        }
    }

    @ColorInt
    public static int resolveColor(Context context, @AttrRes int attr, @ColorInt int fallback) {
        try {
            return ThemeAttrResolver.resolveColor(
                    context,
                    attr
            );
        } catch (Exception ignored) {
            TypedValue typedValue =
                    new TypedValue();

            boolean resolved =
                    context
                            .getTheme()
                            .resolveAttribute(
                                    attr,
                                    typedValue,
                                    true
                            );

            return resolved
                    ? typedValue.data
                    : fallback;
        }
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    @NonNull
    public static GradientDrawable rounded(@ColorInt int color, float radiusDp, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, Math.round(radiusDp)));
        return drawable;
    }

    @NonNull
    public static GradientDrawable roundedStroke(
            @ColorInt int color,
            @ColorInt int strokeColor,
            int strokeDp,
            float radiusDp,
            Context context
    ) {
        GradientDrawable drawable = rounded(color, radiusDp, context);
        drawable.setStroke(dp(context, strokeDp), strokeColor);
        return drawable;
    }

    @NonNull
    public static TextView text(Context context, CharSequence value, float sp, int color, int style) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setIncludeFontPadding(true);
        return view;
    }

    @NonNull
    public static LinearLayout vertical(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    @NonNull
    public static LinearLayout horizontal(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    @NonNull
    public static LinearLayout card(Context context) {
        LinearLayout layout = vertical(context);
        layout.setBackground(rounded(cardColor(context), 22, context));
        layout.setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14));
        return layout;
    }

    @NonNull
    public static ImageButton backButton(@NonNull AppCompatActivity activity) {
        ImageButton button = new ImageButton(activity);
        button.setImageResource(R.drawable.ic_arrow_back);
        button.setColorFilter(colorPrimary(activity));
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
        button.setOnClickListener(v -> activity.finish());
        button.setContentDescription(activity.getString(R.string.back_button_description));
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));
        return button;
    }

    @NonNull
    public static TextView actionText(Context context, CharSequence text) {
        TextView view = text(context, text, 16, colorPrimary(context), Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10));
        return view;
    }

    public static void addDivider(Context context, LinearLayout parent) {
        View divider = new View(context);
        divider.setBackgroundColor(isDark(context) ? 0x1FFFFFFF : 0x14000000);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.max(1, dp(context, 1))
        );
        params.setMargins(dp(context, 48), dp(context, 8), 0, dp(context, 8));
        parent.addView(divider, params);
    }

    public static void tintIcon(ImageView imageView, int color) {
        imageView.setColorFilter(color);
    }

    @NonNull
    public static ImageView icon(Context context, @DrawableRes int iconRes, int backgroundColor) {
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.WHITE);
        icon.setBackground(rounded(backgroundColor, 13, context));
        icon.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 10));
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)));
        return icon;
    }

    @NonNull
    public static String categoryTitle(@NonNull Context context, @NonNull CacheCategory category) {
        switch (category) {
            case VIDEO:
                return context.getString(R.string.cache_category_video);
            case FILES:
                return context.getString(R.string.cache_category_files);
            case IMAGES:
                return context.getString(R.string.cache_category_images);
            case VOICE:
                return context.getString(R.string.cache_category_voice);
            case MUSIC:
                return context.getString(R.string.cache_category_music);
            case OTHER:
            default:
                return context.getString(R.string.cache_category_other);
        }
    }

    @NonNull
    public static String formatCacheLimit(long bytes) {
        long safe = Math.max(MIN_CACHE_LIMIT, Math.min(MAX_CACHE_LIMIT, bytes));

        if (safe >= GB) {
            float gb = safe / (float) GB;
            if (Math.abs(gb - Math.round(gb)) < 0.01f) {
                return String.format(java.util.Locale.US, "%d GB", Math.round(gb));
            }
            return String.format(java.util.Locale.US, "%.1f GB", gb);
        }

        return String.format(java.util.Locale.US, "%d MB", Math.round(safe / (float) MB));
    }

    public static int limitToSeekProgress(long bytes) {
        long safe = Math.max(MIN_CACHE_LIMIT, Math.min(MAX_CACHE_LIMIT, bytes));
        return (int) ((safe - MIN_CACHE_LIMIT) / CACHE_LIMIT_STEP);
    }

    public static long seekProgressToLimit(int progress) {
        long result = MIN_CACHE_LIMIT + Math.max(0, progress) * CACHE_LIMIT_STEP;
        return Math.max(MIN_CACHE_LIMIT, Math.min(MAX_CACHE_LIMIT, result));
    }

    public static int maxLimitSeekProgress() {
        return (int) ((MAX_CACHE_LIMIT - MIN_CACHE_LIMIT) / CACHE_LIMIT_STEP);
    }
}
