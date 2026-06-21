package com.example.aichat.view.main.chat.helpers;

import android.content.pm.PackageManager;
import android.Manifest;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.model.utils.media.audio.AudioTrimUtils;
import com.example.aichat.model.utils.media.recording.RecordedChatMedia;
import com.example.aichat.model.utils.media.voice.VoiceMessageRecorder;
import com.example.aichat.R;
import java.io.File;

public class ChatMediaInputController {

    private static final String TAG = "ChatMediaInputController";
    private static final int REQUEST_RECORD_AUDIO = 8101;
    private static final int REQUEST_CAMERA_AND_AUDIO = 8102;

    private static final long TICK_INTERVAL_MS = 80L;
    private static final long INPUT_ANIMATION_MS = 180L;
    private static final float CANCEL_RIGHT_DISTANCE_DP = 42f;
    private static final float LOCK_UP_DISTANCE_DP = 86f;
    private static final float UNLOCK_DOWN_DISTANCE_DP = 58f;

    public interface Listener {
        void onVoiceRecorded(@NonNull RecordedChatMedia media);

        void onVideoCircleRequested();

        void onRecordingStarted();

        void onRecordingCanceled();

        void onRecordingTooShort();
    }

    private final Fragment fragment;
    private final Listener listener;
    private final VoiceMessageRecorder voiceRecorder;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private View rootView;
    private View inputPanel;
    private RecyclerView messagesRecyclerView;
    private ImageButton voiceButton;
    private ImageButton videoCircleButton;
    private VoiceRecordingOverlay voiceOverlay;

    private MediaPlayer previewPlayer;
    private RecordedChatMedia draftVoice;

    private boolean fingerMovedToCancel = false;
    private boolean recordingActive = false;
    private boolean recordingLocked = false;
    private boolean recordingStoppedForTrim = false;
    private boolean inputHiddenForVoice = false;
    private boolean recyclerScrollDisabledForVoice = false;
    private boolean previousRecyclerNestedScrollingEnabled = true;
    private boolean recyclerBottomPaddingReservedForVoice = false;
    private int previousRecyclerPaddingLeft = 0;
    private int previousRecyclerPaddingTop = 0;
    private int previousRecyclerPaddingRight = 0;
    private int previousRecyclerPaddingBottom = 0;
    private boolean lockSwipeTriggeredFromOverlay = false;

    private float startX = 0f;
    private float startY = 0f;
    private float lockGestureStartY = 0f;
    private float cancelDistancePx = 0f;
    private float lockDistancePx = 0f;
    private float unlockDownDistancePx = 0f;

    private long trimStartMs = 0L;
    private long trimEndMs = 0L;
    private long trimPlaybackPositionMs = 0L;

    private final Runnable voiceTickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!recordingActive || recordingStoppedForTrim) {
                return;
            }

            long duration = voiceRecorder.getDurationMs();
            int amplitude = voiceRecorder.getMaxAmplitude();

            if (voiceOverlay != null) {
                voiceOverlay.update(duration, amplitude);
            }

            if (duration >= voiceRecorder.getMaxDurationMs()) {
                finishVoiceRecordingImmediately();
                return;
            }

            handler.postDelayed(this, TICK_INTERVAL_MS);
        }
    };

    private final Runnable previewTickRunnable = new Runnable() {
        @Override
        public void run() {
            if (previewPlayer == null || draftVoice == null || voiceOverlay == null) {
                return;
            }

            int current = previewPlayer.getCurrentPosition();
            trimPlaybackPositionMs = current;
            voiceOverlay.setTrimPlaybackPosition(current);

            if (previewPlayer.isPlaying() && trimEndMs > 0L && current >= trimEndMs) {
                stopPreviewPlayer(false);
                if (voiceOverlay != null) {
                    voiceOverlay.setPreviewPlaying(false);
                    trimPlaybackPositionMs = trimStartMs;
                    voiceOverlay.setTrimPlaybackPosition(trimStartMs);
                }
                return;
            }

            if (previewPlayer != null) {
                handler.postDelayed(this, 40L);
            }
        }
    };

    public ChatMediaInputController(
            @NonNull Fragment fragment,
            @NonNull Listener listener
    ) {
        this.fragment = fragment;
        this.listener = listener;
        this.voiceRecorder = new VoiceMessageRecorder(fragment.requireContext());
    }

    public void bind(@NonNull View root) {
        rootView = root;
        inputPanel = root.findViewById(R.id.input_panel);
        messagesRecyclerView = root.findViewById(R.id.rv_messages);
        voiceButton = root.findViewById(R.id.btn_record_voice);
        videoCircleButton = root.findViewById(R.id.btn_record_video_circle);

        cancelDistancePx = dp(CANCEL_RIGHT_DISTANCE_DP);
        lockDistancePx = dp(LOCK_UP_DISTANCE_DP);
        unlockDownDistancePx = dp(UNLOCK_DOWN_DISTANCE_DP);

        try {
            voiceOverlay = new VoiceRecordingOverlay(
                    fragment.requireContext(),
                    root,
                    new VoiceRecordingOverlay.Callback() {
                        @Override
                        public void onDeleteClicked() {
                            cancelVoiceRecording();
                        }

                        @Override
                        public void onTrimClicked() {
                            stopVoiceForTrim();
                        }

                        @Override
                        public void onPreviewPlayClicked() {
                            toggleDraftPreviewPlayback();
                        }

                        @Override
                        public void onSendClicked() {
                            sendCurrentVoice();
                        }

                        @Override
                        public void onTrimChanged(long startMs, long endMs) {
                            trimStartMs = Math.max(0L, startMs);
                            trimEndMs = Math.max(trimStartMs, endMs);

                            if (trimPlaybackPositionMs < trimStartMs || trimPlaybackPositionMs > trimEndMs) {
                                trimPlaybackPositionMs = trimStartMs;
                            }

                            if (previewPlayer != null && recordingStoppedForTrim) {
                                int current = previewPlayer.getCurrentPosition();
                                if (current < trimStartMs || current > trimEndMs) {
                                    try {
                                        previewPlayer.seekTo((int) trimPlaybackPositionMs);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }

                            if (voiceOverlay != null) {
                                voiceOverlay.setTrimPlaybackPosition(trimPlaybackPositionMs);
                            }
                        }

                        @Override
                        public void onTrimSeekChanged(long positionMs) {
                            trimPlaybackPositionMs = Math.max(trimStartMs, Math.min(positionMs, trimEndMs));

                            if (previewPlayer != null && recordingStoppedForTrim) {
                                try {
                                    previewPlayer.seekTo((int) trimPlaybackPositionMs);
                                } catch (Exception ignored) {
                                }
                            }

                            if (voiceOverlay != null) {
                                voiceOverlay.setTrimPlaybackPosition(trimPlaybackPositionMs);
                            }
                        }

                        @Override
                        public void onUnlockSwipeDown() {
                            unlockVoiceRecordingBySwipeDown();
                        }

                        @Override
                        public void onUnlockReleased() {
                            if (lockSwipeTriggeredFromOverlay
                                    && recordingActive
                                    && !recordingLocked
                                    && !recordingStoppedForTrim) {
                                finishVoiceRecordingImmediately();
                            }
                        }

                        @Override
                        public void onUnlockCancelSwipeRight() {
                            if (recordingActive && !recordingStoppedForTrim) {
                                cancelVoiceRecording();
                            }
                        }
                    }
            );
            voiceOverlay.setLockAnchorView(voiceButton);
        } catch (Exception exception) {
            Log.e(TAG, "Failed to create voice overlay", exception);
            voiceOverlay = null;
        }

        Log.d(
                TAG,
                "bind: voiceButton=" + (voiceButton != null)
                        + ", videoCircleButton=" + (videoCircleButton != null)
                        + ", inputPanel=" + (inputPanel != null)
        );

        if (voiceButton != null) {
            prepareButton(voiceButton);
            voiceButton.setOnTouchListener(this::onVoiceTouch);
        }

        if (videoCircleButton != null) {
            prepareButton(videoCircleButton);
            videoCircleButton.setOnTouchListener(null);
            videoCircleButton.setOnClickListener(v -> openVideoCircle());
        }
    }

    private void prepareButton(@NonNull ImageButton button) {
        button.setClickable(true);
        button.setFocusable(true);
        button.setEnabled(true);
        button.bringToFront();
        button.setAlpha(1f);
    }

    public void cancelActiveRecordingBecauseChatLeft() {
        if (!recordingActive && !recordingStoppedForTrim && draftVoice == null) {
            return;
        }

        handler.removeCallbacksAndMessages(null);
        stopPreviewPlayer(false);

        if (recordingActive && !recordingStoppedForTrim) {
            voiceRecorder.cancel();
        }

        deleteDraftFile();
        resetVoiceState();

        if (voiceOverlay != null) {
            voiceOverlay.hide();
        }

        showInputPanelAfterVoice(false);
        restoreRecyclerScrollAfterVoice();
    }

    public void destroy() {
        handler.removeCallbacksAndMessages(null);
        stopPreviewPlayer(false);
        voiceRecorder.cancel();
        deleteDraftFile();
        recordingActive = false;
        recordingLocked = false;
        recordingStoppedForTrim = false;
        fingerMovedToCancel = false;
        showInputPanelAfterVoice(false);
        restoreRecyclerScrollAfterVoice();

        if (voiceOverlay != null) {
            voiceOverlay.destroy();
            voiceOverlay = null;
        }
    }

    private boolean onVoiceTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                requestParentDisallowIntercept(view, true);
                disableRecyclerScrollForVoice();
                startX = event.getRawX();
                startY = event.getRawY();
                lockGestureStartY = startY;
                fingerMovedToCancel = false;
                lockSwipeTriggeredFromOverlay = false;
                recordingLocked = false;
                recordingStoppedForTrim = false;
                recordingActive = startVoiceRecording();
                view.setPressed(recordingActive);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!recordingActive || recordingStoppedForTrim) {
                    return true;
                }

                float dx = event.getRawX() - startX;
                float dy = event.getRawY() - startY;

                if (recordingLocked) {
                    handleLockedMove(event.getRawX() - startX, event.getRawY() - lockGestureStartY);
                    return true;
                }

                handleUnlockedMove(view, dx, dy, event.getRawY());
                return true;

            case MotionEvent.ACTION_UP:
                view.setPressed(false);
                view.setAlpha(1f);
                requestParentDisallowIntercept(view, false);

                if (!recordingActive || recordingStoppedForTrim) {
                    return true;
                }

                if (recordingLocked) {
                    return true;
                }

                if (fingerMovedToCancel) {
                    cancelVoiceRecording();
                } else {
                    finishVoiceRecordingImmediately();
                }

                return true;

            case MotionEvent.ACTION_CANCEL:
                view.setPressed(false);
                view.setAlpha(1f);
                requestParentDisallowIntercept(view, false);

                if (recordingActive && !recordingLocked && !recordingStoppedForTrim) {
                    cancelVoiceRecording();
                }

                return true;

            default:
                return true;
        }
    }

    private void handleUnlockedMove(@NonNull View view, float dx, float dy, float rawY) {
        boolean vertical = Math.abs(dy) > Math.abs(dx) * 0.70f;
        boolean horizontal = Math.abs(dx) > Math.abs(dy) * 0.70f;
        boolean lockSwipe = vertical && dy < 0f;
        boolean cancelSwipe = horizontal && dx > 0f;

        float lockProgress = lockSwipe
                ? Math.min(1f, Math.max(0f, -dy / lockDistancePx))
                : 0f;

        float cancelProgress = cancelSwipe
                ? Math.min(1f, Math.max(0f, dx / cancelDistancePx))
                : 0f;

        if (voiceOverlay != null) {
            voiceOverlay.setLockProgress(lockProgress);
            voiceOverlay.setCancelProgress(cancelProgress);
        }

        if (lockSwipe && lockProgress >= 1f) {
            lockGestureStartY = rawY;
            lockVoiceRecording();
            return;
        }

        fingerMovedToCancel = cancelProgress >= 1f;
        view.setAlpha(fingerMovedToCancel ? 0.45f : 1f);
    }

    private void handleLockedMove(float dx, float dyFromLockPoint) {
        boolean movedDown = dyFromLockPoint > unlockDownDistancePx
                && Math.abs(dyFromLockPoint) > Math.abs(dx) * 0.55f;

        if (!movedDown) {
            return;
        }

        unlockVoiceRecordingBySwipeDown();
    }

    private boolean startVoiceRecording() {
        if (!hasAudioPermission()) {
            Log.w(TAG, "RECORD_AUDIO permission is missing");

            fragment.requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO
            );

            Toast.makeText(
                    fragment.requireContext(),
                    fragment.getString(R.string.voice_permission_microphone),
                    Toast.LENGTH_SHORT
            ).show();

            restoreRecyclerScrollAfterVoice();
            return false;
        }

        try {
            stopPreviewPlayer(false);
            deleteDraftFile();
            draftVoice = null;
            trimStartMs = 0L;
            trimEndMs = 0L;
            trimPlaybackPositionMs = 0L;
            voiceRecorder.start();
            hideInputPanelForVoice();

            if (voiceOverlay != null) {
                voiceOverlay.showRecording();
            }

            handler.removeCallbacks(voiceTickRunnable);
            handler.post(voiceTickRunnable);
            listener.onRecordingStarted();
            return true;
        } catch (Exception exception) {
            Log.e(TAG, "Failed to start voice recording", exception);
            showInputPanelAfterVoice(true);
            restoreRecyclerScrollAfterVoice();

            Toast.makeText(
                    fragment.requireContext(),
                    fragment.getString(R.string.voice_start_failed),
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }
    }

    private void lockVoiceRecording() {
        if (!recordingActive || recordingLocked || recordingStoppedForTrim) {
            return;
        }

        recordingLocked = true;
        fingerMovedToCancel = false;

        if (voiceButton != null) {
            voiceButton.setAlpha(1f);
            voiceButton.setPressed(false);
        }

        if (voiceOverlay != null) {
            voiceOverlay.setLocked();
        }
    }

    private void unlockVoiceRecordingBySwipeDown() {
        if (!recordingActive || !recordingLocked || recordingStoppedForTrim) {
            return;
        }

        recordingLocked = false;
        fingerMovedToCancel = false;
        lockSwipeTriggeredFromOverlay = true;

        if (voiceOverlay != null) {
            voiceOverlay.setUnlockedBySwipe();
        }
    }

    private void finishVoiceRecordingAfterUnlockSwipe() {
        if (!recordingActive || recordingStoppedForTrim) {
            return;
        }

        recordingLocked = false;
        fingerMovedToCancel = false;
        lockSwipeTriggeredFromOverlay = true;

        if (voiceOverlay != null) {
            voiceOverlay.setUnlockedBySwipe();
        }

        finishVoiceRecordingImmediately();
    }

    private void stopVoiceForTrim() {
        if (!recordingActive || recordingStoppedForTrim) {
            return;
        }

        handler.removeCallbacks(voiceTickRunnable);
        draftVoice = voiceRecorder.stopAndBuildResult();
        recordingStoppedForTrim = true;
        recordingActive = false;
        recordingLocked = true;
        fingerMovedToCancel = false;

        if (voiceButton != null) {
            voiceButton.setPressed(false);
            voiceButton.setAlpha(1f);
        }

        if (draftVoice == null) {
            resetVoiceState();
            if (voiceOverlay != null) {
                voiceOverlay.hide();
            }
            showInputPanelAfterVoice(true);
            restoreRecyclerScrollAfterVoice();
            listener.onRecordingTooShort();
            return;
        }

        trimStartMs = 0L;
        trimEndMs = draftVoice.getDurationMs();
        trimPlaybackPositionMs = trimStartMs;

        if (voiceOverlay != null) {
            voiceOverlay.showTrimDraft(draftVoice.getDurationMs(), false);
            voiceOverlay.setTrimPlaybackPosition(trimPlaybackPositionMs);
        }
    }

    private void toggleDraftPreviewPlayback() {
        if (draftVoice == null || draftVoice.getFile() == null || !draftVoice.getFile().exists()) {
            return;
        }

        try {
            if (previewPlayer != null && previewPlayer.isPlaying()) {
                previewPlayer.pause();
                if (voiceOverlay != null) {
                    voiceOverlay.setPreviewPlaying(false);
                }
                return;
            }

            stopPreviewPlayer(false);

            previewPlayer = new MediaPlayer();
            previewPlayer.setDataSource(draftVoice.getFile().getAbsolutePath());
            previewPlayer.setOnPreparedListener(player -> {
                int start = (int) Math.max(trimStartMs, Math.min(trimPlaybackPositionMs, trimEndMs > 0L ? trimEndMs : draftVoice.getDurationMs()));
                player.seekTo(start);
                player.start();
                if (voiceOverlay != null) {
                    voiceOverlay.setPreviewPlaying(true);
                    voiceOverlay.setTrimPlaybackPosition(start);
                }
                handler.removeCallbacks(previewTickRunnable);
                handler.post(previewTickRunnable);
            });
            previewPlayer.setOnCompletionListener(player -> {
                stopPreviewPlayer(false);
                if (voiceOverlay != null) {
                    voiceOverlay.setPreviewPlaying(false);
                    trimPlaybackPositionMs = trimStartMs;
                    voiceOverlay.setTrimPlaybackPosition(trimStartMs);
                }
            });
            previewPlayer.prepareAsync();
        } catch (Exception exception) {
            Log.e(TAG, "Cannot play draft voice", exception);
            stopPreviewPlayer(false);
        }
    }

    private void sendCurrentVoice() {
        if (recordingActive && !recordingStoppedForTrim) {
            finishVoiceRecordingImmediately();
            return;
        }

        sendDraftVoice();
    }

    private void finishVoiceRecordingImmediately() {
        if (!recordingActive) {
            return;
        }

        handler.removeCallbacks(voiceTickRunnable);

        RecordedChatMedia result = voiceRecorder.stopAndBuildResult();
        resetVoiceState();

        if (voiceOverlay != null) {
            voiceOverlay.hide();
        }

        showInputPanelAfterVoice(true);
        restoreRecyclerScrollAfterVoice();

        if (result == null) {
            Log.d(TAG, "voice recording too short or empty");
            listener.onRecordingTooShort();
            return;
        }

        Log.d(TAG, "voice recording finished: " + result.getFile().getAbsolutePath());
        listener.onVoiceRecorded(result);
    }

    private void sendDraftVoice() {
        if (draftVoice == null || draftVoice.getFile() == null || !draftVoice.getFile().exists()) {
            return;
        }

        stopPreviewPlayer(false);

        RecordedChatMedia toSend = draftVoice;
        long start = Math.max(0L, trimStartMs);
        long end = trimEndMs > 0L ? trimEndMs : draftVoice.getDurationMs();
        end = Math.min(end, draftVoice.getDurationMs());

        if (start > 0L || end < draftVoice.getDurationMs() - 60L) {
            try {
                File trimmed = AudioTrimUtils.trimM4a(
                        fragment.requireContext(),
                        draftVoice.getFile(),
                        start,
                        end
                );

                if (trimmed != null && trimmed.exists() && trimmed.length() > 0L) {
                    toSend = RecordedChatMedia.voice(trimmed, Math.max(0L, end - start));
                    if (!trimmed.equals(draftVoice.getFile())) {
                        boolean ignored = draftVoice.getFile().delete();
                    }
                }
            } catch (Exception exception) {
                Log.e(TAG, "Failed to trim voice. Sending original file", exception);
            }
        }

        draftVoice = null;
        resetVoiceState();

        if (voiceOverlay != null) {
            voiceOverlay.hide();
        }

        showInputPanelAfterVoice(true);
        restoreRecyclerScrollAfterVoice();
        listener.onVoiceRecorded(toSend);
    }

    private void cancelVoiceRecording() {
        Log.d(TAG, "voice recording canceled");
        handler.removeCallbacks(voiceTickRunnable);
        stopPreviewPlayer(false);

        if (recordingActive && !recordingStoppedForTrim) {
            voiceRecorder.cancel();
        }

        deleteDraftFile();
        resetVoiceState();

        if (voiceOverlay != null) {
            voiceOverlay.hide();
        }

        showInputPanelAfterVoice(true);
        restoreRecyclerScrollAfterVoice();
        listener.onRecordingCanceled();
    }

    private void resetVoiceState() {
        recordingActive = false;
        recordingLocked = false;
        recordingStoppedForTrim = false;
        fingerMovedToCancel = false;
        lockSwipeTriggeredFromOverlay = false;
        trimStartMs = 0L;
        trimEndMs = 0L;
        trimPlaybackPositionMs = 0L;

        if (voiceButton != null) {
            voiceButton.setPressed(false);
            voiceButton.setAlpha(1f);
        }

        if (voiceOverlay != null) {
            voiceOverlay.setLockProgress(0f);
            voiceOverlay.setCancelProgress(0f);
        }
    }

    private void deleteDraftFile() {
        if (draftVoice != null && draftVoice.getFile() != null && draftVoice.getFile().exists()) {
            boolean ignored = draftVoice.getFile().delete();
        }
        draftVoice = null;
    }

    private void stopPreviewPlayer(boolean restartTickIfRecording) {
        handler.removeCallbacks(previewTickRunnable);

        if (previewPlayer != null) {
            try {
                if (previewPlayer.isPlaying()) {
                    previewPlayer.stop();
                }
            } catch (Exception ignored) {
            }

            try {
                previewPlayer.release();
            } catch (Exception ignored) {
            }
        }

        previewPlayer = null;

        if (restartTickIfRecording && recordingActive && !recordingStoppedForTrim) {
            handler.post(voiceTickRunnable);
        }
    }

    private void hideInputPanelForVoice() {
        reserveMessagesBottomForVoice();

        if (inputPanel == null || inputHiddenForVoice) {
            return;
        }

        inputHiddenForVoice = true;
        inputPanel.animate().cancel();
        inputPanel.setVisibility(View.VISIBLE);
        inputPanel.setTranslationY(0f);
        inputPanel.setAlpha(1f);
        inputPanel.animate()
                .alpha(0f)
                .translationY(dp(18f))
                .setDuration(INPUT_ANIMATION_MS)
                .setListener(null)
                .start();
    }

    private void showInputPanelAfterVoice(boolean animated) {
        restoreMessagesBottomAfterVoice();

        if (inputPanel == null) {
            return;
        }

        inputHiddenForVoice = false;
        inputPanel.animate().cancel();
        inputPanel.setVisibility(View.VISIBLE);

        if (!animated) {
            inputPanel.setAlpha(1f);
            inputPanel.setTranslationY(0f);
            return;
        }

        inputPanel.setAlpha(0f);
        inputPanel.setTranslationY(dp(18f));
        inputPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(INPUT_ANIMATION_MS)
                .setListener(null)
                .start();
    }

    private void disableRecyclerScrollForVoice() {
        if (messagesRecyclerView == null || recyclerScrollDisabledForVoice) {
            return;
        }

        recyclerScrollDisabledForVoice = true;
        previousRecyclerNestedScrollingEnabled = messagesRecyclerView.isNestedScrollingEnabled();
        reserveMessagesBottomForVoice();
        messagesRecyclerView.setNestedScrollingEnabled(false);

        // Не используем suppressLayout(true): во время записи мы добавляем нижний отступ,
        // чтобы последние сообщения не оказывались под панелью диктофона. Полноэкранный
        // voice overlay сам перехватывает тачи и не даёт списку прокручиваться пальцем.
        if (rootView != null && rootView.getParent() != null) {
            rootView.getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    private void restoreRecyclerScrollAfterVoice() {
        restoreMessagesBottomAfterVoice();

        if (messagesRecyclerView == null || !recyclerScrollDisabledForVoice) {
            return;
        }

        recyclerScrollDisabledForVoice = false;
        messagesRecyclerView.setNestedScrollingEnabled(previousRecyclerNestedScrollingEnabled);

        if (rootView != null && rootView.getParent() != null) {
            rootView.getParent().requestDisallowInterceptTouchEvent(false);
        }
    }


    private void reserveMessagesBottomForVoice() {
        if (messagesRecyclerView == null || recyclerBottomPaddingReservedForVoice) {
            return;
        }

        recyclerBottomPaddingReservedForVoice = true;
        previousRecyclerPaddingLeft = messagesRecyclerView.getPaddingLeft();
        previousRecyclerPaddingTop = messagesRecyclerView.getPaddingTop();
        previousRecyclerPaddingRight = messagesRecyclerView.getPaddingRight();
        previousRecyclerPaddingBottom = messagesRecyclerView.getPaddingBottom();

        int extraBottomSpace = Math.round(dp(104f));
        messagesRecyclerView.setClipToPadding(false);
        messagesRecyclerView.setPadding(
                previousRecyclerPaddingLeft,
                previousRecyclerPaddingTop,
                previousRecyclerPaddingRight,
                previousRecyclerPaddingBottom + extraBottomSpace
        );

        messagesRecyclerView.post(() -> messagesRecyclerView.smoothScrollBy(0, extraBottomSpace));
    }

    private void restoreMessagesBottomAfterVoice() {
        if (messagesRecyclerView == null || !recyclerBottomPaddingReservedForVoice) {
            return;
        }

        recyclerBottomPaddingReservedForVoice = false;
        messagesRecyclerView.setPadding(
                previousRecyclerPaddingLeft,
                previousRecyclerPaddingTop,
                previousRecyclerPaddingRight,
                previousRecyclerPaddingBottom
        );
    }

    private void openVideoCircle() {
        if (!hasCameraPermission()) {
            Log.w(TAG, "CAMERA or RECORD_AUDIO permission is missing");

            fragment.requestPermissions(
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                    },
                    REQUEST_CAMERA_AND_AUDIO
            );

            Toast.makeText(
                    fragment.requireContext(),
                    fragment.getString(R.string.video_circle_permission_camera_microphone),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Log.d(TAG, "request video circle recording");
        listener.onVideoCircleRequested();
    }

    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(
                fragment.requireContext(),
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                fragment.requireContext(),
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED && hasAudioPermission();
    }

    private void requestParentDisallowIntercept(View view, boolean disallow) {
        View current = view;

        while (current != null && current.getParent() != null) {
            current.getParent().requestDisallowInterceptTouchEvent(disallow);

            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                break;
            }
        }
    }

    private float dp(float value) {
        return value * fragment.requireContext().getResources().getDisplayMetrics().density;
    }
}
