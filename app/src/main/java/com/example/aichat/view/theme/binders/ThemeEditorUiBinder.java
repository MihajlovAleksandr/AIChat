package com.example.aichat.view.theme.binders;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.example.aichat.databinding.ActivityThemeEditorBinding;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.R;
import com.example.aichat.view.theme.ThemeEditorDrawableUtils;
import com.example.aichat.view.theme.ThemeEditorState;
import com.example.aichat.view.theme.ThemesActivity;
import java.io.File;
import java.util.Locale;

public class ThemeEditorUiBinder {

    private ThemeEditorUiBinder() {
    }

    public static void applyScreenTheme(
            Context context,
            ActivityThemeEditorBinding binding
    ) {
        binding.themeBackgroundImage.setVisibility(View.GONE);
        binding.themeBackgroundScrim.setVisibility(View.GONE);
        Glide.with(context).clear(binding.themeBackgroundImage);
        binding.themeBackgroundImage.setImageDrawable(null);
        binding.themeBackgroundImage.setImageBitmap(null);
        binding.themeBackgroundImage.setBackground(null);

        int surface = ThemeAttrResolver.resolveColor(
                context,
                R.attr.colorSurface
        );

        int onSurface = ThemeAttrResolver.resolveColor(
                context,
                R.attr.colorOnSurface
        );

        int onSurfaceVariant = ThemeAttrResolver.resolveColor(
                context,
                R.attr.colorOnSurfaceVariant
        );

        int primary = ThemeAttrResolver.resolveColor(
                context,
                R.attr.colorPrimary
        );

        binding.getRoot().setBackground(
                resolveActivityBackgroundDrawable(context)
        );

        binding.editorContentRoot.setBackgroundColor(Color.TRANSPARENT);
        binding.nestedScroll.setBackgroundColor(Color.TRANSPARENT);
        binding.topBar.setBackgroundColor(Color.TRANSPARENT);

        binding.tvTitle.setTextColor(onSurface);

        binding.btnBack.setImageTintList(
                ColorStateList.valueOf(onSurface)
        );

        binding.btnSave.setTextColor(primary);

        binding.tilThemeName.setBoxBackgroundColor(
                makeSurfaceForInput(surface)
        );

        binding.tilThemeName.setBoxStrokeColor(primary);

        binding.tilThemeName.setHintTextColor(
                ColorStateList.valueOf(onSurfaceVariant)
        );

        binding.tilThemeName.setEndIconTintList(
                ColorStateList.valueOf(onSurfaceVariant)
        );

        binding.etThemeName.setTextColor(onSurface);
        binding.etThemeName.setHintTextColor(onSurfaceVariant);
    }

    public static void updateColorViews(
            ActivityThemeEditorBinding binding,
            ThemeEditorState state
    ) {
        ThemeEditorDrawableUtils.setCircleColor(
                binding.dotPrimary,
                state.colorPrimary
        );

        ThemeEditorDrawableUtils.setCircleColor(
                binding.dotText,
                state.colorOnSurface
        );

        ThemeEditorDrawableUtils.setCircleColor(
                binding.dotTextSecondary,
                state.colorOnSurfaceVariant
        );

        ThemeEditorDrawableUtils.setCircleColor(
                binding.dotMyMessage,
                state.myMessageColor
        );

        ThemeEditorDrawableUtils.setCircleColor(
                binding.dotOtherMessage,
                state.otherMessageColor
        );

        binding.tvPrimaryHex.setText(
                state.colorPrimary
        );

        binding.tvTextHex.setText(
                state.colorOnSurface
        );

        binding.tvTextSecondaryHex.setText(
                state.colorOnSurfaceVariant
        );

        binding.tvMyMessageHex.setText(
                state.myMessageColor
        );

        binding.tvOtherMessageHex.setText(
                state.otherMessageColor
        );

        if (state.backgroundImageName != null
                && !state.backgroundImageName.trim().isEmpty()) {

            binding.tvChatBackgroundValue.setText(
                    state.backgroundImageName
            );

        } else {

            binding.tvChatBackgroundValue.setText(
                    state.backgroundColor
            );
        }

        String animationLabel =
                state.messageAnimationDisplayName != null
                        && !state.messageAnimationDisplayName.trim().isEmpty()
                        ? state.messageAnimationDisplayName
                        : state.messageAnimation;

        binding.tvMessageAnimationValue.setText(
                animationLabel
        );
    }

    public static void updatePreview(
            Context context,
            ActivityThemeEditorBinding binding,
            ThemeEditorState state
    ) {
        boolean hasPreviewImage =
                state.backgroundImagePath != null
                        && !state.backgroundImagePath.trim().isEmpty()
                        && new File(state.backgroundImagePath).exists();

        if (hasPreviewImage) {
            applyPreviewImage(
                    context,
                    binding,
                    state.backgroundImagePath
            );
        } else {
            clearPreviewImage(
                    context,
                    binding
            );

            applyPreviewBackgroundColor(
                    binding,
                    state.backgroundColor
            );
        }

        ThemeEditorDrawableUtils.setCircleColor(
                binding.previewOtherAvatar,
                state.colorPrimary
        );

        binding.previewOtherMessage.setBackground(
                ThemeEditorDrawableUtils.createBubbleDrawable(
                        context,
                        state.otherMessageColor,
                        true
                )
        );

        binding.previewOtherMessage.setTextColor(
                ThemeEditorDrawableUtils.getContrastColor(
                        state.otherMessageColor
                )
        );

        binding.previewMyMessage.setBackground(
                ThemeEditorDrawableUtils.createBubbleDrawable(
                        context,
                        state.myMessageColor,
                        false
                )
        );

        binding.previewMyMessage.setTextColor(
                ThemeEditorDrawableUtils.getContrastColor(
                        state.myMessageColor
                )
        );

        binding.previewTimeOther.setTextColor(
                ThemeEditorDrawableUtils.parseColor(
                        state.colorOnSurfaceVariant
                )
        );

        binding.previewTimeMy.setTextColor(
                ThemeEditorDrawableUtils.parseColor(
                        state.colorOnSurfaceVariant
                )
        );
    }

    public static void playPreviewAnimation(
            Context context,
            ActivityThemeEditorBinding binding,
            ThemeEditorState state
    ) {
        if (binding == null || state == null) {
            return;
        }

        String animation = ThemeMessageAnimationBinder.normalize(
                state.messageAnimation
        );

        if (ThemeMessageAnimationBinder.ANIMATION_DEFAULT.equals(animation)) {
            return;
        }

        binding.previewOtherMessage.postDelayed(
                () -> ThemeMessageAnimationBinder.apply(
                        binding.previewOtherMessage,
                        animation
                ),
                40L
        );

        binding.previewMyMessage.postDelayed(
                () -> ThemeMessageAnimationBinder.apply(
                        binding.previewMyMessage,
                        animation
                ),
                130L
        );
    }

    private static void applyPreviewImage(
            Context context,
            ActivityThemeEditorBinding binding,
            String imagePath
    ) {
        File imageFile = new File(imagePath);

        Glide.with(context).clear(
                binding.previewBackgroundImage
        );

        binding.previewBackgroundImage.setImageDrawable(null);
        binding.previewBackgroundImage.setImageBitmap(null);
        binding.previewBackgroundImage.setBackground(null);

        binding.previewCard.setCardBackgroundColor(Color.TRANSPARENT);
        binding.previewContentRoot.setBackgroundColor(Color.TRANSPARENT);

        binding.previewBackgroundImage.setVisibility(View.VISIBLE);
        binding.previewImageScrim.setVisibility(View.VISIBLE);

        Glide.with(context)
                .load(imageFile)
                .centerCrop()
                .into(binding.previewBackgroundImage);

        binding.previewContentRoot.invalidate();
        binding.previewCard.invalidate();
    }

    private static void clearPreviewImage(
            Context context,
            ActivityThemeEditorBinding binding
    ) {
        Glide.with(context).clear(
                binding.previewBackgroundImage
        );

        binding.previewBackgroundImage.setImageDrawable(null);
        binding.previewBackgroundImage.setImageBitmap(null);
        binding.previewBackgroundImage.setBackground(null);
        binding.previewBackgroundImage.setVisibility(View.GONE);

        binding.previewImageScrim.setVisibility(View.GONE);
    }

    private static void applyPreviewBackgroundColor(
            ActivityThemeEditorBinding binding,
            String backgroundColor
    ) {
        int color = parsePreviewBackgroundColor(
                backgroundColor
        );

        binding.previewCard.setCardBackgroundColor(Color.TRANSPARENT);
        binding.previewContentRoot.setBackgroundColor(color);

        binding.previewContentRoot.invalidate();
        binding.previewContentRoot.requestLayout();

        binding.previewCard.invalidate();
        binding.previewCard.requestLayout();
    }

    private static int parsePreviewBackgroundColor(
            String backgroundColor
    ) {
        if (backgroundColor == null
                || backgroundColor.trim().isEmpty()) {

            return Color.WHITE;
        }

        try {
            return Color.parseColor(
                    backgroundColor
            );
        } catch (Exception ex) {
            return Color.WHITE;
        }
    }

    private static Drawable resolveActivityBackgroundDrawable(
            Context context
    ) {
        Drawable windowBackground =
                resolveWindowBackgroundDrawable(context);

        if (windowBackground != null
                && !isFlatWrongDarkBackground(
                context,
                windowBackground
        )) {

            return windowBackground;
        }

        int color = ThemeAttrResolver.resolveColor(
                context,
                android.R.attr.colorBackground
        );

        if (isDarkUiMode(context)
                && isVeryDarkColor(color)) {

            int primary = ThemeAttrResolver.resolveColor(
                    context,
                    R.attr.colorPrimary
            );

            color = blendColors(
                    color,
                    primary,
                    0.18f
            );
        }

        return new ColorDrawable(color);
    }

    private static Drawable resolveWindowBackgroundDrawable(
            Context context
    ) {
        TypedValue typedValue = new TypedValue();

        boolean resolved = context.getTheme().resolveAttribute(
                android.R.attr.windowBackground,
                typedValue,
                true
        );

        if (!resolved) {
            return null;
        }

        Drawable drawable = null;

        if (typedValue.resourceId != 0) {
            drawable = ContextCompat.getDrawable(
                    context,
                    typedValue.resourceId
            );

        } else if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {

            drawable = new ColorDrawable(
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
                ((ColorDrawable) drawable).getColor();

        return toHex(color).equalsIgnoreCase("#141218")
                || toHex(color).equalsIgnoreCase("#121212")
                || toHex(color).equalsIgnoreCase("#0F0F0F");
    }

    private static int makeSurfaceForInput(
            int surface
    ) {
        if (isVeryDarkColor(surface)) {
            return blendColors(
                    surface,
                    Color.WHITE,
                    0.10f
            );
        }

        return surface;
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

    private static int blendColors(
            int baseColor,
            int overlayColor,
            float ratio
    ) {
        float inverseRatio = 1f - ratio;

        int red = Math.round(
                Color.red(baseColor) * inverseRatio
                        + Color.red(overlayColor) * ratio
        );

        int green = Math.round(
                Color.green(baseColor) * inverseRatio
                        + Color.green(overlayColor) * ratio
        );

        int blue = Math.round(
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
