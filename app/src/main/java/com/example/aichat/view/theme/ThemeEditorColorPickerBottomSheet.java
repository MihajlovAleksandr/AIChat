package com.example.aichat.view.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class ThemeEditorColorPickerBottomSheet {

    private static final String[] PALETTE_COLORS = {
            "#20A39A",
            "#2F5AC9",
            "#4169E1",
            "#9B59B6",
            "#FF5A5F",
            "#FF3B3B",
            "#FF7A00",
            "#FF8C2A",
            "#FFA22B",
            "#FFC043",
            "#C9C22E",
            "#7DB957",
            "#777777",
            "#666666",
            "#000000",
            "#FFFFFF",
            "#F1F1F1",
            "#121212",
            "#2B2B2B",
            "#FF9800",
            "#E91E63",
            "#3F51B5",
            "#009688",
            "#607D8B"
    };

    private ThemeEditorColorPickerBottomSheet() {
    }

    public static void show(
            Context context,
            String title,
            String currentColor,
            OnColorSelectedListener listener
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

        int onSurfaceVariant =
                ThemeAttrResolver.resolveColor(
                        context,
                        R.attr.colorOnSurfaceVariant
                );

        int primary =
                ThemeAttrResolver.resolveColor(
                        context,
                        R.attr.colorPrimary
                );

        container.setBackgroundColor(
                surface
        );

        TextView titleView =
                new TextView(
                        context
                );

        titleView.setText(
                title
        );

        titleView.setTextColor(
                onSurface
        );

        titleView.setTextSize(
                18
        );

        titleView.setTypeface(
                null,
                Typeface.BOLD
        );

        container.addView(
                titleView
        );

        LinearLayout tabs =
                new LinearLayout(
                        context
                );

        tabs.setOrientation(
                LinearLayout.HORIZONTAL
        );

        tabs.setGravity(
                Gravity.CENTER_VERTICAL
        );

        tabs.setPadding(
                0,
                dp(context, 18),
                0,
                dp(context, 12)
        );

        TextView paletteTab =
                createTabText(
                        context,
                        context.getString(
                                R.string.theme_editor_palette
                        ),
                        primary,
                        true
                );

        TextView spectrumTab =
                createTabText(
                        context,
                        context.getString(
                                R.string.theme_editor_spectrum
                        ),
                        onSurfaceVariant,
                        false
                );

        tabs.addView(
                paletteTab,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        tabs.addView(
                spectrumTab,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        container.addView(
                tabs
        );

        FrameLayout pickerContainer =
                new FrameLayout(
                        context
                );

        String[] selectedColor = {
                ThemeEditorDrawableUtils.normalizeColor(
                        currentColor
                )
        };

        paletteTab.setOnClickListener(v -> {

            setTabSelected(
                    paletteTab,
                    spectrumTab,
                    primary,
                    onSurfaceVariant
            );

            showPaletteContent(
                    context,
                    pickerContainer,
                    selectedColor
            );
        });

        spectrumTab.setOnClickListener(v -> {

            setTabSelected(
                    spectrumTab,
                    paletteTab,
                    primary,
                    onSurfaceVariant
            );

            showSpectrumContent(
                    context,
                    pickerContainer,
                    selectedColor,
                    onSurface,
                    onSurfaceVariant,
                    primary
            );
        });

        showPaletteContent(
                context,
                pickerContainer,
                selectedColor
        );

        container.addView(
                pickerContainer
        );

        LinearLayout bottomActions =
                new LinearLayout(
                        context
                );

        bottomActions.setGravity(
                Gravity.END | Gravity.CENTER_VERTICAL
        );

        bottomActions.setOrientation(
                LinearLayout.HORIZONTAL
        );

        bottomActions.setPadding(
                0,
                dp(context, 14),
                0,
                0
        );

        TextView cancel =
                createActionText(
                        context,
                        context.getString(
                                R.string.theme_editor_cancel
                        ),
                        primary
                );

        TextView choose =
                createActionText(
                        context,
                        context.getString(
                                R.string.theme_editor_choose
                        ),
                        primary
                );

        cancel.setOnClickListener(
                v -> dialog.dismiss()
        );

        choose.setOnClickListener(v -> {

            listener.onSelected(
                    selectedColor[0]
            );

            dialog.dismiss();
        });

        bottomActions.addView(
                cancel
        );

        bottomActions.addView(
                choose
        );

        container.addView(
                bottomActions
        );

        dialog.setContentView(
                container
        );

        dialog.show();
    }

    private static void showPaletteContent(
            Context context,
            FrameLayout pickerContainer,
            String[] selectedColor
    ) {

        pickerContainer.removeAllViews();

        GridLayout grid =
                new GridLayout(
                        context
                );

        grid.setColumnCount(
                6
        );

        renderPalette(
                context,
                grid,
                selectedColor
        );

        pickerContainer.addView(
                grid
        );
    }

    private static void showSpectrumContent(
            Context context,
            FrameLayout pickerContainer,
            String[] selectedColor,
            int onSurface,
            int onSurfaceVariant,
            int primary
    ) {

        pickerContainer.removeAllViews();

        LinearLayout layout =
                new LinearLayout(
                        context
                );

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        SpectrumColorPickerView spectrumView =
                new SpectrumColorPickerView(
                        context
                );

        spectrumView.setColor(
                selectedColor[0]
        );

        TextView hexValue =
                new TextView(
                        context
                );

        hexValue.setTextColor(
                onSurface
        );

        hexValue.setTextSize(
                14
        );

        hexValue.setTypeface(
                null,
                Typeface.BOLD
        );

        hexValue.setText(
                selectedColor[0]
        );

        TextView hexLabel =
                new TextView(
                        context
                );

        hexLabel.setText(
                R.string.theme_editor_hex_label
        );

        hexLabel.setTextColor(
                onSurfaceVariant
        );

        hexLabel.setTextSize(
                12
        );

        SeekBar hueSeekBar =
                new SeekBar(
                        context
                );

        hueSeekBar.setMax(
                360
        );

        hueSeekBar.setProgress(
                spectrumView.getHueInt()
        );

        hueSeekBar.setProgressTintList(
                ColorStateList.valueOf(
                        primary
                )
        );

        hueSeekBar.setThumbTintList(
                ColorStateList.valueOf(
                        primary
                )
        );

        spectrumView.setOnColorChangedListener(color -> {

            selectedColor[0] =
                    color;

            hexValue.setText(
                    color
            );
        });

        hueSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {

                        spectrumView.setHue(
                                progress
                        );

                        selectedColor[0] =
                                spectrumView.getSelectedColorHex();

                        hexValue.setText(
                                selectedColor[0]
                        );
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }
                }
        );

        layout.addView(
                spectrumView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(context, 220)
                )
        );

        layout.addView(
                hueSeekBar
        );

        LinearLayout hexRow =
                new LinearLayout(
                        context
                );

        hexRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        hexRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        hexRow.setPadding(
                0,
                dp(context, 10),
                0,
                0
        );

        hexRow.addView(
                hexLabel,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        hexRow.addView(
                hexValue
        );

        layout.addView(
                hexRow
        );

        pickerContainer.addView(
                layout
        );
    }

    private static void renderPalette(
            Context context,
            GridLayout grid,
            String[] selectedColor
    ) {

        grid.removeAllViews();

        for (String color : PALETTE_COLORS) {

            TextView colorView =
                    new TextView(
                            context
                    );

            colorView.setGravity(
                    Gravity.CENTER
            );

            colorView.setTextSize(
                    15
            );

            colorView.setTypeface(
                    null,
                    Typeface.BOLD
            );

            colorView.setText(
                    color.equalsIgnoreCase(
                            selectedColor[0]
                    )
                            ? "✓"
                            : ""
            );

            colorView.setTextColor(
                    ThemeEditorDrawableUtils.getContrastColor(
                            color
                    )
            );

            colorView.setBackground(
                    ThemeEditorDrawableUtils.createCircleDrawable(
                            context,
                            color
                    )
            );

            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams();

            params.width =
                    dp(
                            context,
                            36
                    );

            params.height =
                    dp(
                            context,
                            36
                    );

            params.setMargins(
                    0,
                    0,
                    dp(context, 14),
                    dp(context, 14)
            );

            colorView.setLayoutParams(
                    params
            );

            colorView.setOnClickListener(v -> {

                selectedColor[0] =
                        color;

                renderPalette(
                        context,
                        grid,
                        selectedColor
                );
            });

            grid.addView(
                    colorView
            );
        }
    }

    private static TextView createTabText(
            Context context,
            String text,
            int color,
            boolean selected
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

        textView.setGravity(
                Gravity.CENTER
        );

        textView.setTextSize(
                14
        );

        if (selected) {

            textView.setTypeface(
                    null,
                    Typeface.BOLD
            );
        }

        return textView;
    }

    private static void setTabSelected(
            TextView selected,
            TextView unselected,
            int primary,
            int onSurfaceVariant
    ) {

        selected.setTextColor(
                primary
        );

        selected.setTypeface(
                null,
                Typeface.BOLD
        );

        unselected.setTextColor(
                onSurfaceVariant
        );

        unselected.setTypeface(
                null,
                Typeface.NORMAL
        );
    }

    static TextView createActionText(
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
                14
        );

        textView.setTypeface(
                null,
                Typeface.BOLD
        );

        textView.setGravity(
                Gravity.CENTER
        );

        textView.setPadding(
                dp(context, 16),
                dp(context, 12),
                dp(context, 16),
                dp(context, 12)
        );

        return textView;
    }

    static int dp(
            Context context,
            int value
    ) {

        return ThemeEditorDrawableUtils.dp(
                context,
                value
        );
    }

    public interface OnColorSelectedListener {

        void onSelected(
                String color
        );
    }
}
