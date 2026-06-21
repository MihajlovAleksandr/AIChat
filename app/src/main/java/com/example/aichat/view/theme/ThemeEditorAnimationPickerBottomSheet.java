package com.example.aichat.view.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import com.example.aichat.R;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.view.theme.binders.ThemeMessageAnimationBinder;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class ThemeEditorAnimationPickerBottomSheet {

    private static final String[] MESSAGE_ANIMATION_CODES = {
            ThemeMessageAnimationBinder.ANIMATION_DEFAULT,
            ThemeMessageAnimationBinder.ANIMATION_SMOOTH,
            ThemeMessageAnimationBinder.ANIMATION_SLIDE_UP,
            ThemeMessageAnimationBinder.ANIMATION_SLIDE_UP_BOUNCE,
            ThemeMessageAnimationBinder.ANIMATION_FADE,
            ThemeMessageAnimationBinder.ANIMATION_SCALE
    };

    private ThemeEditorAnimationPickerBottomSheet() {
    }

    public static void show(
            Context context,
            ThemeEditorState state,
            Runnable onChanged
    ) {
        if (context == null || state == null) {
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(context, 20), dp(context, 18), dp(context, 20), dp(context, 12));

        int surface = ThemeAttrResolver.resolveColor(context, R.attr.colorSurface);
        int onSurface = ThemeAttrResolver.resolveColor(context, R.attr.colorOnSurface);
        int primary = ThemeAttrResolver.resolveColor(context, R.attr.colorPrimary);

        container.setBackgroundColor(surface);

        TextView title = new TextView(context);
        title.setText(R.string.theme_editor_message_animation);
        title.setTextColor(onSurface);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        container.addView(title);

        String[] animations = context.getResources().getStringArray(R.array.theme_editor_message_animations);
        String currentAnimation = ThemeMessageAnimationBinder.normalize(state.messageAnimation);
        String[] selectedAnimation = {currentAnimation};

        RadioGroup radioGroup = new RadioGroup(context);
        radioGroup.setOrientation(RadioGroup.VERTICAL);
        radioGroup.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);

        int checkedId = -1;

        for (int i = 0; i < animations.length; i++) {
            String animationLabel = animations[i];
            String animationCode = getAnimationCodeByIndex(i, animationLabel);

            RadioButton radioButton = new RadioButton(context);
            radioButton.setId(View.generateViewId());
            radioButton.setTag(animationCode);
            radioButton.setText(animationLabel);
            radioButton.setTextColor(onSurface);
            radioButton.setTextSize(15);
            radioButton.setButtonTintList(ColorStateList.valueOf(primary));
            radioButton.setGravity(Gravity.CENTER_VERTICAL);
            radioButton.setMinHeight(dp(context, 50));
            radioButton.setPadding(0, dp(context, 8), 0, dp(context, 8));
            radioButton.setLayoutParams(new RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT
            ));

            if (checkedId == -1 && animationCode.equals(currentAnimation)) {
                checkedId = radioButton.getId();
            }

            radioGroup.addView(radioButton);
        }

        if (checkedId != -1) {
            radioGroup.check(checkedId);
        }

        radioGroup.setOnCheckedChangeListener((group, checkedRadioId) -> {
            View selectedView = group.findViewById(checkedRadioId);

            if (selectedView != null && selectedView.getTag() != null) {
                selectedAnimation[0] = ThemeMessageAnimationBinder.normalize(
                        selectedView.getTag().toString()
                );
            }
        });

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(false);
        scrollView.addView(radioGroup);
        container.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        Math.min(dp(context, 330), Math.max(dp(context, 120), animations.length * dp(context, 52)))
                )
        );

        LinearLayout bottomActions = new LinearLayout(context);
        bottomActions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        bottomActions.setPadding(0, dp(context, 12), 0, 0);

        TextView cancel = ThemeEditorColorPickerBottomSheet.createActionText(
                context,
                context.getString(R.string.theme_editor_cancel),
                primary
        );

        TextView choose = ThemeEditorColorPickerBottomSheet.createActionText(
                context,
                context.getString(R.string.theme_editor_choose),
                primary
        );

        cancel.setOnClickListener(v -> dialog.dismiss());

        choose.setOnClickListener(v -> {
            state.messageAnimation = ThemeMessageAnimationBinder.normalize(selectedAnimation[0]);

            if (onChanged != null) {
                onChanged.run();
            }

            dialog.dismiss();
        });

        bottomActions.addView(cancel);
        bottomActions.addView(choose);
        container.addView(bottomActions);

        dialog.setContentView(container);
        dialog.show();
    }

    private static String getAnimationCodeByIndex(int index, String fallbackLabel) {
        String codeFromLabel = ThemeMessageAnimationBinder.normalize(fallbackLabel);

        if (!ThemeMessageAnimationBinder.ANIMATION_DEFAULT.equals(codeFromLabel) || index == 0) {
            return codeFromLabel;
        }

        if (index >= 0 && index < MESSAGE_ANIMATION_CODES.length) {
            return MESSAGE_ANIMATION_CODES[index];
        }

        return codeFromLabel;
    }

    private static int dp(Context context, int value) {
        return ThemeEditorDrawableUtils.dp(context, value);
    }
}
