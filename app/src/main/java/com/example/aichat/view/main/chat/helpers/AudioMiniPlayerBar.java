package com.example.aichat.view.main.chat.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.utils.media.audio.AudioPlayerManager;
import java.lang.ref.WeakReference;
import java.util.UUID;

public final class AudioMiniPlayerBar {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static WeakReference<Activity> activityRef;
    private static AudioPlayerManager audioPlayerManager;
    private static UUID currentFileId;

    private static FrameLayout rootView;
    private static ImageView artworkView;
    private static TextView titleView;
    private static TextView subtitleView;
    private static TextView currentTimeView;
    private static TextView totalTimeView;
    private static SeekBar seekBar;
    private static ImageButton playPauseButton;
    private static ImageButton nextButton;
    private static ImageButton stopButton;

    private static String displayedTitle;
    private static String displayedSubtitle;

    private static boolean userSeeking;
    private static float touchStartY;
    private static int startTopMargin;

    private AudioMiniPlayerBar() {
    }

    public static void show(
            @NonNull Context context,
            @NonNull AudioPlayerManager manager,
            @NonNull UUID fileId,
            @Nullable String title,
            @Nullable String subtitle
    ) {
        MAIN_HANDLER.post(() -> showInternal(context, manager, fileId, title, subtitle));
    }

    private static void showInternal(
            @NonNull Context context,
            @NonNull AudioPlayerManager manager,
            @NonNull UUID fileId,
            @Nullable String title,
            @Nullable String subtitle
    ) {
        Activity activity = findActivity(context);
        if (!isMainActivity(activity)) {
            return;
        }

        Window window = activity.getWindow();
        if (window == null) {
            return;
        }

        View decorView = window.getDecorView();
        if (!(decorView instanceof ViewGroup)) {
            return;
        }

        audioPlayerManager = manager;
        currentFileId = fileId;
        activityRef = new WeakReference<>(activity);

        ViewGroup decorGroup = (ViewGroup) decorView;

        if (rootView == null) {
            rootView = createMiniPlayerView(activity);
        }

        if (rootView.getParent() == null) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(activity, 76)
            );
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.leftMargin = dp(activity, 14);
            params.rightMargin = dp(activity, 14);
            params.topMargin = getMinimumTopMargin(activity);

            decorGroup.addView(rootView, params);
        }

        rootView.setVisibility(View.VISIBLE);
        rootView.bringToFront();

        updateTexts(title, subtitle);
        update(fileId, manager.getDuration(fileId), manager.getCurrentPosition(fileId), manager.isPlaying(fileId));
    }

    private static FrameLayout createMiniPlayerView(@NonNull Activity activity) {
        FrameLayout container = new FrameLayout(activity);
        container.setClickable(true);
        container.setFocusable(false);
        container.setElevation(dp(activity, 12));
        container.setPadding(dp(activity, 14), dp(activity, 8), dp(activity, 14), dp(activity, 7));
        container.setBackground(createBackground());

        LinearLayout vertical = new LinearLayout(activity);
        vertical.setOrientation(LinearLayout.VERTICAL);
        vertical.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout.LayoutParams verticalParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        container.addView(vertical, verticalParams);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        vertical.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        artworkView = new ImageView(activity);
        artworkView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        artworkView.setBackground(createArtworkBackground());
        artworkView.setImageResource(android.R.drawable.ic_media_play);
        artworkView.setColorFilter(Color.WHITE);

        LinearLayout.LayoutParams artworkParams = new LinearLayout.LayoutParams(
                dp(activity, 42),
                dp(activity, 42)
        );
        row.addView(artworkView, artworkParams);

        LinearLayout textColumn = new LinearLayout(activity);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setGravity(Gravity.CENTER_VERTICAL);
        textColumn.setPadding(dp(activity, 10), 0, dp(activity, 8), 0);

        row.addView(textColumn, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
        ));

        titleView = new TextView(activity);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        titleView.setMarqueeRepeatLimit(-1);
        titleView.setSelected(true);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(14f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setIncludeFontPadding(false);

        textColumn.addView(titleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 22)
        ));

        subtitleView = new TextView(activity);
        subtitleView.setSingleLine(true);
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        subtitleView.setTextColor(0xCCFFFFFF);
        subtitleView.setTextSize(11f);
        subtitleView.setIncludeFontPadding(false);

        textColumn.addView(subtitleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 18)
        ));

        playPauseButton = createIconButton(activity, android.R.drawable.ic_media_pause);
        row.addView(playPauseButton, new LinearLayout.LayoutParams(dp(activity, 42), dp(activity, 42)));

        nextButton = createIconButton(activity, android.R.drawable.ic_media_next);
        row.addView(nextButton, new LinearLayout.LayoutParams(dp(activity, 42), dp(activity, 42)));

        stopButton = createIconButton(activity, android.R.drawable.ic_menu_close_clear_cancel);
        row.addView(stopButton, new LinearLayout.LayoutParams(dp(activity, 42), dp(activity, 42)));

        LinearLayout progressRow = new LinearLayout(activity);
        progressRow.setOrientation(LinearLayout.HORIZONTAL);
        progressRow.setGravity(Gravity.CENTER_VERTICAL);

        vertical.addView(progressRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 24)
        ));

        currentTimeView = createTimeText(activity, Gravity.START | Gravity.CENTER_VERTICAL);
        progressRow.addView(currentTimeView, new LinearLayout.LayoutParams(dp(activity, 42), ViewGroup.LayoutParams.MATCH_PARENT));

        seekBar = new SeekBar(activity);
        seekBar.setMax(1000);
        progressRow.addView(seekBar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        totalTimeView = createTimeText(activity, Gravity.END | Gravity.CENTER_VERTICAL);
        progressRow.addView(totalTimeView, new LinearLayout.LayoutParams(dp(activity, 42), ViewGroup.LayoutParams.MATCH_PARENT));

        playPauseButton.setOnClickListener(v -> {
            if (audioPlayerManager != null) {
                audioPlayerManager.togglePlayback();
            }
        });

        nextButton.setOnClickListener(v -> {
            if (audioPlayerManager != null) {
                audioPlayerManager.playNextOrStop();
            }
        });

        stopButton.setOnClickListener(v -> {
            if (audioPlayerManager != null) {
                audioPlayerManager.stopFromMiniPlayer();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || audioPlayerManager == null || currentFileId == null) {
                    return;
                }

                int duration = Math.max(0, audioPlayerManager.getDuration(currentFileId));
                if (duration <= 0) {
                    return;
                }

                int target = (int) ((progress / 1000f) * duration);
                currentTimeView.setText(formatTime(target));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (audioPlayerManager != null && currentFileId != null) {
                    int duration = Math.max(0, audioPlayerManager.getDuration(currentFileId));
                    if (duration > 0) {
                        int target = (int) ((seekBar.getProgress() / 1000f) * duration);
                        audioPlayerManager.seekTo(target);
                    }
                }
                userSeeking = false;
            }
        });

        container.setOnTouchListener((view, event) -> handleDrag(activity, view, event));

        return container;
    }

    private static boolean handleDrag(@NonNull Activity activity, @NonNull View view, @NonNull MotionEvent event) {
        ViewGroup.LayoutParams rawParams = view.getLayoutParams();
        if (!(rawParams instanceof FrameLayout.LayoutParams)) {
            return false;
        }

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) rawParams;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchStartY = event.getRawY();
                startTopMargin = params.topMargin;
                return true;

            case MotionEvent.ACTION_MOVE:
                int minTop = getMinimumTopMargin(activity);
                int maxTop = Math.max(minTop, getMaximumTopMargin(activity, view));
                int targetTop = startTopMargin + Math.round(event.getRawY() - touchStartY);
                params.topMargin = Math.max(minTop, Math.min(maxTop, targetTop));
                view.setLayoutParams(params);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return true;

            default:
                return false;
        }
    }

    public static void update(
            @Nullable UUID fileId,
            int duration,
            int position,
            boolean playing
    ) {
        MAIN_HANDLER.post(() -> updateInternal(fileId, duration, position, playing));
    }

    private static void updateInternal(@Nullable UUID fileId, int duration, int position, boolean playing) {
        if (rootView == null || rootView.getVisibility() != View.VISIBLE) {
            return;
        }

        if (audioPlayerManager != null) {
            UUID actualFileId = audioPlayerManager.getCurrentFileId();
            if (actualFileId != null) {
                currentFileId = actualFileId;
                duration = Math.max(duration, audioPlayerManager.getDuration(actualFileId));
                position = Math.max(0, audioPlayerManager.getCurrentPosition(actualFileId));
                playing = audioPlayerManager.isPlaying(actualFileId) || audioPlayerManager.isPreparing(actualFileId);
                updateTexts(audioPlayerManager.getCurrentTitle(), audioPlayerManager.getCurrentSubtitle());
                updateArtwork(audioPlayerManager.getCurrentArtwork());
                nextButton.setVisibility(audioPlayerManager.hasNextInCurrentQueue() ? View.VISIBLE : View.GONE);
            }
        } else if (fileId != null) {
            currentFileId = fileId;
        }

        playPauseButton.setImageResource(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);

        int safeDuration = Math.max(0, duration);
        int safePosition = Math.max(0, Math.min(position, safeDuration > 0 ? safeDuration : position));

        if (!userSeeking) {
            if (safeDuration > 0) {
                seekBar.setProgress(Math.round((safePosition * 1000f) / safeDuration));
            } else {
                seekBar.setProgress(0);
            }
        }

        currentTimeView.setText(formatTime(safePosition));
        totalTimeView.setText(safeDuration > 0 ? formatTime(safeDuration) : "0:00");
    }

    public static void hide() {
        MAIN_HANDLER.post(() -> {
            if (rootView == null) {
                return;
            }

            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null) {
                parent.removeView(rootView);
            }

            currentFileId = null;
            audioPlayerManager = null;
            activityRef = null;
            displayedTitle = null;
            displayedSubtitle = null;
        });
    }

    private static void updateTexts(@Nullable String title, @Nullable String subtitle) {
        String safeTitle = title != null && !title.trim().isEmpty() ? title : "Аудио";
        String safeSubtitle = subtitle != null ? subtitle : "";

        if (titleView != null && !safeTitle.equals(displayedTitle)) {
            displayedTitle = safeTitle;
            titleView.animate().cancel();
            titleView.setSelected(false);
            titleView.setText(safeTitle);
            titleView.postDelayed(() -> {
                if (titleView != null && safeTitle.equals(displayedTitle)) {
                    titleView.setSelected(true);
                }
            }, 120L);
        } else if (titleView != null && !titleView.isSelected()) {
            titleView.setSelected(true);
        }

        if (subtitleView != null && !safeSubtitle.equals(displayedSubtitle)) {
            displayedSubtitle = safeSubtitle;
            subtitleView.setText(safeSubtitle);
        }
    }

    private static void updateArtwork(@Nullable Bitmap bitmap) {
        if (artworkView == null) {
            return;
        }

        artworkView.clearColorFilter();

        if (bitmap != null) {
            artworkView.setImageBitmap(bitmap);
        } else {
            artworkView.setImageResource(android.R.drawable.ic_media_play);
            artworkView.setColorFilter(Color.WHITE);
        }
    }

    private static ImageButton createIconButton(@NonNull Context context, int iconRes) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(iconRes);
        button.setColorFilter(Color.WHITE);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 10));
        return button;
    }

    private static TextView createTimeText(@NonNull Context context, int gravity) {
        TextView textView = new TextView(context);
        textView.setGravity(gravity);
        textView.setTextColor(0xCCFFFFFF);
        textView.setTextSize(11f);
        textView.setIncludeFontPadding(false);
        textView.setText("0:00");
        return textView;
    }

    private static GradientDrawable createBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(0xF2292A2E);
        drawable.setCornerRadius(28f);
        drawable.setStroke(1, 0x3342969E);
        return drawable;
    }

    private static GradientDrawable createArtworkBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(0xFF3A3B40);
        drawable.setCornerRadius(16f);
        return drawable;
    }

    @Nullable
    private static Activity findActivity(@Nullable Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }

    private static boolean isMainActivity(@Nullable Activity activity) {
        return activity != null && "MainActivity".equals(activity.getClass().getSimpleName());
    }

    private static int getMinimumTopMargin(@NonNull Context context) {
        return getStatusBarHeight(context) + dp(context, 56);
    }

    private static int getMaximumTopMargin(@NonNull Context context, @NonNull View view) {
        Activity activity = findActivity(context);
        if (activity == null || activity.getWindow() == null) {
            return getMinimumTopMargin(context);
        }

        View decor = activity.getWindow().getDecorView();
        int height = decor.getHeight();
        if (height <= 0) {
            return getMinimumTopMargin(context);
        }

        return height - view.getHeight() - dp(context, 96);
    }

    private static int getStatusBarHeight(@NonNull Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return dp(context, 24);
    }

    private static int dp(@NonNull Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static String formatTime(int milliseconds) {
        int totalSeconds = Math.max(0, milliseconds / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }
}
