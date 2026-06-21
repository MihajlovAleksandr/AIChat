package com.example.aichat.view.main.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.RenderEffect;
import android.graphics.Typeface;
import android.Manifest;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.view.main.BaseActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import com.example.aichat.model.LocaleManager;
import com.example.aichat.model.utils.media.subtitles.LocalSubtitleTranslationManager;
import com.example.aichat.model.utils.media.subtitles.VideoSttSubtitleManager;
import com.example.aichat.model.utils.media.video.VideoPlayerReturnGuard;
import com.example.aichat.R;
import com.example.aichat.view.main.chat.helpers.VideoPlayerIntentFactory;
import com.example.aichat.model.utils.media.video.VideoQualityOption;
import com.example.aichat.model.utils.media.subtitles.VideoSubtitleConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@UnstableApi
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class VideoPlayerActivity extends BaseActivity {

    public static final String EXTRA_LOCAL_PATH = "localPath";
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_MIME_TYPE = "mimeType";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_TOKEN = "token";
    public static final String EXTRA_ENGINE = "engine";

    public static final String EXTRA_LOCAL_PATHS = "localPaths";
    public static final String EXTRA_URLS = "urls";
    public static final String EXTRA_MIME_TYPES = "mimeTypes";
    public static final String EXTRA_TITLES = "titles";
    public static final String EXTRA_INITIAL_INDEX = "initialIndex";

    public static final String EXTRA_YOUTUBE_URL = "youtubeUrl";
    public static final String EXTRA_IS_YOUTUBE = "isYouTube";
    public static final String EXTRA_STT_LANGUAGE = "sttLanguage";
    public static final String EXTRA_STT_LANGUAGES = "sttLanguages";

    public static final String ENGINE_EXO_PLAYER = "exo";
    public static final String ENGINE_MEDIA_PLAYER = "media";

    private static final int REQUEST_RECORD_AUDIO_FOR_STT = 7781;
    private static final long DOUBLE_TAP_SEEK_MS = 5000L;
    private static final String TAG = "VideoPlayerActivity";

    private static final String STATE_CURRENT_INDEX = "state_current_index";
    private static final String STATE_POSITION_MS = "state_position_ms";
    private static final String STATE_PLAY_WHEN_READY = "state_play_when_ready";
    private static final String STATE_ENGINE = "state_engine";
    private static final String STATE_ZOOM = "state_zoom";
    private static final String STATE_QUALITY_KEY = "state_quality_key";
    private static final String STATE_RESIZE_MODE = "state_resize_mode";
    private static final String STATE_ENHANCEMENT_MODE = "state_enhancement_mode";
    private static final String STATE_CAPTIONS_ENABLED = "state_captions_enabled";
    private static final String STATE_STT_CAPTIONS_ENABLED = "state_stt_captions_enabled";
    private static final String STATE_STT_LANGUAGE = "state_stt_language";
    private static final String STATE_STT_TRANSLATION_MODE = "state_stt_translation_mode";

    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 4.0f;
    private static final int BOTTOM_GESTURE_GUARD_DP = 148;

    private static final String PANEL_NONE = "none";
    private static final String PANEL_QUALITY = "quality";
    private static final String PANEL_SCALE = "scale";
    private static final String PANEL_FILTER = "filter";
    private static final String PANEL_CAPTIONS = "captions";

    private static final String STT_LANGUAGE_RU = "ru";
    private static final String STT_LANGUAGE_EN = "en";
    private static final String STT_LANGUAGE_ZH = "zh";

    private static final String SUBTITLE_MODE_ORIGINAL = "original";
    private static final String SUBTITLE_MODE_TRANSLATED = "translated";
    private static final String SUBTITLE_MODE_BILINGUAL = "bilingual";

    private static final int FILTER_NONE = 0;
    private static final int FILTER_BRIGHTNESS = 1;
    private static final int FILTER_SHARPNESS = 2;
    private static final int FILTER_BRIGHTNESS_SHARPNESS = 3;
    private static final int FILTER_BLACK_WHITE = 4;
    private static final int FILTER_NOIR = 5;
    private static final int FILTER_WARM = 6;
    private static final int FILTER_COLD = 7;
    private static final int FILTER_HIGH_CONTRAST = 8;

    private View rootView;
    private View controlsOverlay;
    private View videoContentContainer;
    private View brightnessOverlay;
    private View sharpnessOverlay;
    private LinearLayout topBar;
    private LinearLayout bottomBar;
    private LinearLayout optionsPanel;
    private LinearLayout optionsContainer;
    private PlayerView exoPlayerView;
    private SurfaceView mediaSurfaceView;
    private WebView youtubeWebView;
    private ProgressBar progressBar;
    private TextView titleView;
    private TextView optionsPanelTitle;
    private TextView qualityButton;
    private TextView enhanceButton;
    private TextView filterButton;
    private TextView captionsButton;
    private TextView autoCaptionsOverlay;
    private TextView openYoutubeButton;
    private TextView seekFeedbackLeft;
    private TextView seekFeedbackRight;
    private ImageButton closeButton;

    private ExoPlayer exoPlayer;
    private DefaultTrackSelector trackSelector;
    private MediaPlayer mediaPlayer;
    private MediaController mediaController;

    private final ArrayList<String> localPaths = new ArrayList<>();
    private final ArrayList<String> urls = new ArrayList<>();
    private final ArrayList<String> mimeTypes = new ArrayList<>();
    private final ArrayList<String> titles = new ArrayList<>();
    private final ArrayList<String> sttLanguages = new ArrayList<>();

    private String token;
    private String preferredEngine;
    private String youtubeUrl;
    private boolean isYouTube;
    private int currentIndex = 0;
    private long startPositionMs = C.TIME_UNSET;
    private boolean startPlayWhenReady = true;
    private boolean wasRestoredFromState = false;
    private boolean mediaPlayerFallbackAlreadyTried = false;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = this::hideControls;
    private boolean controlsVisible = true;

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private float zoomScale = 1f;

    private final List<VideoQualityOption> availableQualityOptions = new ArrayList<>();
    private String selectedQualityKey = VideoQualityOption.KEY_AUTO;
    private String selectedQualityLabel = "Auto";
    private int selectedMaxWidth = Integer.MAX_VALUE;
    private int selectedMaxHeight = Integer.MAX_VALUE;
    private int detectedMaxWidth = 0;
    private int detectedMaxHeight = 0;

    private int resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private int filterMode = FILTER_NONE;
    private boolean captionsEnabled = true;
    private boolean sttCaptionsEnabled = false;
    private String selectedSttLanguage = STT_LANGUAGE_RU;
    private boolean sttListeningActive = false;
    private boolean activityResumed = false;
    private boolean pendingSttPermissionStart = false;
    private VideoSttSubtitleManager sttSubtitleManager;
    private LocalSubtitleTranslationManager subtitleTranslator;
    private String sttTranslationMode = SUBTITLE_MODE_ORIGINAL;
    private String activePanel = PANEL_NONE;

    public static Intent createLocalIntent(
            @NonNull Context context,
            @NonNull String localPath,
            @Nullable String mimeType,
            @Nullable String title
    ) {
        return VideoPlayerIntentFactory.createLocalIntent(context, localPath, mimeType, title);
    }

    public static Intent createLocalIntent(
            @NonNull Context context,
            @NonNull String localPath,
            @Nullable String mimeType,
            @Nullable String title,
            @Nullable String sttLanguage
    ) {
        return VideoPlayerIntentFactory.createLocalIntent(context, localPath, mimeType, title, sttLanguage);
    }

    public static Intent createRemoteIntent(
            @NonNull Context context,
            @NonNull String url,
            @Nullable String mimeType,
            @Nullable String title,
            @Nullable String token
    ) {
        return VideoPlayerIntentFactory.createRemoteIntent(context, url, mimeType, title, token);
    }

    public static Intent createRemoteIntent(
            @NonNull Context context,
            @NonNull String url,
            @Nullable String mimeType,
            @Nullable String title,
            @Nullable String token,
            @Nullable String sttLanguage
    ) {
        return VideoPlayerIntentFactory.createRemoteIntent(context, url, mimeType, title, token, sttLanguage);
    }

    public static Intent createYouTubeIntent(
            @NonNull Context context,
            @NonNull String youtubeUrl,
            @Nullable String title
    ) {
        return VideoPlayerIntentFactory.createYouTubeIntent(context, youtubeUrl, title);
    }

    public static Intent createGalleryIntent(
            @NonNull Context context,
            @NonNull ArrayList<String> localPaths,
            @NonNull ArrayList<String> urls,
            @NonNull ArrayList<String> mimeTypes,
            @NonNull ArrayList<String> titles,
            int initialIndex,
            @Nullable String token
    ) {
        return VideoPlayerIntentFactory.createGalleryIntent(
                context,
                localPaths,
                urls,
                mimeTypes,
                titles,
                initialIndex,
                token
        );
    }

    public static Intent createGalleryIntent(
            @NonNull Context context,
            @NonNull ArrayList<String> localPaths,
            @NonNull ArrayList<String> urls,
            @NonNull ArrayList<String> mimeTypes,
            @NonNull ArrayList<String> titles,
            @Nullable ArrayList<String> sttLanguages,
            int initialIndex,
            @Nullable String token
    ) {
        return VideoPlayerIntentFactory.createGalleryIntent(
                context,
                localPaths,
                urls,
                mimeTypes,
                titles,
                sttLanguages,
                initialIndex,
                token
        );
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        VideoPlayerReturnGuard.markStarted(this);
        configureWindowForVideo();
        forceFullscreenVideoMode();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        bindViews();
        disableForceDarkForViewTree(rootView);
        setupGestures();
        applyAdaptiveInsets();
        readIntent();
        restorePlaybackState(savedInstanceState);
        setupControls();
        setupSttSubtitleManager();
        subtitleTranslator = new LocalSubtitleTranslationManager(this);
        applyEnhancementState();
        updateCaptionsButtonText();

        Log.d(TAG, "onCreate, restored=" + wasRestoredFromState
                + ", index=" + currentIndex
                + ", position=" + startPositionMs
                + ", engine=" + preferredEngine
                + ", youtube=" + isYouTube);

        if (isYouTube) {
            startYouTubePlayer();
        } else if (ENGINE_MEDIA_PLAYER.equals(preferredEngine)) {
            startMediaPlayer();
        } else {
            startExoPlayer();
        }

        hideControlsImmediately();
    }

    private void configureWindowForVideo() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.BLACK));
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        disableForceDarkForWindow();

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        getWindow().setAttributes(params);
    }

    private void forceFullscreenVideoMode() {
        View decorView = getWindow().getDecorView();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), decorView);

        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            forceFullscreenVideoMode();
        }
    }

    private void disableForceDarkForWindow() {
        try {
            getWindow().getDecorView().setForceDarkAllowed(false);
        } catch (Exception ignored) {
        }
    }

    private void disableForceDarkForViewTree(@Nullable View view) {
        if (view == null) {
            return;
        }

        try {
            view.setForceDarkAllowed(false);
        } catch (Exception ignored) {
        }

        if (!(view instanceof ViewGroup)) {
            return;
        }

        ViewGroup group = (ViewGroup) view;

        for (int i = 0; i < group.getChildCount(); i++) {
            disableForceDarkForViewTree(group.getChildAt(i));
        }
    }


    private void bindViews() {
        rootView = findViewById(R.id.video_player_root);
        controlsOverlay = findViewById(R.id.video_controls_overlay);
        videoContentContainer = findViewById(R.id.video_content_container);
        brightnessOverlay = findViewById(R.id.video_brightness_overlay);
        sharpnessOverlay = findViewById(R.id.video_sharpness_overlay);
        topBar = findViewById(R.id.video_top_bar);
        bottomBar = findViewById(R.id.video_bottom_bar);
        optionsPanel = findViewById(R.id.video_quality_panel);
        optionsPanelTitle = findViewById(R.id.video_quality_panel_title);
        optionsContainer = findViewById(R.id.video_quality_options);
        exoPlayerView = findViewById(R.id.video_exo_player_view);

        if (exoPlayerView != null) {
            exoPlayerView.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER);
            exoPlayerView.setKeepContentOnPlayerReset(true);
        }

        mediaSurfaceView = findViewById(R.id.video_media_surface);
        youtubeWebView = findViewById(R.id.video_youtube_webview);
        progressBar = findViewById(R.id.video_progress);
        titleView = findViewById(R.id.video_title);
        qualityButton = findViewById(R.id.video_quality);
        enhanceButton = findViewById(R.id.video_enhance);
        captionsButton = findViewById(R.id.video_captions);
        ensureExtraBottomButtons();
        ensureAutoCaptionsOverlay();
        openYoutubeButton = findViewById(R.id.video_open_youtube);
        seekFeedbackLeft = findViewById(R.id.video_seek_feedback_left);
        seekFeedbackRight = findViewById(R.id.video_seek_feedback_right);
        closeButton = findViewById(R.id.video_close);
    }


    private void ensureExtraBottomButtons() {
        if (bottomBar == null || filterButton != null) {
            return;
        }

        filterButton = new TextView(this);
        filterButton.setId(View.generateViewId());
        filterButton.setText(R.string.video_player_filter_short);
        filterButton.setTextColor(Color.WHITE);
        filterButton.setTextSize(12f);
        filterButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        filterButton.setGravity(Gravity.CENTER);
        filterButton.setMinWidth(dp(44));
        filterButton.setPadding(dp(10), 0, dp(10), 0);
        filterButton.setBackgroundColor(Color.TRANSPARENT);
        filterButton.setContentDescription(getString(R.string.video_player_filters_content_description));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(40)
        );

        int insertIndex = bottomBar.indexOfChild(qualityButton);
        if (insertIndex < 0) {
            bottomBar.addView(filterButton, params);
        } else {
            bottomBar.addView(filterButton, insertIndex, params);
        }
    }

    private void ensureAutoCaptionsOverlay() {
        if (!(rootView instanceof ViewGroup) || autoCaptionsOverlay != null) {
            return;
        }

        autoCaptionsOverlay = new TextView(this);
        autoCaptionsOverlay.setTextColor(Color.WHITE);
        autoCaptionsOverlay.setTextSize(16f);
        autoCaptionsOverlay.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        autoCaptionsOverlay.setGravity(Gravity.CENTER);
        autoCaptionsOverlay.setPadding(dp(16), dp(9), dp(16), dp(9));
        autoCaptionsOverlay.setBackgroundColor(0xB0000000);
        autoCaptionsOverlay.setMaxLines(3);
        autoCaptionsOverlay.setLineSpacing(dp(1), 1.04f);
        autoCaptionsOverlay.setMaxWidth(getResources().getDisplayMetrics().widthPixels - dp(48));
        autoCaptionsOverlay.setVisibility(View.GONE);
        autoCaptionsOverlay.setAlpha(0f);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
        );
        params.leftMargin = dp(20);
        params.rightMargin = dp(20);
        params.bottomMargin = dp(118);

        ((ViewGroup) rootView).addView(autoCaptionsOverlay, params);
    }

    private void setupSttSubtitleManager() {
        sttSubtitleManager = new VideoSttSubtitleManager(this, new VideoSttSubtitleManager.Listener() {
            @Override
            public void onPartialText(@NonNull String text) {
                showSttCaptionText(text, false);
            }

            @Override
            public void onFinalText(@NonNull String text) {
                showSttCaptionText(text, true);
            }

            @Override
            public void onInfo(@NonNull String message) {
            }

            @Override
            public void onError(@NonNull String message) {
                showAutoCaptionText(message);
                sttCaptionsEnabled = false;
                updateCaptionsButtonText();
                if (PANEL_CAPTIONS.equals(activePanel) && isOptionsPanelVisible()) {
                    renderCaptionsOptions();
                }
            }
        });
    }

    private void setupControls() {
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> finish());
        }

        if (qualityButton != null) {
            qualityButton.setOnClickListener(v -> {
                showControls();
                toggleOptionsPanel(PANEL_QUALITY);
            });
        }

        if (enhanceButton != null) {
            enhanceButton.setText(R.string.video_player_scale_short);
            enhanceButton.setContentDescription(getString(R.string.video_player_scale_content_description));
            enhanceButton.setOnClickListener(v -> {
                showControls();
                toggleOptionsPanel(PANEL_SCALE);
            });
        }

        if (filterButton != null) {
            filterButton.setOnClickListener(v -> {
                showControls();
                toggleOptionsPanel(PANEL_FILTER);
            });
        }

        if (captionsButton != null) {
            captionsButton.setOnClickListener(v -> {
                showControls();
                toggleOptionsPanel(PANEL_CAPTIONS);
            });
        }

        if (openYoutubeButton != null) {
            openYoutubeButton.setOnClickListener(v -> openCurrentYouTubeInExternalApp());
        }

        updateYouTubeControlsVisibility();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupGestures() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
                if (isOptionsPanelVisible()) {
                    hideOptionsPanel();
                    hideControls();
                    return true;
                }

                if (controlsVisible) {
                    hideControls();
                } else {
                    showControls();
                }

                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent event) {
                if (isYouTube || rootView == null) {
                    return true;
                }

                float x = event.getRawX();
                int[] rootLocation = new int[2];
                rootView.getLocationOnScreen(rootLocation);
                int width = rootView.getWidth();

                if (width <= 0) {
                    return true;
                }

                float localX = x - rootLocation[0];

                if (localX < width / 2f) {
                    seekByMillis(-DOUBLE_TAP_SEEK_MS);
                    showSeekFeedback(false);
                } else {
                    seekByMillis(DOUBLE_TAP_SEEK_MS);
                    showSeekFeedback(true);
                }

                showControls();
                return true;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                if (isYouTube) {
                    return false;
                }

                zoomScale *= detector.getScaleFactor();
                zoomScale = Math.max(MIN_ZOOM, Math.min(zoomScale, MAX_ZOOM));
                applyZoom();
                showControls();
                return true;
            }
        });

        View.OnTouchListener touchListener = (view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                    && isOptionsPanelVisible()
                    && !isTouchInsideView(event, optionsPanel)
                    && !isTouchInsideBottomButtons(event)) {
                hideOptionsPanel();
                hideControls();
                return true;
            }

            if (isTouchInsideControls(event)) {
                handleControlsTouch();
                return false;
            }

            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                uiHandler.removeCallbacks(hideControlsRunnable);
            }

            if (scaleGestureDetector != null) {
                scaleGestureDetector.onTouchEvent(event);
            }

            if (gestureDetector != null
                    && (scaleGestureDetector == null || !scaleGestureDetector.isInProgress())) {
                gestureDetector.onTouchEvent(event);
            }

            return false;
        };

        if (rootView != null) {
            rootView.setOnTouchListener(touchListener);
        }

        if (exoPlayerView != null) {
            exoPlayerView.setOnTouchListener(touchListener);
        }

        if (mediaSurfaceView != null) {
            mediaSurfaceView.setOnTouchListener(touchListener);
        }
    }

    private void handleControlsTouch() {
        uiHandler.removeCallbacks(hideControlsRunnable);
    }

    private boolean isTouchInsideControls(@NonNull MotionEvent event) {
        if (!controlsVisible) {
            return false;
        }

        return isTouchInsideView(event, topBar)
                || isTouchInsideView(event, bottomBar)
                || isTouchInsideView(event, optionsPanel)
                || isTouchInsideBottomPlayerControls(event);
    }

    private boolean isTouchInsideBottomButtons(@NonNull MotionEvent event) {
        return isTouchInsideView(event, qualityButton)
                || isTouchInsideView(event, enhanceButton)
                || isTouchInsideView(event, filterButton)
                || isTouchInsideView(event, captionsButton)
                || isTouchInsideView(event, openYoutubeButton);
    }

    private boolean isTouchInsideBottomPlayerControls(@NonNull MotionEvent event) {
        if (rootView == null) {
            return false;
        }

        int[] rootLocation = new int[2];
        rootView.getLocationOnScreen(rootLocation);

        float rawY = event.getRawY();
        int bottom = rootLocation[1] + rootView.getHeight();
        int guardTop = bottom - dp(BOTTOM_GESTURE_GUARD_DP);

        return rawY >= guardTop && rawY <= bottom;
    }

    private boolean isTouchInsideView(@NonNull MotionEvent event, @Nullable View view) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return false;
        }

        int[] location = new int[2];
        view.getLocationOnScreen(location);

        float rawX = event.getRawX();
        float rawY = event.getRawY();

        return rawX >= location[0]
                && rawX <= location[0] + view.getWidth()
                && rawY >= location[1]
                && rawY <= location[1] + view.getHeight();
    }

    private void applyAdaptiveInsets() {
        if (rootView == null || topBar == null) {
            return;
        }

        int baseTopBarPaddingStart = topBar.getPaddingStart();
        int baseTopBarPaddingTop = topBar.getPaddingTop();
        int baseTopBarPaddingEnd = topBar.getPaddingEnd();
        int baseTopBarPaddingBottom = topBar.getPaddingBottom();

        int baseBottomBarPaddingStart = bottomBar != null ? bottomBar.getPaddingStart() : 0;
        int baseBottomBarPaddingTop = bottomBar != null ? bottomBar.getPaddingTop() : 0;
        int baseBottomBarPaddingEnd = bottomBar != null ? bottomBar.getPaddingEnd() : 0;
        int baseBottomBarPaddingBottom = bottomBar != null ? bottomBar.getPaddingBottom() : 0;

        int baseOptionsPanelPaddingStart = optionsPanel != null ? optionsPanel.getPaddingStart() : 0;
        int baseOptionsPanelPaddingTop = optionsPanel != null ? optionsPanel.getPaddingTop() : 0;
        int baseOptionsPanelPaddingEnd = optionsPanel != null ? optionsPanel.getPaddingEnd() : 0;
        int baseOptionsPanelPaddingBottom = optionsPanel != null ? optionsPanel.getPaddingBottom() : 0;

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            topBar.setPaddingRelative(
                    baseTopBarPaddingStart + systemBars.left,
                    baseTopBarPaddingTop + systemBars.top,
                    baseTopBarPaddingEnd + systemBars.right,
                    baseTopBarPaddingBottom
            );

            if (bottomBar != null) {
                bottomBar.setPaddingRelative(
                        baseBottomBarPaddingStart,
                        baseBottomBarPaddingTop,
                        baseBottomBarPaddingEnd + systemBars.right,
                        baseBottomBarPaddingBottom + systemBars.bottom
                );
            }

            if (optionsPanel != null) {
                optionsPanel.setPaddingRelative(
                        baseOptionsPanelPaddingStart + systemBars.left,
                        baseOptionsPanelPaddingTop,
                        baseOptionsPanelPaddingEnd + systemBars.right,
                        baseOptionsPanelPaddingBottom + systemBars.bottom
                );
            }

            if (exoPlayerView != null) {
                exoPlayerView.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            }

            if (mediaSurfaceView != null) {
                mediaSurfaceView.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            }

            if (youtubeWebView != null) {
                youtubeWebView.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            }

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.requestApplyInsets(rootView);
    }

    private void readIntent() {
        Intent intent = getIntent();

        isYouTube = intent.getBooleanExtra(EXTRA_IS_YOUTUBE, false);
        youtubeUrl = intent.getStringExtra(EXTRA_YOUTUBE_URL);

        ArrayList<String> incomingLocalPaths = intent.getStringArrayListExtra(EXTRA_LOCAL_PATHS);
        ArrayList<String> incomingUrls = intent.getStringArrayListExtra(EXTRA_URLS);
        ArrayList<String> incomingMimeTypes = intent.getStringArrayListExtra(EXTRA_MIME_TYPES);
        ArrayList<String> incomingTitles = intent.getStringArrayListExtra(EXTRA_TITLES);
        ArrayList<String> incomingSttLanguages = intent.getStringArrayListExtra(EXTRA_STT_LANGUAGES);

        if (incomingLocalPaths != null) {
            localPaths.addAll(incomingLocalPaths);
        }

        if (incomingUrls != null) {
            urls.addAll(incomingUrls);
        }

        if (incomingMimeTypes != null) {
            mimeTypes.addAll(incomingMimeTypes);
        }

        if (incomingTitles != null) {
            titles.addAll(incomingTitles);
        }

        if (incomingSttLanguages != null) {
            for (String language : incomingSttLanguages) {
                sttLanguages.add(normalizeSttLanguageCode(language));
            }
        }

        if (localPaths.isEmpty() && urls.isEmpty() && !isYouTube) {
            String singleLocalPath = intent.getStringExtra(EXTRA_LOCAL_PATH);
            String singleUrl = intent.getStringExtra(EXTRA_URL);
            String singleMimeType = intent.getStringExtra(EXTRA_MIME_TYPE);
            String singleTitle = intent.getStringExtra(EXTRA_TITLE);

            localPaths.add(singleLocalPath != null ? singleLocalPath : "");
            urls.add(singleUrl != null ? singleUrl : "");
            mimeTypes.add(singleMimeType != null ? singleMimeType : "");
            titles.add(singleTitle != null ? singleTitle : "");
        }

        if (isYouTube) {
            String singleTitle = intent.getStringExtra(EXTRA_TITLE);
            if (titles.isEmpty()) {
                titles.add(singleTitle != null ? singleTitle : "YouTube");
            }
        }

        normalizeGalleryLists();

        currentIndex = intent.getIntExtra(EXTRA_INITIAL_INDEX, 0);

        if (currentIndex < 0 || currentIndex >= getVideoCount()) {
            currentIndex = 0;
        }

        token = intent.getStringExtra(EXTRA_TOKEN);
        preferredEngine = intent.getStringExtra(EXTRA_ENGINE);

        String incomingSttLanguage = intent.getStringExtra(EXTRA_STT_LANGUAGE);
        if (incomingSttLanguage != null && !incomingSttLanguage.trim().isEmpty()) {
            selectedSttLanguage = normalizeSttLanguageCode(incomingSttLanguage);
            setSttLanguageForCurrentVideo(selectedSttLanguage);
        } else {
            selectedSttLanguage = getSttLanguageForCurrentVideo();
        }

        if (preferredEngine == null || preferredEngine.trim().isEmpty()) {
            preferredEngine = ENGINE_EXO_PLAYER;
        }

        updateTitle();
        updateNavigationButtons();
    }

    private void restorePlaybackState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        wasRestoredFromState = true;

        int restoredIndex = savedInstanceState.getInt(STATE_CURRENT_INDEX, currentIndex);

        if (restoredIndex >= 0 && restoredIndex < getVideoCount()) {
            currentIndex = restoredIndex;
        }

        startPositionMs = savedInstanceState.getLong(STATE_POSITION_MS, C.TIME_UNSET);
        startPlayWhenReady = savedInstanceState.getBoolean(STATE_PLAY_WHEN_READY, true);

        String restoredEngine = savedInstanceState.getString(STATE_ENGINE);

        if (restoredEngine != null && !restoredEngine.trim().isEmpty()) {
            preferredEngine = restoredEngine;
        }

        zoomScale = savedInstanceState.getFloat(STATE_ZOOM, 1f);
        zoomScale = Math.max(MIN_ZOOM, Math.min(zoomScale, MAX_ZOOM));

        selectedQualityKey = savedInstanceState.getString(STATE_QUALITY_KEY, VideoQualityOption.KEY_AUTO);
        resizeMode = savedInstanceState.getInt(STATE_RESIZE_MODE, AspectRatioFrameLayout.RESIZE_MODE_FIT);
        filterMode = savedInstanceState.getInt(STATE_ENHANCEMENT_MODE, FILTER_NONE);
        captionsEnabled = savedInstanceState.getBoolean(STATE_CAPTIONS_ENABLED, true);
        sttCaptionsEnabled = savedInstanceState.getBoolean(STATE_STT_CAPTIONS_ENABLED, false);
        selectedSttLanguage = normalizeSttLanguageCode(
                savedInstanceState.getString(STATE_STT_LANGUAGE, selectedSttLanguage)
        );
        sttTranslationMode = normalizeSubtitleTranslationMode(
                savedInstanceState.getString(STATE_STT_TRANSLATION_MODE, sttTranslationMode)
        );
        setSttLanguageForCurrentVideo(selectedSttLanguage);

        updateTitle();
        updateNavigationButtons();
    }

    private void normalizeGalleryLists() {
        int count = Math.max(localPaths.size(), urls.size());

        if (count == 0 && isYouTube) {
            count = 1;
        }

        while (localPaths.size() < count) {
            localPaths.add("");
        }

        while (urls.size() < count) {
            urls.add("");
        }

        while (mimeTypes.size() < count) {
            mimeTypes.add("");
        }

        while (titles.size() < count) {
            titles.add("");
        }

        while (sttLanguages.size() < count) {
            sttLanguages.add(selectedSttLanguage);
        }
    }

    private int getVideoCount() {
        if (isYouTube) {
            return 1;
        }

        return Math.max(localPaths.size(), urls.size());
    }

    private void updateYouTubeControlsVisibility() {
        int youtubeVisibility = isYouTube ? View.VISIBLE : View.GONE;
        int regularVisibility = isYouTube ? View.GONE : View.VISIBLE;

        if (openYoutubeButton != null) {
            openYoutubeButton.setVisibility(youtubeVisibility);
        }

        if (qualityButton != null) {
            qualityButton.setVisibility(regularVisibility);
        }

        if (enhanceButton != null) {
            enhanceButton.setVisibility(regularVisibility);
        }

        if (filterButton != null) {
            filterButton.setVisibility(regularVisibility);
        }

        if (captionsButton != null) {
            captionsButton.setVisibility(regularVisibility);
        }
    }

    private void startExoPlayer() {
        stopSttListeningOnly();
        releaseMediaPlayer();
        releaseYouTubePlayer();
        preferredEngine = ENGINE_EXO_PLAYER;
        isYouTube = false;
        updateYouTubeControlsVisibility();

        if (exoPlayerView != null) {
            exoPlayerView.setVisibility(View.VISIBLE);
        }

        if (mediaSurfaceView != null) {
            mediaSurfaceView.setVisibility(View.GONE);
        }

        if (youtubeWebView != null) {
            youtubeWebView.setVisibility(View.GONE);
        }

        showLoading(true);

        try {
            DefaultDataSource.Factory dataSourceFactory = buildDataSourceFactory();
            trackSelector = new DefaultTrackSelector(this);
            applyCurrentQualityToTrackSelector();
            applyCaptionStateToTrackSelector();

            exoPlayer = new ExoPlayer.Builder(this)
                    .setTrackSelector(trackSelector)
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
                    .build();

            if (exoPlayerView != null) {
                exoPlayerView.setPlayer(exoPlayer);
            }

            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                        showLoading(false);
                        rebuildQualityOptionsFromCurrentTracks();
                    }

                    if (playbackState == Player.STATE_READY && exoPlayer != null && exoPlayer.getPlayWhenReady()) {
                        hideControlsImmediately();
                    }

                    if (playbackState == Player.STATE_ENDED) {
                        showControls();
                    }

                    syncSttListeningWithPlayback();
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    syncSttListeningWithPlayback();
                }

                @Override
                public void onTracksChanged(@NonNull Tracks tracks) {
                    rebuildQualityOptionsFromTracks(tracks);
                }

                @Override
                public void onRenderedFirstFrame() {
                    showLoading(false);
                    forceFullscreenVideoMode();
                }

                @Override
                public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                    if (exoPlayer != null) {
                        int index = exoPlayer.getCurrentMediaItemIndex();

                        if (index >= 0 && index < getVideoCount()) {
                            currentIndex = index;
                            selectedSttLanguage = getSttLanguageForCurrentVideo();
                            restartSttForCurrentVideoIfNeeded();
                            resetQualityForNewVideo(false);
                            updateTitle();
                            updateNavigationButtons();
                        }
                    }
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    if (!mediaPlayerFallbackAlreadyTried) {
                        mediaPlayerFallbackAlreadyTried = true;
                        startMediaPlayer();
                    } else {
                        showLoading(false);
                        Toast.makeText(VideoPlayerActivity.this, R.string.video_player_error, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            List<MediaItem> mediaItems = buildMediaItems();

            if (mediaItems.isEmpty()) {
                showLoading(false);
                Toast.makeText(this, R.string.video_player_error, Toast.LENGTH_SHORT).show();
                return;
            }

            long startPosition = startPositionMs != C.TIME_UNSET
                    ? Math.max(0L, startPositionMs)
                    : C.TIME_UNSET;

            exoPlayer.setMediaItems(mediaItems, currentIndex, startPosition);
            exoPlayer.prepare();
            exoPlayer.setPlayWhenReady(startPlayWhenReady);
            startPositionMs = C.TIME_UNSET;
            startPlayWhenReady = true;
            applyZoom();
            applyEnhancementState();

        } catch (Exception e) {
            if (!mediaPlayerFallbackAlreadyTried) {
                mediaPlayerFallbackAlreadyTried = true;
                startMediaPlayer();
            } else {
                showLoading(false);
                Toast.makeText(this, R.string.video_player_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void startYouTubePlayer() {
        releaseExoPlayer();
        releaseMediaPlayer();
        releaseYouTubePlayer();

        isYouTube = true;
        updateYouTubeControlsVisibility();
        hideOptionsPanel();
        updateTitle();
        showLoading(false);

        if (exoPlayerView != null) {
            exoPlayerView.setVisibility(View.GONE);
        }

        if (mediaSurfaceView != null) {
            mediaSurfaceView.setVisibility(View.GONE);
        }

        if (youtubeWebView != null) {
            youtubeWebView.setVisibility(View.GONE);
            try {
                youtubeWebView.stopLoading();
                youtubeWebView.loadUrl("about:blank");
            } catch (Exception ignored) {
            }
        }

        Log.w(TAG, "YouTube WebView playback is disabled. Opening externally: " + youtubeUrl);

        openCurrentYouTubeInExternalApp();

        uiHandler.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                finish();
            }
        }, 250L);
    }







    private void openCurrentYouTubeInExternalApp() {
        String targetUrl = getCurrentYouTubeUrl();

        if (targetUrl == null || targetUrl.trim().isEmpty()) {
            return;
        }

        try {
            Intent youtubeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
            youtubeIntent.setPackage("com.google.android.youtube");
            startActivity(youtubeIntent);
            return;
        } catch (Exception ignored) {
        }

        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
            startActivity(browserIntent);
        } catch (Exception ignored) {
            Toast.makeText(this, R.string.video_player_error, Toast.LENGTH_SHORT).show();
        }
    }

    private String getCurrentYouTubeUrl() {
        if (youtubeUrl != null && !youtubeUrl.trim().isEmpty()) {
            return youtubeUrl;
        }

        if (!urls.isEmpty()) {
            String firstUrl = urls.get(0);

            if (firstUrl != null && !firstUrl.trim().isEmpty()) {
                return firstUrl;
            }
        }

        return "https://www.youtube.com";
    }



    private List<MediaItem> buildMediaItems() {
        List<MediaItem> items = new ArrayList<>();

        for (int i = 0; i < getVideoCount(); i++) {
            Uri uri = resolveVideoUri(i);

            if (uri != null) {
                items.add(MediaItem.fromUri(uri));
            }
        }

        return items;
    }

    private DefaultDataSource.Factory buildDataSourceFactory() {
        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory();
        Map<String, String> headers = new HashMap<>();

        headers.put("device", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);

        if (token != null && !token.trim().isEmpty()) {
            headers.put("Authorization", "Bearer " + token);
        }

        httpFactory.setDefaultRequestProperties(headers);

        return new DefaultDataSource.Factory(this, httpFactory);
    }

    private void startMediaPlayer() {
        stopSttListeningOnly();
        releaseExoPlayer();
        releaseYouTubePlayer();
        preferredEngine = ENGINE_MEDIA_PLAYER;
        isYouTube = false;
        updateYouTubeControlsVisibility();

        if (exoPlayerView != null) {
            exoPlayerView.setVisibility(View.GONE);
        }

        if (mediaSurfaceView != null) {
            mediaSurfaceView.setVisibility(View.VISIBLE);
        }

        if (youtubeWebView != null) {
            youtubeWebView.setVisibility(View.GONE);
        }

        showLoading(true);

        if (mediaSurfaceView == null) {
            showLoading(false);
            return;
        }

        SurfaceHolder holder = mediaSurfaceView.getHolder();

        if (holder.getSurface() != null && holder.getSurface().isValid()) {
            prepareMediaPlayer(holder);
            return;
        }

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                prepareMediaPlayer(holder);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            }
        });
    }

    private void prepareMediaPlayer(SurfaceHolder holder) {
        try {
            releaseMediaPlayer();
            Uri currentUri = resolveVideoUri(currentIndex);

            if (currentUri == null) {
                showLoading(false);
                Toast.makeText(this, R.string.video_player_error, Toast.LENGTH_SHORT).show();
                return;
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setSurface(holder.getSurface());

            String currentLocalPath = getLocalPath(currentIndex);

            if (currentLocalPath != null && !currentLocalPath.trim().isEmpty()) {
                mediaPlayer.setDataSource(currentLocalPath);
            } else {
                Map<String, String> headers = new HashMap<>();
                headers.put("device", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);

                if (token != null && !token.trim().isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                }

                mediaPlayer.setDataSource(this, currentUri, headers);
            }

            mediaPlayer.setOnPreparedListener(mp -> {
                showLoading(false);
                attachMediaController();

                if (startPositionMs != C.TIME_UNSET && startPositionMs > 0) {
                    mp.seekTo((int) Math.min(startPositionMs, Integer.MAX_VALUE));
                }

                if (startPlayWhenReady) {
                    mp.start();
                }

                syncSttListeningWithPlayback();

                startPositionMs = C.TIME_UNSET;
                startPlayWhenReady = true;
                applyZoom();
                applyEnhancementState();
                updateTitle();
                updateNavigationButtons();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                stopSttListeningOnly();

                if (currentIndex < getVideoCount() - 1) {
                    playNextVideo();
                    return;
                }

                if (mediaController != null) {
                    mediaController.show(1500);
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stopSttListeningOnly();
                showLoading(false);
                Toast.makeText(VideoPlayerActivity.this, R.string.video_player_error, Toast.LENGTH_SHORT).show();
                return true;
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            showLoading(false);
            Toast.makeText(this, R.string.video_player_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void attachMediaController() {
        if (mediaSurfaceView == null || mediaPlayer == null) {
            return;
        }

        mediaController = new MediaController(this);
        mediaController.setAnchorView(mediaSurfaceView);
        mediaController.setMediaPlayer(new MediaController.MediaPlayerControl() {
            @Override public void start() {
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                    syncSttListeningWithPlayback();
                }
            }

            @Override public void pause() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    syncSttListeningWithPlayback();
                }
            }
            @Override public int getDuration() { return mediaPlayer != null ? mediaPlayer.getDuration() : 0; }
            @Override public int getCurrentPosition() { return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0; }
            @Override public void seekTo(int pos) { if (mediaPlayer != null) mediaPlayer.seekTo(pos); }
            @Override public boolean isPlaying() { return mediaPlayer != null && mediaPlayer.isPlaying(); }
            @Override public int getBufferPercentage() { return 0; }
            @Override public boolean canPause() { return true; }
            @Override public boolean canSeekBackward() { return true; }
            @Override public boolean canSeekForward() { return true; }
            @Override public int getAudioSessionId() { return mediaPlayer != null ? mediaPlayer.getAudioSessionId() : 0; }
        });

        mediaSurfaceView.setOnClickListener(v -> {
            showControlsTemporarily();

            if (mediaController != null) {
                mediaController.show();
            }
        });

        mediaController.setEnabled(true);
        mediaController.show();
    }


    private void playNextVideo() {
        if (currentIndex >= getVideoCount() - 1) {
            return;
        }

        if (exoPlayer != null) {
            exoPlayer.seekToNextMediaItem();
            return;
        }

        playIndex(currentIndex + 1);
    }

    private void playIndex(int index) {
        if (index < 0 || index >= getVideoCount()) {
            return;
        }

        currentIndex = index;
        selectedSttLanguage = getSttLanguageForCurrentVideo();
        restartSttForCurrentVideoIfNeeded();
        mediaPlayerFallbackAlreadyTried = false;
        resetQualityForNewVideo(true);
        updateTitle();
        updateNavigationButtons();

        if (ENGINE_MEDIA_PLAYER.equals(preferredEngine)) {
            startMediaPlayer();
        } else {
            startExoPlayer();
        }
    }

    @Nullable
    private Uri resolveVideoUri(int index) {
        String currentLocalPath = getLocalPath(index);

        if (currentLocalPath != null && !currentLocalPath.trim().isEmpty()) {
            File file = new File(currentLocalPath);

            if (file.exists()) {
                return Uri.fromFile(file);
            }
        }

        String currentUrl = getUrl(index);

        if (currentUrl != null && !currentUrl.trim().isEmpty()) {
            return Uri.parse(currentUrl);
        }

        return null;
    }

    @Nullable
    private String getLocalPath(int index) {
        if (index < 0 || index >= localPaths.size()) {
            return null;
        }

        return localPaths.get(index);
    }

    @Nullable
    private String getUrl(int index) {
        if (index < 0 || index >= urls.size()) {
            return null;
        }

        return urls.get(index);
    }

    @Nullable
    private String getTitle(int index) {
        if (index < 0 || index >= titles.size()) {
            return null;
        }

        return titles.get(index);
    }

    private void updateTitle() {
        if (titleView == null) {
            return;
        }

        String currentTitle;

        if (isYouTube) {
            currentTitle = !titles.isEmpty() ? titles.get(0) : "YouTube";
        } else {
            currentTitle = getTitle(currentIndex);
        }

        if (currentTitle == null || currentTitle.trim().isEmpty()) {
            currentTitle = getString(isYouTube ? R.string.video_player_youtube_title : R.string.video_player_title);
        }

        String displayTitle = currentTitle;

        if (!isYouTube && getVideoCount() > 1) {
            displayTitle = String.format(
                    Locale.US,
                    "%s  %d/%d",
                    currentTitle,
                    currentIndex + 1,
                    getVideoCount()
            );
        }

        titleView.setText(displayTitle);
    }

    private void updateNavigationButtons() {
    }

    private void toggleOptionsPanel(String requestedPanel) {
        if (optionsPanel == null) {
            return;
        }

        if (optionsPanel.getVisibility() == View.VISIBLE && requestedPanel.equals(activePanel)) {
            hideOptionsPanel();
            return;
        }

        if (PANEL_QUALITY.equals(requestedPanel)) {
            showQualityPanel();
        } else if (PANEL_SCALE.equals(requestedPanel)) {
            showScalePanel();
        } else if (PANEL_FILTER.equals(requestedPanel)) {
            showFilterPanel();
        } else if (PANEL_CAPTIONS.equals(requestedPanel)) {
            showCaptionsPanel();
        }
    }

    private boolean isOptionsPanelVisible() {
        return optionsPanel != null && optionsPanel.getVisibility() == View.VISIBLE;
    }

    private void showQualityPanel() {
        if (isYouTube) {
            return;
        }

        activePanel = PANEL_QUALITY;
        rebuildQualityOptionsFromCurrentTracks();
        renderQualityOptions();
        showOptionsPanelAnimated();
    }

    private void showScalePanel() {
        if (isYouTube) {
            return;
        }

        activePanel = PANEL_SCALE;
        renderScaleOptions();
        showOptionsPanelAnimated();
    }

    private void showFilterPanel() {
        if (isYouTube) {
            return;
        }

        activePanel = PANEL_FILTER;
        renderFilterOptions();
        showOptionsPanelAnimated();
    }

    private void showCaptionsPanel() {
        if (isYouTube) {
            return;
        }

        activePanel = PANEL_CAPTIONS;
        renderCaptionsOptions();
        showOptionsPanelAnimated();
    }

    private void showOptionsPanelAnimated() {
        if (optionsPanel == null) {
            return;
        }

        uiHandler.removeCallbacks(hideControlsRunnable);
        optionsPanel.animate().cancel();
        optionsPanel.setVisibility(View.VISIBLE);
        optionsPanel.setAlpha(0f);
        optionsPanel.setTranslationY(dp(20));
        optionsPanel.setScaleX(0.98f);
        optionsPanel.setScaleY(0.98f);
        optionsPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180L)
                .start();
    }

    private void hideOptionsPanel() {
        if (optionsPanel == null || optionsPanel.getVisibility() != View.VISIBLE) {
            activePanel = PANEL_NONE;
            return;
        }

        activePanel = PANEL_NONE;
        optionsPanel.animate().cancel();
        optionsPanel.animate()
                .alpha(0f)
                .translationY(dp(20))
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(140L)
                .withEndAction(() -> {
                    if (optionsPanel != null) {
                        optionsPanel.setVisibility(View.GONE);
                        optionsPanel.setAlpha(1f);
                        optionsPanel.setTranslationY(0f);
                        optionsPanel.setScaleX(1f);
                        optionsPanel.setScaleY(1f);
                    }
                })
                .start();
    }

    private void rebuildQualityOptionsFromCurrentTracks() {
        if (exoPlayer == null) {
            buildUnavailableQualityOptions();
            return;
        }

        rebuildQualityOptionsFromTracks(exoPlayer.getCurrentTracks());
    }

    private void rebuildQualityOptionsFromTracks(@Nullable Tracks tracks) {
        availableQualityOptions.clear();
        detectedMaxWidth = 0;
        detectedMaxHeight = 0;

        if (tracks == null || tracks.getGroups().isEmpty()) {
            buildUnavailableQualityOptions();
            return;
        }

        Map<Integer, VideoQualityOption> byHeight = new LinkedHashMap<>();
        List<Tracks.Group> groups = tracks.getGroups();

        for (Tracks.Group group : groups) {
            if (group == null || group.getType() != C.TRACK_TYPE_VIDEO) {
                continue;
            }

            for (int i = 0; i < group.length; i++) {
                if (!group.isTrackSupported(i)) {
                    continue;
                }

                Format format = group.getTrackFormat(i);

                if (format.width <= 0 || format.height <= 0) {
                    continue;
                }

                int width = format.width;
                int height = format.height;
                int bitrate = Math.max(format.bitrate, 0);

                detectedMaxWidth = Math.max(detectedMaxWidth, width);
                detectedMaxHeight = Math.max(detectedMaxHeight, height);

                VideoQualityOption old = byHeight.get(height);

                if (old == null || width > old.width || bitrate > old.bitrate) {
                    byHeight.put(
                            height,
                            new VideoQualityOption(
                                    buildQualityKey(width, height),
                                    buildQualityLabel(height),
                                    buildQualityDetails(width, height, bitrate),
                                    width,
                                    height,
                                    bitrate,
                                    false,
                                    true
                            )
                    );
                }
            }
        }

        if (byHeight.isEmpty()) {
            buildUnavailableQualityOptions();
            return;
        }

        List<Integer> heights = new ArrayList<>(byHeight.keySet());
        heights.sort(Collections.reverseOrder());

        String maxLabel = detectedMaxHeight > 0
                ? buildQualityLabel(detectedMaxHeight)
                : getString(R.string.video_player_quality_auto);

        String autoQualityDetails = getString(R.string.video_player_quality_auto_details);

        if (!maxLabel.trim().isEmpty()) {
            autoQualityDetails = autoQualityDetails + " • " + maxLabel;
        }

        availableQualityOptions.add(
                new VideoQualityOption(
                        VideoQualityOption.KEY_AUTO,
                        getString(R.string.video_player_quality_auto),
                        autoQualityDetails,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        0,
                        true,
                        true
                )
        );

        for (Integer height : heights) {
            VideoQualityOption option = byHeight.get(height);

            if (option != null) {
                availableQualityOptions.add(option);
            }
        }

        if (!containsQualityKey(selectedQualityKey)) {
            selectQualityInternal(VideoQualityOption.KEY_AUTO, false);
        } else {
            updateQualityButtonText();
        }

        if (PANEL_QUALITY.equals(activePanel) && isOptionsPanelVisible()) {
            renderQualityOptions();
        }
    }

    private void buildUnavailableQualityOptions() {
        availableQualityOptions.clear();
        availableQualityOptions.add(
                new VideoQualityOption(
                        VideoQualityOption.KEY_AUTO,
                        getString(R.string.video_player_quality_auto),
                        getString(R.string.video_player_quality_unavailable),
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        0,
                        true,
                        false
                )
        );

        selectedQualityKey = VideoQualityOption.KEY_AUTO;
        selectedQualityLabel = getString(R.string.video_player_quality_auto);
        selectedMaxWidth = Integer.MAX_VALUE;
        selectedMaxHeight = Integer.MAX_VALUE;
        updateQualityButtonText();

        if (PANEL_QUALITY.equals(activePanel) && isOptionsPanelVisible()) {
            renderQualityOptions();
        }
    }

    private void renderQualityOptions() {
        if (optionsContainer == null) {
            return;
        }

        if (optionsPanelTitle != null) {
            optionsPanelTitle.setText(R.string.video_player_quality_available);
        }

        optionsContainer.removeAllViews();

        for (VideoQualityOption option : availableQualityOptions) {
            addOptionRow(
                    option.label,
                    option.details,
                    option.key.equals(selectedQualityKey),
                    option.selectable,
                    () -> {
                        selectQualityInternal(option.key, true);
                        hideOptionsPanel();
                    }
            );
        }
    }

    private void renderScaleOptions() {
        if (optionsContainer == null) {
            return;
        }

        if (optionsPanelTitle != null) {
            optionsPanelTitle.setText(R.string.video_player_scale_title);
        }

        optionsContainer.removeAllViews();

        addOptionRow(
                getString(R.string.video_player_scale_fit_title),
                getString(R.string.video_player_scale_fit_description),
                resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT,
                true,
                () -> {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
                    zoomScale = 1f;
                    applyEnhancementState();
                    applyZoom();
                    renderScaleOptions();
                }
        );

        addOptionRow(
                getString(R.string.video_player_scale_fill_title),
                getString(R.string.video_player_scale_fill_description),
                resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL,
                true,
                () -> {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL;
                    zoomScale = 1f;
                    applyEnhancementState();
                    applyZoom();
                    renderScaleOptions();
                }
        );

        addOptionRow(
                getString(R.string.video_player_scale_crop_title),
                getString(R.string.video_player_scale_crop_description),
                resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                true,
                () -> {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
                    zoomScale = 1f;
                    applyEnhancementState();
                    applyZoom();
                    renderScaleOptions();
                }
        );

        addOptionRow(
                getString(R.string.video_player_scale_pinch_title),
                getString(R.string.video_player_scale_pinch_description, String.format(Locale.US, "%.1fx", zoomScale)),
                zoomScale > 1.01f,
                false,
                null
        );
    }

    private void renderFilterOptions() {
        if (optionsContainer == null) {
            return;
        }

        if (optionsPanelTitle != null) {
            optionsPanelTitle.setText(R.string.video_player_filters_title);
        }

        optionsContainer.removeAllViews();

        addFilterOption(getString(R.string.video_player_filter_none_title), getString(R.string.video_player_filter_none_description), FILTER_NONE);
        addFilterOption(getString(R.string.video_player_filter_brightness_title), getString(R.string.video_player_filter_brightness_description), FILTER_BRIGHTNESS);
        addFilterOption(getString(R.string.video_player_filter_sharpness_title), getString(R.string.video_player_filter_sharpness_description), FILTER_SHARPNESS);
        addFilterOption(getString(R.string.video_player_filter_brightness_sharpness_title), getString(R.string.video_player_filter_brightness_sharpness_description), FILTER_BRIGHTNESS_SHARPNESS);
        addFilterOption(getString(R.string.video_player_filter_black_white_title), getString(R.string.video_player_filter_black_white_description), FILTER_BLACK_WHITE);
        addFilterOption(getString(R.string.video_player_filter_noir_title), getString(R.string.video_player_filter_noir_description), FILTER_NOIR);
        addFilterOption(getString(R.string.video_player_filter_warm_title), getString(R.string.video_player_filter_warm_description), FILTER_WARM);
        addFilterOption(getString(R.string.video_player_filter_cold_title), getString(R.string.video_player_filter_cold_description), FILTER_COLD);
        addFilterOption(getString(R.string.video_player_filter_high_contrast_title), getString(R.string.video_player_filter_high_contrast_description), FILTER_HIGH_CONTRAST);
    }

    private void addFilterOption(String label, String details, int mode) {
        addOptionRow(
                label,
                details,
                filterMode == mode,
                true,
                () -> {
                    filterMode = mode;
                    applyEnhancementState();
                    renderFilterOptions();
                }
        );
    }

    @NonNull
    private String normalizeSttLanguageCode(@Nullable String languageCode) {
        return VideoSubtitleConfig.normalizeLanguageCode(languageCode);
    }

    @NonNull
    private String getSttLanguageForCurrentVideo() {
        if (currentIndex >= 0 && currentIndex < sttLanguages.size()) {
            return normalizeSttLanguageCode(sttLanguages.get(currentIndex));
        }

        return normalizeSttLanguageCode(selectedSttLanguage);
    }

    private void setSttLanguageForCurrentVideo(@NonNull String languageCode) {
        String normalized = normalizeSttLanguageCode(languageCode);

        while (sttLanguages.size() <= currentIndex && sttLanguages.size() < getVideoCount()) {
            sttLanguages.add(selectedSttLanguage);
        }

        if (currentIndex >= 0 && currentIndex < sttLanguages.size()) {
            sttLanguages.set(currentIndex, normalized);
        }
    }

    @NonNull
    private String getPreferredEmbeddedCaptionLanguage() {
        if (sttCaptionsEnabled) {
            return selectedSttLanguage;
        }

        String systemLanguage = Locale.getDefault().getLanguage();

        return !systemLanguage.trim().isEmpty()
                ? systemLanguage
                : selectedSttLanguage;
    }

    private void restartSttForCurrentVideoIfNeeded() {
        if (!sttCaptionsEnabled) {
            return;
        }

        stopSttListeningOnly();
        updateCaptionsButtonText();
        syncSttListeningWithPlayback();
    }

    private void renderCaptionsOptions() {
        if (optionsContainer == null) {
            return;
        }

        if (optionsPanelTitle != null) {
            optionsPanelTitle.setText(R.string.video_player_captions_title);
        }

        optionsContainer.removeAllViews();

        boolean sttAvailable = VideoSttSubtitleManager.isAnyModelAvailable(this);

        addOptionRow(
                getString(R.string.video_player_captions_auto),
                getString(R.string.video_player_captions_auto_description),
                captionsEnabled,
                true,
                () -> {
                    sttCaptionsEnabled = false;
                    stopSttCaptions();
                    captionsEnabled = true;
                    sttTranslationMode = SUBTITLE_MODE_ORIGINAL;
                    applyCaptionStateToTrackSelector();
                    updateCaptionsButtonText();
                    showAutoCaptionsStatus();
                    hideOptionsPanel();
                    scheduleControlsHide();
                }
        );

        if (sttAvailable) {
            addSttModeOption(
                    getString(R.string.video_player_stt_original_title),
                    getString(R.string.video_player_stt_original_description),
                    SUBTITLE_MODE_ORIGINAL
            );

            addSttModeOption(
                    getString(R.string.video_player_stt_translated_title),
                    getString(R.string.video_player_stt_translated_description),
                    SUBTITLE_MODE_TRANSLATED
            );

            addSttModeOption(
                    getString(R.string.video_player_stt_bilingual_title),
                    getString(R.string.video_player_stt_bilingual_description),
                    SUBTITLE_MODE_BILINGUAL
            );

            addSttLanguageOption(
                    getString(R.string.video_player_stt_language_ru_title),
                    getString(R.string.video_player_stt_language_ru_description),
                    STT_LANGUAGE_RU
            );

            addSttLanguageOption(
                    getString(R.string.video_player_stt_language_en_zh_title),
                    getString(R.string.video_player_stt_language_en_zh_description),
                    STT_LANGUAGE_EN
            );
        } else {
            addOptionRow(
                    "Автосубтитры недоступны",
                    "Модели Sherpa-ONNX не найдены. Видео, обычные субтитры и остальные функции плеера работают без них.",
                    false,
                    false,
                    null
            );
        }

        addOptionRow(
                getString(R.string.video_player_captions_off),
                getString(R.string.video_player_captions_off_description),
                !captionsEnabled && !sttCaptionsEnabled,
                true,
                () -> {
                    sttCaptionsEnabled = false;
                    stopSttCaptions();
                    captionsEnabled = false;
                    sttTranslationMode = SUBTITLE_MODE_ORIGINAL;
                    applyCaptionStateToTrackSelector();
                    updateCaptionsButtonText();
                    hideOptionsPanel();
                    scheduleControlsHide();
                }
        );
    }

    private void addSttModeOption(@NonNull String label,
                                  @NonNull String details,
                                  @NonNull String mode) {
        addOptionRow(
                label,
                details,
                sttCaptionsEnabled && mode.equals(sttTranslationMode),
                true,
                () -> {
                    sttTranslationMode = normalizeSubtitleTranslationMode(mode);
                    startSttCaptions();
                    hideOptionsPanel();
                    scheduleControlsHide();
                }
        );
    }

    private void addSttLanguageOption(@NonNull String label,
                                      @NonNull String details,
                                      @NonNull String languageCode) {
        addOptionRow(
                label,
                details,
                sttCaptionsEnabled
                        && VideoSubtitleConfig.isSameVisibleLanguageOption(
                        languageCode,
                        getSttLanguageForCurrentVideo()
                ),
                true,
                () -> {
                    startSttCaptionsForLanguage(languageCode);
                    hideOptionsPanel();
                    scheduleControlsHide();
                }
        );
    }

    private void startSttCaptions() {
        startSttCaptionsForLanguage(selectedSttLanguage);
    }

    private void startSttCaptionsForLanguage(@NonNull String languageCode) {
        String normalizedLanguage = normalizeSttLanguageCode(languageCode);
        boolean languageChanged = !normalizedLanguage.equals(selectedSttLanguage);

        selectedSttLanguage = normalizedLanguage;
        setSttLanguageForCurrentVideo(selectedSttLanguage);

        if (languageChanged && sttListeningActive) {
            stopSttListeningOnly();
        }

        if (!VideoSttSubtitleManager.isModelAvailableForLanguage(this, normalizedLanguage)) {
            sttCaptionsEnabled = false;
            pendingSttPermissionStart = false;
            updateCaptionsButtonText();
            showAutoCaptionText("Автосубтитры недоступны: модели Sherpa-ONNX не установлены.");
            if (PANEL_CAPTIONS.equals(activePanel) && isOptionsPanelVisible()) {
                renderCaptionsOptions();
            }
            return;
        }

        if (isYouTube) {
            showAutoCaptionText(getString(R.string.video_player_stt_youtube_unavailable));
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            pendingSttPermissionStart = true;
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_FOR_STT
            );
            return;
        }

        pendingSttPermissionStart = false;
        captionsEnabled = false;
        applyCaptionStateToTrackSelector();
        sttCaptionsEnabled = true;
        updateCaptionsButtonText();
        syncSttListeningWithPlayback();
    }

    private void stopSttCaptions() {
        sttCaptionsEnabled = false;
        pendingSttPermissionStart = false;
        stopSttListeningOnly();
        updateCaptionsButtonText();
    }

    private void stopSttListeningOnly() {
        sttListeningActive = false;

        if (sttSubtitleManager != null) {
            sttSubtitleManager.stop();
        }
    }

    private void showSttCaptionText(@Nullable String text, boolean isFinal) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        String originalText = prepareSubtitleText(text);

        if (originalText.isEmpty()) {
            return;
        }

        String sourceLanguage = getSttLanguageForCurrentVideo();
        String targetLanguage = getAppLanguageForTranslation();

        if (SUBTITLE_MODE_ORIGINAL.equals(sttTranslationMode)
                || sourceLanguage.equalsIgnoreCase(targetLanguage)
                || subtitleTranslator == null) {
            showAutoCaptionText(originalText);
            return;
        }

        if (!isFinal) {
            if (SUBTITLE_MODE_BILINGUAL.equals(sttTranslationMode)) {
                showAutoCaptionText(originalText);
            }
            return;
        }

        subtitleTranslator.translate(
                originalText,
                sourceLanguage,
                targetLanguage,
                new LocalSubtitleTranslationManager.Callback() {
                    @Override
                    public void onTranslated(@NonNull String originalText, @NonNull String translatedText) {
                        if (SUBTITLE_MODE_BILINGUAL.equals(sttTranslationMode)) {
                            showAutoCaptionText(originalText + "\n" + translatedText);
                        } else {
                            showAutoCaptionText(translatedText);
                        }
                    }

                    @Override
                    public void onOriginalFallback(@NonNull String originalText) {
                        showAutoCaptionText(originalText);
                    }

                    @Override
                    public void onModelDownloadStarted(@NonNull String sourceLanguage, @NonNull String targetLanguage) {
                        showAutoCaptionText(getString(
                                R.string.video_player_translation_model_preparing,
                                sourceLanguage,
                                targetLanguage
                        ));
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        Log.w(TAG, message);
                    }
                }
        );
    }

    @NonNull
    private String getAppLanguageForTranslation() {
        try {
            Locale locale = LocaleManager.getLocale(this);
            String language = locale.getLanguage();

            if (!language.trim().isEmpty()) {
                return language.toLowerCase(Locale.US);
            }
        } catch (Exception ignored) {
        }

        String systemLanguage = Locale.getDefault().getLanguage();

        return !systemLanguage.trim().isEmpty()
                ? systemLanguage.toLowerCase(Locale.US)
                : STT_LANGUAGE_RU;
    }

    @NonNull
    private String normalizeSubtitleTranslationMode(@Nullable String mode) {
        return VideoSubtitleConfig.normalizeSubtitleMode(mode);
    }

    private void syncSttListeningWithPlayback() {
        if (!sttCaptionsEnabled || isYouTube || !activityResumed) {
            if (sttListeningActive) {
                stopSttListeningOnly();
            }
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        boolean shouldListen = isPlaybackActuallyRunning();

        if (shouldListen) {
            if (!sttListeningActive && sttSubtitleManager != null) {
                sttListeningActive = true;
                sttSubtitleManager.startByLanguageCode(getSttLanguageForCurrentVideo());
            }
        } else if (sttListeningActive) {
            stopSttListeningOnly();
        }
    }

    private boolean isPlaybackActuallyRunning() {
        if (exoPlayer != null) {
            return exoPlayer.isPlaying()
                    && exoPlayer.getPlaybackState() == Player.STATE_READY;
        }

        if (mediaPlayer != null) {
            try {
                return mediaPlayer.isPlaying();
            } catch (Exception ignored) {
                return false;
            }
        }

        return false;
    }

    private void showAutoCaptionText(@Nullable String text) {
        if (autoCaptionsOverlay == null || text == null || text.trim().isEmpty()) {
            return;
        }

        String preparedText = prepareSubtitleText(text);

        if (preparedText.isEmpty()) {
            return;
        }

        autoCaptionsOverlay.animate().cancel();
        autoCaptionsOverlay.setText(preparedText);

        if (autoCaptionsOverlay.getVisibility() != View.VISIBLE) {
            autoCaptionsOverlay.setAlpha(0f);
            autoCaptionsOverlay.setTranslationY(dp(8));
            autoCaptionsOverlay.setVisibility(View.VISIBLE);
            autoCaptionsOverlay.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(90L)
                    .start();
        } else {
            autoCaptionsOverlay.setAlpha(1f);
            autoCaptionsOverlay.setTranslationY(0f);
        }
    }

    private String prepareSubtitleText(@Nullable String text) {
        if (text == null) {
            return "";
        }

        String value = text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\t ]+", " ")
                .replaceAll(" *\n *", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();

        if (value.isEmpty()) {
            return "";
        }

        return value;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_RECORD_AUDIO_FOR_STT) {
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (pendingSttPermissionStart) {
                startSttCaptions();
            }
        } else {
            pendingSttPermissionStart = false;
            sttCaptionsEnabled = false;
            stopSttListeningOnly();
            updateCaptionsButtonText();
            showAutoCaptionText(getString(R.string.video_player_stt_microphone_permission_missing));
        }
    }

    private void addOptionRow(
            String label,
            @Nullable String details,
            boolean selected,
            boolean selectable,
            @Nullable Runnable action
    ) {
        if (optionsContainer == null) {
            return;
        }

        TextView item = new TextView(this);
        item.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        item.setMinHeight(dp(48));
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(16), dp(8), dp(16), dp(8));
        item.setTextColor(Color.WHITE);
        item.setTextSize(15f);
        item.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        item.setAlpha(selectable ? 1f : 0.55f);
        item.setBackgroundResource(android.R.drawable.list_selector_background);
        item.setText(details == null || details.trim().isEmpty()
                ? (selected ? "✓ " + label : label)
                : (selected ? "✓ " + label : label) + "\n" + details);

        if (selectable && action != null) {
            item.setOnClickListener(v -> action.run());
        }

        optionsContainer.addView(item);
    }

    private void selectQualityInternal(String key, boolean showWarningWhenNeeded) {
        VideoQualityOption selected = findQualityOption(key);

        if (selected == null) {
            selected = findQualityOption(VideoQualityOption.KEY_AUTO);
        }

        if (selected == null) {
            return;
        }

        selectedQualityKey = selected.key;
        selectedQualityLabel = selected.label;
        selectedMaxWidth = selected.width;
        selectedMaxHeight = selected.height;

        updateQualityButtonText();
        applyCurrentQualityToTrackSelector();

        if (exoPlayer == null && showWarningWhenNeeded) {
            Toast.makeText(this, R.string.video_player_quality_exoplayer_only, Toast.LENGTH_SHORT).show();
        }
    }

    private void applyCurrentQualityToTrackSelector() {
        if (trackSelector == null) {
            return;
        }

        DefaultTrackSelector.Parameters.Builder builder = trackSelector.buildUponParameters()
                .setMaxVideoSize(selectedMaxWidth, selectedMaxHeight);

        trackSelector.setParameters(builder);
    }

    private void applyCaptionStateToTrackSelector() {
        if (trackSelector == null) {
            return;
        }

        DefaultTrackSelector.Parameters.Builder builder = trackSelector.buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !captionsEnabled)
                .setPreferredTextLanguage(getPreferredEmbeddedCaptionLanguage());

        trackSelector.setParameters(builder);
    }

    private void updateQualityButtonText() {
        if (qualityButton == null) {
            return;
        }

        if (VideoQualityOption.KEY_AUTO.equals(selectedQualityKey)) {
            qualityButton.setText(R.string.video_player_quality_short);
            return;
        }

        qualityButton.setText(shortQualityLabel(selectedQualityLabel));
    }

    private void updateCaptionsButtonText() {
        if (captionsButton != null) {
            if (sttCaptionsEnabled) {
                String language = getShortSttLanguageLabel(selectedSttLanguage);

                if (SUBTITLE_MODE_TRANSLATED.equals(sttTranslationMode)) {
                    captionsButton.setText(getString(R.string.video_player_captions_short_translate, language));
                    return;
                }

                if (SUBTITLE_MODE_BILINGUAL.equals(sttTranslationMode)) {
                    captionsButton.setText(getString(R.string.video_player_captions_short_bilingual, language));
                    return;
                }

                captionsButton.setText(getString(R.string.video_player_captions_short_stt, language));
                return;
            }

            captionsButton.setText(captionsEnabled
                    ? R.string.video_player_captions_short_on
                    : R.string.video_player_captions_short_off);
        }
    }

    @NonNull
    private String getShortSttLanguageLabel(@Nullable String languageCode) {
        String normalized = normalizeSttLanguageCode(languageCode);

        if (VideoSubtitleConfig.isEnglishChineseLanguage(normalized)) {
            return getString(R.string.video_player_stt_language_en_zh_short);
        }

        return getString(R.string.video_player_stt_language_ru_short);
    }

    private void applyEnhancementState() {
        if (exoPlayerView != null) {
            exoPlayerView.setResizeMode(resizeMode);
        }

        applyVideoFilter();
    }

    private void applyVideoFilter() {
        boolean useBrightnessOverlay = filterMode == FILTER_BRIGHTNESS
                || filterMode == FILTER_BRIGHTNESS_SHARPNESS;

        boolean useSharpnessOverlay = filterMode == FILTER_SHARPNESS
                || filterMode == FILTER_BRIGHTNESS_SHARPNESS;

        if (brightnessOverlay != null) {
            brightnessOverlay.setVisibility(useBrightnessOverlay ? View.VISIBLE : View.GONE);
            brightnessOverlay.setAlpha(useBrightnessOverlay ? 0.14f : 0f);
        }

        if (sharpnessOverlay != null) {
            sharpnessOverlay.setVisibility(useSharpnessOverlay ? View.VISIBLE : View.GONE);
            sharpnessOverlay.setAlpha(useSharpnessOverlay ? 0.16f : 0f);
        }

        if (videoContentContainer == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }

        RenderEffect effect = buildRenderEffectForCurrentFilter();
        videoContentContainer.setRenderEffect(effect);
    }

    @Nullable
    private RenderEffect buildRenderEffectForCurrentFilter() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return null;
        }

        ColorMatrix matrix = new ColorMatrix();

        switch (filterMode) {
            case FILTER_BLACK_WHITE:
                matrix.setSaturation(0f);
                return RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(matrix));

            case FILTER_NOIR:
                matrix.setSaturation(0f);
                ColorMatrix noir = new ColorMatrix(new float[]{
                        1.35f, 0f, 0f, 0f, -35f,
                        0f, 1.35f, 0f, 0f, -35f,
                        0f, 0f, 1.35f, 0f, -35f,
                        0f, 0f, 0f, 1f, 0f
                });
                matrix.postConcat(noir);
                return RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(matrix));

            case FILTER_WARM:
                matrix.set(new float[]{
                        1.18f, 0.03f, 0f, 0f, 8f,
                        0f, 1.05f, 0f, 0f, 2f,
                        0f, 0f, 0.88f, 0f, -4f,
                        0f, 0f, 0f, 1f, 0f
                });
                return RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(matrix));

            case FILTER_COLD:
                matrix.set(new float[]{
                        0.90f, 0f, 0f, 0f, -4f,
                        0f, 1.03f, 0f, 0f, 0f,
                        0f, 0.03f, 1.22f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                });
                return RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(matrix));

            case FILTER_HIGH_CONTRAST:
                matrix.set(new float[]{
                        1.45f, 0f, 0f, 0f, -42f,
                        0f, 1.45f, 0f, 0f, -42f,
                        0f, 0f, 1.45f, 0f, -42f,
                        0f, 0f, 0f, 1f, 0f
                });
                return RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(matrix));

            case FILTER_BRIGHTNESS:
                matrix.set(new float[]{
                        1.08f, 0f, 0f, 0f, 18f,
                        0f, 1.08f, 0f, 0f, 18f,
                        0f, 0f, 1.08f, 0f, 18f,
                        0f, 0f, 0f, 1f, 0f
                });
                return RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(matrix));

            case FILTER_SHARPNESS:
            case FILTER_BRIGHTNESS_SHARPNESS:
            case FILTER_NONE:
            default:
                return null;
        }
    }

    private void showAutoCaptionsStatus() {
        if (autoCaptionsOverlay == null) {
            return;
        }

        autoCaptionsOverlay.setText(R.string.video_player_captions_auto_status);
        autoCaptionsOverlay.setVisibility(View.VISIBLE);
        autoCaptionsOverlay.animate().cancel();
        autoCaptionsOverlay.setAlpha(0f);
        autoCaptionsOverlay.animate()
                .alpha(1f)
                .setDuration(140L)
                .withEndAction(() -> autoCaptionsOverlay.animate()
                        .alpha(0f)
                        .setStartDelay(1800L)
                        .setDuration(220L)
                        .withEndAction(() -> autoCaptionsOverlay.setVisibility(View.GONE))
                        .start())
                .start();
    }

    private void resetQualityForNewVideo(boolean hidePanel) {
        selectedQualityKey = VideoQualityOption.KEY_AUTO;
        selectedQualityLabel = getString(R.string.video_player_quality_auto);
        selectedMaxWidth = Integer.MAX_VALUE;
        selectedMaxHeight = Integer.MAX_VALUE;
        detectedMaxWidth = 0;
        detectedMaxHeight = 0;
        updateQualityButtonText();

        if (hidePanel) {
            hideOptionsPanel();
        }
    }

    private boolean containsQualityKey(String key) {
        return findQualityOption(key) != null;
    }

    @Nullable
    private VideoQualityOption findQualityOption(String key) {
        if (key == null) {
            return null;
        }

        for (VideoQualityOption option : availableQualityOptions) {
            if (option.key.equals(key)) {
                return option;
            }
        }

        return null;
    }

    private String buildQualityKey(int width, int height) {
        return width + "x" + height;
    }

    private String buildQualityLabel(int height) {
        if (height >= 2160) return "2160p 4K";
        if (height >= 1440) return "1440p QHD";
        if (height >= 1080) return "1080p Full HD";
        if (height >= 720) return "720p HD";
        if (height >= 480) return "480p";
        return height > 0 ? height + "p" : getString(R.string.video_player_quality_unknown);
    }

    private String shortQualityLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            return getString(R.string.video_player_quality_short);
        }

        return label
                .replace(" Full HD", "")
                .replace(" QHD", "")
                .replace(" 4K", "");
    }

    private String buildQualityDetails(int width, int height, int bitrate) {
        StringBuilder builder = new StringBuilder();

        if (width > 0 && height > 0) {
            builder.append(width).append("×").append(height);
        }

        if (bitrate > 0) {
            if (builder.length() > 0) {
                builder.append(" • ");
            }

            builder.append(formatBitrate(bitrate));
        }

        return builder.toString();
    }

    private String formatBitrate(int bitrate) {
        if (bitrate >= 1_000_000) {
            return String.format(Locale.US, "%.1f Mbps", bitrate / 1_000_000f);
        }

        if (bitrate >= 1_000) {
            return Math.round(bitrate / 1000f) + " Kbps";
        }

        return bitrate + " bps";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void seekByMillis(long deltaMs) {
        if (exoPlayer != null) {
            long duration = exoPlayer.getDuration();
            long currentPosition = exoPlayer.getCurrentPosition();
            long targetPosition = currentPosition + deltaMs;

            if (duration > 0) {
                targetPosition = Math.min(targetPosition, duration);
            }

            targetPosition = Math.max(0L, targetPosition);
            exoPlayer.seekTo(targetPosition);
            return;
        }

        if (mediaPlayer != null) {
            int duration = mediaPlayer.getDuration();
            int currentPosition = mediaPlayer.getCurrentPosition();
            long targetPosition = currentPosition + deltaMs;

            if (duration > 0) {
                targetPosition = Math.min(targetPosition, duration);
            }

            targetPosition = Math.max(0L, targetPosition);
            mediaPlayer.seekTo((int) targetPosition);
        }
    }

    private void showSeekFeedback(boolean forward) {
        TextView target = forward ? seekFeedbackRight : seekFeedbackLeft;

        if (target == null) {
            return;
        }

        TextView other = forward ? seekFeedbackLeft : seekFeedbackRight;

        if (other != null) {
            other.animate().cancel();
            other.setVisibility(View.GONE);
            other.setAlpha(0f);
            other.setScaleX(1f);
            other.setScaleY(1f);
        }

        target.animate().cancel();
        target.setText(forward ? "+5" : "-5");
        target.setVisibility(View.VISIBLE);
        target.setAlpha(0f);
        target.setScaleX(0.86f);
        target.setScaleY(0.86f);

        target.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(110L)
                .withEndAction(() -> target.animate()
                        .alpha(0f)
                        .scaleX(1.08f)
                        .scaleY(1.08f)
                        .setStartDelay(260L)
                        .setDuration(180L)
                        .withEndAction(() -> {
                            target.setVisibility(View.GONE);
                            target.setScaleX(1f);
                            target.setScaleY(1f);
                        })
                        .start())
                .start();
    }

    private void applyZoom() {
        if (exoPlayerView != null) {
            View videoSurface = exoPlayerView.getVideoSurfaceView();

            if (videoSurface != null) {
                videoSurface.setPivotX(videoSurface.getWidth() / 2f);
                videoSurface.setPivotY(videoSurface.getHeight() / 2f);
                videoSurface.setScaleX(zoomScale);
                videoSurface.setScaleY(zoomScale);
            }

            exoPlayerView.setScaleX(1f);
            exoPlayerView.setScaleY(1f);
        }

        if (mediaSurfaceView != null) {
            mediaSurfaceView.setPivotX(mediaSurfaceView.getWidth() / 2f);
            mediaSurfaceView.setPivotY(mediaSurfaceView.getHeight() / 2f);
            mediaSurfaceView.setScaleX(zoomScale);
            mediaSurfaceView.setScaleY(zoomScale);
        }

        if (youtubeWebView != null) {
            youtubeWebView.setScaleX(1f);
            youtubeWebView.setScaleY(1f);
        }
    }

    private void showControlsTemporarily() {
        showControls();
    }

    private void showControls() {
        forceFullscreenVideoMode();
        uiHandler.removeCallbacks(hideControlsRunnable);
        controlsVisible = true;

        if (controlsOverlay != null) {
            controlsOverlay.animate().cancel();
            controlsOverlay.setVisibility(View.VISIBLE);
            controlsOverlay.animate().alpha(1f).setDuration(140L).start();
        }

        if (exoPlayerView != null) {
            exoPlayerView.showController();
        }
    }

    private void scheduleControlsHide() {
        uiHandler.removeCallbacks(hideControlsRunnable);
    }

    private void hideControls() {
        forceFullscreenVideoMode();
        uiHandler.removeCallbacks(hideControlsRunnable);
        controlsVisible = false;
        hideOptionsPanelImmediately();

        if (controlsOverlay != null) {
            controlsOverlay.animate().cancel();
            controlsOverlay.animate()
                    .alpha(0f)
                    .setDuration(180L)
                    .withEndAction(() -> {
                        if (!controlsVisible && controlsOverlay != null) {
                            controlsOverlay.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }

        if (exoPlayerView != null) {
            exoPlayerView.hideController();
        }

        if (mediaController != null) {
            mediaController.hide();
        }
    }

    private void hideControlsImmediately() {
        forceFullscreenVideoMode();
        uiHandler.removeCallbacks(hideControlsRunnable);
        controlsVisible = false;
        hideOptionsPanelImmediately();

        if (controlsOverlay != null) {
            controlsOverlay.animate().cancel();
            controlsOverlay.setAlpha(0f);
            controlsOverlay.setVisibility(View.GONE);
        }

        if (exoPlayerView != null) {
            exoPlayerView.hideController();
        }

        if (mediaController != null) {
            mediaController.hide();
        }
    }

    private void hideOptionsPanelImmediately() {
        activePanel = PANEL_NONE;

        if (optionsPanel == null) {
            return;
        }

        optionsPanel.animate().cancel();
        optionsPanel.setVisibility(View.GONE);
        optionsPanel.setAlpha(1f);
        optionsPanel.setTranslationY(0f);
        optionsPanel.setScaleX(1f);
        optionsPanel.setScaleY(1f);
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private long getCurrentPlaybackPosition() {
        if (exoPlayer != null) {
            long position = exoPlayer.getCurrentPosition();
            return position >= 0 ? position : 0L;
        }

        if (mediaPlayer != null) {
            try {
                return Math.max(0, mediaPlayer.getCurrentPosition());
            } catch (Exception ignored) {
                return 0L;
            }
        }

        return startPositionMs != C.TIME_UNSET ? Math.max(0L, startPositionMs) : 0L;
    }

    private boolean shouldResumePlayback() {
        if (exoPlayer != null) {
            return exoPlayer.getPlayWhenReady();
        }

        if (mediaPlayer != null) {
            try {
                return mediaPlayer.isPlaying();
            } catch (Exception ignored) {
                return false;
            }
        }

        return startPlayWhenReady;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_CURRENT_INDEX, currentIndex);
        outState.putLong(STATE_POSITION_MS, getCurrentPlaybackPosition());
        outState.putBoolean(STATE_PLAY_WHEN_READY, shouldResumePlayback());
        outState.putString(STATE_ENGINE, preferredEngine);
        outState.putFloat(STATE_ZOOM, zoomScale);
        outState.putString(STATE_QUALITY_KEY, selectedQualityKey);
        outState.putInt(STATE_RESIZE_MODE, resizeMode);
        outState.putInt(STATE_ENHANCEMENT_MODE, filterMode);
        outState.putBoolean(STATE_CAPTIONS_ENABLED, captionsEnabled);
        outState.putBoolean(STATE_STT_CAPTIONS_ENABLED, sttCaptionsEnabled);
        outState.putString(STATE_STT_LANGUAGE, selectedSttLanguage);
        outState.putString(STATE_STT_TRANSLATION_MODE, sttTranslationMode);

        Log.d(TAG, "onSaveInstanceState, index=" + currentIndex
                + ", position=" + getCurrentPlaybackPosition()
                + ", engine=" + preferredEngine);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        forceFullscreenVideoMode();
        activityResumed = true;
        syncSttListeningWithPlayback();
    }

    @Override
    protected void onPause() {
        startPositionMs = getCurrentPlaybackPosition();
        startPlayWhenReady = shouldResumePlayback();

        Log.d(TAG, "onPause, index=" + currentIndex
                + ", position=" + startPositionMs
                + ", playWhenReady=" + startPlayWhenReady);

        activityResumed = false;
        stopSttListeningOnly();

        super.onPause();

        if (exoPlayer != null) {
            exoPlayer.pause();
        }

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy, finishing=" + isFinishing()
                + ", changingConfig=" + isChangingConfigurations());

        if (isFinishing() || !isChangingConfigurations()) {
            VideoPlayerReturnGuard.markFinished(this);
        }

        uiHandler.removeCallbacksAndMessages(null);
        if (sttSubtitleManager != null) {
            sttSubtitleManager.release();
            sttSubtitleManager = null;
        }

        if (subtitleTranslator != null) {
            subtitleTranslator.release();
            subtitleTranslator = null;
        }

        releaseExoPlayer();
        releaseMediaPlayer();
        releaseYouTubePlayer();

        super.onDestroy();
    }

    private void releaseExoPlayer() {
        if (exoPlayerView != null) {
            exoPlayerView.setPlayer(null);
        }

        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }

        trackSelector = null;
    }

    private void releaseMediaPlayer() {
        if (mediaController != null) {
            mediaController.hide();
            mediaController = null;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void releaseYouTubePlayer() {
        if (youtubeWebView != null) {
            try {
                youtubeWebView.stopLoading();
                youtubeWebView.loadUrl("about:blank");
                youtubeWebView.clearHistory();
                youtubeWebView.clearCache(false);
                youtubeWebView.clearFormData();
                youtubeWebView.removeJavascriptInterface("AndroidYouTubeBridge");
                youtubeWebView.setWebChromeClient(null);
                youtubeWebView.removeAllViews();
            } catch (Exception ignored) {
            }
        }
    }


}
