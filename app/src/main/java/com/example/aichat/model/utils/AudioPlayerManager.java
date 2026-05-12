package com.example.aichat.model.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@UnstableApi
public class AudioPlayerManager {

    public enum PlaybackState {
        IDLE,
        PREPARING,
        PLAYING,
        PAUSED,
        STOPPED,
        ERROR
    }

    private ExoPlayer player;

    private UUID pendingPlayFileId;
    private boolean pendingPlayAfterDownload;

    private UUID currentFileId;
    private UUID playbackFileId;

    private long currentPosition = 0;

    private PlaybackState playbackState =
            PlaybackState.IDLE;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private Runnable progressRunnable;

    private String authToken;

    public interface Listener {

        void onStart(UUID fileId);

        void onStop(UUID fileId);

        void onProgress(UUID fileId);
    }

    private Listener listener;

    public void setListener(Listener listener) {

        this.listener = listener;
    }

    public synchronized void play(
            Context context,
            String url,
            UUID fileId,
            String token
    ) {

        authToken = token;

        if (isCurrentFile(fileId)) {

            if (playbackState == PlaybackState.PAUSED) {
                resume();
            }

            return;
        }

        ensurePlayer(context);

        playbackFileId = fileId;

        playbackState =
                PlaybackState.PREPARING;

        currentFileId = fileId;

        currentPosition = 0;

        player.stop();

        player.clearMediaItems();

        MediaItem mediaItem =
                new MediaItem.Builder()
                        .setUri(url)
                        .build();

        player.setMediaItem(mediaItem);

        player.prepare();

        player.play();
    }

    public synchronized void playLocal(
            Context context,
            String localPath,
            UUID fileId
    ) {

        File file = new File(localPath);

        if (!file.exists()) {
            return;
        }

        boolean sameFile =
                isCurrentFile(fileId);

        if (sameFile &&
                playbackState == PlaybackState.PLAYING) {

            return;
        }

        if (sameFile &&
                playbackState == PlaybackState.PAUSED) {

            resume();

            return;
        }

        ensurePlayer(context);

        playbackFileId = fileId;

        playbackState =
                PlaybackState.PREPARING;

        currentFileId = fileId;

        currentPosition = 0;

        player.stop();

        player.clearMediaItems();

        MediaItem mediaItem =
                MediaItem.fromUri(
                        android.net.Uri.fromFile(file)
                );

        player.setMediaItem(mediaItem);

        player.prepare();

        player.play();
    }

    private synchronized void ensurePlayer(
            Context context
    ) {

        if (player != null) {
            return;
        }

        DefaultDataSource.Factory dataSourceFactory =
                buildDataSourceFactory(context);

        player = new ExoPlayer.Builder(context)
                .setMediaSourceFactory(
                        new DefaultMediaSourceFactory(
                                dataSourceFactory
                        )
                )
                .build();

        attachPlayerListeners();
    }

    private DefaultDataSource.Factory buildDataSourceFactory(
            Context context
    ) {

        DefaultHttpDataSource.Factory httpFactory =
                new DefaultHttpDataSource.Factory();

        Map<String, String> headers =
                new HashMap<>();

        headers.put(
                "device",
                android.os.Build.MANUFACTURER
                        + " "
                        + android.os.Build.MODEL
        );

        if (authToken != null) {

            headers.put(
                    "Authorization",
                    "Bearer " + authToken
            );
        }

        httpFactory.setDefaultRequestProperties(headers);

        return new DefaultDataSource.Factory(
                context,
                httpFactory
        );
    }

    private void attachPlayerListeners() {

        if (player == null) {
            return;
        }

        player.addListener(new Player.Listener() {

            @Override
            public void onPlaybackStateChanged(int state) {

                if (state == Player.STATE_READY) {

                    playbackState =
                            PlaybackState.PLAYING;
                }

                if (state == Player.STATE_ENDED) {

                    stop();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {

                if (currentFileId == null) {
                    return;
                }

                if (isPlaying) {

                    playbackState =
                            PlaybackState.PLAYING;

                    startProgressUpdates();

                    if (listener != null) {

                        listener.onStart(currentFileId);
                    }

                    return;
                }

                if (player == null) {
                    return;
                }

                if (player.getPlaybackState()
                        == Player.STATE_ENDED) {

                    return;
                }

                currentPosition =
                        player.getCurrentPosition();

                playbackState =
                        PlaybackState.PAUSED;

                stopProgressUpdates();

                if (listener != null) {

                    listener.onStop(currentFileId);
                }
            }

            @Override
            public void onPlayerError(
                    @NonNull PlaybackException error
            ) {

                playbackState =
                        PlaybackState.ERROR;

                stop();
            }
        });
    }

    public void requestPlayAfterDownload(UUID fileId) {

        pendingPlayFileId = fileId;

        pendingPlayAfterDownload = true;
    }

    public boolean shouldAutoPlay(UUID fileId) {

        return pendingPlayAfterDownload
                && pendingPlayFileId != null
                && pendingPlayFileId.equals(fileId);
    }

    public void clearPendingPlay() {

        pendingPlayAfterDownload = false;

        pendingPlayFileId = null;
    }

    public synchronized void pause() {

        if (player == null) {
            return;
        }

        if (!player.isPlaying()) {
            return;
        }

        currentPosition =
                player.getCurrentPosition();

        playbackState =
                PlaybackState.PAUSED;

        player.pause();
    }

    public synchronized void resume() {

        if (player == null) {
            return;
        }

        if (playbackState != PlaybackState.PAUSED) {
            return;
        }

        player.seekTo(currentPosition);

        playbackState =
                PlaybackState.PLAYING;

        player.play();
    }

    public synchronized void stop() {

        UUID oldFileId = currentFileId;

        stopProgressUpdates();

        pendingPlayAfterDownload = false;

        pendingPlayFileId = null;

        currentFileId = null;

        currentPosition = 0;

        playbackState =
                PlaybackState.STOPPED;

        playbackFileId = null;

        if (player != null) {

            player.stop();

            player.clearMediaItems();
        }

        if (listener != null &&
                oldFileId != null) {

            listener.onStop(oldFileId);
        }
    }

    public synchronized void detach() {

        stopProgressUpdates();

        if (player != null) {

            currentPosition =
                    player.getCurrentPosition();

            playbackState =
                    PlaybackState.PAUSED;

            player.pause();
        }
    }

    public synchronized void release() {

        stopProgressUpdates();

        currentPosition = 0;

        currentFileId = null;

        playbackFileId = null;

        playbackState =
                PlaybackState.IDLE;

        pendingPlayAfterDownload = false;

        pendingPlayFileId = null;

        if (player != null) {

            player.stop();

            player.clearMediaItems();

            player.release();

            player = null;
        }
    }

    public PlaybackState getPlaybackState(UUID fileId) {

        if (playbackFileId == null ||
                !playbackFileId.equals(fileId)) {

            return PlaybackState.IDLE;
        }

        return playbackState;
    }

    public boolean isPlaying(UUID fileId) {

        return getPlaybackState(fileId)
                == PlaybackState.PLAYING;
    }

    public boolean isPaused(UUID fileId) {

        return getPlaybackState(fileId)
                == PlaybackState.PAUSED;
    }

    public boolean isPreparing(UUID fileId) {

        return getPlaybackState(fileId)
                == PlaybackState.PREPARING;
    }

    public int getCurrentPosition(UUID fileId) {

        if (!isCurrentFile(fileId)) {
            return 0;
        }

        if (player != null) {

            return (int) player.getCurrentPosition();
        }

        return (int) currentPosition;
    }

    public int getDuration(UUID fileId) {

        if (!isCurrentFile(fileId)) {
            return 0;
        }

        if (player == null) {
            return 0;
        }

        long duration = player.getDuration();

        return duration > 0
                ? (int) duration
                : 0;
    }

    public synchronized void seekTo(int position) {

        if (player == null) {
            return;
        }

        player.seekTo(position);

        currentPosition = position;

        if (listener != null &&
                currentFileId != null) {

            listener.onProgress(currentFileId);
        }
    }

    private void startProgressUpdates() {

        stopProgressUpdates();

        progressRunnable = new Runnable() {

            @Override
            public void run() {

                if (player == null ||
                        currentFileId == null ||
                        listener == null) {

                    return;
                }

                listener.onProgress(currentFileId);

                if (playbackState == PlaybackState.PLAYING ||
                        playbackState == PlaybackState.PAUSED) {

                    handler.postDelayed(this, 200);
                }
            }
        };

        handler.post(progressRunnable);
    }

    private void stopProgressUpdates() {

        if (progressRunnable != null) {

            handler.removeCallbacks(progressRunnable);

            progressRunnable = null;
        }
    }

    private boolean isCurrentFile(UUID fileId) {

        return currentFileId != null
                && currentFileId.equals(fileId);
    }
}