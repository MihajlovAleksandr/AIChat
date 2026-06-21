package com.example.aichat.model.utils.media.audio;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.Manifest;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import com.example.aichat.R;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
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

    public static final class AudioQueueItem {
        public final UUID fileId;
        public final String localPath;
        public final String title;
        public final String subtitle;
        public final boolean voice;

        public AudioQueueItem(
                @NonNull UUID fileId,
                @NonNull String localPath,
                @Nullable String title,
                @Nullable String subtitle,
                boolean voice
        ) {
            this.fileId = fileId;
            this.localPath = localPath;
            this.title = title != null && !title.trim().isEmpty() ? title : "Аудио";
            this.subtitle = subtitle != null ? subtitle : "";
            this.voice = voice;
        }
    }

    private static final String CHANNEL_ID = "aichat_audio_playback_channel";
    private static final String CHANNEL_NAME = "Audio playback";
    private static final int NOTIFICATION_ID = 7081;

    private static final String ACTION_TOGGLE_PLAYBACK =
            "com.example.aichat.ACTION_TOGGLE_AUDIO_PLAYBACK";
    private static final String ACTION_STOP_PLAYBACK =
            "com.example.aichat.ACTION_STOP_AUDIO_PLAYBACK";
    private static final String ACTION_NEXT_PLAYBACK =
            "com.example.aichat.ACTION_NEXT_AUDIO_PLAYBACK";
    private static final String ACTION_NOTIFICATION_DELETED =
            "com.example.aichat.ACTION_AUDIO_NOTIFICATION_DELETED";

    private static volatile AudioPlayerManager activeInstance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Bitmap> embeddedArtworkCache = new ConcurrentHashMap<>();

    private ExoPlayer player;
    private MediaSessionCompat mediaSession;

    private UUID pendingPlayFileId;
    private boolean pendingPlayAfterDownload;

    private UUID currentFileId;
    private UUID playbackFileId;

    private long currentPosition = 0;
    private PlaybackState playbackState = PlaybackState.IDLE;

    private Runnable progressRunnable;
    private String authToken;
    private Context appContext;

    private String currentTitle = "Аудио";
    private String currentSubtitle = "";
    private String currentLocalPath;
    private boolean currentIsVoice = false;

    private final List<AudioQueueItem> queue = new ArrayList<>();

    private boolean lifecycleWatcherRegistered = false;
    private int aliveActivityCount = 1;
    private boolean appExitPending = false;

    private final Runnable appDestroyedStopRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (AudioPlayerManager.this) {
                if (appExitPending && aliveActivityCount <= 0) {
                    stop();
                }
            }
        }
    };

    public interface Listener {
        void onStart(UUID fileId);

        void onStop(UUID fileId);

        void onProgress(UUID fileId);
    }

    private Listener listener;

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public synchronized void setQueue(
            @Nullable List<AudioQueueItem> items,
            @Nullable UUID currentFileId
    ) {
        queue.clear();

        if (items != null) {
            for (AudioQueueItem item : items) {
                if (item == null || item.fileId == null || item.localPath == null) {
                    continue;
                }

                File file = new File(item.localPath);
                if (file.exists()) {
                    queue.add(item);
                }
            }
        }

        if (currentFileId != null) {
            for (AudioQueueItem item : queue) {
                if (currentFileId.equals(item.fileId)) {
                    currentTitle = item.title;
                    currentSubtitle = item.subtitle;
                    currentIsVoice = item.voice;
                    break;
                }
            }
        }
    }

    public synchronized void play(
            Context context,
            String url,
            UUID fileId,
            String token
    ) {
        if (context == null || url == null || fileId == null) {
            return;
        }

        authToken = token;
        appContext = context.getApplicationContext();
        activeInstance = this;
        registerAppCloseWatcher(context);

        if (isCurrentFile(fileId)) {
            if (playbackState == PlaybackState.PAUSED) {
                resume();
            }
            return;
        }

        ensurePlayer(context);

        playbackFileId = fileId;
        playbackState = PlaybackState.PREPARING;
        currentFileId = fileId;
        currentPosition = 0;
        currentLocalPath = null;
        currentTitle = "Аудио";
        currentSubtitle = "";
        currentIsVoice = false;

        player.stop();
        player.clearMediaItems();

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(url)
                .build();

        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        updateMediaSessionMetadata();
        updateMediaSessionPlaybackState();
        showOrUpdateNotification();
    }

    public synchronized void playLocal(
            Context context,
            String localPath,
            UUID fileId
    ) {
        playLocal(context, localPath, fileId, null, null, false);
    }

    public synchronized void playLocal(
            Context context,
            String localPath,
            UUID fileId,
            @Nullable String title,
            @Nullable String subtitle
    ) {
        playLocal(context, localPath, fileId, title, subtitle, false);
    }

    public synchronized void playLocal(
            Context context,
            String localPath,
            UUID fileId,
            @Nullable String title,
            @Nullable String subtitle,
            boolean isVoice
    ) {
        if (context == null || localPath == null || fileId == null) {
            return;
        }

        File file = new File(localPath);
        if (!file.exists()) {
            return;
        }

        appContext = context.getApplicationContext();
        activeInstance = this;
        registerAppCloseWatcher(context);

        boolean sameFile = isCurrentFile(fileId);

        if (sameFile && playbackState == PlaybackState.PLAYING) {
            return;
        }

        if (sameFile && playbackState == PlaybackState.PAUSED) {
            resume();
            return;
        }

        ensurePlayer(context);

        playbackFileId = fileId;
        playbackState = PlaybackState.PREPARING;
        currentFileId = fileId;
        currentPosition = 0;
        currentLocalPath = localPath;
        currentTitle = title != null && !title.trim().isEmpty() ? title : file.getName();
        currentSubtitle = subtitle != null ? subtitle : "";
        currentIsVoice = isVoice;

        player.stop();
        player.clearMediaItems();

        MediaItem mediaItem = MediaItem.fromUri(android.net.Uri.fromFile(file));

        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        updateMediaSessionMetadata();
        updateMediaSessionPlaybackState();
        showOrUpdateNotification();
    }

    private synchronized void ensurePlayer(Context context) {
        if (context == null) {
            return;
        }

        appContext = context.getApplicationContext();
        activeInstance = this;
        registerAppCloseWatcher(context);

        createNotificationChannel();
        ensureMediaSession();

        if (player != null) {
            return;
        }

        DefaultDataSource.Factory dataSourceFactory = buildDataSourceFactory(context);

        player = new ExoPlayer.Builder(context)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
                .build();

        attachPlayerListeners();
    }

    private DefaultDataSource.Factory buildDataSourceFactory(Context context) {
        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory();

        Map<String, String> headers = new HashMap<>();
        headers.put("device", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);

        if (authToken != null) {
            headers.put("Authorization", "Bearer " + authToken);
        }

        httpFactory.setDefaultRequestProperties(headers);

        return new DefaultDataSource.Factory(context, httpFactory);
    }

    private void attachPlayerListeners() {
        if (player == null) {
            return;
        }

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY && player != null && player.isPlaying()) {
                    playbackState = PlaybackState.PLAYING;
                    updateMediaSessionMetadata();
                    updateMediaSessionPlaybackState();
                    showOrUpdateNotification();
                }

                if (state == Player.STATE_ENDED) {
                    playNextOrStop();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (currentFileId == null) {
                    return;
                }

                if (isPlaying) {
                    playbackState = PlaybackState.PLAYING;
                    startProgressUpdates();

                    if (listener != null) {
                        listener.onStart(currentFileId);
                    }

                    updateMediaSessionPlaybackState();
                    showOrUpdateNotification();
                    return;
                }

                if (player == null || player.getPlaybackState() == Player.STATE_ENDED) {
                    return;
                }

                currentPosition = player.getCurrentPosition();
                playbackState = PlaybackState.PAUSED;
                stopProgressUpdates();

                if (listener != null) {
                    listener.onStop(currentFileId);
                }

                updateMediaSessionPlaybackState();
                showOrUpdateNotification();
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                playbackState = PlaybackState.ERROR;

                if (currentLocalPath != null && !new File(currentLocalPath).exists()) {
                    playNextOrStop();
                    return;
                }

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
        if (player == null || !player.isPlaying()) {
            return;
        }

        currentPosition = player.getCurrentPosition();
        playbackState = PlaybackState.PAUSED;
        player.pause();

        updateMediaSessionPlaybackState();
        showOrUpdateNotification();
    }

    public synchronized void resume() {
        if (player == null || playbackState != PlaybackState.PAUSED) {
            return;
        }

        player.seekTo(currentPosition);
        playbackState = PlaybackState.PLAYING;
        player.play();

        updateMediaSessionPlaybackState();
        showOrUpdateNotification();
    }

    public synchronized void togglePlayback() {
        if (playbackState == PlaybackState.PLAYING) {
            pause();
            return;
        }

        if (playbackState == PlaybackState.PAUSED) {
            resume();
        }
    }

    public synchronized void playNextOrStop() {
        AudioQueueItem nextItem = findNextQueueItem();

        if (nextItem == null || appContext == null) {
            stop();
            return;
        }

        playLocal(
                appContext,
                nextItem.localPath,
                nextItem.fileId,
                nextItem.title,
                nextItem.subtitle,
                nextItem.voice
        );
    }

    @Nullable
    private AudioQueueItem findNextQueueItem() {
        if (currentFileId == null || queue.isEmpty()) {
            return null;
        }

        List<AudioQueueItem> sameKind = new ArrayList<>();

        for (AudioQueueItem item : queue) {
            if (item.voice == currentIsVoice && new File(item.localPath).exists()) {
                sameKind.add(item);
            }
        }

        if (sameKind.size() <= 1) {
            return null;
        }

        int currentIndex = -1;

        for (int i = 0; i < sameKind.size(); i++) {
            if (currentFileId.equals(sameKind.get(i).fileId)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            return sameKind.get(0);
        }

        return sameKind.get((currentIndex + 1) % sameKind.size());
    }

    public synchronized void stop() {
        UUID oldFileId = currentFileId;

        stopProgressUpdates();
        pendingPlayAfterDownload = false;
        pendingPlayFileId = null;

        currentFileId = null;
        currentPosition = 0;
        currentLocalPath = null;
        playbackState = PlaybackState.STOPPED;
        playbackFileId = null;

        if (player != null) {
            player.stop();
            player.clearMediaItems();
        }

        updateMediaSessionPlaybackState();
        if (mediaSession != null) {
            mediaSession.setActive(false);
        }
        clearNotification();

        if (listener != null && oldFileId != null) {
            listener.onStop(oldFileId);
        }
    }

    public synchronized void detach() {
        stopProgressUpdates();

        if (player != null) {
            currentPosition = player.getCurrentPosition();
            playbackState = PlaybackState.PAUSED;
            player.pause();
            updateMediaSessionPlaybackState();
            showOrUpdateNotification();
        }
    }

    public synchronized void release() {
        stopProgressUpdates();

        currentPosition = 0;
        currentFileId = null;
        playbackFileId = null;
        currentLocalPath = null;
        playbackState = PlaybackState.IDLE;
        pendingPlayAfterDownload = false;
        pendingPlayFileId = null;

        clearNotification();

        if (player != null) {
            player.stop();
            player.clearMediaItems();
            player.release();
            player = null;
        }

        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }

        if (activeInstance == this) {
            activeInstance = null;
        }
    }

    public PlaybackState getPlaybackState(UUID fileId) {
        if (playbackFileId == null || !playbackFileId.equals(fileId)) {
            return PlaybackState.IDLE;
        }

        return playbackState;
    }

    public boolean isPlaying(UUID fileId) {
        return getPlaybackState(fileId) == PlaybackState.PLAYING;
    }

    public boolean isPaused(UUID fileId) {
        return getPlaybackState(fileId) == PlaybackState.PAUSED;
    }

    public boolean isPreparing(UUID fileId) {
        return getPlaybackState(fileId) == PlaybackState.PREPARING;
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
        if (!isCurrentFile(fileId) || player == null) {
            return 0;
        }

        long duration = player.getDuration();
        return duration > 0 ? (int) duration : 0;
    }

    public synchronized void seekTo(int position) {
        if (player == null) {
            return;
        }

        int safePosition = Math.max(0, position);
        player.seekTo(safePosition);
        currentPosition = safePosition;

        if (listener != null && currentFileId != null) {
            listener.onProgress(currentFileId);
        }

        updateMediaSessionPlaybackState();
        showOrUpdateNotification();
    }

    private void startProgressUpdates() {
        stopProgressUpdates();

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (player == null || currentFileId == null) {
                    return;
                }

                if (listener != null) {
                    listener.onProgress(currentFileId);
                }

                updateMediaSessionPlaybackState();

                if (playbackState == PlaybackState.PLAYING || playbackState == PlaybackState.PAUSED) {
                    handler.postDelayed(this, 500);
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
        return currentFileId != null && currentFileId.equals(fileId);
    }

    private void createNotificationChannel() {
        if (appContext == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null || notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
        );

        channel.setDescription("Уведомление воспроизведения аудио");
        channel.setShowBadge(false);

        notificationManager.createNotificationChannel(channel);
    }

    private void ensureMediaSession() {
        if (appContext == null || mediaSession != null) {
            return;
        }

        mediaSession = new MediaSessionCompat(appContext, "AIChatAudioSession");
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                        | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                resume();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onStop() {
                stop();
            }

            @Override
            public void onSkipToNext() {
                playNextOrStop();
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo((int) pos);
            }
        });

        mediaSession.setActive(true);
    }

    private void updateMediaSessionMetadata() {
        if (mediaSession == null) {
            return;
        }

        mediaSession.setActive(true);

        long duration = player != null && player.getDuration() > 0 ? player.getDuration() : 0;

        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSubtitle)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);

        Bitmap artwork = readEmbeddedArtwork(currentLocalPath);
        if (artwork != null) {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork);
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artwork);
        }

        mediaSession.setMetadata(builder.build());
    }

    private void updateMediaSessionPlaybackState() {
        if (mediaSession == null) {
            return;
        }

        int state;
        switch (playbackState) {
            case PLAYING:
                state = PlaybackStateCompat.STATE_PLAYING;
                break;
            case PAUSED:
                state = PlaybackStateCompat.STATE_PAUSED;
                break;
            case PREPARING:
                state = PlaybackStateCompat.STATE_BUFFERING;
                break;
            case ERROR:
                state = PlaybackStateCompat.STATE_ERROR;
                break;
            case STOPPED:
                state = PlaybackStateCompat.STATE_STOPPED;
                break;
            case IDLE:
            default:
                state = PlaybackStateCompat.STATE_NONE;
                break;
        }

        long position = player != null ? Math.max(0, player.getCurrentPosition()) : Math.max(0, currentPosition);
        float speed = playbackState == PlaybackState.PLAYING ? 1f : 0f;

        long actions = PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_STOP
                | PlaybackStateCompat.ACTION_SEEK_TO;

        if (hasNextInCurrentQueueKind()) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        }

        mediaSession.setPlaybackState(
                new PlaybackStateCompat.Builder()
                        .setActions(actions)
                        .setState(state, position, speed)
                        .build()
        );
    }

    private void showOrUpdateNotification() {
        if (appContext == null || currentFileId == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        createNotificationChannel();
        ensureMediaSession();
        updateMediaSessionMetadata();
        updateMediaSessionPlaybackState();

        boolean isPlaying = playbackState == PlaybackState.PLAYING;

        PendingIntent togglePendingIntent = buildBroadcastPendingIntent(
                ACTION_TOGGLE_PLAYBACK,
                1001
        );
        PendingIntent nextPendingIntent = buildBroadcastPendingIntent(
                ACTION_NEXT_PLAYBACK,
                1002
        );
        PendingIntent stopPendingIntent = buildBroadcastPendingIntent(
                ACTION_STOP_PLAYBACK,
                1003
        );
        PendingIntent deletedPendingIntent = buildBroadcastPendingIntent(
                ACTION_NOTIFICATION_DELETED,
                1004
        );

        int toggleIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        String toggleTitle = isPlaying ? "Пауза" : "Продолжить";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_aichat_launcher)
                .setContentTitle(currentTitle)
                .setContentText(currentSubtitle)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setOngoing(isPlaying)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setDeleteIntent(deletedPendingIntent)
                .addAction(toggleIcon, toggleTitle, togglePendingIntent);

        boolean hasNext = hasNextInCurrentQueueKind();
        if (hasNext) {
            builder.addAction(android.R.drawable.ic_media_next, "Следующий", nextPendingIntent);
        }

        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Остановить", stopPendingIntent);

        Bitmap artwork = readEmbeddedArtwork(currentLocalPath);
        if (artwork != null) {
            builder.setLargeIcon(artwork);
        }

        if (mediaSession != null) {
            builder.setStyle(
                    new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowActionsInCompactView(hasNext ? new int[]{0, 1, 2} : new int[]{0, 1})
            );
        }

        try {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    private PendingIntent buildBroadcastPendingIntent(@NonNull String action, int requestCode) {
        Intent intent = new Intent(appContext, NotificationActionReceiver.class)
                .setAction(action)
                .setPackage(appContext.getPackageName());

        return PendingIntent.getBroadcast(
                appContext,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void clearNotification() {
        if (appContext == null) {
            return;
        }

        try {
            NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID);
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private Bitmap readEmbeddedArtwork(@Nullable String localPath) {
        if (localPath == null || localPath.trim().isEmpty()) {
            return null;
        }

        Bitmap cached = embeddedArtworkCache.get(localPath);
        if (cached != null) {
            return cached;
        }

        File file = new File(localPath);
        if (!file.exists()) {
            return null;
        }

        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(file.getAbsolutePath());
            byte[] data = retriever.getEmbeddedPicture();

            if (data == null || data.length == 0) {
                return null;
            }

            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (bitmap != null) {
                embeddedArtworkCache.put(localPath, bitmap);
            }

            return bitmap;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean hasNextInCurrentQueueKind() {
        if (currentFileId == null || queue.isEmpty()) {
            return false;
        }

        int sameKindCount = 0;

        for (AudioQueueItem item : queue) {
            if (item != null && item.voice == currentIsVoice && new File(item.localPath).exists()) {
                sameKindCount++;
            }
        }

        return sameKindCount > 1;
    }

    private boolean isPlayingNow() {
        return playbackState == PlaybackState.PLAYING
                || playbackState == PlaybackState.PREPARING
                || (player != null && player.isPlaying());
    }

    private synchronized void handleNotificationDeleted() {
        if (isPlayingNow()) {
            handler.postDelayed(this::showOrUpdateNotification, 250L);
            return;
        }

        stop();
    }

    @Nullable
    public synchronized UUID getCurrentFileId() {
        return currentFileId;
    }

    @NonNull
    public synchronized String getCurrentTitle() {
        return currentTitle != null && !currentTitle.trim().isEmpty() ? currentTitle : "Аудио";
    }

    @NonNull
    public synchronized String getCurrentSubtitle() {
        return currentSubtitle != null ? currentSubtitle : "";
    }

    public synchronized boolean isCurrentVoice() {
        return currentIsVoice;
    }

    public synchronized boolean hasNextInCurrentQueue() {
        return hasNextInCurrentQueueKind();
    }

    public synchronized void stopFromMiniPlayer() {
        stop();
    }

    @Nullable
    public synchronized Bitmap getCurrentArtwork() {
        return readEmbeddedArtwork(currentLocalPath);
    }

    public synchronized boolean isCurrentProtectedLocalPath(@Nullable String localPath) {
        return localPath != null
                && currentLocalPath != null
                && currentFileId != null
                && localPath.equals(currentLocalPath)
                && new File(localPath).exists();
    }

    public static boolean isProtectedAudioLocalPath(@Nullable String localPath) {
        AudioPlayerManager manager = activeInstance;
        return manager != null && manager.isCurrentProtectedLocalPath(localPath);
    }

    private void registerAppCloseWatcher(@NonNull Context context) {
        if (lifecycleWatcherRegistered) {
            return;
        }

        Context applicationContext = context.getApplicationContext();
        if (!(applicationContext instanceof Application)) {
            return;
        }

        lifecycleWatcherRegistered = true;
        aliveActivityCount = Math.max(1, aliveActivityCount);
        appExitPending = false;

        ((Application) applicationContext).registerActivityLifecycleCallbacks(
                new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                        synchronized (AudioPlayerManager.this) {
                            aliveActivityCount++;
                            appExitPending = false;
                            handler.removeCallbacks(appDestroyedStopRunnable);
                        }
                    }

                    @Override
                    public void onActivityStarted(@NonNull Activity activity) {
                        synchronized (AudioPlayerManager.this) {
                            appExitPending = false;
                            handler.removeCallbacks(appDestroyedStopRunnable);
                        }
                    }

                    @Override
                    public void onActivityResumed(@NonNull Activity activity) {
                    }

                    @Override
                    public void onActivityPaused(@NonNull Activity activity) {
                    }

                    @Override
                    public void onActivityStopped(@NonNull Activity activity) {
                    }

                    @Override
                    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
                    }

                    @Override
                    public void onActivityDestroyed(@NonNull Activity activity) {
                        synchronized (AudioPlayerManager.this) {
                            aliveActivityCount = Math.max(0, aliveActivityCount - 1);

                            if (activity.isChangingConfigurations()) {
                                return;
                            }

                            if (activity.isFinishing() && aliveActivityCount == 0) {
                                appExitPending = true;
                                handler.removeCallbacks(appDestroyedStopRunnable);
                                handler.postDelayed(appDestroyedStopRunnable, 350L);
                            }
                        }
                    }
                }
        );
    }

    public static class NotificationActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            AudioPlayerManager manager = activeInstance;
            if (manager == null) {
                return;
            }

            String action = intent.getAction();

            if (ACTION_TOGGLE_PLAYBACK.equals(action)) {
                manager.togglePlayback();
                return;
            }

            if (ACTION_NEXT_PLAYBACK.equals(action)) {
                manager.playNextOrStop();
                return;
            }

            if (ACTION_STOP_PLAYBACK.equals(action)) {
                manager.stop();
                return;
            }

            if (ACTION_NOTIFICATION_DELETED.equals(action)) {
                manager.handleNotificationDeleted();
            }
        }
    }
}
