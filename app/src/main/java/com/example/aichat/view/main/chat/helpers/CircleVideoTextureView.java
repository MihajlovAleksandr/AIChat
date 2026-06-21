package com.example.aichat.view.main.chat.helpers;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.IOException;

public class CircleVideoTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    private static final String TAG = "CircleVideoTextureView";

    @Nullable
    private Uri videoUri;

    @Nullable
    private Surface surface;

    @Nullable
    private MediaPlayer mediaPlayer;

    @Nullable
    private MediaPlayer.OnPreparedListener preparedListener;

    @Nullable
    private MediaPlayer.OnErrorListener errorListener;

    @Nullable
    private MediaPlayer.OnCompletionListener completionListener;

    private boolean autoStart = false;
    private boolean prepared = false;
    private boolean looping = true;
    private boolean muted = false;

    private int metadataWidth = 0;
    private int metadataHeight = 0;
    private int metadataRotation = 0;

    public CircleVideoTextureView(@NonNull Context context) {
        super(context);
        init();
    }

    public CircleVideoTextureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleVideoTextureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSurfaceTextureListener(this);
        setOpaque(false);
    }

    public void setVideoURI(@Nullable Uri uri) {
        videoUri = uri;
        autoStart = false;
        readMetadata(uri);
        releasePlayer(false);
        prepareIfPossible();
    }

    public void setLooping(boolean looping) {
        this.looping = looping;

        if (mediaPlayer != null) {
            mediaPlayer.setLooping(looping);
        }
    }

    public void setMuted(boolean muted) {
        this.muted = muted;

        if (mediaPlayer != null) {
            float volume = muted ? 0f : 1f;
            mediaPlayer.setVolume(volume, volume);
        }
    }

    public void setOnPreparedListener(@Nullable MediaPlayer.OnPreparedListener listener) {
        preparedListener = listener;
    }

    public void setOnErrorListener(@Nullable MediaPlayer.OnErrorListener listener) {
        errorListener = listener;
    }

    public void setOnCompletionListener(@Nullable MediaPlayer.OnCompletionListener listener) {
        completionListener = listener;

        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(listener);
        }
    }

    public void start() {
        autoStart = true;

        if (mediaPlayer == null) {
            prepareIfPossible();
            return;
        }

        if (prepared) {
            try {
                mediaPlayer.start();
            } catch (Exception ignored) {
            }
        }
    }

    public void pause() {
        autoStart = false;

        if (mediaPlayer != null && prepared) {
            try {
                mediaPlayer.pause();
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isPlaying() {
        if (mediaPlayer == null || !prepared) {
            return false;
        }

        try {
            return mediaPlayer.isPlaying();
        } catch (Exception ignored) {
            return false;
        }
    }

    public int getDuration() {
        if (mediaPlayer == null || !prepared) {
            return 0;
        }

        try {
            return mediaPlayer.getDuration();
        } catch (Exception ignored) {
            return 0;
        }
    }

    public int getCurrentPosition() {
        if (mediaPlayer == null || !prepared) {
            return 0;
        }

        try {
            return mediaPlayer.getCurrentPosition();
        } catch (Exception ignored) {
            return 0;
        }
    }

    public void seekTo(int positionMs) {
        if (mediaPlayer == null || !prepared) {
            return;
        }

        try {
            mediaPlayer.seekTo(Math.max(0, positionMs));
        } catch (Exception ignored) {
        }
    }

    public void stopPlayback() {
        autoStart = false;
        releasePlayer(true);
    }

    private void prepareIfPossible() {
        if (videoUri == null || getSurfaceTexture() == null || mediaPlayer != null) {
            return;
        }

        try {
            surface = new Surface(getSurfaceTexture());
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getContext().getApplicationContext(), videoUri);
            mediaPlayer.setSurface(surface);
            mediaPlayer.setLooping(looping);

            float volume = muted ? 0f : 1f;
            mediaPlayer.setVolume(volume, volume);

            mediaPlayer.setOnPreparedListener(mp -> {
                prepared = true;
                applyCenterCropTransform(mp.getVideoWidth(), mp.getVideoHeight());

                if (preparedListener != null) {
                    preparedListener.onPrepared(mp);
                }

                if (autoStart) {
                    try {
                        mp.start();
                    } catch (Exception ignored) {
                    }
                }
            });

            mediaPlayer.setOnVideoSizeChangedListener((mp, width, height) ->
                    applyCenterCropTransform(width, height)
            );

            mediaPlayer.setOnCompletionListener(completionListener);

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                boolean handled = false;

                if (errorListener != null) {
                    handled = errorListener.onError(mp, what, extra);
                }

                if (!handled) {
                    releasePlayer(true);
                }

                return true;
            });

            mediaPlayer.prepareAsync();
        } catch (IOException | RuntimeException exception) {
            Log.w(TAG, "Failed to prepare circle video", exception);
            releasePlayer(true);
        }
    }

    private void readMetadata(@Nullable Uri uri) {
        metadataWidth = 0;
        metadataHeight = 0;
        metadataRotation = 0;

        if (uri == null) {
            return;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(getContext().getApplicationContext(), uri);

            metadataWidth = parseIntSafe(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            );

            metadataHeight = parseIntSafe(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            );

            metadataRotation = normalizeRotation(
                    parseIntSafe(
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    )
            );
        } catch (Exception exception) {
            metadataWidth = 0;
            metadataHeight = 0;
            metadataRotation = 0;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    private int parseIntSafe(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int normalizeRotation(int rotation) {
        int normalized = rotation % 360;

        if (normalized < 0) {
            normalized += 360;
        }

        if (normalized == 90 || normalized == 180 || normalized == 270) {
            return normalized;
        }

        return 0;
    }

    private void applyCenterCropTransform(int playerVideoWidth, int playerVideoHeight) {
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        int sourceWidth = playerVideoWidth > 0 ? playerVideoWidth : metadataWidth;
        int sourceHeight = playerVideoHeight > 0 ? playerVideoHeight : metadataHeight;

        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return;
        }

        boolean hasRotationMetadata = metadataRotation == 90 || metadataRotation == 270;
        boolean playerAlreadyAppliedRotation = false;

        if (hasRotationMetadata
                && metadataWidth > 0
                && metadataHeight > 0
                && playerVideoWidth > 0
                && playerVideoHeight > 0) {
            playerAlreadyAppliedRotation = approximatelySame(playerVideoWidth, metadataHeight)
                    && approximatelySame(playerVideoHeight, metadataWidth);
        }

        boolean shouldApplyRotation = hasRotationMetadata && !playerAlreadyAppliedRotation;

        int visualVideoWidth = shouldApplyRotation ? sourceHeight : sourceWidth;
        int visualVideoHeight = shouldApplyRotation ? sourceWidth : sourceHeight;

        float viewRatio = viewWidth / (float) viewHeight;
        float videoRatio = visualVideoWidth / (float) visualVideoHeight;

        float scaleX = 1f;
        float scaleY = 1f;

        if (videoRatio > viewRatio) {
            scaleX = videoRatio / viewRatio;
        } else if (videoRatio < viewRatio) {
            scaleY = viewRatio / videoRatio;
        }

        Matrix matrix = new Matrix();
        float pivotX = viewWidth * 0.5f;
        float pivotY = viewHeight * 0.5f;

        matrix.setScale(scaleX, scaleY, pivotX, pivotY);

        if (shouldApplyRotation) {
            matrix.postRotate(metadataRotation, pivotX, pivotY);
        }

        setTransform(matrix);
    }

    private boolean approximatelySame(int first, int second) {
        return Math.abs(first - second) <= 4;
    }

    private void releasePlayer(boolean clearUri) {
        prepared = false;

        if (mediaPlayer != null) {
            try {
                mediaPlayer.setOnPreparedListener(null);
                mediaPlayer.setOnErrorListener(null);
                mediaPlayer.setOnCompletionListener(null);
                mediaPlayer.setOnVideoSizeChangedListener(null);
            } catch (Exception ignored) {
            }

            try {
                if (prepared) {
                    mediaPlayer.stop();
                }
            } catch (Exception ignored) {
            }

            try {
                mediaPlayer.reset();
            } catch (Exception ignored) {
            }

            try {
                mediaPlayer.release();
            } catch (Exception ignored) {
            }

            mediaPlayer = null;
        }

        if (surface != null) {
            try {
                surface.release();
            } catch (Exception ignored) {
            }

            surface = null;
        }

        setTransform(null);

        if (clearUri) {
            videoUri = null;
            metadataWidth = 0;
            metadataHeight = 0;
            metadataRotation = 0;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        prepareIfPossible();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        if (mediaPlayer != null && prepared) {
            applyCenterCropTransform(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        releasePlayer(false);
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
    }

    @Override
    protected void onDetachedFromWindow() {
        stopPlayback();
        super.onDetachedFromWindow();
    }
}
