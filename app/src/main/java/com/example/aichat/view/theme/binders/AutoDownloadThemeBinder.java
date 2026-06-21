package com.example.aichat.view.theme.binders;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.R;
import com.example.aichat.view.cache.AutoDownloadNetworkActivity;
import com.example.aichat.view.cache.CacheUi;
import com.example.aichat.view.cache.MediaAutoDownloadActivity;
import com.google.android.material.materialswitch.MaterialSwitch;

public final class AutoDownloadThemeBinder {

    private AutoDownloadThemeBinder() {
    }

    public static void applyMedia(@NonNull MediaAutoDownloadActivity activity) {
        CacheUi.applyWindowBackground(
                activity
        );

        ScrollView scrollView =
                activity.findViewById(
                        R.id.media_auto_download_scroll
                );

        LinearLayout root =
                activity.findViewById(
                        R.id.media_auto_download_root
                );

        LinearLayout card =
                activity.findViewById(
                        R.id.media_auto_download_card
                );

        ImageButton back =
                activity.findViewById(
                        R.id.media_auto_download_back
                );

        TextView title =
                activity.findViewById(
                        R.id.media_auto_download_title
                );

        TextView cardTitle =
                activity.findViewById(
                        R.id.media_auto_download_card_title
                );

        TextView reset =
                activity.findViewById(
                        R.id.media_auto_download_reset
                );

        TextView note =
                activity.findViewById(
                        R.id.media_auto_download_note
                );

        if (scrollView != null) {
            scrollView.setBackgroundColor(
                    CacheUi.colorBackground(
                            activity
                    )
            );
        }

        if (root != null) {
            root.setBackgroundColor(
                    CacheUi.colorBackground(
                            activity
                    )
            );
        }

        if (card != null) {
            card.setBackground(
                    CacheUi.rounded(
                            CacheUi.cardColor(
                                    activity
                            ),
                            22,
                            activity
                    )
            );
        }

        if (back != null) {
            back.setColorFilter(
                    CacheUi.colorPrimary(
                            activity
                    )
            );
        }

        if (title != null) {
            title.setTextColor(
                    CacheUi.colorOnSurface(
                            activity
                    )
            );
        }

        if (cardTitle != null) {
            cardTitle.setTextColor(
                    CacheUi.colorPrimary(
                            activity
                    )
            );
        }

        if (reset != null) {
            reset.setTextColor(
                    0xFFFF6B6B
            );
        }

        if (note != null) {
            note.setTextColor(
                    CacheUi.colorOnSurfaceVariant(
                            activity
                    )
            );
        }

        tintRowsRecursively(
                activity.findViewById(
                        R.id.media_auto_download_rows
                )
        );
    }

    public static void applyNetwork(@NonNull AutoDownloadNetworkActivity activity) {
        CacheUi.applyWindowBackground(
                activity
        );

        ScrollView scrollView =
                activity.findViewById(
                        R.id.network_auto_download_scroll
                );

        LinearLayout root =
                activity.findViewById(
                        R.id.network_auto_download_root
                );

        LinearLayout masterRow =
                activity.findViewById(
                        R.id.network_auto_download_master_row
                );

        LinearLayout trafficCard =
                activity.findViewById(
                        R.id.network_traffic_card
                );

        LinearLayout mediaCard =
                activity.findViewById(
                        R.id.network_media_card
                );

        ImageButton back =
                activity.findViewById(
                        R.id.network_auto_download_back
                );

        TextView title =
                activity.findViewById(
                        R.id.network_auto_download_title
                );

        TextView masterTitle =
                activity.findViewById(
                        R.id.network_auto_download_master_title
                );

        TextView trafficTitle =
                activity.findViewById(
                        R.id.network_traffic_title
                );

        TextView mediaTitle =
                activity.findViewById(
                        R.id.network_media_title
                );

        TextView low =
                activity.findViewById(
                        R.id.network_traffic_low
                );

        TextView medium =
                activity.findViewById(
                        R.id.network_traffic_medium
                );

        TextView high =
                activity.findViewById(
                        R.id.network_traffic_high
                );

        TextView note =
                activity.findViewById(
                        R.id.network_note
                );

        MaterialSwitch masterSwitch =
                activity.findViewById(
                        R.id.network_auto_download_master_switch
                );

        SeekBar trafficSeekBar =
                activity.findViewById(
                        R.id.network_traffic_seekbar
                );

        if (scrollView != null) {
            scrollView.setBackgroundColor(
                    CacheUi.colorBackground(
                            activity
                    )
            );
        }

        if (root != null) {
            root.setBackgroundColor(
                    CacheUi.colorBackground(
                            activity
                    )
            );
        }

        if (masterRow != null) {
            masterRow.setBackground(
                    CacheUi.rounded(
                            CacheUi.colorPrimary(
                                    activity
                            ),
                            18,
                            activity
                    )
            );
        }

        if (trafficCard != null) {
            trafficCard.setBackground(
                    CacheUi.rounded(
                            CacheUi.cardColor(
                                    activity
                            ),
                            22,
                            activity
                    )
            );
        }

        if (mediaCard != null) {
            mediaCard.setBackground(
                    CacheUi.rounded(
                            CacheUi.cardColor(
                                    activity
                            ),
                            22,
                            activity
                    )
            );
        }

        if (back != null) {
            back.setColorFilter(
                    CacheUi.colorPrimary(
                            activity
                    )
            );
        }

        if (title != null) {
            title.setTextColor(
                    CacheUi.colorOnSurface(
                            activity
                    )
            );
        }

        if (masterTitle != null) {
            masterTitle.setTextColor(
                    CacheUi.colorOnPrimary(
                            activity
                    )
            );
        }

        if (trafficTitle != null) {
            trafficTitle.setTextColor(
                    CacheUi.colorPrimary(
                            activity
                    )
            );
        }

        if (mediaTitle != null) {
            mediaTitle.setTextColor(
                    CacheUi.colorPrimary(
                            activity
                    )
            );
        }

        if (low != null) {
            low.setTextColor(
                    CacheUi.colorOnSurfaceVariant(
                            activity
                    )
            );
        }

        if (medium != null) {
            medium.setTextColor(
                    CacheUi.colorOnSurfaceVariant(
                            activity
                    )
            );
        }

        if (high != null) {
            high.setTextColor(
                    CacheUi.colorOnSurfaceVariant(
                            activity
                    )
            );
        }

        if (note != null) {
            note.setTextColor(
                    CacheUi.colorOnSurfaceVariant(
                            activity
                    )
            );
        }

        tintSwitch(
                masterSwitch
        );

        tintSeekBar(
                trafficSeekBar
        );

        tintRowsRecursively(
                activity.findViewById(
                        R.id.network_media_rows
                )
        );
    }

    public static void tintRowsRecursively(@Nullable View view) {
        if (view == null) {
            return;
        }

        if (view instanceof TextView) {
            TextView textView =
                    (TextView) view;

            float sp =
                    textView.getTextSize()
                            / view.getResources()
                            .getDisplayMetrics()
                            .scaledDensity;

            boolean bold =
                    textView.getTypeface() != null
                            && textView.getTypeface().getStyle() == Typeface.BOLD;

            textView.setTextColor(
                    sp >= 16f || bold
                            ? CacheUi.colorOnSurface(
                            view.getContext()
                    )
                            : CacheUi.colorOnSurfaceVariant(
                            view.getContext()
                    )
            );
        }

        if (view instanceof MaterialSwitch) {
            tintSwitch(
                    (MaterialSwitch) view
            );
        }

        if (view instanceof SeekBar) {
            tintSeekBar(
                    (SeekBar) view
            );
        }

        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group =
                    (android.view.ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                tintRowsRecursively(
                        group.getChildAt(
                                i
                        )
                );
            }
        }
    }

    private static void tintSwitch(@Nullable MaterialSwitch switchView) {
        if (switchView == null) {
            return;
        }

        Context context =
                switchView.getContext();

        int primary =
                CacheUi.colorPrimary(
                        context
                );

        int onPrimary =
                CacheUi.colorOnPrimary(
                        context
                );

        int surface =
                CacheUi.colorSurface(
                        context
                );

        int variant =
                CacheUi.colorOnSurfaceVariant(
                        context
                );

        switchView.setThumbTintList(
                new ColorStateList(
                        new int[][]{
                                new int[]{
                                        android.R.attr.state_checked,
                                        android.R.attr.state_enabled
                                },
                                new int[]{
                                        -android.R.attr.state_checked,
                                        android.R.attr.state_enabled
                                },
                                new int[]{
                                        -android.R.attr.state_enabled
                                }
                        },
                        new int[]{
                                onPrimary,
                                surface,
                                blend(
                                        surface,
                                        variant,
                                        0.22f
                                )
                        }
                )
        );

        switchView.setTrackTintList(
                new ColorStateList(
                        new int[][]{
                                new int[]{
                                        android.R.attr.state_checked,
                                        android.R.attr.state_enabled
                                },
                                new int[]{
                                        -android.R.attr.state_checked,
                                        android.R.attr.state_enabled
                                },
                                new int[]{
                                        -android.R.attr.state_enabled
                                }
                        },
                        new int[]{
                                primary,
                                withAlpha(
                                        variant,
                                        82
                                ),
                                withAlpha(
                                        variant,
                                        42
                                )
                        }
                )
        );
    }

    private static void tintSeekBar(@Nullable SeekBar seekBar) {
        if (seekBar == null) {
            return;
        }

        Context context =
                seekBar.getContext();

        seekBar.setProgressTintList(
                ColorStateList.valueOf(
                        CacheUi.colorPrimary(
                                context
                        )
                )
        );

        seekBar.setThumbTintList(
                ColorStateList.valueOf(
                        CacheUi.colorPrimary(
                                context
                        )
                )
        );

        seekBar.setProgressBackgroundTintList(
                ColorStateList.valueOf(
                        CacheUi.colorOnSurfaceVariant(
                                context
                        )
                )
        );
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

    private static int blend(
            int base,
            int overlay,
            float ratio
    ) {
        float inverse =
                1f - ratio;

        return android.graphics.Color.rgb(
                Math.round(
                        android.graphics.Color.red(base) * inverse
                                + android.graphics.Color.red(overlay) * ratio
                ),
                Math.round(
                        android.graphics.Color.green(base) * inverse
                                + android.graphics.Color.green(overlay) * ratio
                ),
                Math.round(
                        android.graphics.Color.blue(base) * inverse
                                + android.graphics.Color.blue(overlay) * ratio
                )
        );
    }
}
