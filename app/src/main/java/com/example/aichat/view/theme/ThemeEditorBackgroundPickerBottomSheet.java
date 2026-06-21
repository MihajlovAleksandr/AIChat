package com.example.aichat.view.theme;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class ThemeEditorBackgroundPickerBottomSheet {

    private ThemeEditorBackgroundPickerBottomSheet() {
    }

    public static void show(
            Context context,
            ThemeEditorState state,
            ActivityResultLauncher<String> backgroundImagePickerLauncher,
            Runnable onChanged
    ) {

        BottomSheetDialog dialog =
                new BottomSheetDialog(
                        context
                );

        LinearLayout container =
                new LinearLayout(
                        context
                );

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        container.setPadding(
                dp(context, 20),
                dp(context, 18),
                dp(context, 20),
                dp(context, 12)
        );

        int surface =
                ThemeAttrResolver.resolveColor(
                        context,
                        R.attr.colorSurface
                );

        int onSurface =
                ThemeAttrResolver.resolveColor(
                        context,
                        R.attr.colorOnSurface
                );

        int primary =
                ThemeAttrResolver.resolveColor(
                        context,
                        R.attr.colorPrimary
                );

        container.setBackgroundColor(
                surface
        );

        TextView title =
                new TextView(
                        context
                );

        title.setText(
                R.string.theme_editor_background_picker_title
        );

        title.setTextColor(
                onSurface
        );

        title.setTextSize(
                18
        );

        title.setTypeface(
                null,
                Typeface.BOLD
        );

        container.addView(
                title
        );

        TextView chooseColor =
                createOption(
                        context,
                        context.getString(
                                R.string.theme_editor_choose_background_color_action
                        ),
                        onSurface
                );

        TextView chooseImage =
                createOption(
                        context,
                        context.getString(
                                R.string.theme_editor_choose_background_image
                        ),
                        onSurface
                );

        TextView removeImage =
                createOption(
                        context,
                        context.getString(
                                R.string.theme_editor_remove_background_image
                        ),
                        onSurface
                );

        TextView cancel =
                ThemeEditorColorPickerBottomSheet.createActionText(
                        context,
                        context.getString(
                                R.string.theme_editor_cancel
                        ),
                        primary
                );

        chooseColor.setOnClickListener(v -> {

            dialog.dismiss();

            ThemeEditorColorPickerBottomSheet.show(
                    context,
                    context.getString(
                            R.string.theme_editor_choose_chat_background
                    ),
                    state.backgroundColor,
                    color -> {

                        state.backgroundColor =
                                color;

                        state.backgroundImagePath =
                                null;

                        state.backgroundImageName =
                                null;

                        onChanged.run();
                    }
            );
        });

        chooseImage.setOnClickListener(v -> {

            dialog.dismiss();

            backgroundImagePickerLauncher.launch(
                    "image/*"
            );
        });

        removeImage.setOnClickListener(v -> {

            state.backgroundImagePath =
                    null;

            state.backgroundImageName =
                    null;

            onChanged.run();

            dialog.dismiss();
        });

        cancel.setOnClickListener(
                v -> dialog.dismiss()
        );

        container.addView(
                chooseColor
        );

        container.addView(
                chooseImage
        );

        if (state.backgroundImagePath != null
                && !state.backgroundImagePath.trim().isEmpty()) {

            container.addView(
                    removeImage
            );
        }

        LinearLayout bottomActions =
                new LinearLayout(
                        context
                );

        bottomActions.setGravity(
                Gravity.END
        );

        bottomActions.addView(
                cancel
        );

        container.addView(
                bottomActions
        );

        dialog.setContentView(
                container
        );

        dialog.show();
    }

    private static TextView createOption(
            Context context,
            String text,
            int color
    ) {

        TextView textView =
                new TextView(
                        context
                );

        textView.setText(
                text
        );

        textView.setTextColor(
                color
        );

        textView.setTextSize(
                16
        );

        textView.setGravity(
                Gravity.CENTER_VERTICAL
        );

        textView.setPadding(
                0,
                dp(context, 18),
                0,
                dp(context, 18)
        );

        return textView;
    }

    private static int dp(
            Context context,
            int value
    ) {

        return ThemeEditorDrawableUtils.dp(
                context,
                value
        );
    }
}
