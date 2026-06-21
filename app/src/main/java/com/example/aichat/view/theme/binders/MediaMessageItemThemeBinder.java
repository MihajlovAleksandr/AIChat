package com.example.aichat.view.theme.binders;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.example.aichat.R;
import com.google.android.material.card.MaterialCardView;

public final class MediaMessageItemThemeBinder {

    private MediaMessageItemThemeBinder() {
    }

    public static void applyFileItem(@Nullable View itemView) {
        if (itemView == null) {
            return;
        }

        ScreenThemeBinder.surfaceRoundedBackground(
                itemView,
                14
        );

        ScreenThemeBinder.surfaceText(
                itemView.findViewById(
                        R.id.file_name
                )
        );

        ScreenThemeBinder.variantText(
                itemView.findViewById(
                        R.id.file_size
                )
        );

        ScreenThemeBinder.variantText(
                itemView.findViewById(
                        R.id.file_ext
                )
        );

        ScreenThemeBinder.variantText(
                itemView.findViewById(
                        R.id.file_date
                )
        );

        ScreenThemeBinder.tintIconPrimary(
                itemView.findViewById(
                        R.id.play_icon
                )
        );

        ScreenThemeBinder.primarySeekBar(
                itemView.findViewById(
                        R.id.audio_seekbar
                )
        );
    }

    public static void applyVideoPreview(@Nullable View itemView) {
        if (itemView == null) {
            return;
        }

        if (itemView instanceof MaterialCardView) {
            ((MaterialCardView) itemView).setCardBackgroundColor(
                    ScreenThemeBinder.surface(
                            itemView.getContext()
                    )
            );
        }

        ScreenThemeBinder.surfaceText(
                itemView.findViewById(
                        R.id.video_name
                )
        );

        ScreenThemeBinder.variantText(
                itemView.findViewById(
                        R.id.video_ext
                )
        );

        ScreenThemeBinder.tintIconPrimary(
                itemView.findViewById(
                        R.id.video_play
                )
        );

        ScreenThemeBinder.tintIconPrimary(
                itemView.findViewById(
                        R.id.file_status
                )
        );
    }

    public static void applyVideoCircle(@Nullable View itemView) {
        if (itemView == null) {
            return;
        }

        View play =
                itemView.findViewById(
                        R.id.video_circle_play
                );

        if (play != null) {
            play.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ScreenThemeBinder.primary(
                                    itemView.getContext()
                            )
                    )
            );
        }

        ScreenThemeBinder.primarySeekBar(
                itemView.findViewById(
                        R.id.video_circle_timeline
                )
        );

        TextView duration =
                itemView.findViewById(
                        R.id.video_circle_duration
                );

        if (duration != null) {
            duration.setTextColor(
                    ScreenThemeBinder.onSurface(
                            itemView.getContext()
                    )
            );
        }
    }

    public static void applyYoutubePreview(@Nullable View itemView) {
        if (itemView == null) {
            return;
        }

        if (itemView instanceof MaterialCardView) {
            ((MaterialCardView) itemView).setCardBackgroundColor(
                    ScreenThemeBinder.surface(
                            itemView.getContext()
                    )
            );
        }

        ScreenThemeBinder.surfaceText(
                itemView.findViewById(
                        R.id.youtube_preview_title
                )
        );

        ScreenThemeBinder.variantText(
                itemView.findViewById(
                        R.id.youtube_preview_subtitle
                )
        );
    }

    public static void applyVideoDraft(@Nullable View itemView) {
        if (itemView == null) {
            return;
        }

        TextView number =
                itemView.findViewById(
                        R.id.video_circle_draft_number
                );

        if (number != null) {
            number.setTextColor(
                    ScreenThemeBinder.onSurface(
                            itemView.getContext()
                    )
            );
        }

        View selected =
                itemView.findViewById(
                        R.id.video_circle_draft_selected
                );

        if (selected != null) {
            selected.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ScreenThemeBinder.primary(
                                    itemView.getContext()
                            )
                    )
            );
        }
    }
}
