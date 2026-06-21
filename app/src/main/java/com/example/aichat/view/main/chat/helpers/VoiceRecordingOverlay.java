package com.example.aichat.view.main.chat.helpers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.animation.LinearInterpolator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import com.example.aichat.R;

public class VoiceRecordingOverlay {

    public interface Callback {
        void onDeleteClicked();

        void onTrimClicked();

        void onPreviewPlayClicked();

        void onSendClicked();

        void onTrimChanged(long startMs, long endMs);

        void onTrimSeekChanged(long positionMs);

        void onUnlockSwipeDown();

        void onUnlockReleased();

        void onUnlockCancelSwipeRight();
    }

    private static final long ANIMATION_MS = 170L;
    private static final int UNLOCK_DRAG_DOWN_DP = 48;
    private static final int UNLOCK_CANCEL_RIGHT_DP = 46;
    private static final int PANEL_HEIGHT_DP = 126;

    private final Context context;
    private final ViewGroup root;
    private final Callback callback;

    private FrameLayout overlayRoot;
    private LinearLayout panel;
    private LinearLayout recordingColumn;
    private LinearLayout actionButtons;
    private TextView timerText;
    private TextView hintText;
    private TextView statusText;
    private TextView micDot;
    private TextView lockArrowsText;
    private ImageButton lockButton;
    private VoiceWaveformView waveformView;
    private VoiceTrimView trimView;
    private ImageButton deleteButton;
    private ImageButton trimButton;
    private ImageButton previewPlayButton;
    private ImageButton sendButton;

    private ObjectAnimator lockArrowsAnimator;
    private View lockAnchorView;

    private int colorSurface;
    private int colorSurfaceVariant;
    private int colorOnSurface;
    private int colorOnSurfaceVariant;
    private int colorPrimary;
    private int colorOnPrimary;
    private int colorError;

    private boolean visible = false;
    private boolean locked = false;
    private boolean trimMode = false;
    private boolean cancelActive = false;
    private boolean unlockDragTriggered = false;
    private boolean lockGestureSession = false;
    private boolean unlockCancelTriggered = false;
    private float lockButtonTouchStartY = 0f;
    private float lockButtonTouchStartX = 0f;
    private float unlockSwipeRightStartX = 0f;

    public VoiceRecordingOverlay(
            @NonNull Context context,
            @NonNull View rootView,
            @NonNull Callback callback
    ) {
        this.context = context;
        this.callback = callback;

        if (!(rootView instanceof ViewGroup)) {
            throw new IllegalArgumentException("VoiceRecordingOverlay requires ViewGroup root");
        }

        this.root = (ViewGroup) rootView;
        resolveColors();
        buildIfNeeded();
    }

    public void setLockAnchorView(@Nullable View lockAnchorView) {
        this.lockAnchorView = lockAnchorView;
    }

    public void showRecording() {
        buildIfNeeded();

        locked = false;
        trimMode = false;
        cancelActive = false;
        unlockDragTriggered = false;
        lockGestureSession = false;
        unlockCancelTriggered = false;

        timerText.setText("0:00");
        hintText.setText(getString(R.string.voice_swipe_right_cancel));
        hintText.setTextColor(colorOnSurfaceVariant);
        statusText.setText(getString(R.string.voice_recording));
        statusText.setTextColor(colorOnSurfaceVariant);
        micDot.setTextColor(colorError);
        panel.setAlpha(1f);
        waveformView.clear();
        waveformView.setPaused(false);
        trimView.setVisibility(View.GONE);
        trimView.setPlaybackPositionMs(0L);
        setLockProgress(0f);
        setCancelProgress(0f);

        applyUnlockedRecordingState();
        show();
    }

    public void update(long durationMs, int amplitude) {
        if (!visible || trimMode) {
            return;
        }

        timerText.setText(formatDuration(durationMs));
        waveformView.addAmplitude(amplitude);
    }

    public void setCancelProgress(float progress) {
        if (!visible || locked || trimMode) {
            return;
        }

        float safe = Math.max(0f, Math.min(1f, progress));
        boolean active = safe >= 1f;
        cancelActive = active;

        hintText.setText(active ? getString(R.string.voice_release_delete) : getString(R.string.voice_swipe_right_cancel));
        hintText.setTextColor(active ? colorError : colorOnSurfaceVariant);
        panel.setAlpha(1f - safe * 0.26f);
        micDot.setTextColor(colorError);
    }

    public void setLockProgress(float progress) {
        if (!visible || locked || trimMode || lockButton == null) {
            return;
        }

        float safe = Math.max(0f, Math.min(1f, progress));
        float scale = 1f + safe * 0.18f;

        lockButton.setScaleX(scale);
        lockButton.setScaleY(scale);
        lockButton.setAlpha(0.78f + safe * 0.22f);
        lockButton.setBackground(makeOvalBg(
                safe >= 1f ? colorPrimary : colorSurface,
                safe >= 1f ? colorPrimary : colorSurfaceVariant,
                dp(1)
        ));
        lockButton.setImageTintList(ColorStateList.valueOf(safe >= 1f ? colorOnPrimary : colorOnSurface));

        if (lockArrowsText != null) {
            lockArrowsText.setAlpha(0.55f + safe * 0.45f);
            lockArrowsText.setTextColor(safe >= 1f ? colorPrimary : colorOnSurfaceVariant);
        }
    }

    public void setLocked() {
        if (!visible || locked || trimMode) {
            return;
        }

        locked = true;
        cancelActive = false;
        unlockDragTriggered = false;
        lockGestureSession = false;
        unlockCancelTriggered = false;
        panel.setAlpha(1f);
        hintText.setText(getString(R.string.voice_swipe_down_unlock));
        hintText.setTextColor(colorOnSurfaceVariant);
        statusText.setText(getString(R.string.voice_locked));
        statusText.setTextColor(colorOnSurfaceVariant);
        waveformView.setPaused(false);
        setLockedIconState();
        applyLockedRecordingState();
    }

    public void setUnlockedBySwipe() {
        if (!visible || !locked || trimMode) {
            return;
        }

        locked = false;
        cancelActive = false;
        panel.setAlpha(1f);
        hintText.setText(getString(R.string.voice_unlocked_release_send));
        hintText.setTextColor(colorOnSurfaceVariant);
        statusText.setText(getString(R.string.voice_recording));
        statusText.setTextColor(colorOnSurfaceVariant);
        setLockProgress(0f);
        applyUnlockedRecordingState();
    }

    public void showTrimDraft(long durationMs, boolean isPreviewPlaying) {
        if (!visible) {
            show();
        }

        trimMode = true;
        locked = true;
        cancelActive = false;
        unlockDragTriggered = false;
        lockGestureSession = false;
        unlockCancelTriggered = false;
        panel.setAlpha(1f);
        waveformView.setPaused(true);
        statusText.setText(getString(R.string.voice_trim));
        statusText.setTextColor(colorOnSurfaceVariant);
        hintText.setText(getString(R.string.voice_trim_hint));
        hintText.setTextColor(colorOnSurfaceVariant);
        timerText.setText(formatDuration(0L) + " / " + formatDuration(durationMs));
        trimView.setDuration(durationMs);
        trimView.setPlaybackPositionMs(0L);
        trimView.setVisibility(View.VISIBLE);
        lockButton.setVisibility(View.GONE);
        lockArrowsText.setVisibility(View.GONE);
        stopLockArrowAnimation();
        applyTrimDraftState(isPreviewPlaying);
    }

    public void setPreviewPlaying(boolean playing) {
        if (previewPlayButton != null) {
            previewPlayButton.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        }
        if (trimView != null) {
            trimView.setPlaying(playing);
        }
    }

    public void setTrimPlaybackPosition(long positionMs) {
        if (trimView != null) {
            trimView.setPlaybackPositionMs(positionMs);

            if (trimMode && timerText != null) {
                long start = trimView.getTrimStartMs();
                long end = trimView.getTrimEndMs();
                long selected = Math.max(0L, end - start);
                long current = Math.max(0L, Math.min(positionMs - start, selected));
                        timerText.setText(formatDuration(current) + " / " + formatDuration(selected));
            }
        }
    }

    public long getTrimStartMs() {
        return trimView != null ? trimView.getTrimStartMs() : 0L;
    }

    public long getTrimEndMs() {
        return trimView != null ? trimView.getTrimEndMs() : 0L;
    }

    public void hide() {
        if (!visible || overlayRoot == null) {
            return;
        }

        visible = false;
        lockGestureSession = false;
        unlockDragTriggered = false;
        stopLockArrowAnimation();

        overlayRoot.animate()
                .alpha(0f)
                .translationY(dp(18))
                .setDuration(ANIMATION_MS)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!visible && overlayRoot != null) {
                            overlayRoot.setVisibility(View.GONE);
                        }
                    }
                })
                .start();
    }

    public void destroy() {
        stopLockArrowAnimation();

        if (overlayRoot != null) {
            root.removeView(overlayRoot);
        }

        overlayRoot = null;
        panel = null;
        recordingColumn = null;
        actionButtons = null;
        timerText = null;
        hintText = null;
        statusText = null;
        micDot = null;
        lockArrowsText = null;
        lockButton = null;
        waveformView = null;
        trimView = null;
        deleteButton = null;
        trimButton = null;
        previewPlayButton = null;
        sendButton = null;
        visible = false;
    }

    private void show() {
        if (overlayRoot == null) {
            return;
        }

        visible = true;
        overlayRoot.bringToFront();
        overlayRoot.setVisibility(View.VISIBLE);
        overlayRoot.setAlpha(0f);
        overlayRoot.setTranslationY(dp(18));
        updateLockAnchorPosition();
        overlayRoot.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ANIMATION_MS)
                .setListener(null)
                .start();

        startUpArrowAnimation();
    }

    private void buildIfNeeded() {
        if (overlayRoot != null) {
            return;
        }

        overlayRoot = new FrameLayout(context);
        overlayRoot.setVisibility(View.GONE);
        overlayRoot.setClickable(true);
        overlayRoot.setFocusable(true);
        overlayRoot.setElevation(dp(32));
        overlayRoot.setOnTouchListener(this::handleOverlayRootTouch);

        root.addView(
                overlayRoot,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        lockButton = makeIconButton(R.drawable.ic_voice_lock, colorOnSurface, colorSurface);
        lockButton.setClickable(true);
        lockButton.setFocusable(true);
        lockButton.setOnTouchListener(this::handleLockButtonTouch);
        FrameLayout.LayoutParams lockParams = new FrameLayout.LayoutParams(dp(58), dp(58));
        lockParams.gravity = Gravity.BOTTOM | Gravity.START;
        lockParams.setMargins(dp(62), 0, 0, dp(126));
        overlayRoot.addView(lockButton, lockParams);

        lockArrowsText = new TextView(context);
        lockArrowsText.setText("⌃\n⌃\n⌃");
        lockArrowsText.setSingleLine(false);
        lockArrowsText.setGravity(Gravity.CENTER);
        lockArrowsText.setIncludeFontPadding(false);
        lockArrowsText.setLineSpacing(-dp(3), 1f);
        lockArrowsText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        lockArrowsText.setTypeface(Typeface.DEFAULT_BOLD);
        lockArrowsText.setTextColor(colorOnSurfaceVariant);
        lockArrowsText.setAlpha(0.72f);
        FrameLayout.LayoutParams arrowParams = new FrameLayout.LayoutParams(dp(58), dp(64));
        arrowParams.gravity = Gravity.BOTTOM | Gravity.START;
        arrowParams.setMargins(dp(62), 0, 0, dp(70));
        overlayRoot.addView(lockArrowsText, arrowParams);

        panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(8), dp(8), dp(8), dp(8));
        panel.setBackground(makeRoundBg(colorSurface, dp(32), colorPrimary, dp(1)));
        panel.setClickable(true);
        panel.setFocusable(false);
        panel.setElevation(dp(34));

        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(PANEL_HEIGHT_DP)
        );
        panelParams.gravity = Gravity.BOTTOM;
        panelParams.setMargins(dp(10), 0, dp(10), dp(14));
        overlayRoot.addView(panel, panelParams);

        micDot = new TextView(context);
        micDot.setText("●");
        micDot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        micDot.setGravity(Gravity.CENTER);
        micDot.setTextColor(colorError);
        panel.addView(micDot, new LinearLayout.LayoutParams(dp(26), ViewGroup.LayoutParams.MATCH_PARENT));

        timerText = new TextView(context);
        timerText.setText("0:00");
        timerText.setGravity(Gravity.CENTER_VERTICAL);
        timerText.setTextColor(colorOnSurface);
        timerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        timerText.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams timerParams = new LinearLayout.LayoutParams(dp(50), ViewGroup.LayoutParams.MATCH_PARENT);
        timerParams.setMargins(dp(2), 0, dp(6), 0);
        panel.addView(timerText, timerParams);

        recordingColumn = new LinearLayout(context);
        recordingColumn.setOrientation(LinearLayout.VERTICAL);
        recordingColumn.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams recordingParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        recordingParams.setMargins(0, 0, dp(6), 0);
        panel.addView(recordingColumn, recordingParams);

        statusText = new TextView(context);
        statusText.setText(getString(R.string.voice_recording));
        statusText.setSingleLine(true);
        statusText.setEllipsize(TextUtils.TruncateAt.END);
        statusText.setIncludeFontPadding(false);
        statusText.setTextColor(colorOnSurfaceVariant);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(statusText, 9, 12, 1, TypedValue.COMPLEX_UNIT_SP);
        recordingColumn.addView(statusText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(22)));

        waveformView = new VoiceWaveformView(context);
        waveformView.setColors(colorPrimary, adjustAlpha(colorOnSurfaceVariant, 0.42f));
        recordingColumn.addView(waveformView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(22)));

        trimView = new VoiceTrimView(context);
        trimView.setColors(colorPrimary, adjustAlpha(colorOnSurfaceVariant, 0.42f), colorOnSurface, colorOnSurfaceVariant, colorError);
        trimView.setVisibility(View.GONE);
        trimView.setListener(new VoiceTrimView.Listener() {
            @Override
            public void onTrimChanged(long startMs, long endMs) {
                callback.onTrimChanged(startMs, endMs);
            }

            @Override
            public void onSeekChanged(long positionMs) {
                callback.onTrimSeekChanged(positionMs);
            }
        });
        recordingColumn.addView(trimView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));

        hintText = new TextView(context);
        hintText.setText(getString(R.string.voice_swipe_right_cancel));
        hintText.setSingleLine(true);
        hintText.setEllipsize(TextUtils.TruncateAt.END);
        hintText.setIncludeFontPadding(false);
        hintText.setTextColor(colorOnSurfaceVariant);
        hintText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(hintText, 9, 12, 1, TypedValue.COMPLEX_UNIT_SP);
        recordingColumn.addView(hintText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));

        actionButtons = new LinearLayout(context);
        actionButtons.setOrientation(LinearLayout.HORIZONTAL);
        actionButtons.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(actionButtons, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        deleteButton = makeIconButton(R.drawable.ic_delete, colorError, colorSurfaceVariant);
        deleteButton.setOnClickListener(v -> callback.onDeleteClicked());
        actionButtons.addView(deleteButton, new LinearLayout.LayoutParams(dp(42), dp(42)));

        trimButton = makeIconButton(R.drawable.ic_content_cut, colorOnSurface, colorSurfaceVariant);
        trimButton.setOnClickListener(v -> callback.onTrimClicked());
        LinearLayout.LayoutParams trimParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        trimParams.setMargins(dp(4), 0, 0, 0);
        actionButtons.addView(trimButton, trimParams);

        previewPlayButton = makeIconButton(R.drawable.ic_play_arrow, colorOnSurface, colorSurfaceVariant);
        previewPlayButton.setOnClickListener(v -> callback.onPreviewPlayClicked());
        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        playParams.setMargins(dp(4), 0, 0, 0);
        actionButtons.addView(previewPlayButton, playParams);

        sendButton = makeIconButton(R.drawable.ic_send_telegram, colorOnPrimary, colorPrimary);
        sendButton.setOnClickListener(v -> callback.onSendClicked());
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        sendParams.setMargins(dp(5), 0, 0, 0);
        actionButtons.addView(sendButton, sendParams);
    }

    private boolean handleLockButtonTouch(View view, MotionEvent event) {
        if (!visible || trimMode) {
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!locked) {
                    return true;
                }

                lockGestureSession = true;
                unlockDragTriggered = false;
                unlockCancelTriggered = false;
                lockButtonTouchStartY = event.getRawY();
                lockButtonTouchStartX = event.getRawX();
                unlockSwipeRightStartX = event.getRawX();
                view.getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!lockGestureSession) {
                    return true;
                }

                float dy = event.getRawY() - lockButtonTouchStartY;

                if (!unlockDragTriggered && dy >= dp(UNLOCK_DRAG_DOWN_DP)) {
                    unlockDragTriggered = true;
                    unlockSwipeRightStartX = event.getRawX();
                    animateButtonCluster(false);
                    callback.onUnlockSwipeDown();
                    return true;
                }

                if (unlockDragTriggered && !unlockCancelTriggered) {
                    float dxAfterUnlock = event.getRawX() - unlockSwipeRightStartX;
                    float cancelProgress = Math.max(0f, Math.min(1f, dxAfterUnlock / dp(UNLOCK_CANCEL_RIGHT_DP)));
                    setCancelProgress(cancelProgress);

                    if (cancelProgress >= 1f) {
                        unlockCancelTriggered = true;
                        callback.onUnlockCancelSwipeRight();
                    }
                }

                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                view.getParent().requestDisallowInterceptTouchEvent(false);

                if (lockGestureSession && unlockDragTriggered && !unlockCancelTriggered) {
                    callback.onUnlockReleased();
                }

                lockGestureSession = false;
                unlockDragTriggered = false;
                unlockCancelTriggered = false;
                return true;

            default:
                return true;
        }
    }

    private void updateLockAnchorPosition() {
        if (overlayRoot == null || lockButton == null || lockArrowsText == null || lockAnchorView == null) {
            return;
        }

        int[] rootLocation = new int[2];
        int[] anchorLocation = new int[2];
        root.getLocationOnScreen(rootLocation);
        lockAnchorView.getLocationOnScreen(anchorLocation);

        int anchorCenterX = anchorLocation[0] - rootLocation[0] + lockAnchorView.getWidth() / 2;
        int anchorBottomY = anchorLocation[1] - rootLocation[1] + lockAnchorView.getHeight();
        int rootHeight = Math.max(1, root.getHeight());

        int lockSize = dp(58);
        int left = Math.max(dp(10), Math.min(root.getWidth() - lockSize - dp(10), anchorCenterX - lockSize / 2));
        int lockBottom = Math.max(dp(166), rootHeight - anchorBottomY + dp(138));
        int arrowsBottom = Math.max(dp(92), rootHeight - anchorBottomY + dp(70));

        FrameLayout.LayoutParams lockParams = (FrameLayout.LayoutParams) lockButton.getLayoutParams();
        lockParams.gravity = Gravity.BOTTOM | Gravity.START;
        lockParams.setMargins(left, 0, 0, lockBottom);
        lockButton.setLayoutParams(lockParams);

        FrameLayout.LayoutParams arrowParams = (FrameLayout.LayoutParams) lockArrowsText.getLayoutParams();
        arrowParams.gravity = Gravity.BOTTOM | Gravity.START;
        arrowParams.setMargins(left, 0, 0, arrowsBottom);
        lockArrowsText.setLayoutParams(arrowParams);
    }

    private void applyUnlockedRecordingState() {
        micDot.setVisibility(View.VISIBLE);
        timerText.setVisibility(View.VISIBLE);
        setCompactActionButtons(false);
        setTimerWidth(dp(50));
        animateButtonCluster(false);
        hintText.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        waveformView.setVisibility(View.VISIBLE);
        trimView.setVisibility(View.GONE);
        showLockControls(true);
        lockButton.setClickable(true);
        startUpArrowAnimation();
    }

    private void applyLockedRecordingState() {
        micDot.setVisibility(View.VISIBLE);
        timerText.setVisibility(View.VISIBLE);
        setCompactActionButtons(false);
        setTimerWidth(dp(50));
        deleteButton.setVisibility(View.VISIBLE);
        trimButton.setVisibility(View.VISIBLE);
        previewPlayButton.setVisibility(View.GONE);
        sendButton.setVisibility(View.VISIBLE);
        animateButtonCluster(true);
        hintText.setVisibility(View.VISIBLE);
        waveformView.setVisibility(View.VISIBLE);
        trimView.setVisibility(View.GONE);
        showLockControls(true);
        lockButton.setClickable(true);
        startDownArrowAnimation();
    }

    private void applyTrimDraftState(boolean previewPlaying) {
        deleteButton.setVisibility(View.VISIBLE);
        trimButton.setVisibility(View.GONE);
        previewPlayButton.setVisibility(View.VISIBLE);
        sendButton.setVisibility(View.VISIBLE);
        previewPlayButton.setImageResource(previewPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        animateButtonCluster(true);

        micDot.setVisibility(View.GONE);
        timerText.setVisibility(View.VISIBLE);
        waveformView.setVisibility(View.GONE);
        trimView.setVisibility(View.VISIBLE);

        setCompactActionButtons(true);
        setTimerWidth(dp(92));
        showLockControls(false);
    }


    private boolean handleOverlayRootTouch(View view, MotionEvent event) {
        if (!visible) {
            return false;
        }

        // While the recorder is visible the overlay intentionally consumes background touches:
        // this prevents the messages list from scrolling under the recorder. Unlocking is handled
        // only by the lock button itself, so random vertical gestures on the chat cannot send the voice.
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && view.getParent() != null) {
            view.getParent().requestDisallowInterceptTouchEvent(true);
        } else if ((event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL)
                && view.getParent() != null) {
            view.getParent().requestDisallowInterceptTouchEvent(false);
        }

        return true;
    }

    private void animateButtonCluster(boolean show) {
        if (actionButtons == null) {
            return;
        }

        actionButtons.animate().cancel();

        if (show) {
            if (actionButtons.getVisibility() != View.VISIBLE) {
                actionButtons.setVisibility(View.VISIBLE);
                actionButtons.setAlpha(0f);
                actionButtons.setScaleX(0.92f);
                actionButtons.setScaleY(0.92f);
            }

            actionButtons.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(ANIMATION_MS)
                    .setListener(null)
                    .start();
        } else {
            if (actionButtons.getVisibility() != View.VISIBLE) {
                actionButtons.setAlpha(0f);
                return;
            }

            actionButtons.animate()
                    .alpha(0f)
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(ANIMATION_MS)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (!locked && !trimMode && actionButtons != null) {
                                actionButtons.setVisibility(View.GONE);
                            }
                        }
                    })
                    .start();
        }
    }

    private void showLockControls(boolean show) {
        if (lockButton == null || lockArrowsText == null) {
            return;
        }

        lockButton.animate().cancel();
        lockArrowsText.animate().cancel();

        if (show) {
            lockButton.setVisibility(View.VISIBLE);
            lockArrowsText.setVisibility(View.VISIBLE);
            lockButton.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(ANIMATION_MS).start();
            lockArrowsText.animate().alpha(0.72f).setDuration(ANIMATION_MS).start();
        } else {
            stopLockArrowAnimation();
            lockButton.animate()
                    .alpha(0f)
                    .scaleX(0.86f)
                    .scaleY(0.86f)
                    .setDuration(ANIMATION_MS)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (trimMode && lockButton != null) {
                                lockButton.setVisibility(View.GONE);
                            }
                        }
                    })
                    .start();
            lockArrowsText.animate()
                    .alpha(0f)
                    .setDuration(ANIMATION_MS)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (trimMode && lockArrowsText != null) {
                                lockArrowsText.setVisibility(View.GONE);
                            }
                        }
                    })
                    .start();
        }
    }

    private void setTimerWidth(int widthPx) {
        ViewGroup.LayoutParams params = timerText.getLayoutParams();
        if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).width = widthPx;
            timerText.setLayoutParams(params);
        }
    }

    private void setCompactActionButtons(boolean compact) {
        int normal = dp(42);
        int compactSize = dp(38);
        int sendNormal = dp(48);
        int sendCompact = dp(44);

        setButtonSize(deleteButton, compact ? compactSize : normal, compact ? compactSize : normal);
        setButtonSize(trimButton, compact ? compactSize : normal, compact ? compactSize : normal);
        setButtonSize(previewPlayButton, compact ? compactSize : normal, compact ? compactSize : normal);
        setButtonSize(sendButton, compact ? sendCompact : sendNormal, compact ? sendCompact : sendNormal);
    }

    private void setButtonSize(View button, int width, int height) {
        if (button == null) {
            return;
        }

        ViewGroup.LayoutParams params = button.getLayoutParams();
        if (params instanceof LinearLayout.LayoutParams) {
            params.width = width;
            params.height = height;
            button.setLayoutParams(params);
        }
    }

    private ImageButton makeIconButton(int iconRes, int iconTint, int backgroundColor) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(iconRes);
        button.setImageTintList(ColorStateList.valueOf(iconTint));
        button.setBackground(makeOvalBg(backgroundColor, 0, 0));
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setScaleType(ImageButton.ScaleType.CENTER);
        return button;
    }

    private void setLockedIconState() {
        lockButton.setScaleX(1.12f);
        lockButton.setScaleY(1.12f);
        lockButton.setAlpha(1f);
        lockButton.setBackground(makeOvalBg(colorPrimary, colorPrimary, dp(1)));
        lockButton.setImageTintList(ColorStateList.valueOf(colorOnPrimary));
    }

    private void startUpArrowAnimation() {
        startArrowAnimation(false);
    }

    private void startDownArrowAnimation() {
        startArrowAnimation(true);
    }

    private void startArrowAnimation(boolean down) {
        if (lockArrowsText == null || !visible || trimMode) {
            return;
        }

        stopLockArrowAnimation();
        lockArrowsText.setText(down ? "⌄\n⌄\n⌄" : "⌃\n⌃\n⌃");
        lockArrowsText.setTextColor(colorOnSurfaceVariant);
        lockArrowsText.setTranslationX(0f);
        lockArrowsText.setAlpha(0.72f);

        float from = down ? -dp(7) : dp(12);
        float to = down ? dp(12) : -dp(6);

        lockArrowsAnimator = ObjectAnimator.ofFloat(lockArrowsText, View.TRANSLATION_Y, from, to);
        lockArrowsAnimator.setDuration(760L);
        lockArrowsAnimator.setInterpolator(new LinearInterpolator());
        lockArrowsAnimator.setRepeatCount(ValueAnimator.INFINITE);
        lockArrowsAnimator.setRepeatMode(ValueAnimator.RESTART);
        lockArrowsAnimator.start();
    }

    private void stopLockArrowAnimation() {
        if (lockArrowsAnimator != null) {
            lockArrowsAnimator.cancel();
            lockArrowsAnimator = null;
        }
    }

    private GradientDrawable makeRoundBg(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(radius);

        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }

        return drawable;
    }

    private GradientDrawable makeOvalBg(int color, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);

        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }

        return drawable;
    }

    private void resolveColors() {
        colorSurface = resolveAttr(com.google.android.material.R.attr.colorSurface, Color.rgb(34, 34, 34));
        colorSurfaceVariant = resolveAttr(com.google.android.material.R.attr.colorSurfaceVariant, Color.rgb(55, 55, 55));
        colorOnSurface = resolveAttr(com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
        colorOnSurfaceVariant = resolveAttr(com.google.android.material.R.attr.colorOnSurfaceVariant, Color.LTGRAY);
        colorPrimary = resolveAttr(com.google.android.material.R.attr.colorPrimary, Color.rgb(32, 163, 154));
        colorOnPrimary = resolveAttr(com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
        colorError = resolveAttr(com.google.android.material.R.attr.colorError, Color.rgb(244, 67, 54));
    }

    private int resolveAttr(@AttrRes int attr, int fallback) {
        TypedValue value = new TypedValue();
        boolean found = context.getTheme().resolveAttribute(attr, value, true);

        if (!found) {
            return fallback;
        }

        if (value.resourceId != 0) {
            try {
                return androidx.core.content.ContextCompat.getColor(context, value.resourceId);
            } catch (Exception ignored) {
                return fallback;
            }
        }

        return value.data != 0 ? value.data : fallback;
    }

    private int adjustAlpha(int color, float alpha) {
        int a = Math.min(255, Math.max(0, (int) (Color.alpha(color) * alpha)));
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    private String getString(int resId) {
        return context.getString(resId);
    }

    private String formatDuration(long durationMs) {
        long totalSeconds = Math.max(0L, durationMs / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
