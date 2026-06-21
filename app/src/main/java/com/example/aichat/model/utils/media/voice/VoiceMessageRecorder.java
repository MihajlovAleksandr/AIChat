package com.example.aichat.model.utils.media.voice;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.utils.media.recording.RecordedChatMedia;
import com.example.aichat.model.utils.media.ChatMediaMarkers;
import java.io.File;

public class VoiceMessageRecorder {

    private static final long MIN_DURATION_MS = 700L;
    private static final long MAX_DURATION_MS = 60_000L;

    private final Context appContext;

    private MediaRecorder recorder;
    private File outputFile;
    private long startedAtMs = 0L;
    private long pausedAtMs = 0L;
    private long totalPausedMs = 0L;
    private boolean recording = false;
    private boolean paused = false;

    public VoiceMessageRecorder(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public synchronized void start() throws Exception {
        cancel();

        File dir = new File(appContext.getCacheDir(), "voice_messages");

        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Cannot create voice cache dir: " + dir.getAbsolutePath());
        }

        outputFile = new File(dir, ChatMediaMarkers.buildVoiceFileName());

        recorder = createRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioEncodingBitRate(64_000);
        recorder.setAudioSamplingRate(44_100);
        recorder.setMaxDuration((int) MAX_DURATION_MS);
        recorder.setOutputFile(outputFile.getAbsolutePath());
        recorder.prepare();
        recorder.start();

        startedAtMs = System.currentTimeMillis();
        pausedAtMs = 0L;
        totalPausedMs = 0L;
        recording = true;
        paused = false;
    }

    public synchronized void pause() {
        if (!recording || recorder == null || paused) {
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        try {
            recorder.pause();
            pausedAtMs = System.currentTimeMillis();
            paused = true;
        } catch (Exception ignored) {
        }
    }

    public synchronized void resume() {
        if (!recording || recorder == null || !paused) {
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        try {
            recorder.resume();

            if (pausedAtMs > 0L) {
                totalPausedMs += Math.max(0L, System.currentTimeMillis() - pausedAtMs);
            }

            pausedAtMs = 0L;
            paused = false;
        } catch (Exception ignored) {
        }
    }

    @Nullable
    public synchronized RecordedChatMedia stopAndBuildResult() {
        if (!recording || recorder == null) {
            cleanupBrokenFile();
            return null;
        }

        if (paused) {
            resume();
        }

        long durationMs = getDurationMsLocked();
        File finishedFile = outputFile;

        try {
            recorder.stop();
        } catch (RuntimeException exception) {
            cleanupRecorderOnly();
            deleteFile(finishedFile);
            return null;
        }

        cleanupRecorderOnly();

        if (durationMs < MIN_DURATION_MS
                || finishedFile == null
                || !finishedFile.exists()
                || finishedFile.length() <= 0) {
            deleteFile(finishedFile);
            return null;
        }

        return RecordedChatMedia.voice(finishedFile, durationMs);
    }

    public synchronized void cancel() {
        File fileToDelete = outputFile;

        if (recorder != null) {
            try {
                if (recording) {
                    recorder.stop();
                }
            } catch (Exception ignored) {
            }
        }

        cleanupRecorderOnly();
        deleteFile(fileToDelete);
    }

    public synchronized boolean isRecording() {
        return recording;
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    public synchronized long getDurationMs() {
        return getDurationMsLocked();
    }

    public synchronized long getMaxDurationMs() {
        return MAX_DURATION_MS;
    }

    public synchronized int getMaxAmplitude() {
        if (!recording || paused || recorder == null) {
            return 0;
        }

        try {
            return recorder.getMaxAmplitude();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private long getDurationMsLocked() {
        if (!recording || startedAtMs <= 0L) {
            return 0L;
        }

        long now = paused && pausedAtMs > 0L ? pausedAtMs : System.currentTimeMillis();
        return Math.max(0L, now - startedAtMs - totalPausedMs);
    }

    private MediaRecorder createRecorder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new MediaRecorder(appContext);
        }

        return new MediaRecorder();
    }

    private void cleanupBrokenFile() {
        File fileToDelete = outputFile;
        cleanupRecorderOnly();
        deleteFile(fileToDelete);
    }

    private void cleanupRecorderOnly() {
        if (recorder != null) {
            try {
                recorder.reset();
            } catch (Exception ignored) {
            }

            try {
                recorder.release();
            } catch (Exception ignored) {
            }
        }

        recorder = null;
        outputFile = null;
        startedAtMs = 0L;
        pausedAtMs = 0L;
        totalPausedMs = 0L;
        recording = false;
        paused = false;
    }

    private void deleteFile(@Nullable File file) {
        if (file != null && file.exists()) {
            boolean ignored = file.delete();
        }
    }
}
