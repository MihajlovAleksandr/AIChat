package com.example.aichat.view.main.chat.helpers;

import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.Manifest;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import com.example.aichat.model.utils.media.recording.RecordedChatMedia;
import com.example.aichat.model.utils.media.ChatMediaMarkers;
import com.example.aichat.R;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Locale;

public class VideoCircleRecorderDialogFragment extends DialogFragment {

    public interface Callback {
        void onVideoCircleRecorded(@NonNull RecordedChatMedia media);

        void onVideoCircleCanceled();
    }

    private static final long MAX_DURATION_MS = 45_000L;
    private static final long MIN_DURATION_MS = 700L;
    private static final int MAX_DRAFTS = 5;

    private static final int AFTER_FINALIZE_NONE = 0;
    private static final int AFTER_FINALIZE_SWITCH_AND_CONTINUE_TAKE = 1;

    private Callback callback;

    private PreviewView previewView;
    private CircleVideoTextureView playbackView;
    private LinearLayout draftsContainer;
    private ImageButton btnCancel;
    private ImageButton btnSwitchCamera;
    private ImageButton btnRecord;
    private ImageButton btnStop;
    private ImageButton btnSend;
    private TextView tvTimer;
    private TextView tvHint;
    private TextView tvDraftCounter;
    private TextView tvDraftBadge;

    private ProcessCameraProvider cameraProvider;
    private VideoCapture<Recorder> videoCapture;
    private Recording currentRecording;
    private File outputFile;

    private final List<RecordedDraft> drafts = new ArrayList<>();
    private final List<RecordedSegment> currentTakeSegments = new ArrayList<>();
    private RecordedDraft selectedDraft;

    private int lensFacing = CameraSelector.LENS_FACING_FRONT;
    private int afterFinalizeAction = AFTER_FINALIZE_NONE;

    private long startedAtMs = 0L;
    private long pausedStartedAtMs = 0L;
    private long totalPausedMs = 0L;
    private long finalDurationMs = 0L;

    private boolean sent = false;
    private boolean finalizing = false;
    private boolean discardCurrentFile = false;
    private boolean finishAfterFinalize = false;
    private boolean recordingReady = false;
    private boolean paused = false;
    private boolean continuingAfterCameraSwitch = false;

    private final android.os.Handler timerHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (startedAtMs <= 0L || currentRecording == null || paused) {
                return;
            }

            long elapsed = calculateActiveDurationMs();
            updateTimer(elapsed);

            if (elapsed >= MAX_DURATION_MS) {
                stopRecording(false, AFTER_FINALIZE_NONE);
                return;
            }

            timerHandler.postDelayed(this, 250L);
        }
    };

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog == null) return;

        Window window = dialog.getWindow();
        if (window == null) return;

        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setGravity(Gravity.CENTER);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        WindowManager.LayoutParams params = window.getAttributes();
        params.dimAmount = 0.72f;
        window.setAttributes(params);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.dialog_video_circle_recorder, container, false);

        previewView = view.findViewById(R.id.video_circle_preview);
        playbackView = view.findViewById(R.id.video_circle_playback);
        draftsContainer = view.findViewById(R.id.video_circle_drafts);
        btnCancel = view.findViewById(R.id.btn_video_circle_cancel);
        btnSwitchCamera = view.findViewById(R.id.btn_video_circle_switch_camera);
        btnRecord = view.findViewById(R.id.btn_video_circle_record);
        btnStop = view.findViewById(R.id.btn_video_circle_stop);
        btnSend = view.findViewById(R.id.btn_video_circle_send);
        tvTimer = view.findViewById(R.id.tv_video_circle_timer);
        tvHint = view.findViewById(R.id.tv_video_circle_hint);
        tvDraftCounter = view.findViewById(R.id.tv_video_circle_draft_counter);
        tvDraftBadge = view.findViewById(R.id.tv_video_circle_draft_badge);

        setSendEnabled(false);
        setStopEnabled(false);
        setRecordEnabled(false);
        setSwitchEnabled(false);
        setDraftBadgeVisible(false);
        updateTimer(0L);
        updateDraftStrip();
        updateHint("Подготовка камеры…");

        btnCancel.setOnClickListener(v -> cancelAndDismiss());
        btnSwitchCamera.setOnClickListener(v -> switchCamera());

        btnRecord.setOnClickListener(v -> {
            if (finalizing || !recordingReady) return;

            if (currentRecording == null) {
                startRecording(false);
                return;
            }

            if (paused) {
                resumeRecording();
            } else {
                pauseRecording();
            }
        });

        btnStop.setOnClickListener(v -> stopRecording(false, AFTER_FINALIZE_NONE));
        btnSend.setOnClickListener(v -> finishWithResult());

        startCamera();
        return view;
    }

    private void startCamera() {
        if (!isAdded()) return;

        recordingReady = false;
        setRecordEnabled(false);
        setSwitchEnabled(false);

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());

        future.addListener(() -> {
            if (!isAdded() || getView() == null) return;

            try {
                cameraProvider = future.get();
                bindCameraUseCases();

                recordingReady = true;
                setRecordEnabled(true);
                setSwitchEnabled(true);
                updateHint(resolveCameraHint());
            } catch (Exception exception) {
                recordingReady = false;
                setRecordEnabled(false);
                setSwitchEnabled(false);

                Toast.makeText(
                        requireContext(),
                        "Не удалось открыть камеру",
                        Toast.LENGTH_SHORT
                ).show();

                dismissAllowingStateLoss();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null || previewView == null) return;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.LOWEST))
                .build();

        videoCapture = VideoCapture.withOutput(recorder);

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                getViewLifecycleOwner(),
                selector,
                preview,
                videoCapture
        );
    }

    private void switchCamera() {
        if (finalizing) return;

        if (currentRecording != null) {
            updateHint("Переключаем камеру и продолжаем этот же кружок…");
            stopRecording(false, AFTER_FINALIZE_SWITCH_AND_CONTINUE_TAKE);
            return;
        }

        flipLensFacing();

        try {
            bindCameraUseCases();
            showCameraPreview();
            updateHint(resolveCameraHint());
        } catch (Exception exception) {
            Toast.makeText(requireContext(), "Не удалось сменить камеру", Toast.LENGTH_SHORT).show();
        }
    }

    private void flipLensFacing() {
        lensFacing = lensFacing == CameraSelector.LENS_FACING_FRONT
                ? CameraSelector.LENS_FACING_BACK
                : CameraSelector.LENS_FACING_FRONT;
    }

    private void startRecording(boolean continuationAfterSwitch) {
        if (!recordingReady || videoCapture == null || finalizing) return;

        if (!continuationAfterSwitch && drafts.size() >= MAX_DRAFTS) {
            Toast.makeText(requireContext(), "Можно сохранить максимум 5 кружков", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasRequiredPermissions()) {
            Toast.makeText(requireContext(), "Нет разрешений на камеру и микрофон", Toast.LENGTH_SHORT).show();
            return;
        }

        stopPreviewPlayback();
        showCameraPreview();
        setDraftBadgeVisible(false);

        if (!continuationAfterSwitch) {
            deleteSegmentsQuietly(currentTakeSegments);
            currentTakeSegments.clear();
        }

        outputFile = new File(requireContext().getCacheDir(), ChatMediaMarkers.buildCircleVideoFileName());

        FileOutputOptions outputOptions = new FileOutputOptions.Builder(outputFile).build();
        PendingRecording pendingRecording = videoCapture.getOutput()
                .prepareRecording(requireContext(), outputOptions);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            pendingRecording = pendingRecording.withAudioEnabled();
        }

        startedAtMs = System.currentTimeMillis();
        pausedStartedAtMs = 0L;
        totalPausedMs = 0L;
        finalDurationMs = 0L;
        paused = false;
        finalizing = false;
        discardCurrentFile = false;
        finishAfterFinalize = false;
        afterFinalizeAction = AFTER_FINALIZE_NONE;
        continuingAfterCameraSwitch = continuationAfterSwitch;

        currentRecording = pendingRecording.start(
                ContextCompat.getMainExecutor(requireContext()),
                this::handleRecordEvent
        );

        selectedDraft = null;
        updateDraftStrip();
        setSendEnabled(false);
        setStopEnabled(true);
        setSwitchEnabled(true);
        setRecordIconPause();
        updateHint("Идёт запись. Смену камеры сохраним внутри этого же кружка");
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);
    }

    private void handleRecordEvent(@NonNull VideoRecordEvent event) {
        if (event instanceof VideoRecordEvent.Finalize) {
            handleRecordingFinalized((VideoRecordEvent.Finalize) event);
        }
    }

    private void pauseRecording() {
        if (currentRecording == null || paused || finalizing) return;

        try {
            currentRecording.pause();
            paused = true;
            pausedStartedAtMs = System.currentTimeMillis();
            timerHandler.removeCallbacks(timerRunnable);
            setRecordIconResume();
            updateHint("Пауза. Нажмите ещё раз, чтобы продолжить запись");
        } catch (Exception ignored) {
        }
    }

    private void resumeRecording() {
        if (currentRecording == null || !paused || finalizing) return;

        try {
            currentRecording.resume();
            paused = false;

            if (pausedStartedAtMs > 0L) {
                totalPausedMs += System.currentTimeMillis() - pausedStartedAtMs;
                pausedStartedAtMs = 0L;
            }

            setRecordIconPause();
            updateHint("Идёт запись. Смену камеры сохраним внутри этого же кружка");
            timerHandler.removeCallbacks(timerRunnable);
            timerHandler.post(timerRunnable);
        } catch (Exception ignored) {
        }
    }

    private void stopRecording(boolean discard, int nextAction) {
        if (currentRecording == null || finalizing) return;

        finalDurationMs = calculateActiveDurationMs();
        discardCurrentFile = discard;
        afterFinalizeAction = nextAction;
        finalizing = true;
        timerHandler.removeCallbacks(timerRunnable);
        setRecordEnabled(false);
        setStopEnabled(false);
        setSwitchEnabled(false);
        updateHint(nextAction == AFTER_FINALIZE_SWITCH_AND_CONTINUE_TAKE
                ? "Переключаем камеру…"
                : "Сохраняем кружок…");

        try {
            currentRecording.stop();
        } catch (Exception exception) {
            finalizing = false;
            currentRecording = null;
            setRecordEnabled(true);
            setSwitchEnabled(true);
            updateHint(resolveCameraHint());
        }
    }

    private void handleRecordingFinalized(@NonNull VideoRecordEvent.Finalize event) {
        timerHandler.removeCallbacks(timerRunnable);

        File finalizedFile = outputFile;
        long duration = finalDurationMs > 0L ? finalDurationMs : calculateActiveDurationMs();
        int nextAction = afterFinalizeAction;

        currentRecording = null;
        outputFile = null;
        finalizing = false;
        paused = false;
        startedAtMs = 0L;
        pausedStartedAtMs = 0L;
        totalPausedMs = 0L;
        finalDurationMs = 0L;
        afterFinalizeAction = AFTER_FINALIZE_NONE;
        setRecordIconRecord();
        setStopEnabled(false);

        boolean hasError = event.hasError();
        boolean validFile = finalizedFile != null
                && finalizedFile.exists()
                && finalizedFile.length() > 0L
                && duration >= MIN_DURATION_MS;

        if (discardCurrentFile || hasError || !validFile) {
            deleteFileQuietly(finalizedFile);
            discardCurrentFile = false;

            if (hasError) {
                Toast.makeText(requireContext(), "Не удалось сохранить кружок", Toast.LENGTH_SHORT).show();
            } else if (!validFile && nextAction != AFTER_FINALIZE_SWITCH_AND_CONTINUE_TAKE) {
                Toast.makeText(requireContext(), "Запись слишком короткая", Toast.LENGTH_SHORT).show();
            }
        } else if (nextAction == AFTER_FINALIZE_SWITCH_AND_CONTINUE_TAKE) {
            currentTakeSegments.add(new RecordedSegment(finalizedFile, duration, lensFacing));
        } else {
            addCompletedTake(finalizedFile, duration);
        }

        if (finishAfterFinalize) {
            finishAfterFinalize = false;
            finishWithResult();
            return;
        }

        if (nextAction == AFTER_FINALIZE_SWITCH_AND_CONTINUE_TAKE) {
            flipLensFacing();

            try {
                bindCameraUseCases();
            } catch (Exception exception) {
                Toast.makeText(requireContext(), "Не удалось сменить камеру", Toast.LENGTH_SHORT).show();
            }

            setRecordEnabled(true);
            setSwitchEnabled(true);
            updateHint("Камера переключена. Продолжаем этот же кружок…");
            previewView.postDelayed(() -> startRecording(true), 220L);
            return;
        }

        continuingAfterCameraSwitch = false;
        setRecordEnabled(true);
        setSwitchEnabled(true);
        updateTimer(0L);

        if (selectedDraft != null) {
            updateHint("Кружок сохранён. Можно посмотреть, записать ещё или отправить выбранный");
            startPreviewPlayback(selectedDraft);
        } else {
            updateHint(resolveCameraHint());
        }
    }

    private void addCompletedTake(@NonNull File lastSegmentFile, long lastSegmentDurationMs) {
        List<RecordedSegment> takeSegments = new ArrayList<>(currentTakeSegments);
        takeSegments.add(new RecordedSegment(lastSegmentFile, lastSegmentDurationMs, lensFacing));
        currentTakeSegments.clear();

        File finalFile;
        long totalDuration = 0L;

        for (RecordedSegment segment : takeSegments) {
            totalDuration += Math.max(0L, segment.durationMs);
        }

        if (takeSegments.size() == 1) {
            finalFile = takeSegments.get(0).file;
        } else {
            finalFile = mergeSegmentsToSingleFile(takeSegments);
        }

        if (finalFile == null || !finalFile.exists() || finalFile.length() <= 0L) {
            Toast.makeText(requireContext(), "Не удалось собрать кружок после смены камеры", Toast.LENGTH_SHORT).show();
            deleteSegmentsQuietly(takeSegments);
            return;
        }

        for (RecordedSegment segment : takeSegments) {
            if (!finalFile.equals(segment.file)) {
                deleteFileQuietly(segment.file);
            }
        }

        addDraft(new RecordedDraft(finalFile, totalDuration, lensFacing));
    }

    @Nullable
    private File mergeSegmentsToSingleFile(@NonNull List<RecordedSegment> segments) {
        if (segments.isEmpty()) return null;
        if (segments.size() == 1) return segments.get(0).file;

        File output = new File(requireContext().getCacheDir(), ChatMediaMarkers.buildCircleVideoFileName());

        MediaMuxer muxer = null;

        try {
            TrackSpec firstVideo = findTrack(segments.get(0).file, "video/");
            TrackSpec firstAudio = findTrack(segments.get(0).file, "audio/");

            if (firstVideo == null) {
                return segments.get(segments.size() - 1).file;
            }

            muxer = new MediaMuxer(output.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int outVideoTrack = muxer.addTrack(firstVideo.format);
            int outAudioTrack = firstAudio != null ? muxer.addTrack(firstAudio.format) : -1;

            muxer.start();

            long videoOffsetUs = 0L;
            long audioOffsetUs = 0L;

            for (RecordedSegment segment : segments) {
                long durationUs = Math.max(1L, segment.durationMs) * 1000L;

                writeTrackSamples(segment.file, "video/", muxer, outVideoTrack, videoOffsetUs);

                if (outAudioTrack >= 0) {
                    writeTrackSamples(segment.file, "audio/", muxer, outAudioTrack, audioOffsetUs);
                }

                videoOffsetUs += durationUs;
                audioOffsetUs += durationUs;
            }

            return output;
        } catch (Exception exception) {
            deleteFileQuietly(output);
            return segments.get(segments.size() - 1).file;
        } finally {
            if (muxer != null) {
                try {
                    muxer.stop();
                } catch (Exception ignored) {
                }

                try {
                    muxer.release();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Nullable
    private TrackSpec findTrack(@NonNull File file, @NonNull String mimePrefix) {
        MediaExtractor extractor = new MediaExtractor();

        try {
            extractor.setDataSource(file.getAbsolutePath());

            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);

                if (mime != null && mime.startsWith(mimePrefix)) {
                    return new TrackSpec(i, format);
                }
            }
        } catch (Exception ignored) {
        } finally {
            try {
                extractor.release();
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private void writeTrackSamples(
            @NonNull File file,
            @NonNull String mimePrefix,
            @NonNull MediaMuxer muxer,
            int outTrack,
            long offsetUs
    ) throws Exception {
        if (outTrack < 0) return;

        MediaExtractor extractor = new MediaExtractor();

        try {
            extractor.setDataSource(file.getAbsolutePath());

            int inTrack = -1;

            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);

                if (mime != null && mime.startsWith(mimePrefix)) {
                    inTrack = i;
                    break;
                }
            }

            if (inTrack < 0) return;

            extractor.selectTrack(inTrack);

            ByteBuffer buffer = ByteBuffer.allocate(4 * 1024 * 1024);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            while (true) {
                int sampleSize = extractor.readSampleData(buffer, 0);

                if (sampleSize < 0) {
                    break;
                }

                info.offset = 0;
                info.size = sampleSize;
                info.flags = extractor.getSampleFlags();
                info.presentationTimeUs = Math.max(0L, extractor.getSampleTime()) + offsetUs;

                muxer.writeSampleData(outTrack, buffer, info);
                extractor.advance();
            }
        } finally {
            try {
                extractor.release();
            } catch (Exception ignored) {
            }
        }
    }

    private void addDraft(@NonNull RecordedDraft draft) {
        if (drafts.size() >= MAX_DRAFTS) {
            deleteFileQuietly(draft.file);
            Toast.makeText(requireContext(), "Можно сохранить максимум 5 кружков", Toast.LENGTH_SHORT).show();
            return;
        }

        drafts.add(draft);
        selectedDraft = draft;
        updateDraftStrip();
        setSendEnabled(true);
    }

    private void updateDraftStrip() {
        if (draftsContainer == null) return;

        draftsContainer.removeAllViews();

        if (tvDraftCounter != null) {
            tvDraftCounter.setText(String.format(Locale.getDefault(), "%d/%d", drafts.size(), MAX_DRAFTS));
        }

        for (int i = 0; i < drafts.size(); i++) {
            RecordedDraft draft = drafts.get(i);
            View item = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_video_circle_draft, draftsContainer, false);

            TextView number = item.findViewById(R.id.video_circle_draft_number);
            View selected = item.findViewById(R.id.video_circle_draft_selected);

            if (number != null) {
                number.setText(String.valueOf(i + 1));
            }

            if (selected != null) {
                selected.setVisibility(draft == selectedDraft ? View.VISIBLE : View.GONE);
            }

            item.setOnClickListener(v -> {
                if (currentRecording != null || finalizing) {
                    Toast.makeText(requireContext(), "Сначала остановите запись", Toast.LENGTH_SHORT).show();
                    return;
                }

                selectedDraft = draft;
                updateDraftStrip();
                setSendEnabled(true);
                startPreviewPlayback(draft);
                updateHint("Просмотр черновика. Одновременно проигрывается только один вариант");
            });

            draftsContainer.addView(item);
        }
    }

    private void startPreviewPlayback(@NonNull RecordedDraft draft) {
        if (playbackView == null || previewView == null || draft.file == null || !draft.file.exists()) return;

        stopPreviewPlayback();

        previewView.setVisibility(View.GONE);
        playbackView.setVisibility(View.VISIBLE);
        setDraftBadgeVisible(true);
        playbackView.setOnPreparedListener(mp -> playbackView.start());
        playbackView.setOnErrorListener((mp, what, extra) -> {
            showCameraPreview();
            return true;
        });
        playbackView.setLooping(true);
        playbackView.setMuted(true);
        playbackView.setVideoURI(Uri.fromFile(draft.file));
        playbackView.start();
    }

    private void stopPreviewPlayback() {
        if (playbackView == null) return;

        try {
            playbackView.stopPlayback();
        } catch (Exception ignored) {
        }

        playbackView.setOnPreparedListener(null);
        playbackView.setOnErrorListener(null);
        playbackView.setVisibility(View.GONE);
    }

    private void showCameraPreview() {
        if (previewView != null) {
            previewView.setVisibility(View.VISIBLE);
        }

        if (playbackView != null) {
            playbackView.setVisibility(View.GONE);
        }

        setDraftBadgeVisible(false);
    }

    private void finishWithResult() {
        if (finalizing) return;

        if (currentRecording != null) {
            finishAfterFinalize = true;
            stopRecording(false, AFTER_FINALIZE_NONE);
            return;
        }

        if (selectedDraft == null || selectedDraft.file == null || !selectedDraft.file.exists()) {
            Toast.makeText(requireContext(), "Сначала запишите кружок", Toast.LENGTH_SHORT).show();
            return;
        }

        sent = true;

        RecordedChatMedia media = RecordedChatMedia.videoCircle(
                selectedDraft.file,
                selectedDraft.durationMs
        );

        deleteAllDraftsExcept(selectedDraft);
        deleteSegmentsQuietly(currentTakeSegments);
        currentTakeSegments.clear();

        if (callback != null) {
            callback.onVideoCircleRecorded(media);
        }

        dismissAllowingStateLoss();
    }

    private void cancelAndDismiss() {
        stopPreviewPlayback();

        if (currentRecording != null && !finalizing) {
            discardCurrentFile = true;
            try {
                currentRecording.stop();
            } catch (Exception ignored) {
            }
        }

        deleteFileQuietly(outputFile);
        deleteSegmentsQuietly(currentTakeSegments);
        currentTakeSegments.clear();
        deleteAllDraftsExcept(null);

        if (callback != null) {
            callback.onVideoCircleCanceled();
        }

        dismissAllowingStateLoss();
    }

    private boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private long calculateActiveDurationMs() {
        if (startedAtMs <= 0L) return 0L;

        long now = System.currentTimeMillis();
        long pausedTime = totalPausedMs;

        if (paused && pausedStartedAtMs > 0L) {
            pausedTime += now - pausedStartedAtMs;
        }

        long elapsedThisSegment = Math.max(0L, now - startedAtMs - pausedTime);
        long previousSegments = 0L;

        for (RecordedSegment segment : currentTakeSegments) {
            previousSegments += Math.max(0L, segment.durationMs);
        }

        return previousSegments + elapsedThisSegment;
    }

    private void updateTimer(long durationMs) {
        if (tvTimer == null) return;

        long safeDuration = Math.max(0L, Math.min(durationMs, MAX_DURATION_MS));
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(safeDuration);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        tvTimer.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
    }

    @NonNull
    private String resolveCameraHint() {
        String camera = lensFacing == CameraSelector.LENS_FACING_FRONT
                ? "Передняя камера"
                : "Задняя камера";

        return camera + ". До 45 секунд. До 5 черновиков справа";
    }

    private void updateHint(@NonNull String hint) {
        if (tvHint != null) {
            tvHint.setText(hint);
        }
    }

    private void setDraftBadgeVisible(boolean visible) {
        if (tvDraftBadge != null) {
            tvDraftBadge.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void setRecordIconRecord() {
        if (btnRecord == null) return;
        btnRecord.setImageResource(R.drawable.ic_fiber_manual_record);
    }

    private void setRecordIconPause() {
        if (btnRecord == null) return;
        btnRecord.setImageResource(android.R.drawable.ic_media_pause);
    }

    private void setRecordIconResume() {
        if (btnRecord == null) return;
        btnRecord.setImageResource(android.R.drawable.ic_media_play);
    }

    private void setSendEnabled(boolean enabled) {
        if (btnSend == null) return;
        btnSend.setEnabled(enabled);
        btnSend.setAlpha(enabled ? 1f : 0.45f);
    }

    private void setStopEnabled(boolean enabled) {
        if (btnStop == null) return;
        btnStop.setEnabled(enabled);
        btnStop.setAlpha(enabled ? 1f : 0.45f);
    }

    private void setRecordEnabled(boolean enabled) {
        if (btnRecord == null) return;
        btnRecord.setEnabled(enabled);
        btnRecord.setAlpha(enabled ? 1f : 0.45f);
    }

    private void setSwitchEnabled(boolean enabled) {
        if (btnSwitchCamera == null) return;
        btnSwitchCamera.setEnabled(enabled);
        btnSwitchCamera.setAlpha(enabled ? 1f : 0.45f);
    }

    private void deleteAllDraftsExcept(@Nullable RecordedDraft keep) {
        for (RecordedDraft draft : new ArrayList<>(drafts)) {
            if (keep != null && draft == keep) continue;
            deleteFileQuietly(draft.file);
        }

        drafts.clear();

        if (keep != null) {
            drafts.add(keep);
        }
    }

    private void deleteSegmentsQuietly(@Nullable List<RecordedSegment> segments) {
        if (segments == null) return;

        for (RecordedSegment segment : segments) {
            if (segment != null) {
                deleteFileQuietly(segment.file);
            }
        }
    }

    private void deleteFileQuietly(@Nullable File file) {
        if (file == null) return;

        try {
            if (file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onDestroyView() {
        timerHandler.removeCallbacksAndMessages(null);
        stopPreviewPlayback();

        if (!sent) {
            if (currentRecording != null) {
                try {
                    discardCurrentFile = true;
                    currentRecording.stop();
                } catch (Exception ignored) {
                }
            }

            deleteFileQuietly(outputFile);
            deleteSegmentsQuietly(currentTakeSegments);
            currentTakeSegments.clear();
            deleteAllDraftsExcept(null);
        }

        currentRecording = null;
        outputFile = null;
        videoCapture = null;

        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
            } catch (Exception ignored) {
            }
        }

        super.onDestroyView();
    }

    private static final class RecordedDraft {
        final File file;
        final long durationMs;
        final int lensFacing;

        RecordedDraft(@NonNull File file, long durationMs, int lensFacing) {
            this.file = file;
            this.durationMs = durationMs;
            this.lensFacing = lensFacing;
        }
    }

    private static final class RecordedSegment {
        final File file;
        final long durationMs;
        final int lensFacing;

        RecordedSegment(@NonNull File file, long durationMs, int lensFacing) {
            this.file = file;
            this.durationMs = durationMs;
            this.lensFacing = lensFacing;
        }
    }

    private static final class TrackSpec {
        final int index;
        final MediaFormat format;

        TrackSpec(int index, @NonNull MediaFormat format) {
            this.index = index;
            this.format = format;
        }
    }
}
