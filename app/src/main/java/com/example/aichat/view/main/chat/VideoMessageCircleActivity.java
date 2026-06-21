package com.example.aichat.view.main.chat;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.example.aichat.view.main.BaseActivity;
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
import com.example.aichat.model.utils.media.ChatMediaMarkers;
import com.example.aichat.R;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.Locale;

public class VideoMessageCircleActivity extends BaseActivity {

    public static final String EXTRA_FILE_PATH = "extra_file_path";
    public static final String EXTRA_DURATION_MS = "extra_duration_ms";

    private static final long MAX_DURATION_MS = 60_000L;
    private static final long MIN_DURATION_MS = 700L;

    private PreviewView previewView;
    private ImageButton btnCancel;
    private ImageButton btnRecord;
    private ImageButton btnSend;
    private TextView tvTimer;

    private VideoCapture<Recorder> videoCapture;
    private Recording currentRecording;
    private File outputFile;

    private long startedAtMs = 0L;
    private long finalDurationMs = 0L;
    private boolean finished = false;
    private boolean finalizing = false;
    private boolean discardCurrentFile = false;
    private boolean finishAfterFinalize = false;

    private final android.os.Handler timerHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (startedAtMs <= 0L || finished || currentRecording == null) {
                return;
            }

            long elapsed = System.currentTimeMillis() - startedAtMs;
            updateTimer(elapsed);

            if (elapsed >= MAX_DURATION_MS) {
                stopRecording(true);
                return;
            }

            timerHandler.postDelayed(this, 250L);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_message_circle);

        previewView = findViewById(R.id.video_circle_preview);
        btnCancel = findViewById(R.id.btn_video_circle_cancel);
        btnRecord = findViewById(R.id.btn_video_circle_record);
        btnSend = findViewById(R.id.btn_video_circle_send);
        tvTimer = findViewById(R.id.tv_video_circle_timer);

        setSendEnabled(false);

        btnCancel.setOnClickListener(v -> cancelAndFinish());

        btnRecord.setOnClickListener(v -> {
            if (finalizing) {
                return;
            }

            if (currentRecording == null) {
                startRecording();
            } else {
                stopRecording(false);
            }
        });

        btnSend.setOnClickListener(v -> finishWithResult());

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(
                                QualitySelector.fromOrderedList(
                                        Arrays.asList(Quality.SD, Quality.LOWEST)
                                )
                        )
                        .build();

                videoCapture = VideoCapture.withOutput(recorder);
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        videoCapture
                );
            } catch (Exception exception) {
                Toast.makeText(
                        this,
                        "Не удалось открыть камеру",
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startRecording() {
        if (videoCapture == null) {
            Toast.makeText(this, "Камера ещё не готова", Toast.LENGTH_SHORT).show();
            return;
        }

        File dir = new File(getCacheDir(), "video_circle_messages");

        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(this, "Не удалось создать папку записи", Toast.LENGTH_SHORT).show();
            return;
        }

        outputFile = new File(dir, ChatMediaMarkers.buildCircleVideoFileName());

        FileOutputOptions options =
                new FileOutputOptions.Builder(outputFile).build();

        PendingRecording pendingRecording =
                videoCapture.getOutput().prepareRecording(this, options);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            pendingRecording = pendingRecording.withAudioEnabled();
        }

        finalizing = false;
        discardCurrentFile = false;
        finishAfterFinalize = false;
        finalDurationMs = 0L;

        currentRecording = pendingRecording.start(
                ContextCompat.getMainExecutor(this),
                event -> {
                    if (event instanceof VideoRecordEvent.Finalize) {
                        handleRecordingFinalized((VideoRecordEvent.Finalize) event);
                    }
                }
        );

        startedAtMs = System.currentTimeMillis();

        btnRecord.setImageResource(R.drawable.ic_stop);
        btnRecord.setEnabled(true);
        setSendEnabled(false);

        timerHandler.post(timerRunnable);
    }

    private void stopRecording(boolean autoFinish) {
        if (currentRecording == null) {
            return;
        }

        finalDurationMs = System.currentTimeMillis() - startedAtMs;
        discardCurrentFile = finalDurationMs < MIN_DURATION_MS;
        finishAfterFinalize = autoFinish && !discardCurrentFile;
        finalizing = true;

        timerHandler.removeCallbacks(timerRunnable);

        try {
            currentRecording.stop();
        } catch (Exception exception) {
            finalizing = false;
            discardCurrentFile = true;
            deleteOutputFile();
            Toast.makeText(this, "Ошибка остановки записи", Toast.LENGTH_SHORT).show();
        }

        currentRecording = null;
        btnRecord.setImageResource(R.drawable.ic_fiber_manual_record);
        btnRecord.setEnabled(false);
        setSendEnabled(false);
    }

    private void handleRecordingFinalized(VideoRecordEvent.Finalize finalizeEvent) {
        finalizing = false;
        btnRecord.setEnabled(true);

        if (finalizeEvent.hasError()) {
            deleteOutputFile();
            setSendEnabled(false);

            if (!finished) {
                Toast.makeText(this, "Ошибка записи видео", Toast.LENGTH_SHORT).show();
            }

            return;
        }

        if (discardCurrentFile) {
            deleteOutputFile();
            setSendEnabled(false);
            Toast.makeText(this, "Слишком короткая запись", Toast.LENGTH_SHORT).show();
            return;
        }

        if (outputFile == null || !outputFile.exists() || outputFile.length() <= 0) {
            deleteOutputFile();
            setSendEnabled(false);
            Toast.makeText(this, "Видео не записано", Toast.LENGTH_SHORT).show();
            return;
        }

        setSendEnabled(true);

        if (finishAfterFinalize) {
            finishWithResult();
        }
    }

    private void finishWithResult() {
        if (finalizing) {
            finishAfterFinalize = true;
            return;
        }

        if (outputFile == null || !outputFile.exists() || outputFile.length() <= 0) {
            Toast.makeText(this, "Видео не записано", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent data = new Intent();
        data.putExtra(EXTRA_FILE_PATH, outputFile.getAbsolutePath());
        data.putExtra(EXTRA_DURATION_MS, finalDurationMs);

        finished = true;
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    private void cancelAndFinish() {
        finished = true;
        discardCurrentFile = true;

        if (currentRecording != null) {
            try {
                currentRecording.stop();
            } catch (Exception ignored) {
            }
        }

        currentRecording = null;
        deleteOutputFile();
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    private void deleteOutputFile() {
        if (outputFile != null && outputFile.exists()) {
            boolean ignored = outputFile.delete();
        }

        outputFile = null;
    }

    private void updateTimer(long durationMs) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs);
        long minutes = seconds / 60L;
        long rest = seconds % 60L;

        tvTimer.setText(String.format(Locale.US, "%d:%02d", minutes, rest));
    }

    private void setSendEnabled(boolean enabled) {
        btnSend.setEnabled(enabled);
        btnSend.setAlpha(enabled ? 1f : 0.45f);
    }

    @Override
    protected void onDestroy() {
        timerHandler.removeCallbacks(timerRunnable);

        if (!finished) {
            discardCurrentFile = true;

            if (currentRecording != null) {
                try {
                    currentRecording.stop();
                } catch (Exception ignored) {
                }
            } else {
                deleteOutputFile();
            }
        }

        currentRecording = null;
        super.onDestroy();
    }
}
