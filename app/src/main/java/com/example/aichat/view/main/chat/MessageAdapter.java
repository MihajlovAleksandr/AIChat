package com.example.aichat.view.main.chat;

import com.example.aichat.model.utils.files.FileDownloader;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.animation.LinearInterpolator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.aichat.BuildConfig;
import com.example.aichat.controller.AIController;
import com.example.aichat.controller.main.chat.actions.ChatMessageActions;
import com.example.aichat.controller.main.chat.MessageController;
import com.example.aichat.model.ai.AIModel;
import com.example.aichat.model.ai.TranslateStyle;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.FileType;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.model.utils.media.audio.AudioPlayerManager;
import com.example.aichat.model.utils.media.ChatMediaMarkers;
import com.example.aichat.model.utils.files.FileDownloadProgressManager;
import com.example.aichat.model.utils.files.FileUploadProgressManager;
import com.example.aichat.model.utils.files.GlideAuthHelper;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.model.utils.theme.ThemeModel;
import com.example.aichat.model.utils.theme.ThemeSelectionCoordinator;
import com.example.aichat.model.utils.theme.ThemeStorage;
import com.example.aichat.R;
import com.example.aichat.view.main.chat.helpers.AudioMiniPlayerBar;
import com.example.aichat.view.main.chat.helpers.AudioWaveformSeekView;
import com.example.aichat.view.main.chat.helpers.CircleVideoTextureView;
import com.example.aichat.view.main.chat.helpers.ReplyRenderer;
import com.example.aichat.view.theme.binders.ThemeMessageAnimationBinder;
import com.example.aichat.view.theme.binders.ThemeMessageBinder;
import com.google.android.material.card.MaterialCardView;
import io.noties.markwon.Markwon;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.json.JSONObject;

@SuppressLint({"SetTextI18n", "StaticFieldLeak", "DefaultLocale", "UseCompatLoadingForDrawables", "NotifyDataSetChanged"})
@SuppressWarnings({"unused", "FieldCanBeLocal", "UnusedAssignment", "SameParameterValue", "ConstantConditions", "SpellCheckingInspection"})
@androidx.media3.common.util.UnstableApi
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>
        implements ReplyRenderer.MessageFinder, ReplyRenderer.ReplyClickListener {

    private static final long READ_DELAY_MS = 1000;
    private static final String TAG = "MessageAdapter";
    private static final String TAG_ANIMATION = "MessageAnimation";
    private static final int MENU_QUOTE_MESSAGE_ID = 0x5A17001;
    private static final int MENU_QUOTE_SELECTION_ID = 0x5A17002;
    private static final int MENU_TRANSLATE_MESSAGE_ID = 0x5A17003;
    private static final int MAX_LOCAL_DURATION_CACHE = 512;
    private static final int MAX_VOICE_WAVEFORM_CACHE = 96;
    private static final int MAX_LOCAL_VIDEO_THUMBNAILS = 40;
    private static final int MAX_AUDIO_ARTWORK_CACHE = 32;
    private static final Map<UUID, String> pendingQuoteSelections = new ConcurrentHashMap<>();
    private static final String QUOTE_MARKER_PREFIX = "<!--AI_CHAT_QUOTE:";
    private static final String QUOTE_MARKER_SUFFIX = "-->";
    private static final java.util.regex.Pattern QUOTE_MARKER_PATTERN =
            java.util.regex.Pattern.compile(
                    "\\s*<!--AI_CHAT_QUOTE:([A-Za-z0-9_\\-=]+)-->\\s*",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );
    private static final String THEME_STORAGE_PREFS = "themes_storage";
    private static final String THEME_ANIMATION_PREFS = "theme_animation_storage";
    private static final String THEME_ANIMATION_PREFIX = "theme_animation_";
    private static final int FILE_NAME_SCROLL_TAG = R.id.file_name;
    private static final int MAX_VISIBLE_PREFETCH_PER_PASS = 3;
    private static final int MAX_PARALLEL_VISIBLE_DOWNLOADS = 3;
    private static final long VISIBLE_DOWNLOAD_STALE_MS = 18_000L;
    private static final long VISIBLE_PREFETCH_RETRY_MS = 1_200L;


    private static final java.util.regex.Pattern URL_PREVIEW_PATTERN =
            java.util.regex.Pattern.compile(
                    "\\b((?:https?://|www\\.)[^\\s<>()]+|(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,}(?:/[^\\s<>()]*)?)",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );

    private static final java.util.regex.Pattern YOUTUBE_URL_PATTERN =
            java.util.regex.Pattern.compile(
                    "\\b((?:https?://)?(?:www\\.|m\\.)?(?:youtube\\.com|youtu\\.be)/[^\\s<>()]+)",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );

    private final Context appContext;
    private final List<Message> messages;
    private final UUID userId;
    private final Markwon markwon;
    private final ReplyRenderer replyRenderer;
    private final MessageController messageController;
    private final ThemeStorage themeStorage;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Map<UUID, com.example.aichat.model.entities.File> fileCache = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> loadingFiles = new ConcurrentHashMap<>();
    private final Map<UUID, String> localFileNames = new HashMap<>();
    private final Map<UUID, Integer> localMediaDurationCache = new ConcurrentHashMap<>();
    private final Set<UUID> loadingLocalMediaDurations =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, float[]> voiceWaveformCache = new ConcurrentHashMap<>();
    private final Map<UUID, Bitmap> localVideoThumbnailCache = new ConcurrentHashMap<>();
    private final Map<UUID, Bitmap> audioArtworkCache = new ConcurrentHashMap<>();
    private final Set<UUID> loadingLocalVideoThumbnails =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> loadingAudioArtwork =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> missingAudioArtwork =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> queuedVisibleDownloads =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> runningVisibleDownloads =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, Long> runningVisibleDownloadStartedAt = new ConcurrentHashMap<>();
    private final Deque<VisibleFileDownloadRequest> visibleDownloadQueue = new ArrayDeque<>();
    private final Runnable visiblePrefetchRunnable = this::prefetchVisibleFilesSafely;


    private final Map<String, LinkPreviewMetadata> linkPreviewCache = new ConcurrentHashMap<>();

    private final Set<String> loadingLinkPreviews =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Map<String, String> youtubeTitleCache = new ConcurrentHashMap<>();

    private final Set<String> loadingYoutubeTitles =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Set<UUID> failedRemoteImageIds =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final long MESSAGE_ANIMATION_RETRY_DELAY_MS = 120L;
    private static final int MESSAGE_ANIMATION_MAX_RETRY_COUNT = 20;

    private final Set<UUID> pendingAnimatedMessageIds =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Set<UUID> alreadyAnimatedMessageIds =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Map<UUID, Integer> pendingAnimationRetryCounts =
            new ConcurrentHashMap<>();

    private final Set<UUID> knownMessageIds =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private boolean firstMessagesLoaded = true;

    private FileDownloadProgressManager progressManager;
    private FileUploadProgressManager uploadProgressManager;
    private RecyclerView recyclerView;
    private OnMessageActionListener actionListener;
    private OnFileDownloadListener fileDownloadListener;
    private AudioPlayerManager audioPlayerManager;
    private UUID activeAudioFileId;
    private UUID highlightedMessageId;
    private Runnable readMessagesRunnable;

    private CircleVideoTextureView activeCircleVideoView;
    private UUID activeCircleVideoFileId;
    private SeekBar activeCircleTimeline;
    private TextView activeCircleDurationText;
    private View activeCircleContainerView;

    private final Map<UUID, String> audioDisplayTitles = new ConcurrentHashMap<>();
    private final Map<UUID, String> audioDisplaySubtitles = new ConcurrentHashMap<>();

    private Runnable circleTimelineRunnable;

    public interface OnFileDownloadListener {
        void onFileDownloadRequired(
                String url,
                String mimeType,
                UUID fileId,
                FileType fileType,
                boolean openAfterDownload
        );
    }

    public interface OnMessageActionListener {
        void onEditMessage(Message message);

        void onDeleteMessage(Message message);

        void onReplyToMessage(Message message);

        void onCopyMessageText(Message message);
    }

    @Nullable
    public static String consumePendingQuoteSelection(@Nullable UUID messageId) {
        if (messageId == null) {
            return null;
        }

        return pendingQuoteSelections.remove(messageId);
    }

    @NonNull
    public static String appendQuoteMarkerToText(
            @Nullable String messageText,
            @Nullable String quoteText
    ) {
        String baseText = messageText != null ? stripEmbeddedQuoteData(messageText).trim() : "";

        if (quoteText == null || quoteText.trim().isEmpty()) {
            return baseText;
        }

        String encodedQuote = encodeQuoteText(quoteText.trim());

        if (encodedQuote.isEmpty()) {
            return baseText;
        }

        if (baseText.isEmpty()) {
            return QUOTE_MARKER_PREFIX + encodedQuote + QUOTE_MARKER_SUFFIX;
        }

        return baseText + "\n" + QUOTE_MARKER_PREFIX + encodedQuote + QUOTE_MARKER_SUFFIX;
    }

    @Nullable
    public static String extractEmbeddedQuoteText(@Nullable String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return null;
        }

        java.util.regex.Matcher matcher = QUOTE_MARKER_PATTERN.matcher(rawText);
        String result = null;

        while (matcher.find()) {
            result = decodeQuoteText(matcher.group(1));
        }

        return result;
    }

    @NonNull
    public static String stripEmbeddedQuoteData(@Nullable String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return "";
        }

        return QUOTE_MARKER_PATTERN.matcher(rawText).replaceAll(" ").trim();
    }

    @NonNull
    private static String encodeQuoteText(@Nullable String quoteText) {
        if (quoteText == null || quoteText.trim().isEmpty()) {
            return "";
        }

        try {
            return android.util.Base64.encodeToString(
                    quoteText.trim().getBytes(StandardCharsets.UTF_8),
                    android.util.Base64.NO_WRAP | android.util.Base64.URL_SAFE
            );
        } catch (Exception ignored) {
            return "";
        }
    }

    @Nullable
    private static String decodeQuoteText(@Nullable String encodedQuote) {
        if (encodedQuote == null || encodedQuote.trim().isEmpty()) {
            return null;
        }

        try {
            byte[] decoded = android.util.Base64.decode(
                    encodedQuote.trim(),
                    android.util.Base64.NO_WRAP | android.util.Base64.URL_SAFE
            );

            return new String(decoded, StandardCharsets.UTF_8).trim();
        } catch (Exception ignored) {
            return null;
        }
    }

    public MessageAdapter(
            UUID currentUserId,
            List<Message> messages,
            Context context,
            FileDownloadProgressManager progressManager
    ) {
        setHasStableIds(true);

        this.appContext = context.getApplicationContext();
        this.userId = currentUserId;
        this.messages = messages != null ? messages : new ArrayList<>();
        this.markwon = Markwon.create(context);
        this.replyRenderer = new ReplyRenderer(context, this, this);
        this.messageController = new MessageController(currentUserId);
        this.themeStorage = new ThemeStorage(appContext);
        this.progressManager = progressManager;

        rememberKnownMessages(this.messages);
        firstMessagesLoaded = this.messages.isEmpty();

        preloadDownloadedFiles();
        setupProgressListener();
    }

    public AudioPlayerManager getAudioPlayerManager() {
        return audioPlayerManager;
    }

    public void setOnFileDownloadListener(OnFileDownloadListener listener) {
        this.fileDownloadListener = listener;
        scheduleVisibleFilePrefetch();
    }

    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.actionListener = listener;
    }

    public void scheduleVisibleFilePrefetch() {
        handler.removeCallbacks(visiblePrefetchRunnable);
        handler.postDelayed(visiblePrefetchRunnable, 180L);
    }


    public void prefetchMessageFiles(@Nullable Message message) {
        if (message == null || fileDownloadListener == null) {
            return;
        }

        List<UUID> files = message.getFiles();
        if (files == null || files.isEmpty()) {
            return;
        }


        if (messageController.isMyMessage(message)) {
            return;
        }

        Map<UUID, FileType> fileTypes = message.getFileTypes();
        Map<UUID, String> mimeTypes = message.getFileMimeTypes();
        Map<UUID, String> parsedNames = extractFileNames(message.getText(), files);

        int enqueued = 0;

        for (UUID fileId : files) {
            if (fileId == null) {
                continue;
            }

            FileType fileType = fileTypes != null ? fileTypes.get(fileId) : null;
            String mimeType = mimeTypes != null ? mimeTypes.get(fileId) : null;
            String name = parsedNames.get(fileId);

            if (name == null) {
                name = resolveFileName(fileId, fileCache.get(fileId), "/api/files/" + fileId);
            }


            if (!isLikelyPreviewOrPlayableFile(fileType, mimeType, name)) {
                continue;
            }

            if (shouldSkipVisibleFilePrefetch(fileId)) {
                continue;
            }

            enqueueVisibleDownload(new VisibleFileDownloadRequest(
                    "/api/files/" + fileId,
                    mimeType,
                    fileId,
                    fileType
            ));

            enqueued++;
        }

        if (enqueued > 0) {
            handler.post(this::startNextVisibleDownload);
        }
    }

    public void setProgressManager(FileDownloadProgressManager manager) {
        this.progressManager = manager;
    }

    public void setUploadProgressManager(FileUploadProgressManager manager) {
        this.uploadProgressManager = manager;

        if (uploadProgressManager == null) {
            return;
        }

        uploadProgressManager.addListener(new FileUploadProgressManager.Listener() {
            @Override
            public void onProgress(UUID fileId, int progress) {
                updateItemByFileId(fileId);
            }

            @Override
            public void onCompleted(UUID fileId) {
                updateItemByFileId(fileId);
            }

            @Override
            public void onError(UUID fileId) {
                updateItemByFileId(fileId);
            }

            @Override
            public void onCleared(UUID fileId) {
                updateItemByFileId(fileId);
            }
        });
    }

    private void setupProgressListener() {
        if (progressManager == null) {
            return;
        }

        progressManager.addListener(new FileDownloadProgressManager.Listener() {
            @Override
            public void onProgress(UUID fileId, int progress) {
                if (fileId != null && progress > 0 && progress < 100) {
                    runningVisibleDownloadStartedAt.put(fileId, System.currentTimeMillis());
                }

                updateItemByFileId(fileId);
            }

            @Override
            public void onCompleted(UUID fileId) {
                queuedVisibleDownloads.remove(fileId);
                runningVisibleDownloads.remove(fileId);
                runningVisibleDownloadStartedAt.remove(fileId);
                new Thread(() -> handleFileDownloadCompleted(fileId)).start();
                startNextVisibleDownload();
                scheduleVisibleFilePrefetch();
            }

            @Override
            public void onError(UUID fileId) {
                queuedVisibleDownloads.remove(fileId);
                runningVisibleDownloads.remove(fileId);
                runningVisibleDownloadStartedAt.remove(fileId);
                startNextVisibleDownload();
                scheduleVisibleFilePrefetch();

                int position = findMessagePositionByFileId(fileId);

                if (position == -1) {
                    return;
                }

                if (recyclerView != null) {
                    recyclerView.post(() -> notifyItemChanged(position));
                } else {
                    notifyItemChanged(position);
                }
            }

            @Override
            public void onCleared() {
                fileCache.clear();
                runningVisibleDownloadStartedAt.clear();

                if (recyclerView != null) {
                    recyclerView.post(MessageAdapter.this::notifyAllVisibleItemsChanged);
                } else {
                    notifyAllVisibleItemsChanged();
                }
            }
        });
    }

    private void handleFileDownloadCompleted(UUID fileId) {
        com.example.aichat.model.entities.File entity = waitForDownloadedFile(fileId);

        if (entity == null) {
            return;
        }

        File localFile = new File(entity.localPath);

        if (!localFile.exists()) {
            return;
        }

        fileCache.put(fileId, entity);
        failedRemoteImageIds.remove(fileId);
        warmLocalMediaDuration(fileId, entity);
        warmLocalAudioArtwork(fileId, entity);
        warmLocalVideoThumbnail(fileId, entity);

        if (recyclerView == null) {
            return;
        }

        recyclerView.post(() -> {
            notifyFileChanged(fileId);

            if (audioPlayerManager != null && audioPlayerManager.shouldAutoPlay(fileId)) {
                audioPlayerManager.clearPendingPlay();
                activeAudioFileId = fileId;

                prepareAudioQueueForPlayback(recyclerView.getContext(), fileId);

                audioPlayerManager.playLocal(
                        recyclerView.getContext(),
                        entity.localPath,
                        fileId,
                        resolveAudioTitle(fileId, entity),
                        resolveAudioSubtitle(fileId, entity),
                        isVoiceAudioFile(fileId)
                );

                showAudioMiniPlayer(fileId);
                notifyFileChanged(fileId);
            }
        });
    }

    private com.example.aichat.model.entities.File waitForDownloadedFile(UUID fileId) {
        for (int i = 0; i < 20; i++) {
            com.example.aichat.model.entities.File entity =
                    DatabaseManager.getDatabase().fileDao().getById(fileId);

            if (hasExistingLocalFile(entity)) {
                return entity;
            }

            try {
                Thread.sleep(150);
            } catch (InterruptedException ignored) {
            }
        }

        return null;
    }

    @Override
    public long getItemId(int position) {
        UUID id = messages.get(position).getId();

        if (id == null) {
            return RecyclerView.NO_ID;
        }

        return id.getMostSignificantBits() ^ id.getLeastSignificantBits();
    }

    private void updateVisibleAudioProgress(UUID fileId) {
        if (recyclerView == null) {
            return;
        }

        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);

            if (!(holder instanceof MessageViewHolder)) {
                continue;
            }

            ((MessageViewHolder) holder).updateAudioProgress(fileId);
            ThemeMessageBinder.bind(holder.itemView);
            ((MessageViewHolder) holder).applyPostThemeStabilization();
        }
    }

    public void detachPlayer() {
        if (audioPlayerManager != null) {
            audioPlayerManager.detach();
        }
    }

    private int findMessagePositionByFileId(UUID fileId) {
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);

            if (message.getFiles() != null && message.getFiles().contains(fileId)) {
                return i;
            }
        }

        return -1;
    }

    private String resolveFileName(
            UUID fileId,
            com.example.aichat.model.entities.File entity,
            String url
    ) {
        String local = localFileNames.get(fileId);

        if (local != null && !local.isEmpty()) {
            return local;
        }

        String declaredName = findDeclaredFileName(fileId);
        if (declaredName != null && !declaredName.trim().isEmpty()) {
            localFileNames.put(fileId, declaredName);
            return declaredName;
        }

        if (entity != null && entity.fileName != null && !entity.fileName.isEmpty()) {
            return entity.fileName;
        }

        if (hasLocalPath(entity)) {
            int index = entity.localPath.lastIndexOf('/');

            if (index != -1 && index < entity.localPath.length() - 1) {
                return entity.localPath.substring(index + 1);
            }
        }

        if (url != null) {
            int index = url.lastIndexOf('/');

            if (index != -1 && index < url.length() - 1) {
                return url.substring(index + 1);
            }
        }

        return "Файл";
    }

    @Nullable
    private String findDeclaredFileName(@Nullable UUID fileId) {
        if (fileId == null) {
            return null;
        }

        for (Message message : messages) {
            if (message == null || message.getFiles() == null || !message.getFiles().contains(fileId)) {
                continue;
            }

            Map<UUID, String> parsedNames = extractFileNames(message.getText(), message.getFiles());
            String parsedName = parsedNames.get(fileId);

            if (parsedName != null && !parsedName.trim().isEmpty()) {
                return parsedName;
            }
        }

        return null;
    }

    private void updateItemByFileId(UUID fileId) {
        int position = findMessagePositionByFileId(fileId);

        if (position == -1) {
            return;
        }

        if (recyclerView != null) {
            recyclerView.post(() -> notifyItemChanged(position, fileId));
        } else {
            notifyItemChanged(position, fileId);
        }
    }

    @Override
    public Message findMessageById(UUID id) {
        for (Message message : messages) {
            if (message.getId().equals(id)) {
                return message;
            }
        }

        return null;
    }

    public void setAudioPlayerManager(AudioPlayerManager manager) {
        this.audioPlayerManager = manager;

        if (audioPlayerManager == null) {
            return;
        }

        audioPlayerManager.setListener(new AudioPlayerManager.Listener() {
            @Override
            public void onStart(UUID fileId) {
                UUID previous = activeAudioFileId;
                activeAudioFileId = fileId;

                showAudioMiniPlayer(fileId);

                if (previous != null && !previous.equals(fileId)) {
                    notifyFileChanged(previous);
                }

                notifyFileChanged(fileId);
            }

            @Override
            public void onStop(UUID fileId) {
                if (fileId == null) {
                    return;
                }

                AudioPlayerManager.PlaybackState state = audioPlayerManager.getPlaybackState(fileId);

                if (state == AudioPlayerManager.PlaybackState.PAUSED
                        || state == AudioPlayerManager.PlaybackState.PREPARING
                        || state == AudioPlayerManager.PlaybackState.PLAYING) {
                    updateAudioMiniPlayerProgress(fileId);
                    notifyFileChanged(fileId);
                    return;
                }

                if (fileId.equals(activeAudioFileId)) {
                    activeAudioFileId = null;
                }

                AudioMiniPlayerBar.update(fileId, 0, 0, false);
                hideAudioMiniPlayerIfSupported();
                notifyFileChanged(fileId);
            }

            @Override
            public void onProgress(UUID fileId) {
                updateVisibleAudioProgress(fileId);
                updateAudioMiniPlayerProgress(fileId);
            }
        });
    }

    private void showAudioMiniPlayer(@Nullable UUID fileId) {
        if (fileId == null || audioPlayerManager == null) {
            return;
        }

        if (recyclerView != null) {
            String title = audioDisplayTitles.get(fileId);
            if (title == null || title.trim().isEmpty()) {
                title = recyclerView.getContext().getString(R.string.audio_now_playing_unknown);
            }

            String subtitle = audioDisplaySubtitles.get(fileId);
            if (subtitle == null) {
                subtitle = "";
            }

            AudioMiniPlayerBar.show(
                    recyclerView.getContext(),
                    audioPlayerManager,
                    fileId,
                    title,
                    subtitle
            );
        }

        updateAudioMiniPlayerProgress(fileId);
    }

    private void updateAudioMiniPlayerProgress(@Nullable UUID fileId) {
        if (fileId == null || audioPlayerManager == null) {
            return;
        }

        int duration = audioPlayerManager.getDuration(fileId);
        int position = audioPlayerManager.getCurrentPosition(fileId);
        boolean playing = audioPlayerManager.isPlaying(fileId);
        boolean preparing = audioPlayerManager.isPreparing(fileId);

        if (duration <= 0) {
            com.example.aichat.model.entities.File entity = fileCache.get(fileId);
            duration = getKnownLocalMediaDuration(fileId, entity);
        }

        AudioMiniPlayerBar.update(fileId, duration, position, playing || preparing);
    }

    private void hideAudioMiniPlayerIfSupported() {
        try {
            Class<?> miniPlayerClass = Class.forName(
                    "com.example.aichat.view.main.chat.helpers.AudioMiniPlayerBar"
            );
            java.lang.reflect.Method hideMethod = miniPlayerClass.getDeclaredMethod("hide");
            hideMethod.invoke(null);
        } catch (Exception ignored) {
        }
    }

    private void notifyFileChanged(UUID fileId) {
        if (fileId == null) {
            return;
        }

        int position = findMessagePositionByFileId(fileId);

        if (position == -1 || recyclerView == null) {
            return;
        }

        recyclerView.post(() -> notifyItemChanged(position, fileId));
    }

    @Nullable
    private UUID resolveFirstReplyMessageId(@Nullable Message sourceMessage) {
        if (sourceMessage == null || sourceMessage.getReplyMessages() == null
                || sourceMessage.getReplyMessages().isEmpty()) {
            return null;
        }

        Object firstReply = sourceMessage.getReplyMessages().get(0);

        return extractReplyMessageId(firstReply);
    }

    @Nullable
    private UUID extractReplyMessageId(@Nullable Object replyObject) {
        if (replyObject == null) {
            return null;
        }

        if (replyObject instanceof UUID) {
            return (UUID) replyObject;
        }

        if (replyObject instanceof Message) {
            return ((Message) replyObject).getId();
        }

        String[] methodNames = {
                "getMessageId",
                "getReplyMessageId",
                "getRepliedMessageId",
                "getOriginalMessageId",
                "getId"
        };

        for (String methodName : methodNames) {
            UUID id = extractUuidFromNoArgMethod(replyObject, methodName);

            if (id != null) {
                return id;
            }
        }

        String[] fieldNames = {
                "messageId",
                "replyMessageId",
                "repliedMessageId",
                "originalMessageId",
                "id"
        };

        for (String fieldName : fieldNames) {
            UUID id = extractUuidFromField(replyObject, fieldName);

            if (id != null) {
                return id;
            }
        }

        return null;
    }

    @Nullable
    private UUID extractUuidFromNoArgMethod(
            @NonNull Object object,
            @NonNull String methodName
    ) {
        try {
            java.lang.reflect.Method method = object.getClass().getMethod(methodName);
            Object value = method.invoke(object);

            return convertToUuid(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private UUID extractUuidFromField(
            @NonNull Object object,
            @NonNull String fieldName
    ) {
        try {
            java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(object);

            return convertToUuid(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private UUID convertToUuid(@Nullable Object value) {
        if (value instanceof UUID) {
            return (UUID) value;
        }

        if (value instanceof String) {
            try {
                return UUID.fromString((String) value);
            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }

    @Override
    public void onReplyClicked(UUID replyMessageId) {
        Message original = findMessageById(replyMessageId);

        if (original != null) {
            highlightMessage(original.getId());
            scrollToMessage(original.getId());
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                scheduleVisibleFilePrefetch();
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scheduleVisibleFilePrefetch();
                }
            }
        });

        scheduleVisibleFilePrefetch();
    }

    private Map<UUID, String> extractFileNames(String text, List<UUID> fileIds) {
        Map<UUID, String> result = new HashMap<>();

        if (text == null || text.isEmpty() || fileIds == null) {
            return result;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[file:([^\\]]*)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        int index = 0;

        while (matcher.find()) {
            String payload = matcher.group(1);

            if (payload == null) {
                continue;
            }

            payload = payload.trim();

            String displayName = ChatMediaMarkers.extractDisplayNameFromMarkerPayload(payload);

            if (payload.length() > 36 && payload.charAt(36) == ':') {
                try {
                    UUID markerFileId = UUID.fromString(payload.substring(0, 36));

                    if (fileIds.contains(markerFileId) && !displayName.isEmpty()) {
                        result.put(markerFileId, displayName);
                        continue;
                    }
                } catch (Exception ignored) {
                }
            }

            while (index < fileIds.size() && result.containsKey(fileIds.get(index))) {
                index++;
            }

            if (index < fileIds.size() && !displayName.isEmpty()) {
                result.put(fileIds.get(index), displayName);
                index++;
            }
        }

        return result;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        if (readMessagesRunnable != null) {
            handler.removeCallbacks(readMessagesRunnable);
            readMessagesRunnable = null;
        }

        visibleDownloadQueue.clear();
        queuedVisibleDownloads.clear();
        runningVisibleDownloads.clear();
        runningVisibleDownloadStartedAt.clear();
        handler.removeCallbacks(visiblePrefetchRunnable);

        this.recyclerView = null;
    }

    public void setMessages(List<Message> newMessages) {
        replaceMessages(newMessages);

        if (firstMessagesLoaded) {
            rememberKnownMessages(this.messages);
            firstMessagesLoaded = false;
        } else {
            markNewOutgoingMessagesForAnimation(this.messages);
        }

        scheduleVisibleFilePrefetch();
    }

    @SuppressWarnings("unused")
    public void updateMessages(List<Message> newMessages) {
        setMessages(newMessages);
    }

    private void replaceMessages(List<Message> newMessages) {
        int oldSize = this.messages.size();

        if (oldSize > 0) {
            this.messages.clear();
            notifyItemRangeRemoved(0, oldSize);
        }

        if (newMessages == null || newMessages.isEmpty()) {
            return;
        }

        this.messages.addAll(newMessages);
        notifyItemRangeInserted(0, this.messages.size());
    }

    private void rememberKnownMessages(List<Message> source) {
        knownMessageIds.clear();

        if (source == null) {
            return;
        }

        for (Message message : source) {
            if (message != null) {
                knownMessageIds.add(message.getId());
            }
        }
    }

    private void markNewOutgoingMessagesForAnimation(List<Message> source) {
        if (source == null) {
            return;
        }

        for (Message message : source) {
            if (message == null) {
                continue;
            }

            boolean isNewMessage = !knownMessageIds.contains(message.getId());

            if (isNewMessage && message.getId() != null) {
                pendingAnimatedMessageIds.add(message.getId());
                Log.d(TAG_ANIMATION, "mark pending from list: " + message.getId());
            }

            knownMessageIds.add(message.getId());
        }
    }

    private void markMessageForAnimationIfNeeded(Message message) {
        if (message == null) {
            return;
        }

        boolean isNewMessage = !knownMessageIds.contains(message.getId());
        knownMessageIds.add(message.getId());

        if (!isNewMessage || message.getId() == null) {
            return;
        }

        pendingAnimatedMessageIds.add(message.getId());
        Log.d(TAG_ANIMATION, "mark pending direct: " + message.getId());
    }

    public void checkVisibleMessages(ChatMessageActions actions) {
        if (actions == null) {
            return;
        }

        List<Message> toRead = collectVisibleUnreadMessages(false);

        handleMessagesToRead(toRead, actions);
    }

    public int readVisibleMessagesImmediately(
            ChatMessageActions actions,
            boolean relaxedVisibility
    ) {
        if (actions == null) {
            return 0;
        }

        if (readMessagesRunnable != null) {
            handler.removeCallbacks(readMessagesRunnable);
            readMessagesRunnable = null;
        }

        List<Message> toRead = collectVisibleUnreadMessages(relaxedVisibility);

        if (toRead.isEmpty()) {
            return 0;
        }

        readMessages(toRead, actions);

        return toRead.size();
    }

    public int readUnreadMessageAt(
            int position,
            ChatMessageActions actions
    ) {
        if (actions == null || position < 0 || position >= messages.size()) {
            return 0;
        }

        Message message = messages.get(position);

        if (!shouldReadMessage(message)) {
            return 0;
        }

        if (readMessagesRunnable != null) {
            handler.removeCallbacks(readMessagesRunnable);
            readMessagesRunnable = null;
        }

        List<Message> toRead = new ArrayList<>();
        toRead.add(message);

        readMessages(toRead, actions);

        return 1;
    }

    private List<Message> collectVisibleUnreadMessages(boolean relaxedVisibility) {
        List<Message> toRead = new ArrayList<>();

        if (recyclerView == null || messages.isEmpty()) {
            return toRead;
        }

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();

        if (!(layoutManager instanceof LinearLayoutManager)) {
            return toRead;
        }

        LinearLayoutManager manager = (LinearLayoutManager) layoutManager;

        int firstVisible = manager.findFirstVisibleItemPosition();
        int lastVisible = manager.findLastVisibleItemPosition();

        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
            return toRead;
        }

        boolean reachedBottom = !recyclerView.canScrollVertically(1);

        for (int i = firstVisible; i <= lastVisible && i < messages.size(); i++) {
            View itemView = manager.findViewByPosition(i);

            if (itemView == null) {
                continue;
            }

            boolean visibleEnough = reachedBottom
                    || (relaxedVisibility
                    ? isMessageVisibleAtLeastPartly(itemView)
                    : isMessageEnoughVisible(itemView));

            if (!visibleEnough) {
                continue;
            }

            Message message = messages.get(i);

            if (shouldReadMessage(message)) {
                toRead.add(message);
            }
        }

        if (reachedBottom) {
            for (int i = 0; i <= lastVisible && i < messages.size(); i++) {
                Message message = messages.get(i);

                if (shouldReadMessage(message) && !toRead.contains(message)) {
                    toRead.add(message);
                }
            }
        }

        return toRead;
    }

    private boolean shouldReadMessage(Message message) {
        if (message == null) {
            return false;
        }

        if (messageController.isMyMessage(message)) {
            return false;
        }

        Map<UUID, MessageStatus> statuses = message.getStatuses();

        return statuses != null
                && statuses.containsKey(userId)
                && statuses.get(userId) != MessageStatus.READ;
    }

    private void handleMessagesToRead(List<Message> toRead, ChatMessageActions actions) {
        if (toRead.isEmpty()) {
            if (readMessagesRunnable != null) {
                handler.removeCallbacks(readMessagesRunnable);
                readMessagesRunnable = null;
            }

            return;
        }

        if (readMessagesRunnable != null) {
            handler.removeCallbacks(readMessagesRunnable);
            readMessagesRunnable = null;
        }

        readMessagesRunnable = () -> {
            readMessages(toRead, actions);
            readMessagesRunnable = null;
        };

        handler.postDelayed(readMessagesRunnable, READ_DELAY_MS);
    }

    private boolean isMessageEnoughVisible(View itemView) {
        if (recyclerView == null || itemView == null) {
            return false;
        }

        int parentTop = recyclerView.getPaddingTop();
        int parentBottom = recyclerView.getHeight() - recyclerView.getPaddingBottom();

        int visibleTop = Math.max(itemView.getTop(), parentTop);
        int visibleBottom = Math.min(itemView.getBottom(), parentBottom);
        int visibleHeight = Math.max(0, visibleBottom - visibleTop);
        int itemHeight = itemView.getHeight();

        if (itemHeight <= 0) {
            return false;
        }

        int adapterPosition = recyclerView.getChildAdapterPosition(itemView);

        if (adapterPosition == messages.size() - 1
                && !recyclerView.canScrollVertically(1)
                && visibleHeight > 0) {
            return true;
        }

        int enoughHeight = Math.min(
                Math.round(itemHeight * 0.6f),
                dp(recyclerView, 180)
        );

        enoughHeight = Math.max(
                dp(recyclerView, 48),
                enoughHeight
        );

        return visibleHeight >= enoughHeight;
    }

    private boolean isMessageVisibleAtLeastPartly(View itemView) {
        if (recyclerView == null || itemView == null) {
            return false;
        }

        int parentTop = recyclerView.getPaddingTop();
        int parentBottom = recyclerView.getHeight() - recyclerView.getPaddingBottom();

        int visibleTop = Math.max(itemView.getTop(), parentTop);
        int visibleBottom = Math.min(itemView.getBottom(), parentBottom);

        return Math.max(0, visibleBottom - visibleTop) > 0;
    }

    private void readMessages(List<Message> messagesToRead, ChatMessageActions actions) {
        if (messagesToRead == null || messagesToRead.isEmpty() || actions == null) {
            return;
        }

        List<Message> uniqueMessagesToRead = new ArrayList<>();

        Set<UUID> addedIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

        for (Message message : messagesToRead) {
            if (message == null) {
                continue;
            }

            if (!shouldReadMessage(message)) {
                continue;
            }

            if (addedIds.add(message.getId())) {
                uniqueMessagesToRead.add(message);
            }
        }

        if (uniqueMessagesToRead.isEmpty()) {
            return;
        }

        for (Message message : uniqueMessagesToRead) {
            if (message.getStatuses() != null) {
                message.getStatuses().replace(userId, MessageStatus.READ);
            }

            int position = getMessagePosition(message.getId());

            if (position != -1) {
                if (recyclerView != null) {
                    recyclerView.post(() -> {
                        if (recyclerView != null) {
                            notifyItemChanged(position);
                        }
                    });
                } else {
                    notifyItemChanged(position);
                }
            }
        }

        actions.readMessages(uniqueMessagesToRead);
    }

    private void applyFileState(
            ProgressBar progressBar,
            TextView progressText,
            ImageView status,
            ImageView menuButton,
            UUID fileId,
            boolean isMyMessage,
            Integer downloadProgress,
            boolean isDownloaded
    ) {
        Integer uploadProgress = uploadProgressManager != null
                ? uploadProgressManager.getProgress(fileId)
                : null;

        boolean isUploading = isMyMessage
                && uploadProgress != null
                && uploadProgress >= 0
                && uploadProgress < 100;

        boolean isUploaded = isMyMessage
                && uploadProgress != null
                && uploadProgress >= 100;

        if (isUploading) {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(uploadProgress);
            }

            if (progressText != null) {
                progressText.setVisibility(View.VISIBLE);
                progressText.setText(formatPercent(uploadProgress));
            }

            if (status != null) {
                status.setVisibility(View.GONE);
            }

            if (menuButton != null) {
                menuButton.setVisibility(View.GONE);
            }

            return;
        }

        if (isUploaded) {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            if (progressText != null) {
                progressText.setVisibility(View.GONE);
            }

            if (status != null) {
                status.setVisibility(View.VISIBLE);
            }

            if (menuButton != null) {
                menuButton.setVisibility(View.VISIBLE);
            }

            return;
        }

        boolean isDownloading = !isDownloaded
                && downloadProgress != null
                && downloadProgress >= 0
                && downloadProgress < 100;

        if (isDownloading) {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(downloadProgress);
            }

            if (progressText != null) {
                progressText.setVisibility(View.VISIBLE);
                progressText.setText(formatPercent(downloadProgress));
            }

            if (status != null) {
                status.setVisibility(View.GONE);
            }

            if (menuButton != null) {
                menuButton.setVisibility(View.GONE);
            }

            return;
        }

        if (isDownloaded) {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            if (progressText != null) {
                progressText.setVisibility(View.GONE);
            }

            if (status != null) {
                status.setVisibility(View.GONE);
            }

            if (menuButton != null) {
                menuButton.setVisibility(View.VISIBLE);
            }

            return;
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (progressText != null) {
            progressText.setVisibility(View.GONE);
        }

        if (status != null) {
            status.setVisibility(View.GONE);
        }

        if (menuButton != null) {
            menuButton.setVisibility(View.VISIBLE);
        }
    }


    private int getKnownLocalMediaDuration(
            @NonNull UUID fileId,
            @Nullable com.example.aichat.model.entities.File entity
    ) {
        Integer cached = localMediaDurationCache.get(fileId);

        if (cached != null && cached > 0) {
            return cached;
        }

        if (entity != null
                && entity.localPath != null
                && !entity.localPath.trim().isEmpty()) {
            File localFile = new File(entity.localPath);

            if (localFile.exists()) {
                int duration = readLocalMediaDurationMs(localFile);

                if (duration > 0) {
                    putBoundedCache(
                            localMediaDurationCache,
                            fileId,
                            duration,
                            MAX_LOCAL_DURATION_CACHE
                    );

                    return duration;
                }
            }
        }

        warmLocalMediaDuration(fileId, entity);

        return 0;
    }

    private void warmLocalVideoThumbnail(
            @Nullable UUID fileId,
            @Nullable com.example.aichat.model.entities.File entity
    ) {
        if (fileId == null || entity == null || entity.localPath == null || entity.localPath.trim().isEmpty()) {
            return;
        }

        if (localVideoThumbnailCache.containsKey(fileId)) {
            return;
        }

        String mime = entity.mimeType != null ? entity.mimeType.toLowerCase(Locale.US) : "";
        String name = entity.fileName != null ? entity.fileName : new File(entity.localPath).getName();

        if (!mime.startsWith("video") && !looksLikeVideoFile(name)) {
            return;
        }

        startLocalVideoThumbnailLoad(fileId, entity.localPath);
    }

    private void startLocalVideoThumbnailLoad(
            @NonNull UUID fileId,
            @Nullable String localPath
    ) {
        if (localPath == null || localPath.trim().isEmpty()) {
            return;
        }

        File file = new File(localPath);
        if (!file.exists()) {
            return;
        }

        if (!loadingLocalVideoThumbnails.add(fileId)) {
            return;
        }

        new Thread(() -> {
            Bitmap frame = null;

            try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
                retriever.setDataSource(file.getAbsolutePath());
                frame = readBestVideoFrame(retriever);

                if (frame != null) {
                    frame = scaleVideoThumbnail(frame, 640);
                }
            } catch (Exception exception) {
                Log.e(TAG, "Cannot load local video thumbnail", exception);
            }

            if (frame == null) {
                frame = readThumbnailUtilsFrame(file);
            }

            Bitmap finalFrame = frame;

            handler.post(() -> {
                loadingLocalVideoThumbnails.remove(fileId);

                if (finalFrame != null && !finalFrame.isRecycled()) {
                    putBoundedCache(
                            localVideoThumbnailCache,
                            fileId,
                            finalFrame,
                            MAX_LOCAL_VIDEO_THUMBNAILS
                    );

                    notifyFileChanged(fileId);
                }
            });
        }).start();
    }

    @Nullable
    private Bitmap readBestVideoFrame(@NonNull MediaMetadataRetriever retriever) {
        long[] candidatesUs = new long[]{
                0L,
                250_000L,
                1_000_000L,
                2_000_000L
        };

        String durationRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long durationMs = 0L;

        try {
            if (durationRaw != null && !durationRaw.trim().isEmpty()) {
                durationMs = Long.parseLong(durationRaw.trim());
            }
        } catch (Exception ignored) {
        }

        for (long timeUs : candidatesUs) {
            if (durationMs > 0 && timeUs / 1000L > durationMs) {
                continue;
            }

            Bitmap frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);

            if (frame == null) {
                frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST);
            }

            if (frame != null && !frame.isRecycled()) {
                return frame;
            }
        }

        try {
            return retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private Bitmap scaleVideoThumbnail(@NonNull Bitmap source, int maxSide) {
        int width = source.getWidth();
        int height = source.getHeight();

        if (width <= 0 || height <= 0) {
            return source;
        }

        int largestSide = Math.max(width, height);

        if (largestSide <= maxSide) {
            return source;
        }

        float scale = maxSide / (float) largestSide;
        int targetWidth = Math.max(1, Math.round(width * scale));
        int targetHeight = Math.max(1, Math.round(height * scale));

        Bitmap scaled = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true);

        if (scaled != source) {
            source.recycle();
        }

        return scaled;
    }

    @Nullable
    private Bitmap readThumbnailUtilsFrame(@NonNull File file) {
        try {
            Bitmap frame = ThumbnailUtils.createVideoThumbnail(
                    file.getAbsolutePath(),
                    MediaStore.Video.Thumbnails.MINI_KIND
            );

            if (frame != null && !frame.isRecycled()) {
                return scaleVideoThumbnail(frame, 640);
            }
        } catch (Exception exception) {
            Log.e(TAG, "Cannot create video thumbnail fallback", exception);
        }

        return null;
    }

    private void warmLocalMediaDuration(
            @Nullable UUID fileId,
            @Nullable com.example.aichat.model.entities.File entity
    ) {
        if (fileId == null || entity == null || entity.localPath == null || entity.localPath.trim().isEmpty()) {
            return;
        }

        if (localMediaDurationCache.containsKey(fileId)) {
            return;
        }

        File localFile = new File(entity.localPath);

        if (!localFile.exists()) {
            return;
        }

        String mime = entity.mimeType != null ? entity.mimeType.toLowerCase(Locale.US) : "";
        String name = entity.fileName != null ? entity.fileName.toLowerCase(Locale.US) : localFile.getName().toLowerCase(Locale.US);

        boolean supported = mime.startsWith("audio")
                || mime.startsWith("video")
                || looksLikeAudioFile(name)
                || looksLikeVideoFile(name);

        if (!supported || !loadingLocalMediaDurations.add(fileId)) {
            return;
        }

        new Thread(() -> {
            int duration = readLocalMediaDurationMs(localFile);
            loadingLocalMediaDurations.remove(fileId);

            if (duration <= 0) {
                return;
            }

            putBoundedCache(localMediaDurationCache, fileId, duration, MAX_LOCAL_DURATION_CACHE);
            handler.post(() -> updateItemByFileId(fileId));
        }).start();
    }

    private boolean looksLikeAudioFile(@Nullable String fileName) {
        if (fileName == null) {
            return false;
        }

        String lower = fileName.toLowerCase(Locale.US);

        return lower.endsWith(".mp3")
                || lower.endsWith(".wav")
                || lower.endsWith(".ogg")
                || lower.endsWith(".m4a")
                || lower.endsWith(".aac")
                || lower.endsWith(".flac");
    }

    private boolean looksLikeImageFile(@Nullable String fileName) {
        if (fileName == null) {
            return false;
        }

        String lower = fileName.toLowerCase(Locale.US);

        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp")
                || lower.endsWith(".gif");
    }

    private boolean looksLikeVideoFile(@Nullable String fileName) {
        if (fileName == null) {
            return false;
        }

        String lower = fileName.toLowerCase(Locale.US);

        return lower.endsWith(".mp4")
                || lower.endsWith(".m4v")
                || lower.endsWith(".mov")
                || lower.endsWith(".mkv")
                || lower.endsWith(".webm")
                || lower.endsWith(".3gp")
                || lower.endsWith(".3gpp");
    }

    private int readLocalMediaDurationMs(@NonNull File file) {
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(file.getAbsolutePath());
            String rawDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

            if (rawDuration == null || rawDuration.trim().isEmpty()) {
                return 0;
            }

            long duration = Long.parseLong(rawDuration.trim());

            if (duration <= 0 || duration > Integer.MAX_VALUE) {
                return 0;
            }

            return (int) duration;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static <T> void putBoundedCache(
            @NonNull Map<UUID, T> cache,
            @NonNull UUID key,
            @NonNull T value,
            int maxSize
    ) {
        if (maxSize <= 0) {
            return;
        }

        if (cache.size() >= maxSize && !cache.containsKey(key)) {
            UUID firstKey = null;

            for (UUID candidate : cache.keySet()) {
                firstKey = candidate;
                break;
            }

            if (firstKey != null) {
                cache.remove(firstKey);
            }
        }

        cache.put(key, value);
    }

    public void clearFileCache() {
        fileCache.clear();
        localMediaDurationCache.clear();
        audioArtworkCache.clear();
        loadingAudioArtwork.clear();
        missingAudioArtwork.clear();
        failedRemoteImageIds.clear();
        notifyAllVisibleItemsChanged();
    }

    public void cacheLocalFile(
            UUID fileId,
            String localPath,
            String mimeType,
            FileType fileType,
            long size,
            String fileName
    ) {
        if (fileId == null || localPath == null || localPath.trim().isEmpty()) {
            return;
        }

        File localFile = new File(localPath);

        if (!localFile.exists()) {
            return;
        }

        long now = System.currentTimeMillis();

        com.example.aichat.model.entities.File entity =
                new com.example.aichat.model.entities.File(
                        fileId,
                        localPath,
                        mimeType,
                        fileType != null ? fileType.name() : null,
                        size > 0 ? size : localFile.length(),
                        now,
                        now,
                        fileName
                );

        fileCache.put(fileId, entity);
        missingAudioArtwork.remove(fileId);

        if (fileName != null && !fileName.trim().isEmpty()) {
            localFileNames.put(fileId, fileName);
        }

        failedRemoteImageIds.remove(fileId);
        warmLocalMediaDuration(fileId, entity);
        warmLocalAudioArtwork(fileId, entity);
        updateItemByFileId(fileId);
    }

    public void onLocalAudioFileReady(
            @Nullable UUID fileId,
            @Nullable String localPath
    ) {
        if (fileId == null || localPath == null || localPath.trim().isEmpty()) {
            return;
        }

        File localFile = new File(localPath);

        if (!localFile.exists()) {
            return;
        }

        String fileName = findDeclaredFileName(fileId);
        FileType fileType = null;
        String mimeType = null;

        for (Message message : messages) {
            if (message == null || message.getFiles() == null || !message.getFiles().contains(fileId)) {
                continue;
            }

            Map<UUID, FileType> types = message.getFileTypes();
            Map<UUID, String> mimes = message.getFileMimeTypes();

            if (types != null) {
                fileType = types.get(fileId);
            }

            if (mimes != null) {
                mimeType = mimes.get(fileId);
            }

            break;
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = localFileNames.get(fileId);
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = localFile.getName();
        }

        if (!isLikelyAudioFile(fileType, mimeType, fileName)) {
            return;
        }

        cacheLocalFile(
                fileId,
                localFile.getAbsolutePath(),
                mimeType,
                fileType,
                localFile.length(),
                fileName
        );
    }

    public void clearFailedRemoteLoads() {
        failedRemoteImageIds.clear();
    }

    public void addOrUpdateMessage(Message newMessage) {
        if (newMessage == null) {
            return;
        }

        int position = getMessagePosition(newMessage.getId());

        if (position != -1) {
            messages.set(position, newMessage);
            notifyItemChanged(position);
        } else {
            messages.add(newMessage);
            markMessageForAnimationIfNeeded(newMessage);
            notifyItemInserted(messages.size() - 1);
        }

        scheduleVisibleFilePrefetch();
    }

    public void replaceLocalMessageWithServerMessage(
            @Nullable UUID localMessageId,
            @Nullable Message serverMessage
    ) {
        if (serverMessage == null) {
            return;
        }

        int serverPosition = getMessagePosition(serverMessage.getId());

        if (serverPosition != -1) {
            Message existingServerMessage = messages.get(serverPosition);
            transferLocalFileCache(existingServerMessage, serverMessage);
            messages.set(serverPosition, serverMessage);
            notifyItemChanged(serverPosition);
            scheduleVisibleFilePrefetch();
            return;
        }

        int localPosition = localMessageId != null ? getMessagePosition(localMessageId) : -1;

        if (localPosition != -1) {
            Message oldMessage = messages.get(localPosition);
            transferLocalFileCache(oldMessage, serverMessage);

            boolean oldMessageWasWaitingForAnimation = false;
            boolean oldMessageWasAlreadyAnimated = false;

            if (oldMessage != null && oldMessage.getId() != null) {
                oldMessageWasWaitingForAnimation = pendingAnimatedMessageIds.remove(oldMessage.getId());
                oldMessageWasAlreadyAnimated = alreadyAnimatedMessageIds.remove(oldMessage.getId());
                pendingAnimationRetryCounts.remove(oldMessage.getId());
                knownMessageIds.remove(oldMessage.getId());
            }

            messages.set(localPosition, serverMessage);

            if (serverMessage.getId() != null) {
                knownMessageIds.add(serverMessage.getId());
                pendingAnimationRetryCounts.remove(serverMessage.getId());

                if (oldMessageWasWaitingForAnimation && !oldMessageWasAlreadyAnimated) {
                    pendingAnimatedMessageIds.add(serverMessage.getId());
                }
            }

            notifyItemChanged(localPosition);
        } else {
            messages.add(serverMessage);
            markMessageForAnimationIfNeeded(serverMessage);
            notifyItemInserted(messages.size() - 1);
        }

        scheduleVisibleFilePrefetch();
    }

    private void transferLocalFileCache(
            @Nullable Message oldMessage,
            @NonNull Message serverMessage
    ) {
        if (oldMessage == null
                || oldMessage.getFiles() == null
                || serverMessage.getFiles() == null) {
            return;
        }

        int count = Math.min(oldMessage.getFiles().size(), serverMessage.getFiles().size());

        for (int i = 0; i < count; i++) {
            UUID oldFileId = oldMessage.getFiles().get(i);
            UUID serverFileId = serverMessage.getFiles().get(i);

            if (oldFileId == null || serverFileId == null || oldFileId.equals(serverFileId)) {
                continue;
            }

            com.example.aichat.model.entities.File localEntity = fileCache.get(oldFileId);

            if (!hasExistingLocalFile(localEntity)) {
                continue;
            }

            String mimeType = null;
            if (serverMessage.getFileMimeTypes() != null) {
                mimeType = serverMessage.getFileMimeTypes().get(serverFileId);
            }
            if ((mimeType == null || mimeType.trim().isEmpty()) && oldMessage.getFileMimeTypes() != null) {
                mimeType = oldMessage.getFileMimeTypes().get(oldFileId);
            }
            if (mimeType == null || mimeType.trim().isEmpty()) {
                mimeType = localEntity.mimeType;
            }

            FileType fileType = null;
            if (serverMessage.getFileTypes() != null) {
                fileType = serverMessage.getFileTypes().get(serverFileId);
            }
            if (fileType == null && oldMessage.getFileTypes() != null) {
                fileType = oldMessage.getFileTypes().get(oldFileId);
            }
            if (fileType == null) {
                fileType = parseFileType(localEntity.fileType);
            }

            String fileName = localFileNames.get(oldFileId);
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = localEntity.fileName;
            }
            if (fileName == null || fileName.trim().isEmpty()) {
                Map<UUID, String> parsedNames = extractFileNames(oldMessage.getText(), oldMessage.getFiles());
                fileName = parsedNames.get(oldFileId);
            }

            cacheLocalFile(
                    serverFileId,
                    localEntity.localPath,
                    mimeType,
                    fileType,
                    localEntity.size,
                    fileName
            );
        }
    }

    @Nullable
    private FileType parseFileType(@Nullable String rawFileType) {
        if (rawFileType == null || rawFileType.trim().isEmpty()) {
            return null;
        }

        try {
            return FileType.valueOf(rawFileType);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void setLocalFileName(UUID fileId, String name) {
        localFileNames.put(fileId, name);
        updateItemByFileId(fileId);
    }

    public void updateStatus(UUID messageId, UUID userId, MessageStatus status) {
        int position = getMessagePosition(messageId);

        if (position == -1) {
            return;
        }

        Message message = messages.get(position);

        if (message.getStatuses() != null && message.getStatuses().containsKey(userId)) {
            message.getStatuses().replace(userId, status);
            notifyItemChanged(position);
        }
    }

    public void removeMessage(UUID id) {
        int position = getMessagePosition(id);

        if (position != -1) {
            messages.remove(position);
            pendingAnimatedMessageIds.remove(id);
            alreadyAnimatedMessageIds.remove(id);
            pendingAnimationRetryCounts.remove(id);
            knownMessageIds.remove(id);
            notifyItemRemoved(position);
        }
    }

    public void highlightMessage(UUID id) {
        UUID previousHighlightedId = highlightedMessageId;
        highlightedMessageId = id;

        if (previousHighlightedId != null && !previousHighlightedId.equals(id)) {
            int previousPosition = getMessagePosition(previousHighlightedId);
            if (previousPosition != -1) {
                if (recyclerView != null) {
                    recyclerView.post(() -> notifyItemChanged(previousPosition));
                } else {
                    notifyItemChanged(previousPosition);
                }
            }
        }

        int position = getMessagePosition(id);

        if (position == -1) {
            return;
        }

        if (recyclerView != null) {
            recyclerView.post(() -> notifyItemChanged(position));
        } else {
            notifyItemChanged(position);
        }
    }

    public void scrollToMessage(UUID id) {
        int position = getMessagePosition(id);

        if (position != -1 && recyclerView != null) {
            recyclerView.smoothScrollToPosition(position);
        }
    }

    public int getMessagePosition(UUID id) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getId().equals(id)) {
                return i;
            }
        }

        return -1;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == 0 ? R.layout.my_message : R.layout.other_message;

        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);

        return new MessageViewHolder(view, viewType == 0);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MessageViewHolder holder,
            int position,
            @NonNull List<Object> payloads
    ) {
        if (!payloads.isEmpty()) {
            Object payload = payloads.get(0);

            if (payload instanceof UUID) {
                UUID fileId = (UUID) payload;

                holder.updateFileProgress(fileId);
                holder.updateAudioProgress(fileId);
                holder.updateAudioArtwork(fileId);
                holder.updateFileNameMarquee(fileId);

                ThemeMessageBinder.bind(holder.itemView);
                holder.applyPostThemeStabilization();

                if (position >= 0 && position < messages.size()) {
                    Message message = messages.get(position);

                    holder.applyMessageHighlight(
                            highlightedMessageId != null && highlightedMessageId.equals(message.getId())
                    );
                }

                return;
            }
        }

        onBindViewHolder(holder, position);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        if (position < 0 || position >= messages.size()) {
            return;
        }

        Message message = messages.get(position);

        holder.bind(message);

        ThemeMessageBinder.bind(holder.itemView);
        holder.applyPostThemeStabilization();

        holder.applyMessageHighlight(
                highlightedMessageId != null && highlightedMessageId.equals(message.getId())
        );

        applyPendingMessageAnimation(holder.itemView, message);
    }

    private static void clearItemHighlight(@Nullable View itemView) {
        if (itemView != null) {
            itemView.setForeground(null);
        }
    }

    private void applyPendingMessageAnimation(View itemView, Message message) {
        if (itemView == null || message == null || message.getId() == null) {
            return;
        }

        UUID messageId = message.getId();

        if (!pendingAnimatedMessageIds.contains(messageId)) {
            return;
        }

        if (alreadyAnimatedMessageIds.contains(messageId)) {
            pendingAnimatedMessageIds.remove(messageId);
            pendingAnimationRetryCounts.remove(messageId);
            return;
        }

        String animation = resolveCurrentMessageAnimation();

        if (ThemeMessageAnimationBinder.isDefault(animation)) {
            Log.d(
                    TAG_ANIMATION,
                    "animation is default now, retry later, messageId = " + messageId
            );

            retryPendingMessageAnimation(messageId);
            return;
        }

        if (!pendingAnimatedMessageIds.remove(messageId)) {
            return;
        }

        pendingAnimationRetryCounts.remove(messageId);
        alreadyAnimatedMessageIds.add(messageId);

        Log.d(TAG_ANIMATION, "apply animation = " + animation + ", messageId = " + messageId);

        View animationTarget = resolveMessageAnimationTarget(itemView);
        animationTarget.setTag(R.id.tag_message_animation_id, messageId);

        animationTarget.postDelayed(() -> {
            Object boundMessageId = animationTarget.getTag(R.id.tag_message_animation_id);

            if (!messageId.equals(boundMessageId)) {
                Log.d(TAG_ANIMATION, "skip recycled animation target, messageId = " + messageId);
                return;
            }

            ThemeMessageAnimationBinder.apply(animationTarget, animation);
        }, 48L);
    }

    private void retryPendingMessageAnimation(@NonNull UUID messageId) {
        Integer previousRetryCount = pendingAnimationRetryCounts.get(messageId);
        int retryCount = previousRetryCount == null ? 1 : previousRetryCount + 1;

        pendingAnimationRetryCounts.put(messageId, retryCount);

        if (retryCount > MESSAGE_ANIMATION_MAX_RETRY_COUNT) {
            pendingAnimatedMessageIds.remove(messageId);
            pendingAnimationRetryCounts.remove(messageId);

            Log.d(
                    TAG_ANIMATION,
                    "animation retry limit reached, drop pending, messageId = " + messageId
            );

            return;
        }

        handler.postDelayed(() -> {
            if (!pendingAnimatedMessageIds.contains(messageId)) {
                return;
            }

            int position = getMessagePosition(messageId);

            if (position == -1) {
                pendingAnimatedMessageIds.remove(messageId);
                pendingAnimationRetryCounts.remove(messageId);
                return;
            }

            notifyItemChanged(position);
        }, MESSAGE_ANIMATION_RETRY_DELAY_MS);
    }

    private View resolveMessageAnimationTarget(@NonNull View itemView) {
        View messageTextView = itemView.findViewById(R.id.message_text);

        if (isVisibleView(messageTextView)) {
            View parent = null;

            if (messageTextView.getParent() instanceof View) {
                parent = (View) messageTextView.getParent();
            }

            if (isVisibleView(parent)
                    && parent != itemView) {
                return parent;
            }

            return messageTextView;
        }

        View audioContainer = itemView.findViewById(R.id.audio_container);

        if (isVisibleView(audioContainer)) {
            return audioContainer;
        }

        View videoCircleCard = itemView.findViewById(R.id.video_circle_card);

        if (isVisibleView(videoCircleCard)) {
            return videoCircleCard;
        }

        View filesContainer = itemView.findViewById(R.id.files_container);

        if (isVisibleView(filesContainer)) {
            return filesContainer;
        }

        View youtubePreviewRoot = itemView.findViewById(R.id.youtubePreviewRoot);

        if (isVisibleView(youtubePreviewRoot)) {
            return youtubePreviewRoot;
        }

        return itemView;
    }

    private boolean isVisibleView(@Nullable View view) {
        return view != null && view.getVisibility() == View.VISIBLE;
    }

    private String resolveCurrentMessageAnimation() {
        String selectedThemeId = resolveSelectedThemeIdForAnimation();

        String savedAnimation = loadSavedAnimationForThemeId(selectedThemeId);

        if (!ThemeMessageAnimationBinder.isDefault(savedAnimation)) {
            Log.d(TAG_ANIMATION, "resolved from SharedPreferences = " + savedAnimation);
            return savedAnimation;
        }

        ThemeModel runtimeTheme = ThemeAttrResolver.getCurrentTheme();

        if (runtimeTheme != null) {
            String runtimeAnimation = ThemeMessageAnimationBinder.normalize(
                    runtimeTheme.getMessageAnimation()
            );

            if (!ThemeMessageAnimationBinder.isDefault(runtimeAnimation)) {
                Log.d(TAG_ANIMATION, "resolved from runtime ThemeAttrResolver = " + runtimeAnimation);
                return runtimeAnimation;
            }

            String runtimeThemeId = runtimeTheme.getId() != null
                    ? runtimeTheme.getId().toString()
                    : null;

            String runtimeSavedAnimation = loadSavedAnimationForThemeId(runtimeThemeId);

            if (!ThemeMessageAnimationBinder.isDefault(runtimeSavedAnimation)) {
                Log.d(TAG_ANIMATION, "resolved from runtime theme id preferences = " + runtimeSavedAnimation);
                return runtimeSavedAnimation;
            }
        }

        ThemeModel selectedTheme = themeStorage.getSelectedTheme();

        if (selectedTheme != null) {
            String selectedModelAnimation = ThemeMessageAnimationBinder.normalize(
                    selectedTheme.getMessageAnimation()
            );

            if (!ThemeMessageAnimationBinder.isDefault(selectedModelAnimation)) {
                Log.d(TAG_ANIMATION, "resolved from ThemeModel = " + selectedModelAnimation);
                return selectedModelAnimation;
            }

            String selectedStorageAnimation = loadSavedAnimationForThemeId(
                    selectedTheme.getId() != null ? selectedTheme.getId().toString() : null
            );

            if (!ThemeMessageAnimationBinder.isDefault(selectedStorageAnimation)) {
                Log.d(TAG_ANIMATION, "resolved from selected ThemeModel preferences = " + selectedStorageAnimation);
                return selectedStorageAnimation;
            }
        }

        Log.d(TAG_ANIMATION, "resolved default");

        return ThemeMessageAnimationBinder.ANIMATION_DEFAULT;
    }

    private String loadSavedAnimationForThemeId(@Nullable String themeId) {
        if (themeId == null || themeId.trim().isEmpty()) {
            return ThemeMessageAnimationBinder.ANIMATION_DEFAULT;
        }

        String key = THEME_ANIMATION_PREFIX + themeId.trim();

        SharedPreferences animationPreferences = appContext.getSharedPreferences(
                THEME_ANIMATION_PREFS,
                Context.MODE_PRIVATE
        );

        String animation = animationPreferences.getString(key, null);

        if (animation == null || animation.trim().isEmpty()) {

            SharedPreferences legacyPreferences = appContext.getSharedPreferences(
                    THEME_STORAGE_PREFS,
                    Context.MODE_PRIVATE
            );

            animation = legacyPreferences.getString(
                    key,
                    ThemeMessageAnimationBinder.ANIMATION_DEFAULT
            );
        }

        return ThemeMessageAnimationBinder.normalize(animation);
    }

    @Nullable
    private String resolveSelectedThemeIdForAnimation() {
        ThemeModel runtimeTheme = ThemeAttrResolver.getCurrentTheme();

        if (runtimeTheme != null && runtimeTheme.getId() != null) {
            return runtimeTheme.getId().toString();
        }

        String selectedThemeId = ThemeSelectionCoordinator.getSelectedCustomThemeId(appContext);

        if (isNonEmptyThemeId(selectedThemeId)) {
            return selectedThemeId.trim();
        }

        String currentThemeMode = appContext.getSharedPreferences(
                ThemeSelectionCoordinator.SETTINGS_PREFS,
                Context.MODE_PRIVATE
        ).getString(
                ThemeSelectionCoordinator.KEY_APP_THEME,
                ThemeSelectionCoordinator.THEME_SYSTEM
        );

        if (currentThemeMode != null
                && !currentThemeMode.trim().isEmpty()
                && !ThemeSelectionCoordinator.isDefaultThemeMode(currentThemeMode)) {
            return currentThemeMode.trim();
        }

        ThemeModel selectedTheme = themeStorage.getSelectedTheme();

        if (selectedTheme != null && selectedTheme.getId() != null) {
            return selectedTheme.getId().toString();
        }

        return null;
    }

    private boolean isNonEmptyThemeId(@Nullable String themeId) {
        return themeId != null && !themeId.trim().isEmpty();
    }

    private boolean isExistingThemeId(@Nullable String themeId) {
        if (themeId == null || themeId.trim().isEmpty()) {
            return false;
        }

        return themeStorage.getThemeById(themeId.trim()) != null;
    }

    private String removeFileTags(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return text.replaceAll("\\[file:.*?]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String removePreviewUrls(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        return URL_PREVIEW_PATTERN.matcher(text)
                .replaceAll("")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String removeYouTubeUrls(String text) {
        return removePreviewUrls(text);
    }

    private LinkPreview findLinkPreview(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        java.util.regex.Matcher matcher = URL_PREVIEW_PATTERN.matcher(text);

        while (matcher.find()) {
            String rawUrl = matcher.group(1);

            if (rawUrl == null || rawUrl.trim().isEmpty()) {
                continue;
            }

            String normalizedUrl = normalizePreviewUrl(rawUrl);

            if (normalizedUrl == null || normalizedUrl.trim().isEmpty()) {
                continue;
            }

            String videoId = extractYouTubeVideoId(normalizedUrl);

            if (videoId != null && !videoId.trim().isEmpty()) {
                return LinkPreview.youtube(normalizedUrl, videoId);
            }

            return LinkPreview.generic(normalizedUrl, getHostTitle(normalizedUrl));
        }

        return null;
    }

    private YouTubePreview findYouTubePreview(String text) {
        LinkPreview preview = findLinkPreview(text);

        if (preview == null || !preview.youtube || preview.videoId == null) {
            return null;
        }

        return new YouTubePreview(preview.originalUrl, preview.videoId);
    }

    private String normalizePreviewUrl(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }

        String url = rawUrl.trim();

        while (url.endsWith(".")
                || url.endsWith(",")
                || url.endsWith(")")
                || url.endsWith("]")
                || url.endsWith("!")
                || url.endsWith("?")
                || url.endsWith(";")
                || url.endsWith(":")) {
            url = url.substring(0, url.length() - 1);
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        return url;
    }

    private String normalizeYouTubeUrl(String rawUrl) {
        return normalizePreviewUrl(rawUrl);
    }

    private String extractYouTubeVideoId(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();

            if (host == null) {
                return null;
            }

            host = host.toLowerCase(Locale.US);

            if (host.contains("youtu.be")) {
                List<String> segments = uri.getPathSegments();

                if (segments != null && !segments.isEmpty()) {
                    return sanitizeYouTubeVideoId(segments.get(0));
                }
            }

            if (host.contains("youtube.com")) {
                String watchId = uri.getQueryParameter("v");

                if (watchId != null && !watchId.trim().isEmpty()) {
                    return sanitizeYouTubeVideoId(watchId);
                }

                List<String> segments = uri.getPathSegments();

                if (segments == null || segments.size() < 2) {
                    return null;
                }

                String firstSegment = segments.get(0);

                if ("shorts".equalsIgnoreCase(firstSegment)
                        || "embed".equalsIgnoreCase(firstSegment)
                        || "live".equalsIgnoreCase(firstSegment)) {
                    return sanitizeYouTubeVideoId(segments.get(1));
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private String sanitizeYouTubeVideoId(String rawId) {
        if (rawId == null) {
            return null;
        }

        String id = rawId.trim();

        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile("^[A-Za-z0-9_-]{6,}$");

        if (!pattern.matcher(id).matches()) {
            return null;
        }

        return id;
    }

    private void loadYouTubeTitle(YouTubePreview preview, TextView titleView) {
        if (preview == null) {
            return;
        }

        loadLinkPreviewMetadata(
                LinkPreview.youtube(preview.originalUrl, preview.videoId),
                titleView,
                null,
                null
        );
    }

    private void loadLinkPreviewMetadata(
            LinkPreview preview,
            @Nullable TextView titleView,
            @Nullable TextView subtitleView,
            @Nullable ImageView thumbnailView
    ) {
        if (preview == null) {
            return;
        }

        LinkPreviewMetadata cached = linkPreviewCache.get(preview.cacheKey);
        if (cached != null) {
            applyLinkPreviewMetadata(preview, cached, titleView, subtitleView, thumbnailView);
            return;
        }

        if (!loadingLinkPreviews.add(preview.cacheKey)) {
            return;
        }

        new Thread(() -> {
            LinkPreviewMetadata metadata = fetchLinkPreviewMetadata(preview);

            loadingLinkPreviews.remove(preview.cacheKey);

            if (metadata == null) {
                metadata = new LinkPreviewMetadata(
                        preview.fallbackTitle,
                        preview.originalUrl,
                        preview.thumbnailUrl
                );
            }

            linkPreviewCache.put(preview.cacheKey, metadata);
            LinkPreviewMetadata finalMetadata = metadata;

            handler.post(() ->
                    applyLinkPreviewMetadata(
                            preview,
                            finalMetadata,
                            titleView,
                            subtitleView,
                            thumbnailView
                    )
            );
        }, "link-preview-" + preview.cacheKey.hashCode()).start();
    }

    private void applyLinkPreviewMetadata(
            LinkPreview preview,
            LinkPreviewMetadata metadata,
            @Nullable TextView titleView,
            @Nullable TextView subtitleView,
            @Nullable ImageView thumbnailView
    ) {
        if (preview == null || metadata == null) {
            return;
        }

        if (titleView != null) {
            Object tag = titleView.getTag();
            if (preview.cacheKey.equals(tag)) {
                String title = firstNotBlank(metadata.title, preview.fallbackTitle, "Ссылка");
                titleView.setText(title);
                titleView.setMinLines(2);
                titleView.setMaxLines(2);
            }
        }

        if (subtitleView != null) {
            Object tag = subtitleView.getTag();
            if (preview.cacheKey.equals(tag)) {
                subtitleView.setText(firstNotBlank(metadata.siteName, preview.originalUrl));
            }
        }

        if (thumbnailView != null) {
            Object tag = thumbnailView.getTag();
            if (preview.cacheKey.equals(tag)) {
                String imageUrl = firstNotBlank(metadata.imageUrl, preview.thumbnailUrl);

                Glide.with(thumbnailView).clear(thumbnailView);

                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    Glide.with(thumbnailView)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_video)
                            .error(R.drawable.ic_video)
                            .centerCrop()
                            .into(thumbnailView);
                } else {
                    thumbnailView.setImageResource(R.drawable.ic_video);
                }
            }
        }
    }

    private LinkPreviewMetadata fetchLinkPreviewMetadata(LinkPreview preview) {
        if (preview == null) {
            return null;
        }

        if (preview.youtube) {
            String title = fetchYouTubeTitle(preview.originalUrl);
            return new LinkPreviewMetadata(
                    firstNotBlank(title, preview.fallbackTitle),
                    "YouTube",
                    preview.thumbnailUrl
            );
        }

        HttpURLConnection connection = null;

        try {
            URL url = new URL(preview.originalUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 AIChat Link Preview");
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 400) {
                return null;
            }

            String contentType = connection.getContentType();
            if (contentType != null && !contentType.toLowerCase(Locale.US).contains("text/html")) {
                return new LinkPreviewMetadata(
                        preview.fallbackTitle,
                        getHostTitle(preview.originalUrl),
                        null
                );
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
            );

            StringBuilder htmlBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null && htmlBuilder.length() < 160_000) {
                htmlBuilder.append(line).append('\n');
            }

            String html = htmlBuilder.toString();
            String title = firstNotBlank(
                    extractMetaContent(html, "property", "og:title"),
                    extractMetaContent(html, "name", "twitter:title"),
                    extractTitleTag(html),
                    preview.fallbackTitle
            );

            String siteName = firstNotBlank(
                    extractMetaContent(html, "property", "og:site_name"),
                    getHostTitle(preview.originalUrl),
                    preview.originalUrl
            );

            String imageUrl = firstNotBlank(
                    extractMetaContent(html, "property", "og:image"),
                    extractMetaContent(html, "name", "twitter:image"),
                    extractMetaContent(html, "property", "twitter:image")
            );

            imageUrl = makeAbsoluteUrl(preview.originalUrl, imageUrl);

            return new LinkPreviewMetadata(
                    htmlDecode(title),
                    htmlDecode(siteName),
                    imageUrl
            );
        } catch (Exception exception) {
            Log.d(TAG, "Cannot fetch link preview for " + preview.originalUrl, exception);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String fetchYouTubeTitle(String videoUrl) {
        HttpURLConnection connection = null;

        try {
            String encodedUrl = URLEncoder.encode(videoUrl, StandardCharsets.UTF_8);
            String oEmbedUrl = "https://www.youtube.com/oembed?format=json&url=" + encodedUrl;

            URL url = new URL(oEmbedUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode < 200 || responseCode >= 300) {
                return null;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
            );

            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            JSONObject json = new JSONObject(builder.toString());
            return json.optString("title", null);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Nullable
    private String extractTitleTag(@Nullable String html) {
        if (html == null || html.isEmpty()) {
            return null;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?is)<title[^>]*>(.*?)</title>")
                .matcher(html);

        if (!matcher.find()) {
            return null;
        }

        return cleanHtmlText(matcher.group(1));
    }

    @Nullable
    private String extractMetaContent(
            @Nullable String html,
            @NonNull String attributeName,
            @NonNull String attributeValue
    ) {
        if (html == null || html.isEmpty()) {
            return null;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?is)<meta\\s+[^>]*(?:" + attributeName + "|property|name)\\s*=\\s*['\\\"]"
                        + java.util.regex.Pattern.quote(attributeValue)
                        + "['\\\"][^>]*>"
        );

        java.util.regex.Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            String tag = matcher.group(0);
            String content = extractAttribute(tag, "content");

            if (content != null && !content.trim().isEmpty()) {
                return cleanHtmlText(content);
            }
        }

        java.util.regex.Pattern reversedPattern = java.util.regex.Pattern.compile(
                "(?is)<meta\\s+[^>]*content\\s*=\\s*['\\\"][^'\\\"]+['\\\"][^>]*(?:"
                        + attributeName
                        + "|property|name)\\s*=\\s*['\\\"]"
                        + java.util.regex.Pattern.quote(attributeValue)
                        + "['\\\"][^>]*>"
        );

        matcher = reversedPattern.matcher(html);

        while (matcher.find()) {
            String tag = matcher.group(0);
            String content = extractAttribute(tag, "content");

            if (content != null && !content.trim().isEmpty()) {
                return cleanHtmlText(content);
            }
        }

        return null;
    }

    @Nullable
    private String extractAttribute(@Nullable String tag, @NonNull String attributeName) {
        if (tag == null || tag.isEmpty()) {
            return null;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?is)" + java.util.regex.Pattern.quote(attributeName) + "\\s*=\\s*(['\\\"])(.*?)\\1"
        );

        java.util.regex.Matcher matcher = pattern.matcher(tag);

        if (!matcher.find()) {
            return null;
        }

        return matcher.group(2);
    }

    private String cleanHtmlText(@Nullable String text) {
        if (text == null) {
            return null;
        }

        return text.replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String htmlDecode(@Nullable String text) {
        if (text == null) {
            return null;
        }

        return text.replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim();
    }

    @Nullable
    private String makeAbsoluteUrl(@NonNull String baseUrl, @Nullable String candidate) {
        if (candidate == null || candidate.trim().isEmpty()) {
            return candidate;
        }

        String value = candidate.trim();

        try {
            URL base = new URL(baseUrl);
            return new URL(base, value).toString();
        } catch (Exception ignored) {
            return value;
        }
    }

    private String getHostTitle(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return "Ссылка";
        }

        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();

            if (host == null || host.trim().isEmpty()) {
                return "Ссылка";
            }

            if (host.toLowerCase(Locale.US).startsWith("www.")) {
                host = host.substring(4);
            }

            return host;
        } catch (Exception ignored) {
            return "Ссылка";
        }
    }

    @Nullable
    private String firstNotBlank(@Nullable String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }

        return null;
    }

    public boolean isMyMessage(Message message) {
        return messageController.isMyMessage(message);
    }

    public int getUnreadMessagesCount(UUID currentUserId) {
        if (currentUserId == null) {
            return 0;
        }

        int count = 0;

        for (Message message : messages) {
            if (isUnreadForCurrentUser(message, currentUserId)) {
                count++;
            }
        }

        return count;
    }

    public int getFirstUnreadMessagePosition(UUID currentUserId) {
        if (currentUserId == null) {
            return -1;
        }

        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);

            if (isUnreadForCurrentUser(message, currentUserId)) {
                return i;
            }
        }

        return -1;
    }

    public int getLastUnreadMessagePosition(UUID currentUserId) {
        if (currentUserId == null) {
            return -1;
        }

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);

            if (isUnreadForCurrentUser(message, currentUserId)) {
                return i;
            }
        }

        return -1;
    }

    public boolean isUnreadForCurrentUser(Message message, UUID currentUserId) {
        if (message == null || currentUserId == null) {
            return false;
        }

        if (messageController.isMyMessage(message)) {
            return false;
        }

        Map<UUID, MessageStatus> statuses = message.getStatuses();

        return statuses != null
                && statuses.containsKey(currentUserId)
                && statuses.get(currentUserId) != MessageStatus.READ;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private void preloadDownloadedFiles() {
        new Thread(() -> {
            List<com.example.aichat.model.entities.File> files =
                    DatabaseManager.getDatabase().fileDao().getAll();

            if (files == null) {
                return;
            }

            for (com.example.aichat.model.entities.File file : files) {
                if (!hasLocalPath(file)) {
                    continue;
                }

                File localFile = new File(file.localPath);

                if (!localFile.exists()) {
                    DatabaseManager.getDatabase().fileDao().delete(file.fileId);
                    continue;
                }

                fileCache.put(file.fileId, file);
                warmLocalMediaDuration(file.fileId, file);
                warmLocalAudioArtwork(file.fileId, file);
            }

            handler.post(this::notifyAllVisibleItemsChanged);
        }).start();
    }

    private void loadFileFromDb(UUID fileId) {
        if (fileCache.containsKey(fileId)) {
            return;
        }

        if (Boolean.TRUE.equals(loadingFiles.get(fileId))) {
            return;
        }

        loadingFiles.put(fileId, true);

        new Thread(() -> {
            com.example.aichat.model.entities.File entity =
                    DatabaseManager.getDatabase().fileDao().getById(fileId);

            loadingFiles.remove(fileId);

            if (!hasLocalPath(entity)) {
                return;
            }

            File localFile = new File(entity.localPath);

            if (!localFile.exists()) {
                DatabaseManager.getDatabase().fileDao().delete(fileId);
                return;
            }

            fileCache.put(fileId, entity);
            warmLocalMediaDuration(fileId, entity);
            warmLocalAudioArtwork(fileId, entity);

            int position = findMessagePositionByFileId(fileId);

            if (position != -1) {
                handler.post(() -> notifyFileChanged(fileId));
            }
        }).start();
    }

    private void notifyAllVisibleItemsChanged() {
        int count = messages.size();

        if (count > 0) {
            notifyItemRangeChanged(0, count);
        }
    }

    private static boolean hasLocalPath(com.example.aichat.model.entities.File entity) {
        return entity != null && !TextUtils.isEmpty(entity.localPath);
    }

    private static boolean hasExistingLocalFile(com.example.aichat.model.entities.File entity) {
        return hasLocalPath(entity) && new File(entity.localPath).exists();
    }

    private static String formatPercent(int progress) {
        return String.format(Locale.US, "%d%%", progress);
    }

    private static boolean isValidDrawableResourceId(int resId) {
        return resId != 0 && (resId & 0xFF000000) == 0x7F000000;
    }

    private static boolean isDarkTheme(@NonNull Context context) {
        int nightModeFlags = context.getResources()
                .getConfiguration()
                .uiMode & Configuration.UI_MODE_NIGHT_MASK;

        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private static int resolveThemeColor(View view, @AttrRes int attr, int fallback) {
        if (view == null || view.getContext() == null) {
            return fallback;
        }

        try {
            return ThemeAttrResolver.resolveColor(
                    view.getContext(),
                    attr
            );
        } catch (Exception ignored) {
            TypedValue typedValue =
                    new TypedValue();

            boolean resolved =
                    view.getContext()
                            .getTheme()
                            .resolveAttribute(
                                    attr,
                                    typedValue,
                                    true
                            );

            if (!resolved) {
                return fallback;
            }

            return typedValue.data;
        }
    }

    private static class LinkPreview {
        final String originalUrl;
        final String cacheKey;
        final String fallbackTitle;
        final String thumbnailUrl;
        final boolean youtube;
        final String videoId;

        private LinkPreview(
                String originalUrl,
                String cacheKey,
                String fallbackTitle,
                String thumbnailUrl,
                boolean youtube,
                String videoId
        ) {
            this.originalUrl = originalUrl;
            this.cacheKey = cacheKey;
            this.fallbackTitle = fallbackTitle;
            this.thumbnailUrl = thumbnailUrl;
            this.youtube = youtube;
            this.videoId = videoId;
        }

        static LinkPreview youtube(String originalUrl, String videoId) {
            return new LinkPreview(
                    originalUrl,
                    "youtube:" + videoId,
                    "YouTube video",
                    "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg",
                    true,
                    videoId
            );
        }

        static LinkPreview generic(String originalUrl, String fallbackTitle) {
            return new LinkPreview(
                    originalUrl,
                    "url:" + originalUrl,
                    fallbackTitle != null ? fallbackTitle : "Ссылка",
                    null,
                    false,
                    null
            );
        }
    }

    private static class LinkPreviewMetadata {
        final String title;
        final String siteName;
        final String imageUrl;

        LinkPreviewMetadata(String title, String siteName, String imageUrl) {
            this.title = title;
            this.siteName = siteName;
            this.imageUrl = imageUrl;
        }
    }

    private static class YouTubePreview {
        final String originalUrl;
        final String videoId;
        final String thumbnailUrl;

        YouTubePreview(String originalUrl, String videoId) {
            this.originalUrl = originalUrl;
            this.videoId = videoId;
            this.thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
        }
    }

    private static class FileNameMarqueeState {
        boolean running;
        boolean firstRun = true;
    }

    private static void resetFileNameMarquee(TextView view) {
        if (view == null) {
            return;
        }

        Object tag = view.getTag(FILE_NAME_SCROLL_TAG);

        if (tag instanceof FileNameMarqueeState) {
            ((FileNameMarqueeState) tag).running = false;
        }

        view.animate().cancel();
        view.setTag(FILE_NAME_SCROLL_TAG, null);
        view.setTranslationX(0f);
        view.setAlpha(1f);
        view.setSingleLine(true);
        view.setHorizontallyScrolling(false);
        view.setEllipsize(TextUtils.TruncateAt.END);

        ViewGroup.LayoutParams params = view.getLayoutParams();

        if (params != null) {
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            view.setLayoutParams(params);
        }
    }

    private static void startFileNameMarquee(TextView view) {
        if (view == null) {
            return;
        }

        Object tag = view.getTag(FILE_NAME_SCROLL_TAG);

        if (tag instanceof FileNameMarqueeState) {
            FileNameMarqueeState oldState = (FileNameMarqueeState) tag;

            if (oldState.running) {
                return;
            }
        }

        FileNameMarqueeState state = new FileNameMarqueeState();
        state.running = true;
        state.firstRun = true;
        view.setTag(FILE_NAME_SCROLL_TAG, state);

        view.post(() -> runFileNameMarquee(view, state));
    }

    private static void runFileNameMarquee(TextView view, FileNameMarqueeState state) {
        if (view == null || state == null || !state.running) {
            return;
        }

        View parent = (View) view.getParent();

        if (parent == null) {
            return;
        }

        int parentWidth = parent.getWidth();

        if (parentWidth <= 0) {
            view.postDelayed(() -> runFileNameMarquee(view, state), 100);
            return;
        }

        String text = view.getText() != null ? view.getText().toString() : "";

        int textWidth = (int) view.getPaint().measureText(text)
                + view.getPaddingLeft()
                + view.getPaddingRight()
                + dp(view, 8);

        if (textWidth <= parentWidth) {
            resetFileNameMarquee(view);
            return;
        }

        ViewGroup.LayoutParams params = view.getLayoutParams();

        if (params != null && params.width != textWidth) {
            params.width = textWidth;
            view.setLayoutParams(params);
        }

        view.animate().cancel();
        view.setSingleLine(true);
        view.setHorizontallyScrolling(true);
        view.setEllipsize(null);

        int gap = dp(view, 28);
        float startX = state.firstRun ? 0f : parentWidth + gap;
        float endX = -(textWidth + gap);
        float distance = Math.abs(startX - endX);
        long duration = Math.max(4500L, (long) (distance * 22L));

        view.setTranslationX(startX);

        view.animate()
                .translationX(endX)
                .setDuration(duration)
                .setStartDelay(state.firstRun ? 600L : 250L)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> {
                    if (!state.running) {
                        return;
                    }

                    state.firstRun = false;
                    view.postDelayed(() -> runFileNameMarquee(view, state), 150);
                })
                .start();
    }

    private static void stopFileNameMarquee(TextView view) {
        if (view == null) {
            return;
        }

        Object tag = view.getTag(FILE_NAME_SCROLL_TAG);

        boolean wasRunning = tag instanceof FileNameMarqueeState
                && ((FileNameMarqueeState) tag).running;

        if (tag instanceof FileNameMarqueeState) {
            ((FileNameMarqueeState) tag).running = false;
        }

        view.animate().cancel();

        if (!wasRunning) {
            resetFileNameMarquee(view);
            return;
        }

        view.animate()
                .translationX(0f)
                .setDuration(220L)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> resetFileNameMarquee(view))
                .start();
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }


    private static final class VisibleFileDownloadRequest {
        final String url;
        final String mimeType;
        final UUID fileId;
        final FileType fileType;

        VisibleFileDownloadRequest(
                @NonNull String url,
                @Nullable String mimeType,
                @NonNull UUID fileId,
                @Nullable FileType fileType
        ) {
            this.url = url;
            this.mimeType = mimeType;
            this.fileId = fileId;
            this.fileType = fileType;
        }
    }

    private void prefetchVisibleFilesSafely() {
        try {
            prefetchVisibleMissingFiles();
        } catch (Exception exception) {
            Log.e(TAG, "Visible file prefetch failed", exception);
        }
    }

    private void prefetchVisibleMissingFiles() {
        if (recyclerView == null || fileDownloadListener == null || messages.isEmpty()) {
            return;
        }

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) {
            return;
        }

        LinearLayoutManager manager = (LinearLayoutManager) layoutManager;
        int first = manager.findFirstVisibleItemPosition();
        int last = manager.findLastVisibleItemPosition();

        if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) {
            return;
        }

        int enqueued = 0;
        for (int i = Math.max(0, first); i <= last && i < messages.size(); i++) {
            Message message = messages.get(i);
            if (message == null || message.getFiles() == null || message.getFiles().isEmpty()) {
                continue;
            }

            if (messageController.isMyMessage(message)) {
                continue;
            }

            Map<UUID, FileType> fileTypes = message.getFileTypes();
            Map<UUID, String> mimeTypes = message.getFileMimeTypes();
            Map<UUID, String> parsedNames = extractFileNames(message.getText(), message.getFiles());

            for (UUID fileId : message.getFiles()) {
                if (fileId == null || shouldSkipVisibleFilePrefetch(fileId)) {
                    continue;
                }

                FileType fileType = fileTypes != null ? fileTypes.get(fileId) : null;
                String mimeType = mimeTypes != null ? mimeTypes.get(fileId) : null;
                String name = parsedNames.get(fileId);
                if (name == null) {
                    name = resolveFileName(fileId, fileCache.get(fileId), "/api/files/" + fileId);
                }

                enqueueVisibleDownload(new VisibleFileDownloadRequest(
                        "/api/files/" + fileId,
                        mimeType,
                        fileId,
                        fileType
                ));

                enqueued++;
                if (enqueued >= MAX_VISIBLE_PREFETCH_PER_PASS) {
                    startNextVisibleDownload();
                    handler.postDelayed(visiblePrefetchRunnable, VISIBLE_PREFETCH_RETRY_MS);
                    return;
                }
            }
        }

        startNextVisibleDownload();
    }

    private boolean shouldSkipVisibleFilePrefetch(@NonNull UUID fileId) {
        com.example.aichat.model.entities.File entity = fileCache.get(fileId);
        if (hasExistingLocalFile(entity)) {
            return true;
        }

        if (queuedVisibleDownloads.contains(fileId) || runningVisibleDownloads.contains(fileId)) {
            return true;
        }

        return isUploadInProgress(fileId);
    }

    private boolean isUploadInProgress(@NonNull UUID fileId) {
        if (uploadProgressManager == null) {
            return false;
        }

        Integer progress = uploadProgressManager.getProgress(fileId);
        return progress != null && progress >= 0 && progress < 100;
    }

    private void enqueueVisibleDownload(@NonNull VisibleFileDownloadRequest request) {
        if (!queuedVisibleDownloads.add(request.fileId)) {
            return;
        }
        visibleDownloadQueue.offer(request);
    }

    private void startNextVisibleDownload() {
        if (fileDownloadListener == null) {
            visibleDownloadQueue.clear();
            queuedVisibleDownloads.clear();
            runningVisibleDownloads.clear();
            runningVisibleDownloadStartedAt.clear();
            return;
        }

        releaseStaleVisibleDownloads();

        while (runningVisibleDownloads.size() < MAX_PARALLEL_VISIBLE_DOWNLOADS) {
            VisibleFileDownloadRequest request = visibleDownloadQueue.poll();
            if (request == null) {
                return;
            }

            queuedVisibleDownloads.remove(request.fileId);

            if (shouldSkipVisibleFilePrefetch(request.fileId)) {
                continue;
            }

            runningVisibleDownloads.add(request.fileId);
            runningVisibleDownloadStartedAt.put(request.fileId, System.currentTimeMillis());

            if (progressManager != null && progressManager.getProgress(request.fileId) == null) {
                progressManager.updateProgress(request.fileId, 0);
            }

            try {
                fileDownloadListener.onFileDownloadRequired(
                        request.url,
                        request.mimeType,
                        request.fileId,
                        request.fileType,
                        false
                );
            } catch (Exception exception) {
                Log.e(TAG, "Cannot start visible file prefetch", exception);
                queuedVisibleDownloads.remove(request.fileId);
                runningVisibleDownloads.remove(request.fileId);
                runningVisibleDownloadStartedAt.remove(request.fileId);
            }
        }

        handler.postDelayed(this::startNextVisibleDownload, VISIBLE_PREFETCH_RETRY_MS);
    }

    private void releaseStaleVisibleDownloads() {
        if (runningVisibleDownloads.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        List<UUID> staleFileIds = new ArrayList<>();

        for (UUID fileId : runningVisibleDownloads) {
            Long startedAt = runningVisibleDownloadStartedAt.get(fileId);
            if (startedAt == null) {
                staleFileIds.add(fileId);
                continue;
            }

            if (now - startedAt < VISIBLE_DOWNLOAD_STALE_MS) {
                continue;
            }

            Integer progress = progressManager != null ? progressManager.getProgress(fileId) : null;
            if (progress == null || progress <= 0 || progress >= 100) {
                staleFileIds.add(fileId);
            }
        }

        for (UUID fileId : staleFileIds) {
            runningVisibleDownloads.remove(fileId);
            runningVisibleDownloadStartedAt.remove(fileId);
        }
    }

    private void maybePrefetchVisibleBoundFile(
            @Nullable UUID fileId,
            @NonNull String url,
            @Nullable String mimeType,
            @Nullable FileType fileType,
            @Nullable String fileName,
            boolean isDownloaded,
            boolean ownerIsCurrentUser
    ) {
        if (fileId == null || isDownloaded || fileDownloadListener == null) {
            return;
        }

        boolean audioFile = isLikelyAudioFile(fileType, mimeType, fileName);
        boolean videoFile = fileType == FileType.VideoMessage
                || (mimeType != null && mimeType.toLowerCase(Locale.US).startsWith("video/"))
                || looksLikeVideoFile(fileName);
        boolean imageFile = fileType == FileType.MessageImage
                || (mimeType != null && mimeType.toLowerCase(Locale.US).startsWith("image/"))
                || looksLikeImageFile(fileName);

        if (ownerIsCurrentUser
                && !audioFile
                && !videoFile
                && !imageFile) {
            return;
        }

        if (!isLikelyPreviewOrPlayableFile(fileType, mimeType, fileName)) {
            return;
        }

        if (shouldSkipVisibleFilePrefetch(fileId)) {
            return;
        }

        enqueueVisibleDownload(new VisibleFileDownloadRequest(
                url,
                mimeType,
                fileId,
                fileType
        ));

        handler.post(this::startNextVisibleDownload);
    }

    private boolean isLikelyPreviewOrPlayableFile(
            @Nullable FileType fileType,
            @Nullable String mimeType,
            @Nullable String fileName
    ) {

        if (fileType == FileType.MessageImage
                || fileType == FileType.MessageFile
                || fileType == FileType.VoiceMessage
                || fileType == FileType.VideoMessage) {
            return true;
        }

        if (mimeType != null && !mimeType.trim().isEmpty()) {
            String lowerMime = mimeType.toLowerCase(Locale.US);
            return lowerMime.startsWith("image/")
                    || lowerMime.startsWith("video/")
                    || lowerMime.startsWith("audio/")
                    || lowerMime.startsWith("text/")
                    || lowerMime.startsWith("application/");
        }

        return fileName != null && !fileName.trim().isEmpty();
    }

    private boolean isLikelyAudioFile(
            @Nullable FileType fileType,
            @Nullable String mimeType,
            @Nullable String fileName
    ) {
        if (fileType == FileType.VoiceMessage) {
            return true;
        }

        if (mimeType != null && mimeType.toLowerCase(Locale.US).startsWith("audio")) {
            return true;
        }

        return looksLikeAudioFile(fileName);
    }



    private void prepareAudioQueueForPlayback(@NonNull Context context, @NonNull UUID currentFileId) {
        if (audioPlayerManager == null) {
            return;
        }

        boolean currentIsVoice = isVoiceAudioFile(currentFileId);
        List<AudioPlayerManager.AudioQueueItem> queueItems = new ArrayList<>();

        for (Message message : messages) {
            if (message == null || message.getFiles() == null || message.getFiles().isEmpty()) {
                continue;
            }

            Map<UUID, FileType> fileTypes = message.getFileTypes();
            Map<UUID, String> mimeTypes = message.getFileMimeTypes();
            Map<UUID, String> parsedNames = extractFileNames(message.getText(), message.getFiles());

            for (UUID fileId : message.getFiles()) {
                if (fileId == null) {
                    continue;
                }

                com.example.aichat.model.entities.File entity = fileCache.get(fileId);
                if (!hasExistingLocalFile(entity)) {
                    continue;
                }

                FileType type = fileTypes != null ? fileTypes.get(fileId) : null;
                String mime = mimeTypes != null ? mimeTypes.get(fileId) : null;
                String name = parsedNames.get(fileId);
                if (name != null && !name.trim().isEmpty()) {
                    localFileNames.put(fileId, name);
                } else {
                    name = resolveFileName(fileId, entity, null);
                }

                boolean voice = isVoiceAudioFile(fileId, type, mime, name);
                boolean audio = voice
                        || (mime != null && mime.toLowerCase(Locale.US).startsWith("audio"))
                        || looksLikeAudioFile(name);

                if (!audio || voice != currentIsVoice) {
                    continue;
                }

                String title = resolveAudioTitle(fileId, entity, name);
                String subtitle = resolveAudioSubtitle(fileId, entity, mime, type, name);

                queueItems.add(new AudioPlayerManager.AudioQueueItem(
                        fileId,
                        entity.localPath,
                        title,
                        subtitle,
                        voice
                ));
            }
        }

        audioPlayerManager.setQueue(queueItems, currentFileId);
    }

    @NonNull
    private String resolveAudioTitle(@NonNull UUID fileId, @Nullable com.example.aichat.model.entities.File entity) {
        return resolveAudioTitle(fileId, entity, null);
    }

    @NonNull
    private String resolveAudioTitle(
            @NonNull UUID fileId,
            @Nullable com.example.aichat.model.entities.File entity,
            @Nullable String fallbackFileName
    ) {
        String title = audioDisplayTitles.get(fileId);
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }

        if (isVoiceAudioFile(fileId)) {
            return "Голосовое сообщение";
        }

        if (fallbackFileName != null && !fallbackFileName.trim().isEmpty()) {
            audioDisplayTitles.put(fileId, fallbackFileName);
            localFileNames.put(fileId, fallbackFileName);
            return fallbackFileName;
        }

        String declaredName = findDeclaredFileName(fileId);
        if (declaredName != null && !declaredName.trim().isEmpty()) {
            audioDisplayTitles.put(fileId, declaredName);
            localFileNames.put(fileId, declaredName);
            return declaredName;
        }

        return resolveFileName(fileId, entity, null);
    }

    @NonNull
    private String resolveAudioSubtitle(@NonNull UUID fileId, @Nullable com.example.aichat.model.entities.File entity) {
        return resolveAudioSubtitle(fileId, entity, null, null, null);
    }

    @NonNull
    private String resolveAudioSubtitle(
            @NonNull UUID fileId,
            @Nullable com.example.aichat.model.entities.File entity,
            @Nullable String mimeType,
            @Nullable FileType fileType,
            @Nullable String fileName
    ) {
        String subtitle = audioDisplaySubtitles.get(fileId);
        if (subtitle != null) {
            return subtitle;
        }

        if (isVoiceAudioFile(fileId, fileType, mimeType, fileName)) {
            return "Voice";
        }

        if (mimeType != null && !mimeType.trim().isEmpty()) {
            return mimeType;
        }

        if (entity != null && entity.mimeType != null && !entity.mimeType.trim().isEmpty()) {
            return entity.mimeType;
        }

        return "Audio";
    }

    private boolean isVoiceAudioFile(@NonNull UUID fileId) {
        for (Message message : messages) {
            if (message == null || message.getFiles() == null || !message.getFiles().contains(fileId)) {
                continue;
            }

            Map<UUID, FileType> types = message.getFileTypes();
            Map<UUID, String> mimes = message.getFileMimeTypes();
            Map<UUID, String> names = extractFileNames(message.getText(), message.getFiles());

            FileType type = types != null ? types.get(fileId) : null;
            String mime = mimes != null ? mimes.get(fileId) : null;
            String name = names.get(fileId);
            if (name == null || name.trim().isEmpty()) {
                name = resolveFileName(fileId, fileCache.get(fileId), null);
            }

            return isVoiceAudioFile(fileId, type, mime, name);
        }

        return false;
    }

    private boolean isVoiceAudioFile(
            @NonNull UUID fileId,
            @Nullable FileType fileType,
            @Nullable String mimeType,
            @Nullable String fileName
    ) {
        if (fileType == FileType.VoiceMessage) {
            return true;
        }

        if (fileType != null && fileType != FileType.MessageFile) {
            return false;
        }

        String title = audioDisplayTitles.get(fileId);
        if (title != null && title.toLowerCase(Locale.US).contains("голос")) {
            return true;
        }

        String subtitle = audioDisplaySubtitles.get(fileId);
        if (subtitle != null) {
            String lowerSubtitle = subtitle.toLowerCase(Locale.US);
            if (lowerSubtitle.contains("voice") || lowerSubtitle.contains("голос")) {
                return true;
            }
        }

        if (fileName != null) {
            String lowerName = fileName.toLowerCase(Locale.US);
            if (lowerName.contains("voice_")
                    || lowerName.contains("voice-")
                    || lowerName.contains("voice message")
                    || lowerName.contains("голосовое")) {
                return true;
            }
        }

        return false;
    }

    private void bindAudioArtwork(
            @NonNull UUID fileId,
            @Nullable com.example.aichat.model.entities.File entity,
            @Nullable ImageView albumArt,
            boolean isVoice
    ) {
        if (albumArt == null) {
            return;
        }

        albumArt.setTag(fileId);

        if (isVoice) {
            albumArt.setImageDrawable(null);
            albumArt.setVisibility(View.GONE);
            return;
        }

        if (!hasExistingLocalFile(entity)) {
            albumArt.setImageResource(R.drawable.ic_audio);
            albumArt.setVisibility(View.VISIBLE);
            return;
        }

        Bitmap cached = audioArtworkCache.get(fileId);
        if (cached != null) {
            albumArt.setImageBitmap(cached);
            albumArt.setVisibility(View.VISIBLE);
            return;
        }

        albumArt.setImageResource(R.drawable.ic_audio);
        albumArt.setVisibility(View.VISIBLE);

        if (missingAudioArtwork.contains(fileId)) {
            return;
        }

        if (!loadingAudioArtwork.add(fileId)) {
            return;
        }

        new Thread(() -> {
            Bitmap bitmap = readEmbeddedAudioArtwork(entity.localPath);
            loadingAudioArtwork.remove(fileId);

            if (bitmap == null) {
                missingAudioArtwork.add(fileId);
                return;
            }

            missingAudioArtwork.remove(fileId);
            putBoundedCache(audioArtworkCache, fileId, bitmap, MAX_AUDIO_ARTWORK_CACHE);

            handler.post(() -> {
                if (albumArt.getTag() instanceof UUID && fileId.equals(albumArt.getTag())) {
                    albumArt.setImageBitmap(bitmap);
                    albumArt.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private void warmLocalAudioArtwork(
            @Nullable UUID fileId,
            @Nullable com.example.aichat.model.entities.File entity
    ) {
        if (fileId == null || entity == null || entity.localPath == null || entity.localPath.trim().isEmpty()) {
            return;
        }

        if (audioArtworkCache.containsKey(fileId)
                || missingAudioArtwork.contains(fileId)
                || loadingAudioArtwork.contains(fileId)) {
            return;
        }

        File localFile = new File(entity.localPath);
        if (!localFile.exists()) {
            return;
        }

        String mime = entity.mimeType != null ? entity.mimeType.toLowerCase(Locale.US) : "";
        String name = entity.fileName != null ? entity.fileName : localFile.getName();

        if (isVoiceAudioFile(fileId, parseFileType(entity.fileType), entity.mimeType, name)) {
            return;
        }

        if (!mime.startsWith("audio") && !looksLikeAudioFile(name)) {
            return;
        }

        if (!loadingAudioArtwork.add(fileId)) {
            return;
        }

        new Thread(() -> {
            Bitmap bitmap = readEmbeddedAudioArtwork(entity.localPath);
            loadingAudioArtwork.remove(fileId);

            if (bitmap == null) {
                missingAudioArtwork.add(fileId);
                return;
            }

            missingAudioArtwork.remove(fileId);
            putBoundedCache(audioArtworkCache, fileId, bitmap, MAX_AUDIO_ARTWORK_CACHE);

            handler.post(() -> notifyFileChanged(fileId));
        }).start();
    }

    @Nullable
    private Bitmap readEmbeddedAudioArtwork(@Nullable String localPath) {
        if (localPath == null || localPath.trim().isEmpty()) {
            return null;
        }

        File file = new File(localPath);
        if (!file.exists()) {
            return null;
        }

        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(file.getAbsolutePath());
            byte[] picture = retriever.getEmbeddedPicture();

            if (picture == null || picture.length == 0) {
                return null;
            }

            return BitmapFactory.decodeByteArray(picture, 0, picture.length);
        } catch (Exception ignored) {
            return null;
        }
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView messageText;
        private final TextView timeText;
        private final ImageView statusIcon;
        private final LinearLayout replyContainer;
        private final LinearLayout filesContainer;
        private final View filesScroll;
        private final boolean isMyMessage;
        private final RequestOptions glideOptions;

        private Message message;
        MessageViewHolder(@NonNull View itemView, boolean isMyMessage) {
            super(itemView);

            this.isMyMessage = isMyMessage;

            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
            replyContainer = itemView.findViewById(R.id.reply_container);
            filesContainer = itemView.findViewById(R.id.files_container);
            filesScroll = itemView.findViewById(R.id.files_scroll);
            statusIcon = isMyMessage ? itemView.findViewById(R.id.status_icon) : null;

            messageText.setMovementMethod(LinkMovementMethod.getInstance());
            messageText.setTextIsSelectable(false);
            messageText.setLongClickable(true);
            messageText.setOnLongClickListener(anchor -> {
                if (message != null && actionListener != null) {
                    showMessageMenu(anchor);
                    return true;
                }
                return false;
            });

            glideOptions = new RequestOptions()
                    .placeholder(R.drawable.ic_file)
                    .error(R.drawable.ic_file)
                    .centerCrop();

            installMessageLongClickTarget(itemView);
        }

        @SuppressWarnings("unused")
        public int getImageIndex(UUID targetFileId) {
            int index = 0;

            for (Message message : messages) {
                if (message.getFiles() == null) {
                    continue;
                }

                Map<UUID, FileType> types = message.getFileTypes();
                Map<UUID, String> mimes = message.getFileMimeTypes();

                for (UUID id : message.getFiles()) {
                    FileType type = types != null ? types.get(id) : null;
                    String mime = mimes != null ? mimes.get(id) : null;

                    String name = resolveFileName(id, fileCache.get(id), "/api/files/" + id);

                    boolean isImage =
                            (mime != null && mime.startsWith("image"))
                                    || type == FileType.MessageImage
                                    || (mime == null && type == null && isImageFile(name));

                    if (!isImage) {
                        continue;
                    }

                    if (id.equals(targetFileId)) {
                        return index;
                    }

                    index++;
                }
            }

            return 0;
        }

        @SuppressWarnings("unused")
        public List<String> getAllImageUrls() {
            List<String> result = new ArrayList<>();

            for (Message message : messages) {
                if (message.getFiles() == null) {
                    continue;
                }

                Map<UUID, FileType> types = message.getFileTypes();
                Map<UUID, String> mimes = message.getFileMimeTypes();

                for (UUID id : message.getFiles()) {
                    FileType type = types != null ? types.get(id) : null;
                    String mime = mimes != null ? mimes.get(id) : null;

                    boolean isImage = (mime != null && mime.startsWith("image"))
                            || type == FileType.MessageImage;

                    if (isImage) {
                        result.add(BuildConfig.SERVER_URL + "/api/files/" + id);
                    }
                }
            }

            return result;
        }

        private boolean isAudioFile(String name) {
            return looksLikeAudioFile(name);
        }

        private boolean isVideoFile(String name) {
            return looksLikeVideoFile(name);
        }


        private void installMessageLongClickTarget(@Nullable View view) {
            if (view == null) {
                return;
            }

            view.setLongClickable(true);
            view.setOnLongClickListener(anchor -> {
                if (message != null && actionListener != null) {
                    showMessageMenu(anchor);
                    return true;
                }
                return false;
            });

            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    installMessageLongClickTarget(group.getChildAt(i));
                }
            }
        }

        private boolean hasEditableText(@Nullable Message message) {
            if (message == null) {
                return false;
            }
            String text = stripEmbeddedQuoteData(removeFileTags(message.getText()));
            return text != null && !text.trim().isEmpty();
        }

        private void requestQuote(@Nullable String selectedText) {
            if (message == null || actionListener == null) {
                return;
            }

            String quoteText = selectedText != null ? selectedText.trim() : null;

            if (quoteText == null || quoteText.isEmpty()) {
                quoteText = stripEmbeddedQuoteData(removeFileTags(message.getText()));
            }

            if (quoteText == null || quoteText.trim().isEmpty()) {
                return;
            }

            pendingQuoteSelections.put(message.getId(), quoteText.trim());
            actionListener.onCopyMessageText(message);
        }

        private void showQuoteSelectionDialog() {
            if (message == null || actionListener == null) {
                return;
            }

            String fullText = stripEmbeddedQuoteData(removeFileTags(message.getText()));

            if (fullText == null || fullText.trim().isEmpty()) {
                return;
            }

            Context context = itemView.getContext();

            TextView quoteTextView = new TextView(context);
            quoteTextView.setText(fullText.trim());
            quoteTextView.setTextIsSelectable(true);
            quoteTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            quoteTextView.setLineSpacing(0f, 1.12f);
            quoteTextView.setPadding(
                    dp(itemView, 18),
                    dp(itemView, 14),
                    dp(itemView, 18),
                    dp(itemView, 14)
            );
            boolean darkTheme = isDarkTheme(context);
            int dialogTextColor = darkTheme ? Color.WHITE : Color.BLACK;
            int dialogBackgroundColor = darkTheme ? 0xFF303030 : Color.WHITE;
            int dialogAccentColor = resolveThemeColor(
                    itemView,
                    com.google.android.material.R.attr.colorPrimary,
                    0xFF20A39A
            );

            quoteTextView.setTextColor(dialogTextColor);
            quoteTextView.setHighlightColor(withAlpha(dialogAccentColor, darkTheme ? 118 : 86));
            quoteTextView.setBackgroundColor(dialogBackgroundColor);

            ScrollView scrollView = new ScrollView(context);
            scrollView.setFillViewport(false);
            scrollView.addView(
                    quoteTextView,
                    new ScrollView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("Выделите текст для цитаты")
                    .setView(scrollView)
                    .setNegativeButton("Отмена", null)
                    .setNeutralButton("Весь текст", null)
                    .setPositiveButton("Цитировать", null)
                    .create();

            dialog.setOnShowListener(dialogInterface -> {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                if (positiveButton != null) {
                    positiveButton.setTextColor(dialogAccentColor);
                    positiveButton.setOnClickListener(v -> {
                        String selectedText = getSelectedDialogText(quoteTextView);

                        if (selectedText == null || selectedText.trim().isEmpty()) {
                            Toast.makeText(
                                    context,
                                    "Выделите нужный фрагмент текста",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        requestQuote(selectedText);
                        dialog.dismiss();
                    });
                }

                if (neutralButton != null) {
                    neutralButton.setTextColor(dialogAccentColor);
                    neutralButton.setOnClickListener(v -> {
                        requestQuote(fullText);
                        dialog.dismiss();
                    });
                }

                if (negativeButton != null) {
                    negativeButton.setTextColor(dialogAccentColor);
                }

                quoteTextView.postDelayed(() -> {
                    quoteTextView.requestFocus();
                    quoteTextView.performLongClick();
                }, 220L);
            });

            dialog.show();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(dialogBackgroundColor));
            }
        }

        @Nullable
        private String getSelectedDialogText(@NonNull TextView textView) {
            CharSequence text = textView.getText();

            if (text == null || text.length() == 0) {
                return null;
            }

            int start = Math.max(0, textView.getSelectionStart());
            int end = Math.max(0, textView.getSelectionEnd());

            if (end < start) {
                int tmp = start;
                start = end;
                end = tmp;
            }

            if (start == end || start >= text.length() || end > text.length()) {
                return null;
            }

            return text.subSequence(start, end).toString();
        }


        private void showAiTranslateLanguageDialog() {
            if (message == null) {
                return;
            }

            String sourceText = stripEmbeddedQuoteData(
                    removeFileTags(
                            message.getText()
                    )
            );

            if (sourceText == null || sourceText.trim().isEmpty()) {
                Toast.makeText(
                        itemView.getContext(),
                        "Нет текста для перевода",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Context context =
                    itemView.getContext();

            String[] languageTitles =
                    new String[]{
                            "Английский",
                            "Русский",
                            "Испанский",
                            "Немецкий",
                            "Французский",
                            "Белорусский",
                            "Украинский"
                    };

            String[] languageCodes =
                    new String[]{
                            "en",
                            "ru",
                            "es",
                            "de",
                            "fr",
                            "be",
                            "uk"
                    };

            new AlertDialog.Builder(
                    context
            )
                    .setTitle(
                            "Перевести сообщение"
                    )
                    .setItems(
                            languageTitles,
                            (dialog, which) -> {
                                if (which < 0 || which >= languageCodes.length) {
                                    return;
                                }

                                requestAiTranslation(
                                        languageCodes[which],
                                        languageTitles[which]
                                );
                            }
                    )
                    .setNegativeButton(
                            "Отмена",
                            null
                    )
                    .show();
        }

        private void requestAiTranslation(
                @NonNull String langCode,
                @NonNull String languageTitle
        ) {
            if (message == null) {
                return;
            }

            String sourceText =
                    stripEmbeddedQuoteData(
                            removeFileTags(
                                    message.getText()
                            )
                    );

            if (sourceText == null || sourceText.trim().isEmpty()) {
                Toast.makeText(
                        itemView.getContext(),
                        "Нет текста для перевода",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            UUID resolvedChatId =
                    resolveMessageChatId(
                            message
                    );

            if (resolvedChatId == null) {
                Toast.makeText(
                        itemView.getContext(),
                        "Не удалось определить чат для перевода",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Context context =
                    itemView.getContext();

            AlertDialog loadingDialog =
                    createAiTranslationLoadingDialog(
                            context,
                            languageTitle
                    );

            loadingDialog.show();

            new AIController(
                    ConnectionSingleton
                            .getInstance()
                            .getConnectionDispatcher()
            )

                    .translate(
                            resolvedChatId,
                            AIModel.Default,
                            langCode,
                            TranslateStyle.Neutral,
                            sourceText.trim()
                    )
                    .thenAccept(rawResult -> itemView.post(() -> {
                        dismissDialogSafely(
                                loadingDialog
                        );

                        String translatedText =
                                extractTranslatedText(
                                        rawResult
                                );

                        if (translatedText.trim().isEmpty()) {
                            Toast.makeText(
                                    context,
                                    "Пустой ответ перевода",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        showAiTranslationResultDialog(
                                translatedText,
                                languageTitle
                        );
                    }))
                    .exceptionally(throwable -> {
                        itemView.post(() -> {
                            dismissDialogSafely(
                                    loadingDialog
                            );

                            Toast.makeText(
                                    context,
                                    "Ошибка перевода",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });

                        Log.e(
                                TAG,
                                "AI translate failed",
                                throwable
                        );

                        return null;
                    });
        }

        @NonNull
        private AlertDialog createAiTranslationLoadingDialog(
                @NonNull Context context,
                @NonNull String languageTitle
        ) {
            LinearLayout layout =
                    new LinearLayout(
                            context
                    );

            layout.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            layout.setGravity(
                    android.view.Gravity.CENTER_VERTICAL
            );

            int horizontalPadding =
                    dp(
                            itemView,
                            22
                    );

            int verticalPadding =
                    dp(
                            itemView,
                            18
                    );

            layout.setPadding(
                    horizontalPadding,
                    verticalPadding,
                    horizontalPadding,
                    verticalPadding
            );

            ProgressBar progressBar =
                    new ProgressBar(
                            context
                    );

            progressBar.setIndeterminate(
                    true
            );

            LinearLayout.LayoutParams progressParams =
                    new LinearLayout.LayoutParams(
                            dp(
                                    itemView,
                                    42
                            ),
                            dp(
                                    itemView,
                                    42
                            )
                    );

            layout.addView(
                    progressBar,
                    progressParams
            );

            TextView textView =
                    new TextView(
                            context
                    );

            textView.setText(
                    "Перевожу на " + languageTitle.toLowerCase(Locale.US) + "..."
            );

            textView.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    15
            );

            textView.setPadding(
                    dp(
                            itemView,
                            14
                    ),
                    0,
                    0,
                    0
            );

            textView.setTextColor(
                    isDarkTheme(
                            context
                    )
                            ? Color.WHITE
                            : Color.BLACK
            );

            layout.addView(
                    textView,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            AlertDialog dialog =
                    new AlertDialog.Builder(
                            context
                    )
                            .setTitle(
                                    "AI-перевод"
                            )
                            .setView(
                                    layout
                            )
                            .create();

            dialog.setCancelable(
                    false
            );

            return dialog;
        }

        private void dismissDialogSafely(@Nullable AlertDialog dialog) {
            if (dialog == null) {
                return;
            }

            try {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            } catch (Exception ignored) {
            }
        }

        @NonNull
        private String extractTranslatedText(@Nullable String rawResult) {
            if (rawResult == null) {
                return "";
            }

            String trimmed =
                    rawResult.trim();

            if (trimmed.isEmpty()) {
                return "";
            }

            String parsedJsonText =
                    extractTranslatedTextFromJson(
                            trimmed
                    );

            if (!parsedJsonText.trim().isEmpty()) {
                return parsedJsonText.trim();
            }

            return trimmed;
        }

        @NonNull
        private String extractTranslatedTextFromJson(@NonNull String rawJson) {
            try {
                JSONObject object =
                        new JSONObject(
                                rawJson
                        );

                String[] preferredKeys =
                        new String[]{
                                "translate",
                                "translation",
                                "translatedText",
                                "translated_text",
                                "text",
                                "message",
                                "result"
                        };

                for (String key : preferredKeys) {
                    if (!object.has(key)) {
                        continue;
                    }

                    Object value =
                            object.opt(
                                    key
                            );

                    String text =
                            extractTextFromJsonValue(
                                    value
                            );

                    if (!text.trim().isEmpty()) {
                        return text.trim();
                    }
                }
            } catch (Exception ignored) {
            }

            return "";
        }

        @NonNull
        private String extractTextFromJsonValue(@Nullable Object value) {
            if (value == null || JSONObject.NULL.equals(value)) {
                return "";
            }

            if (value instanceof String) {
                String stringValue =
                        ((String) value).trim();

                if (looksLikeJsonObject(stringValue)) {
                    String nested =
                            extractTranslatedTextFromJson(
                                    stringValue
                            );

                    if (!nested.trim().isEmpty()) {
                        return nested.trim();
                    }
                }

                return stringValue;
            }

            String valueAsText =
                    String.valueOf(
                            value
                    ).trim();

            if (looksLikeJsonObject(valueAsText)) {
                String nested =
                        extractTranslatedTextFromJson(
                                valueAsText
                        );

                if (!nested.trim().isEmpty()) {
                    return nested.trim();
                }
            }

            return valueAsText;
        }

        private boolean looksLikeJsonObject(@Nullable String value) {
            if (value == null) {
                return false;
            }

            String trimmed =
                    value.trim();

            return trimmed.startsWith("{")
                    && trimmed.endsWith("}");
        }

        private void showAiTranslationResultDialog(
                @NonNull String translatedText,
                @NonNull String languageTitle
        ) {
            Context context =
                    itemView.getContext();

            TextView resultView =
                    new TextView(
                            context
                    );

            resultView.setText(
                    translatedText
            );

            resultView.setTextIsSelectable(
                    true
            );

            resultView.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    17
            );

            resultView.setLineSpacing(
                    0f,
                    1.15f
            );

            resultView.setPadding(
                    dp(
                            itemView,
                            20
                    ),
                    dp(
                            itemView,
                            16
                    ),
                    dp(
                            itemView,
                            20
                    ),
                    dp(
                            itemView,
                            16
                    )
            );

            boolean darkTheme =
                    isDarkTheme(
                            context
                    );

            int dialogTextColor =
                    darkTheme
                            ? Color.WHITE
                            : Color.BLACK;

            int dialogBackgroundColor =
                    darkTheme
                            ? 0xFF303030
                            : Color.WHITE;

            int dialogAccentColor =
                    resolveThemeColor(
                            itemView,
                            com.google.android.material.R.attr.colorPrimary,
                            0xFF20A39A
                    );

            resultView.setTextColor(
                    dialogTextColor
            );

            resultView.setBackgroundColor(
                    dialogBackgroundColor
            );

            ScrollView scrollView =
                    new ScrollView(
                            context
                    );

            scrollView.setFillViewport(
                    false
            );

            scrollView.addView(
                    resultView,
                    new ScrollView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            AlertDialog dialog =
                    new AlertDialog.Builder(
                            context
                    )
                            .setTitle(
                                    "Перевод на " + languageTitle.toLowerCase(Locale.US)
                            )
                            .setView(
                                    scrollView
                            )
                            .setNegativeButton(
                                    "Закрыть",
                                    null
                            )
                            .setNeutralButton(
                                    "Копировать",
                                    null
                            )
                            .create();

            dialog.setOnShowListener(dialogInterface -> {
                Button neutralButton =
                        dialog.getButton(
                                AlertDialog.BUTTON_NEUTRAL
                        );

                Button negativeButton =
                        dialog.getButton(
                                AlertDialog.BUTTON_NEGATIVE
                        );

                if (neutralButton != null) {
                    neutralButton.setTextColor(
                            dialogAccentColor
                    );

                    neutralButton.setOnClickListener(v -> {
                        copyTextToClipboard(
                                context,
                                translatedText
                        );

                        Toast.makeText(
                                context,
                                "Перевод скопирован",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                }

                if (negativeButton != null) {
                    negativeButton.setTextColor(
                            dialogAccentColor
                    );
                }
            });

            dialog.show();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(
                        new ColorDrawable(
                                dialogBackgroundColor
                        )
                );
            }
        }

        private void copyTextToClipboard(
                @NonNull Context context,
                @NonNull String text
        ) {
            ClipboardManager clipboardManager =
                    (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

            if (clipboardManager == null) {
                return;
            }

            clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(
                            "translation",
                            text
                    )
            );
        }

        @Nullable
        private UUID resolveMessageChatId(@Nullable Message sourceMessage) {
            if (sourceMessage == null) {
                return null;
            }

            UUID chatId = tryReadUuidByGetter(sourceMessage, "getChatId");

            if (chatId != null) {
                return chatId;
            }

            chatId = tryReadUuidByGetter(sourceMessage, "getChat");

            if (chatId != null) {
                return chatId;
            }

            chatId = tryReadUuidByField(sourceMessage, "chatId");

            if (chatId != null) {
                return chatId;
            }

            chatId = tryReadUuidByField(sourceMessage, "chat");

            if (chatId != null) {
                return chatId;
            }

            Log.w(
                    TAG,
                    "Cannot resolve chatId for AI translation, messageId = "
                            + sourceMessage.getId()
            );

            return null;
        }

        @Nullable
        private UUID tryReadUuidByGetter(
                @NonNull Message sourceMessage,
                @NonNull String getterName
        ) {
            try {
                Object value = sourceMessage.getClass()
                        .getMethod(getterName)
                        .invoke(sourceMessage);

                return extractUuid(value);
            } catch (Exception ignored) {
                return null;
            }
        }

        @Nullable
        private UUID tryReadUuidByField(
                @NonNull Message sourceMessage,
                @NonNull String fieldName
        ) {
            try {
                java.lang.reflect.Field field = sourceMessage.getClass()
                        .getDeclaredField(fieldName);

                field.setAccessible(true);

                Object value = field.get(sourceMessage);

                return extractUuid(value);
            } catch (Exception ignored) {
                return null;
            }
        }

        @Nullable
        private UUID extractUuid(@Nullable Object value) {
            if (value == null) {
                return null;
            }

            if (value instanceof UUID) {
                return (UUID) value;
            }

            if (value instanceof String) {
                String rawValue = ((String) value).trim();

                if (rawValue.isEmpty()) {
                    return null;
                }

                try {
                    return UUID.fromString(rawValue);
                } catch (Exception ignored) {
                    return null;
                }
            }

            try {
                Object idValue = value.getClass()
                        .getMethod("getId")
                        .invoke(value);

                return extractUuid(idValue);
            } catch (Exception ignored) {
                return null;
            }
        }


        private void showMessageMenu(View anchor) {
            if (message == null || actionListener == null) {
                return;
            }

            PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
            menu.inflate(R.menu.message_context_menu);

            boolean ownMessage = messageController.isMyMessage(message);
            boolean editable = ownMessage && hasEditableText(message);

            if (menu.getMenu().findItem(R.id.menu_edit_message) != null) {
                menu.getMenu().findItem(R.id.menu_edit_message).setVisible(editable);
            }

            if (menu.getMenu().findItem(R.id.menu_delete_message) != null) {
                menu.getMenu().findItem(R.id.menu_delete_message).setVisible(ownMessage);
            }

            if (menu.getMenu().findItem(R.id.menu_copy_text) != null) {
                menu.getMenu().findItem(R.id.menu_copy_text).setVisible(hasEditableText(message));
            }

            if (hasEditableText(message) && menu.getMenu().findItem(MENU_QUOTE_MESSAGE_ID) == null) {
                menu.getMenu().add(0, MENU_QUOTE_MESSAGE_ID, 25, "Цитировать");
            }

            if (hasEditableText(message) && menu.getMenu().findItem(MENU_TRANSLATE_MESSAGE_ID) == null) {
                menu.getMenu().add(0, MENU_TRANSLATE_MESSAGE_ID, 27, "Перевести");
            }

            menu.setOnMenuItemClickListener(item -> {
                if (message == null) {
                    return false;
                }

                if (item.getItemId() == R.id.menu_edit_message) {
                    if (messageController.isMyMessage(message)) {
                        actionListener.onEditMessage(message);
                    }
                    return true;
                }

                if (item.getItemId() == R.id.menu_delete_message) {
                    if (messageController.isMyMessage(message)) {
                        actionListener.onDeleteMessage(message);
                    }
                    return true;
                }

                if (item.getItemId() == R.id.menu_reply_message) {
                    actionListener.onReplyToMessage(message);
                    return true;
                }

                if (item.getItemId() == R.id.menu_copy_text) {
                    actionListener.onCopyMessageText(message);
                    return true;
                }

                if (item.getItemId() == MENU_QUOTE_MESSAGE_ID) {
                    showQuoteSelectionDialog();
                    return true;
                }

                if (item.getItemId() == MENU_TRANSLATE_MESSAGE_ID) {
                    showAiTranslateLanguageDialog();
                    return true;
                }

                return false;
            });

            menu.show();
        }

        private void applyMessageHighlight(boolean highlighted) {
            if (messageText == null) {
                return;
            }

            if (highlighted) {
                messageText.setBackgroundColor(0x44FFF2AA);
            } else {
                messageText.setBackgroundColor(Color.TRANSPARENT);
            }
        }

        private void renderReplyOrQuote(
                @NonNull Message sourceMessage,
                @Nullable String quoteText
        ) {
            replyContainer.removeAllViews();
            replyContainer.setVisibility(View.GONE);

            UUID repliedMessageId = resolveFirstReplyMessageId(sourceMessage);

            if (repliedMessageId == null) {
                return;
            }

            String previewText;
            boolean quoteMode = quoteText != null && !quoteText.trim().isEmpty();

            if (quoteMode) {
                previewText = quoteText.trim();
            } else {
                Message originalMessage = findMessageById(repliedMessageId);
                previewText = buildReplyPreviewText(originalMessage, repliedMessageId);
            }

            if (previewText == null || previewText.trim().isEmpty()) {
                previewText = "Сообщение";
            }

            renderStyledReplyChip(repliedMessageId, previewText.trim(), quoteMode);
        }

        @NonNull
        private String buildReplyPreviewText(
                @Nullable Message originalMessage,
                @NonNull UUID repliedMessageId
        ) {
            if (originalMessage == null) {
                return "Сообщение";
            }

            String text = stripEmbeddedQuoteData(removeFileTags(originalMessage.getText()));

            if (text != null) {
                text = removeYouTubeUrls(text).trim();
            }

            if (text != null && !text.trim().isEmpty()) {
                return text.replaceAll("\\s+", " ").trim();
            }

            List<UUID> originalFiles = originalMessage.getFiles();

            if (originalFiles != null && !originalFiles.isEmpty()) {
                UUID firstFileId = originalFiles.get(0);

                if (firstFileId != null) {
                    Map<UUID, String> parsedNames = extractFileNames(
                            originalMessage.getText(),
                            originalFiles
                    );

                    String parsedName = parsedNames.get(firstFileId);

                    if (parsedName != null && !parsedName.trim().isEmpty()) {
                        return parsedName.trim();
                    }

                    com.example.aichat.model.entities.File entity = fileCache.get(firstFileId);
                    String resolvedName = resolveFileName(
                            firstFileId,
                            entity,
                            "/api/files/" + firstFileId
                    );

                    if (resolvedName != null && !resolvedName.trim().isEmpty()
                            && !"Файл".equals(resolvedName)) {
                        return resolvedName.trim();
                    }
                }

                return "Файл";
            }

            return "Сообщение";
        }

        private void renderStyledReplyChip(
                @NonNull UUID repliedMessageId,
                @NonNull String previewText,
                boolean quoteMode
        ) {
            Context context = itemView.getContext();

            replyContainer.removeAllViews();
            replyContainer.setVisibility(View.VISIBLE);
            replyContainer.setGravity(isMyMessage
                    ? android.view.Gravity.END
                    : android.view.Gravity.START);

            int accentColor = resolveThemeColor(
                    itemView,
                    com.google.android.material.R.attr.colorPrimary,
                    0xFF20A39A
            );

            int textColor = resolveThemeColor(
                    itemView,
                    com.google.android.material.R.attr.colorOnSurface,
                    isDarkTheme(context) ? 0xFFFFFFFF : 0xFF1D1B20
            );

            int secondaryTextColor = resolveThemeColor(
                    itemView,
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    isDarkTheme(context) ? 0xCCFFFFFF : 0xCC1D1B20
            );

            int surfaceVariantColor = resolveThemeColor(
                    itemView,
                    com.google.android.material.R.attr.colorSurfaceVariant,
                    isDarkTheme(context) ? 0xFF2B3336 : 0xFFF1F1F1
            );

            int chipBackgroundColor = isDarkTheme(context)
                    ? withAlpha(surfaceVariantColor, isMyMessage ? 210 : 190)
                    : withAlpha(surfaceVariantColor, isMyMessage ? 245 : 235);

            int chipStrokeColor = withAlpha(accentColor, isMyMessage ? 150 : 125);

            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setGravity(android.view.Gravity.CENTER_VERTICAL);
            root.setClickable(true);
            root.setFocusable(true);
            root.setMinimumHeight(dp(itemView, 34));
            root.setPadding(
                    dp(itemView, 8),
                    dp(itemView, 5),
                    dp(itemView, 10),
                    dp(itemView, 5)
            );

            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(dp(itemView, 11));
            background.setColor(chipBackgroundColor);
            background.setStroke(Math.max(1, dp(itemView, 1)), chipStrokeColor);
            root.setBackground(background);

            View accentLine = new View(context);
            GradientDrawable lineBackground = new GradientDrawable();
            lineBackground.setCornerRadius(dp(itemView, 2));
            lineBackground.setColor(accentColor);
            accentLine.setBackground(lineBackground);

            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                    dp(itemView, 3),
                    dp(itemView, 22)
            );
            lineParams.setMarginEnd(dp(itemView, 7));
            root.addView(accentLine, lineParams);

            TextView icon = new TextView(context);
            icon.setText(quoteMode ? "❝" : "↩");
            icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, quoteMode ? 14 : 15);
            icon.setTextColor(accentColor);
            icon.setIncludeFontPadding(false);
            icon.setGravity(android.view.Gravity.CENTER);
            icon.setMinWidth(dp(itemView, 24));
            icon.setMinHeight(dp(itemView, 24));

            GradientDrawable iconBackground = new GradientDrawable();
            iconBackground.setShape(GradientDrawable.OVAL);
            iconBackground.setColor(withAlpha(accentColor, isDarkTheme(context) ? 42 : 30));
            icon.setBackground(iconBackground);

            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    dp(itemView, 24),
                    dp(itemView, 24)
            );
            iconParams.setMarginEnd(dp(itemView, 7));
            root.addView(icon, iconParams);

            TextView preview = new TextView(context);
            preview.setText(previewText);
            preview.setSingleLine(true);
            preview.setEllipsize(TextUtils.TruncateAt.END);
            preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            preview.setTextColor(quoteMode ? textColor : secondaryTextColor);
            preview.setIncludeFontPadding(false);
            preview.setMaxWidth(dp(itemView, 210));

            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            root.addView(preview, previewParams);

            root.setOnClickListener(v -> {
                highlightMessage(repliedMessageId);
                scrollToMessage(repliedMessageId);
            });

            applyReplyQuoteChipRuntimeTheme(
                    root,
                    accentLine,
                    icon,
                    preview,
                    quoteMode
            );

            LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rootParams.setMargins(
                    isMyMessage ? dp(itemView, 26) : 0,
                    0,
                    isMyMessage ? 0 : dp(itemView, 26),
                    dp(itemView, 4)
            );

            replyContainer.addView(root, rootParams);
        }

        private void reapplyReplyQuoteChipStyle() {
            if (replyContainer == null || replyContainer.getChildCount() == 0) {
                return;
            }

            for (int i = 0; i < replyContainer.getChildCount(); i++) {
                View rootView =
                        replyContainer.getChildAt(
                                i
                        );

                if (!(rootView instanceof ViewGroup)) {
                    continue;
                }

                ViewGroup rootGroup =
                        (ViewGroup) rootView;

                View accentLine =
                        rootGroup.getChildCount() > 0
                                ? rootGroup.getChildAt(
                                0
                        )
                                : null;

                TextView icon =
                        rootGroup.getChildCount() > 1
                                && rootGroup.getChildAt(1) instanceof TextView
                                ? (TextView) rootGroup.getChildAt(
                                1
                        )
                                : null;

                TextView preview =
                        rootGroup.getChildCount() > 2
                                && rootGroup.getChildAt(2) instanceof TextView
                                ? (TextView) rootGroup.getChildAt(
                                2
                        )
                                : null;

                boolean quoteMode =
                        icon != null
                                && "❝".contentEquals(
                                icon.getText()
                        );

                applyReplyQuoteChipRuntimeTheme(
                        rootView,
                        accentLine,
                        icon,
                        preview,
                        quoteMode
                );
            }
        }

        private void applyReplyQuoteChipRuntimeTheme(
                @NonNull View root,
                @Nullable View accentLine,
                @Nullable TextView icon,
                @Nullable TextView preview,
                boolean quoteMode
        ) {
            Context context =
                    root.getContext();

            int accentColor =
                    resolveThemeColor(
                            root,
                            com.google.android.material.R.attr.colorPrimary,
                            0xFF20A39A
                    );

            int onSurface =
                    resolveThemeColor(
                            root,
                            com.google.android.material.R.attr.colorOnSurface,
                            isDarkTheme(context) ? 0xFFFFFFFF : 0xFF1D1B20
                    );

            int onSurfaceVariant =
                    resolveThemeColor(
                            root,
                            com.google.android.material.R.attr.colorOnSurfaceVariant,
                            isDarkTheme(context) ? 0xCCFFFFFF : 0xCC1D1B20
                    );

            int surfaceVariant =
                    resolveThemeColor(
                            root,
                            com.google.android.material.R.attr.colorSurfaceVariant,
                            isDarkTheme(context) ? 0xFF2B3336 : 0xFFF1F1F1
                    );

            int chipBackgroundColor =
                    isDarkTheme(context)
                            ? withAlpha(
                            surfaceVariant,
                            isMyMessage ? 210 : 190
                    )
                            : withAlpha(
                            surfaceVariant,
                            isMyMessage ? 245 : 235
                    );

            int chipStrokeColor =
                    withAlpha(
                            accentColor,
                            isMyMessage ? 150 : 125
                    );

            GradientDrawable rootBackground =
                    new GradientDrawable();

            rootBackground.setCornerRadius(
                    dp(
                            root,
                            11
                    )
            );

            rootBackground.setColor(
                    chipBackgroundColor
            );

            rootBackground.setStroke(
                    Math.max(
                            1,
                            dp(
                                    root,
                                    1
                            )
                    ),
                    chipStrokeColor
            );

            root.setBackground(
                    rootBackground
            );

            if (accentLine != null) {
                GradientDrawable lineBackground =
                        new GradientDrawable();

                lineBackground.setCornerRadius(
                        dp(
                                accentLine,
                                2
                        )
                );

                lineBackground.setColor(
                        accentColor
                );

                accentLine.setBackground(
                        lineBackground
                );
            }

            if (icon != null) {
                icon.setTextColor(
                        accentColor
                );

                GradientDrawable iconBackground =
                        new GradientDrawable();

                iconBackground.setShape(
                        GradientDrawable.OVAL
                );

                iconBackground.setColor(
                        withAlpha(
                                accentColor,
                                isDarkTheme(context) ? 58 : 34
                        )
                );

                icon.setBackground(
                        iconBackground
                );
            }

            if (preview != null) {
                preview.setTextColor(
                        quoteMode
                                ? onSurface
                                : onSurfaceVariant
                );
            }
        }

        private int withAlpha(int color, int alpha) {
            int safeAlpha = Math.max(0, Math.min(255, alpha));
            return (color & 0x00FFFFFF) | (safeAlpha << 24);
        }

        void bind(Message message) {
            this.message = message;

            clearItemHighlight(itemView);
            applyMessageHighlight(highlightedMessageId != null && highlightedMessageId.equals(message.getId()));

            replyContainer.removeAllViews();
            filesContainer.removeAllViews();

            Map<UUID, String> parsedNames = extractFileNames(message.getText(), message.getFiles());

            String quoteText = extractEmbeddedQuoteText(message.getText());
            String cleanedText = stripEmbeddedQuoteData(removeFileTags(message.getText()));
            LinkPreview linkPreview = findLinkPreview(cleanedText);

            String visibleText = linkPreview != null
                    ? removePreviewUrls(cleanedText)
                    : cleanedText;

            boolean hasText = visibleText != null && !visibleText.trim().isEmpty();
            boolean hasFiles = message.getFiles() != null && !message.getFiles().isEmpty();
            boolean hasLinkPreview = linkPreview != null;

            messageText.setVisibility(hasText ? View.VISIBLE : View.GONE);

            if (hasText) {
                markwon.setMarkdown(messageText, visibleText);
            } else {
                messageText.setText("");
            }

            if (filesScroll != null) {
                filesScroll.setVisibility((hasFiles || hasLinkPreview) ? View.VISIBLE : View.GONE);
            }

            if (hasFiles || hasLinkPreview) {
                filesContainer.setVisibility(View.VISIBLE);
                filesContainer.setOrientation(LinearLayout.HORIZONTAL);

                LayoutInflater inflater = LayoutInflater.from(itemView.getContext());

                if (hasLinkPreview) {
                    bindLinkPreview(inflater, linkPreview);
                }

                if (hasFiles) {
                    bindFiles(message, parsedNames);
                }
            } else {
                filesContainer.setVisibility(View.GONE);
            }

            installMessageLongClickTarget(itemView);

            renderReplyOrQuote(message, quoteText);

            timeText.setText(MessageController.getFormattedMessageTime(message));
            updateStatus();
        }

        void applyPostThemeStabilization() {
            reapplyStableLinkPreviewStyle();
            reapplyReplyQuoteChipStyle();
        }

        private void reapplyStableLinkPreviewStyle() {
            for (int i = 0; i < filesContainer.getChildCount(); i++) {
                View child = filesContainer.getChildAt(i);

                if (child == null) {
                    continue;
                }

                if (child.getId() == R.id.youtubePreviewRoot) {
                    applyLinkPreviewStyle(child);
                    continue;
                }

                View nestedRoot = child.findViewById(R.id.youtubePreviewRoot);

                if (nestedRoot != null) {
                    applyLinkPreviewStyle(nestedRoot);
                }
            }
        }

        private void applyLinkPreviewStyle(View previewRoot) {
            if (previewRoot == null) {
                return;
            }

            int surface = resolveThemeColor(
                    previewRoot,
                    com.google.android.material.R.attr.colorSurface,
                    0xFF263A3A
            );

            int outline = resolveThemeColor(
                    previewRoot,
                    com.google.android.material.R.attr.colorOutline,
                    0xFF8A9A9A
            );

            int onSurface = resolveThemeColor(
                    previewRoot,
                    com.google.android.material.R.attr.colorOnSurface,
                    Color.WHITE
            );

            int onSurfaceVariant = resolveThemeColor(
                    previewRoot,
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    0xFFB8C8C8
            );

            if (previewRoot instanceof MaterialCardView) {
                MaterialCardView cardView = (MaterialCardView) previewRoot;
                cardView.setCardBackgroundColor(surface);
                cardView.setStrokeColor(outline);
                cardView.setStrokeWidth(dp(cardView, 1));
                cardView.setCardElevation(0f);
                cardView.setRadius(dp(cardView, 14));
            }

            TextView title = previewRoot.findViewById(R.id.youtube_preview_title);
            TextView subtitle = previewRoot.findViewById(R.id.youtube_preview_subtitle);

            if (title != null) {
                title.setTextColor(onSurface);
                title.setMinLines(2);
                title.setMaxLines(2);
            }

            if (subtitle != null) {
                subtitle.setTextColor(onSurfaceVariant);
                subtitle.setSingleLine(true);
                subtitle.setEllipsize(TextUtils.TruncateAt.END);
            }
        }

        private void bindLinkPreview(LayoutInflater inflater, LinkPreview preview) {
            if (preview == null || inflater == null) {
                return;
            }

            View previewView = inflater.inflate(R.layout.item_youtube_preview, filesContainer, false);
            previewView.setTag("link:" + preview.cacheKey);

            ImageView thumbnail = previewView.findViewById(R.id.youtube_preview_thumbnail);
            TextView title = previewView.findViewById(R.id.youtube_preview_title);
            TextView subtitle = previewView.findViewById(R.id.youtube_preview_subtitle);

            applyLinkPreviewStyle(previewView);

            LinkPreviewMetadata cached = linkPreviewCache.get(preview.cacheKey);

            if (title != null) {
                title.setTag(preview.cacheKey);
                title.setMinLines(2);
                title.setMaxLines(2);
                title.setText(cached != null
                        ? firstNotBlank(cached.title, preview.fallbackTitle, "Ссылка")
                        : firstNotBlank(preview.fallbackTitle, "Ссылка"));
            }

            if (subtitle != null) {
                subtitle.setTag(preview.cacheKey);
                subtitle.setText(cached != null
                        ? firstNotBlank(cached.siteName, preview.originalUrl)
                        : preview.originalUrl);
                subtitle.setSingleLine(true);
                subtitle.setEllipsize(TextUtils.TruncateAt.END);
            }

            if (thumbnail != null) {
                thumbnail.setTag(preview.cacheKey);
                Glide.with(thumbnail).clear(thumbnail);

                String imageUrl = cached != null
                        ? firstNotBlank(cached.imageUrl, preview.thumbnailUrl)
                        : preview.thumbnailUrl;

                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    Glide.with(thumbnail)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_video)
                            .error(R.drawable.ic_video)
                            .centerCrop()
                            .into(thumbnail);
                } else {
                    thumbnail.setImageResource(R.drawable.ic_video);
                }
            }

            if (cached == null || (cached.imageUrl == null && !preview.youtube)) {
                loadLinkPreviewMetadata(preview, title, subtitle, thumbnail);
            }

            previewView.setOnClickListener(v -> openLinkPreview(v.getContext(), preview.originalUrl));

            filesContainer.addView(previewView);
        }

        private void openLinkPreview(Context context, String url) {
            if (context == null || url == null || url.trim().isEmpty()) {
                return;
            }

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(intent);
            } catch (Exception ignored) {
            }
        }

        private void bindFiles(Message message, Map<UUID, String> parsedNames) {
            filesContainer.setVisibility(View.VISIBLE);
            filesContainer.setOrientation(LinearLayout.HORIZONTAL);

            Map<UUID, FileType> fileTypes = message.getFileTypes();
            Map<UUID, String> mimeMap = message.getFileMimeTypes();

            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());

            for (int i = 0; i < message.getFiles().size(); i++) {
                UUID fileId = message.getFiles().get(i);

                String url = "/api/files/" + fileId;
                String fullUrl = BuildConfig.SERVER_URL + url;

                FileType fileType = fileTypes != null ? fileTypes.get(fileId) : null;
                String mimeType = mimeMap != null ? mimeMap.get(fileId) : null;

                com.example.aichat.model.entities.File entity = resolveFileEntity(fileId);

                String nameFromText = parsedNames.get(fileId);
                String name = nameFromText != null ? nameFromText : resolveFileName(fileId, entity, url);

                boolean isImage =
                        fileType == FileType.MessageImage
                                || (mimeType != null && mimeType.startsWith("image"))
                                || isImageFile(name);

                boolean isVoice =
                        fileType == FileType.VoiceMessage
                                || ChatMediaMarkers.isVoiceFileName(name);

                boolean isAudio =
                        isVoice
                                || (mimeType != null && mimeType.startsWith("audio"))
                                || isAudioFile(name);

                boolean isVideo =
                        fileType == FileType.VideoMessage
                                || (mimeType != null && mimeType.startsWith("video"))
                                || isVideoFile(name);

                boolean isCircleVideo = isVideo && ChatMediaMarkers.isCircleVideoFileName(name);

                Integer downloadProgress = progressManager != null
                        ? progressManager.getProgress(fileId)
                        : null;

                boolean isDownloaded = hasExistingLocalFile(entity);

                maybePrefetchVisibleBoundFile(fileId, url, mimeType, fileType, name, isDownloaded, isMyMessage);

                if (isCircleVideo) {
                    bindVideoCircleFile(
                            inflater,
                            fileId,
                            url,
                            fullUrl,
                            mimeType,
                            fileType,
                            name,
                            downloadProgress,
                            isDownloaded,
                            entity
                    );
                } else if (isImage) {
                    bindImageFile(inflater, fileId, fullUrl, entity);
                } else if (isVideo) {
                    bindVideoFile(
                            inflater,
                            fileId,
                            url,
                            fullUrl,
                            mimeType,
                            fileType,
                            name,
                            downloadProgress,
                            isDownloaded,
                            entity
                    );
                } else {
                    bindRegularFile(
                            inflater,
                            fileId,
                            url,
                            mimeType,
                            fileType,
                            name,
                            downloadProgress,
                            isDownloaded,
                            entity,
                            isAudio,
                            false,
                            isVoice,
                            message
                    );
                }
            }
        }

        private com.example.aichat.model.entities.File resolveFileEntity(UUID fileId) {
            com.example.aichat.model.entities.File entity = fileCache.get(fileId);

            if (entity == null) {
                loadFileFromDb(fileId);
                return null;
            }

            if (!hasExistingLocalFile(entity)) {
                fileCache.remove(fileId);
                return null;
            }

            return entity;
        }

        private void bindImageFile(
                LayoutInflater inflater,
                UUID fileId,
                String fullUrl,
                com.example.aichat.model.entities.File finalEntity
        ) {
            View imageView = inflater.inflate(R.layout.item_chat_image, filesContainer, false);
            imageView.setTag(fileId);

            ImageView image = imageView.findViewById(R.id.chat_image);

            Glide.with(image).clear(image);
            image.setImageResource(R.drawable.ic_image);

            if (hasExistingLocalFile(finalEntity)) {
                failedRemoteImageIds.remove(fileId);

                Glide.with(image)
                        .load(new File(finalEntity.localPath))
                        .apply(glideOptions)
                        .into(image);
            } else if (failedRemoteImageIds.contains(fileId)) {
                image.setImageResource(R.drawable.ic_image);
            } else {
                Glide.with(image)
                        .load(GlideAuthHelper.build(fullUrl, image.getContext()))
                        .apply(glideOptions)
                        .listener(new RequestListener<>() {
                            @Override
                            public boolean onLoadFailed(
                                    @Nullable GlideException e,
                                    @NonNull Object model,
                                    @NonNull Target<android.graphics.drawable.Drawable> target,
                                    boolean isFirstResource
                            ) {
                                failedRemoteImageIds.add(fileId);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(
                                    @NonNull android.graphics.drawable.Drawable resource,
                                    @NonNull Object model,
                                    @NonNull Target<android.graphics.drawable.Drawable> target,
                                    @NonNull DataSource dataSource,
                                    boolean isFirstResource
                            ) {
                                failedRemoteImageIds.remove(fileId);
                                return false;
                            }
                        })
                        .into(image);
            }

            image.setOnClickListener(v -> {
                if (failedRemoteImageIds.contains(fileId) && !hasExistingLocalFile(finalEntity)) {
                    return;
                }

                openImagePreview(v.getContext(), fileId, fullUrl);
            });

            filesContainer.addView(imageView);
        }

        private void openImagePreview(Context context, UUID fileId, String fullUrl) {
            ArrayList<String> imageUrls = new ArrayList<>();
            int clickedIndex = 0;

            for (Message innerMessage : messages) {
                if (innerMessage.getFiles() == null) {
                    continue;
                }

                Map<UUID, String> msgParsedNames =
                        extractFileNames(innerMessage.getText(), innerMessage.getFiles());

                Map<UUID, FileType> msgTypes = innerMessage.getFileTypes();
                Map<UUID, String> msgMimes = innerMessage.getFileMimeTypes();

                for (UUID id : innerMessage.getFiles()) {
                    com.example.aichat.model.entities.File entityLocal = fileCache.get(id);

                    String nameInner = msgParsedNames.get(id);

                    if (nameInner == null) {
                        nameInner = resolveFileName(id, entityLocal, "/api/files/" + id);
                    }

                    FileType type = msgTypes != null ? msgTypes.get(id) : null;
                    String mime = msgMimes != null ? msgMimes.get(id) : null;

                    boolean isImageLocal =
                            type == FileType.MessageImage
                                    || (mime != null && mime.startsWith("image"))
                                    || isImageFile(nameInner);

                    if (!isImageLocal) {
                        continue;
                    }

                    String urlItem = BuildConfig.SERVER_URL + "/api/files/" + id;

                    if (id.equals(fileId)) {
                        clickedIndex = imageUrls.size();
                    }

                    imageUrls.add(urlItem);
                }
            }

            if (imageUrls.isEmpty()) {
                imageUrls.add(fullUrl);
                clickedIndex = 0;
            }

            Intent intent = new Intent(context, com.example.aichat.ImagePreviewActivity.class);
            intent.putStringArrayListExtra("urls", imageUrls);
            intent.putExtra("index", clickedIndex);

            context.startActivity(intent);
        }



        private void bindVideoFile(
                LayoutInflater inflater,
                UUID fileId,
                String url,
                String fullUrl,
                String mimeType,
                FileType fileType,
                String name,
                Integer progress,
                boolean isDownloaded,
                com.example.aichat.model.entities.File finalEntity
        ) {
            View videoView = inflater.inflate(R.layout.item_chat_video_file, filesContainer, false);
            videoView.setTag(fileId);

            ImageView thumbnail = videoView.findViewById(R.id.video_file_thumbnail);
            ImageView playButton = videoView.findViewById(R.id.video_file_play);
            ImageView menuButton = videoView.findViewById(R.id.file_menu);
            ImageView status = videoView.findViewById(R.id.file_status);
            ProgressBar progressBar = videoView.findViewById(R.id.file_progress_bar);
            TextView progressText = videoView.findViewById(R.id.file_progress_text);
            TextView fileName = videoView.findViewById(R.id.file_name);
            TextView fileExt = videoView.findViewById(R.id.file_ext);

            if (fileName != null) {
                fileName.setText(name);
                resetFileNameMarquee(fileName);
            }

            if (fileExt != null) {
                fileExt.setText(getFileExtension(mimeType, fileType, name));
            }

            boolean hasLocal = hasExistingLocalFile(finalEntity);
            boolean isBusy = isTransferInProgress(fileId)
                    || (progress != null && progress >= 0 && progress < 100);

            bindRegularVideoThumbnail(thumbnail, fileId, fullUrl, finalEntity, isBusy);


            applyFileState(
                    progressBar,
                    progressText,
                    status,
                    menuButton,
                    fileId,
                    isMyMessage,
                    progress,
                    isDownloaded || hasLocal
            );

            setupFileMenuButton(menuButton, finalEntity, url, mimeType, fileId, fileType);

            View.OnClickListener openListener = v -> {
                if (!hasExistingLocalFile(finalEntity) && isUploadInProgress(fileId)) {
                    android.widget.Toast.makeText(
                            v.getContext(),
                            "Видео ещё отправляется",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                openOrDownloadVideo(v, fileId, url, mimeType, fileType, finalEntity, isMyMessage);
            };

            videoView.setOnClickListener(openListener);

            if (playButton != null) {
                playButton.setOnClickListener(openListener);
            }

            filesContainer.addView(videoView);
        }

        private void bindRegularVideoThumbnail(
                ImageView thumbnail,
                UUID fileId,
                String fullUrl,
                com.example.aichat.model.entities.File finalEntity,
                boolean isUploading
        ) {
            if (thumbnail == null) {
                return;
            }

            Glide.with(thumbnail).clear(thumbnail);
            thumbnail.setImageResource(R.drawable.ic_video);

            if (hasExistingLocalFile(finalEntity)) {
                failedRemoteImageIds.remove(fileId);

                Bitmap cachedFrame = localVideoThumbnailCache.get(fileId);
                if (cachedFrame != null && !cachedFrame.isRecycled()) {
                    thumbnail.setImageBitmap(cachedFrame);
                    return;
                }

                startLocalVideoThumbnailLoad(fileId, finalEntity.localPath);
                return;
            }

            if (isUploading || failedRemoteImageIds.contains(fileId)) {
                return;
            }

            Glide.with(thumbnail)
                    .load(GlideAuthHelper.build(fullUrl, thumbnail.getContext()))
                    .apply(new RequestOptions()
                            .centerCrop()
                            .placeholder(R.drawable.ic_video)
                            .error(R.drawable.ic_video))
                    .listener(new RequestListener<>() {
                        @Override
                        public boolean onLoadFailed(
                                @Nullable GlideException e,
                                @NonNull Object model,
                                @NonNull Target<android.graphics.drawable.Drawable> target,
                                boolean isFirstResource
                        ) {
                            failedRemoteImageIds.add(fileId);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(
                                @NonNull android.graphics.drawable.Drawable resource,
                                @NonNull Object model,
                                @NonNull Target<android.graphics.drawable.Drawable> target,
                                @NonNull DataSource dataSource,
                                boolean isFirstResource
                        ) {
                            failedRemoteImageIds.remove(fileId);
                            return false;
                        }
                    })
                    .into(thumbnail);
        }

        private void startLocalVideoThumbnailLoad(
                @NonNull UUID fileId,
                @Nullable String localPath
        ) {
            if (localPath == null || localPath.trim().isEmpty()) {
                return;
            }

            File file = new File(localPath);
            if (!file.exists()) {
                return;
            }

            if (!loadingLocalVideoThumbnails.add(fileId)) {
                return;
            }

            new Thread(() -> {
                Bitmap frame = null;

                try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
                    retriever.setDataSource(file.getAbsolutePath());
                    frame = readBestVideoFrame(retriever);

                    if (frame != null) {
                        frame = scaleVideoThumbnail(frame, 640);
                    }
                } catch (Exception exception) {
                    Log.e(TAG, "Cannot load local video thumbnail", exception);
                }

                if (frame == null) {
                    frame = readThumbnailUtilsFrame(file);
                }

                Bitmap finalFrame = frame;

                handler.post(() -> {
                    loadingLocalVideoThumbnails.remove(fileId);

                    if (finalFrame != null && !finalFrame.isRecycled()) {
                        putBoundedCache(localVideoThumbnailCache, fileId, finalFrame, MAX_LOCAL_VIDEO_THUMBNAILS);
                        notifyFileChanged(fileId);
                    }
                });
            }).start();
        }

        @Nullable
        private Bitmap readBestVideoFrame(@NonNull MediaMetadataRetriever retriever) {
            long[] candidatesUs = new long[]{
                    0L,
                    250_000L,
                    1_000_000L,
                    2_000_000L
            };

            String durationRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long durationMs = 0L;

            try {
                if (durationRaw != null && !durationRaw.trim().isEmpty()) {
                    durationMs = Long.parseLong(durationRaw.trim());
                }
            } catch (Exception ignored) {
            }

            for (long timeUs : candidatesUs) {
                if (durationMs > 0 && timeUs / 1000L > durationMs) {
                    continue;
                }

                Bitmap frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);

                if (frame == null) {
                    frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST);
                }

                if (frame != null && !frame.isRecycled()) {
                    return frame;
                }
            }

            try {
                return retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST);
            } catch (Exception ignored) {
                return null;
            }
        }

        @Nullable
        private Bitmap scaleVideoThumbnail(@NonNull Bitmap source, int maxSide) {
            int width = source.getWidth();
            int height = source.getHeight();

            if (width <= 0 || height <= 0) {
                return source;
            }

            int largestSide = Math.max(width, height);

            if (largestSide <= maxSide) {
                return source;
            }

            float scale = maxSide / (float) largestSide;
            int targetWidth = Math.max(1, Math.round(width * scale));
            int targetHeight = Math.max(1, Math.round(height * scale));

            Bitmap scaled = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true);

            if (scaled != source) {
                source.recycle();
            }

            return scaled;
        }

        @Nullable
        private Bitmap readThumbnailUtilsFrame(@NonNull File file) {
            try {
                Bitmap frame = ThumbnailUtils.createVideoThumbnail(
                        file.getAbsolutePath(),
                        MediaStore.Video.Thumbnails.MINI_KIND
                );

                if (frame != null && !frame.isRecycled()) {
                    return scaleVideoThumbnail(frame, 640);
                }
            } catch (Exception exception) {
                Log.e(TAG, "Cannot create video thumbnail fallback", exception);
            }

            return null;
        }


        private void bindVideoCircleFile(
                LayoutInflater inflater,
                UUID fileId,
                String url,
                String fullUrl,
                String mimeType,
                FileType fileType,
                String name,
                Integer progress,
                boolean isDownloaded,
                com.example.aichat.model.entities.File finalEntity
        ) {
            View circleView = inflater.inflate(R.layout.item_chat_video_circle, filesContainer, false);
            circleView.setTag(fileId);

            View card = circleView.findViewById(R.id.video_circle_card);
            CircleVideoTextureView inlinePlayer = circleView.findViewById(R.id.video_circle_inline_player);
            ImageView thumbnail = circleView.findViewById(R.id.video_circle_thumbnail);
            ImageView playButton = circleView.findViewById(R.id.video_circle_play);
            View scrim = circleView.findViewById(R.id.video_circle_scrim);
            ImageView menuButton = circleView.findViewById(R.id.file_menu);
            ProgressBar progressBar = circleView.findViewById(R.id.file_progress_bar);
            TextView progressText = circleView.findViewById(R.id.file_progress_text);
            ImageView status = circleView.findViewById(R.id.file_status);
            TextView durationText = circleView.findViewById(R.id.video_circle_duration);
            SeekBar timeline = circleView.findViewById(R.id.video_circle_timeline);

            if (durationText != null) {
                durationText.setText(R.string.video_label);
            }

            if (timeline != null) {
                timeline.setVisibility(View.GONE);
                timeline.setProgress(0);
                timeline.setOnSeekBarChangeListener(null);
            }

            boolean isUploading = isTransferInProgress(fileId)
                    || (progress != null && progress >= 0 && progress < 100);
            boolean hasLocal = hasExistingLocalFile(finalEntity);
            boolean isActiveInline = hasLocal
                    && activeCircleVideoFileId != null
                    && activeCircleVideoFileId.equals(fileId);

            bindCircleThumbnail(thumbnail, fileId, fullUrl, finalEntity, isUploading);

            if (isActiveInline) {
                startInlineCirclePlayback(
                        circleView,
                        card,
                        inlinePlayer,
                        thumbnail,
                        playButton,
                        scrim,
                        timeline,
                        durationText,
                        finalEntity,
                        fileId
                );
            } else {
                showCirclePreviewOnly(inlinePlayer, thumbnail, playButton, scrim, timeline, durationText, card);
            }


            applyFileState(
                    progressBar,
                    progressText,
                    status,
                    menuButton,
                    fileId,
                    isMyMessage,
                    progress,
                    isDownloaded
            );

            setupVideoCircleMenuButton(menuButton, finalEntity, url, mimeType, fileId, fileType);

            View.OnClickListener playInlineListener = v -> {
                if (!hasExistingLocalFile(finalEntity) && isUploadInProgress(fileId)) {
                    return;
                }

                if (!hasExistingLocalFile(finalEntity)) {
                    openOrDownloadVideo(v, fileId, url, mimeType, fileType, finalEntity, isMyMessage);
                    return;
                }

                toggleInlineCirclePlayback(
                        circleView,
                        card,
                        inlinePlayer,
                        thumbnail,
                        playButton,
                        scrim,
                        timeline,
                        durationText,
                        finalEntity,
                        fileId
                );
            };

            circleView.setOnClickListener(playInlineListener);

            if (playButton != null) {
                playButton.setImageResource(R.drawable.ic_play_circle);
                playButton.setOnClickListener(v -> circleView.performClick());
            }

            filesContainer.addView(circleView);
        }

        private void setupVideoCircleMenuButton(
                ImageView menuButton,
                com.example.aichat.model.entities.File finalEntity,
                String url,
                String mimeType,
                UUID fileId,
                FileType fileType
        ) {
            if (menuButton == null) {
                return;
            }

            menuButton.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(v.getContext(), v);
                menu.getMenu().add("Открыть во встроенном видеоплеере");
                menu.getMenu().add("Открыть через другое приложение");

                menu.setOnMenuItemClickListener(item -> {
                    String title = item.getTitle() != null ? item.getTitle().toString() : "";

                    if (title.contains("встроенном")) {
                        if (hasExistingLocalFile(finalEntity)) {
                            openVideoPlayerInternal(v.getContext(), finalEntity, null, mimeType);
                        } else {
                            openOrDownloadVideo(v, fileId, url, mimeType, fileType, finalEntity, isMyMessage);
                        }
                        return true;
                    }

                    if (hasExistingLocalFile(finalEntity)) {
                        openFileExternal(v.getContext(), finalEntity);
                    } else if (isMyMessage) {
                        android.widget.Toast.makeText(
                                v.getContext(),
                                isUploadInProgress(fileId) ? "Видео ещё отправляется" : "Видео ещё подготавливается",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                        return true;
                    } else if (fileDownloadListener != null) {
                        if (isUploadInProgress(fileId)) {
                            android.widget.Toast.makeText(
                                    v.getContext(),
                                    "Видео ещё отправляется",
                                    android.widget.Toast.LENGTH_SHORT
                            ).show();
                            return true;
                        }

                        if (progressManager != null && progressManager.getProgress(fileId) == null) {
                            progressManager.updateProgress(fileId, 0);
                        }

                        fileDownloadListener.onFileDownloadRequired(
                                url,
                                mimeType,
                                fileId,
                                fileType,
                                true
                        );
                    }

                    return true;
                });

                menu.show();
            });
        }

        private void bindCircleThumbnail(
                ImageView thumbnail,
                UUID fileId,
                String fullUrl,
                com.example.aichat.model.entities.File finalEntity,
                boolean isUploading
        ) {
            if (thumbnail == null) {
                return;
            }

            Glide.with(thumbnail).clear(thumbnail);
            thumbnail.setImageResource(R.drawable.ic_video);

            if (hasExistingLocalFile(finalEntity)) {
                failedRemoteImageIds.remove(fileId);

                Bitmap cachedFrame = localVideoThumbnailCache.get(fileId);
                if (cachedFrame != null && !cachedFrame.isRecycled()) {
                    Glide.with(thumbnail)
                            .load(cachedFrame)
                            .apply(RequestOptions.circleCropTransform())
                            .placeholder(R.drawable.ic_video)
                            .error(R.drawable.ic_video)
                            .into(thumbnail);
                    return;
                }

                startLocalVideoThumbnailLoad(fileId, finalEntity.localPath);
                return;
            }


            failedRemoteImageIds.add(fileId);
        }

        private void toggleInlineCirclePlayback(
                View circleRoot,
                View card,
                CircleVideoTextureView inlinePlayer,
                ImageView thumbnail,
                ImageView playButton,
                View scrim,
                SeekBar timeline,
                TextView durationText,
                com.example.aichat.model.entities.File file,
                UUID fileId
        ) {
            if (inlinePlayer == null || !hasExistingLocalFile(file)) {
                return;
            }

            if (activeCircleVideoView == inlinePlayer
                    && activeCircleVideoFileId != null
                    && activeCircleVideoFileId.equals(fileId)) {
                stopActiveInlineCirclePlayback();
                showCirclePreviewOnly(inlinePlayer, thumbnail, playButton, scrim, timeline, durationText, card);
                return;
            }

            stopActiveInlineCirclePlayback();
            activeCircleVideoFileId = fileId;
            startInlineCirclePlayback(circleRoot, card, inlinePlayer, thumbnail, playButton, scrim, timeline, durationText, file, fileId);
        }

        private void startInlineCirclePlayback(
                View circleRoot,
                View card,
                CircleVideoTextureView inlinePlayer,
                ImageView thumbnail,
                ImageView playButton,
                View scrim,
                SeekBar timeline,
                TextView durationText,
                com.example.aichat.model.entities.File file,
                UUID fileId
        ) {
            if (inlinePlayer == null || !hasExistingLocalFile(file)) {
                return;
            }

            try {
                activeCircleVideoView = inlinePlayer;
                activeCircleVideoFileId = fileId;
                activeCircleTimeline = timeline;
                activeCircleDurationText = durationText;
                activeCircleContainerView = card != null ? card : circleRoot;

                if (thumbnail != null) {
                    thumbnail.animate().cancel();
                    thumbnail.setAlpha(1f);
                    thumbnail.setVisibility(View.VISIBLE);
                }

                if (inlinePlayer != null) {
                    inlinePlayer.animate().cancel();
                    inlinePlayer.setAlpha(0f);
                    inlinePlayer.setVisibility(View.VISIBLE);
                }

                if (timeline != null) {
                    timeline.setVisibility(View.VISIBLE);
                    timeline.setMax(1000);
                    timeline.setProgress(0);
                    timeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if (!fromUser || activeCircleVideoView == null) {
                                return;
                            }

                            int duration = activeCircleVideoView.getDuration();
                            if (duration > 0) {
                                activeCircleVideoView.seekTo((int) (duration * (progress / 1000f)));
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });
                }

                if (durationText != null) {
                    durationText.setText("0:00");
                    durationText.setVisibility(View.VISIBLE);
                }

                inlinePlayer.setOnPreparedListener(mp -> {
                    if (thumbnail != null) {
                        thumbnail.animate().cancel();
                        thumbnail.animate()
                                .alpha(0f)
                                .setDuration(120L)
                                .withEndAction(() -> {
                                    thumbnail.setAlpha(1f);
                                    thumbnail.setVisibility(View.GONE);
                                })
                                .start();
                    }

                    if (playButton != null) {
                        playButton.setVisibility(View.GONE);
                    }

                    if (scrim != null) {
                        scrim.setVisibility(View.GONE);
                    }

                    inlinePlayer.post(() -> {
                        inlinePlayer.animate().cancel();
                        inlinePlayer.animate()
                                .alpha(1f)
                                .setDuration(140L)
                                .start();
                    });

                    inlinePlayer.start();
                    startCircleTimelineUpdates();
                });
                inlinePlayer.setOnCompletionListener(mp -> {
                    stopActiveInlineCirclePlayback();
                    showCirclePreviewOnly(inlinePlayer, thumbnail, playButton, scrim, timeline, durationText, card);
                });
                inlinePlayer.setOnErrorListener((mp, what, extra) -> {
                    showCirclePreviewOnly(inlinePlayer, thumbnail, playButton, scrim, timeline, durationText, card);
                    return true;
                });
                inlinePlayer.setLooping(false);
                if (audioPlayerManager != null) {
                    audioPlayerManager.pause();
                }
                inlinePlayer.setMuted(false);
                inlinePlayer.setVideoURI(Uri.fromFile(new File(file.localPath)));
            } catch (Exception exception) {
                showCirclePreviewOnly(inlinePlayer, thumbnail, playButton, scrim, timeline, durationText, card);
            }
        }

        private void startCircleTimelineUpdates() {
            if (circleTimelineRunnable != null) {
                handler.removeCallbacks(circleTimelineRunnable);
            }

            circleTimelineRunnable = new Runnable() {
                @Override
                public void run() {
                    if (activeCircleVideoView == null || activeCircleTimeline == null) {
                        return;
                    }

                    int duration = activeCircleVideoView.getDuration();
                    int current = activeCircleVideoView.getCurrentPosition();

                    if (duration > 0) {
                        activeCircleTimeline.setProgress((int) Math.min(1000, Math.max(0, current * 1000f / duration)));
                        if (activeCircleDurationText != null) {
                            activeCircleDurationText.setText(formatTime(current) + " / " + formatTime(duration));
                        }
                    }

                    if (activeCircleVideoView != null && activeCircleVideoView.isPlaying()) {
                        handler.postDelayed(this, 80L);
                    }
                }
            };

            handler.post(circleTimelineRunnable);
        }

        private void showCirclePreviewOnly(
                CircleVideoTextureView inlinePlayer,
                ImageView thumbnail,
                ImageView playButton,
                View scrim,
                SeekBar timeline,
                TextView durationText,
                View card
        ) {
            if (circleTimelineRunnable != null) {
                handler.removeCallbacks(circleTimelineRunnable);
            }

            if (inlinePlayer != null) {
                try {
                    inlinePlayer.stopPlayback();
                } catch (Exception ignored) {
                }

                inlinePlayer.setOnPreparedListener(null);
                inlinePlayer.setOnCompletionListener(null);
                inlinePlayer.setOnErrorListener(null);
                inlinePlayer.setAlpha(0f);
                inlinePlayer.setVisibility(View.GONE);
            }

            if (timeline != null) {
                timeline.setOnSeekBarChangeListener(null);
                timeline.setProgress(0);
                timeline.setVisibility(View.GONE);
            }

            if (durationText != null) {
                durationText.setText(R.string.video_label);
                durationText.setVisibility(View.VISIBLE);
            }


            if (thumbnail != null) thumbnail.setVisibility(View.VISIBLE);
            if (playButton != null) playButton.setVisibility(View.VISIBLE);
            if (scrim != null) scrim.setVisibility(View.VISIBLE);
        }

        private void stopActiveInlineCirclePlayback() {
            if (circleTimelineRunnable != null) {
                handler.removeCallbacks(circleTimelineRunnable);
                circleTimelineRunnable = null;
            }

            if (activeCircleVideoView != null) {
                try {
                    activeCircleVideoView.stopPlayback();
                } catch (Exception ignored) {
                }

                activeCircleVideoView.setOnPreparedListener(null);
                activeCircleVideoView.setOnCompletionListener(null);
                activeCircleVideoView.setOnErrorListener(null);
                activeCircleVideoView.setAlpha(0f);
                activeCircleVideoView.setVisibility(View.GONE);

                View parent = activeCircleVideoView.getParent() instanceof View
                        ? (View) activeCircleVideoView.getParent()
                        : null;

                if (parent != null) {
                    View thumbnail = parent.findViewById(R.id.video_circle_thumbnail);
                    View play = parent.findViewById(R.id.video_circle_play);
                    View scrim = parent.findViewById(R.id.video_circle_scrim);
                    View timeline = parent.findViewById(R.id.video_circle_timeline);
                    TextView duration = parent.findViewById(R.id.video_circle_duration);

                    if (thumbnail != null) thumbnail.setVisibility(View.VISIBLE);
                    if (play != null) play.setVisibility(View.VISIBLE);
                    if (scrim != null) scrim.setVisibility(View.VISIBLE);
                    if (timeline != null) timeline.setVisibility(View.GONE);
                    if (duration != null) duration.setText(R.string.video_label);
                }
            }


            activeCircleVideoView = null;
            activeCircleVideoFileId = null;
            activeCircleTimeline = null;
            activeCircleDurationText = null;
            activeCircleContainerView = null;
        }

        private void bindRegularFile(
                LayoutInflater inflater,
                UUID fileId,
                String url,
                String mimeType,
                FileType fileType,
                String name,
                Integer progress,
                boolean isDownloaded,
                com.example.aichat.model.entities.File finalEntity,
                boolean isAudio,
                boolean isVideo,
                boolean isVoice,
                Message ownerMessage
        ) {
            View fileView = inflater.inflate(R.layout.item_chat_file, filesContainer, false);

            fileView.setTag(fileId);

            ImageView playIcon = fileView.findViewById(R.id.play_icon);
            ImageView albumArt = fileView.findViewById(R.id.file_album_art);
            ImageView menuButton = fileView.findViewById(R.id.file_menu);
            TextView fileName = fileView.findViewById(R.id.file_name);
            View fileNameClip = fileView.findViewById(R.id.file_name_clip);
            TextView fileExt = fileView.findViewById(R.id.file_ext);
            ProgressBar progressBar = fileView.findViewById(R.id.file_progress_bar);
            TextView progressText = fileView.findViewById(R.id.file_progress_text);
            ImageView status = fileView.findViewById(R.id.file_status);
            View audioContainer = fileView.findViewById(R.id.audio_container);
            SeekBar seekBar = fileView.findViewById(R.id.audio_seekbar);
            TextView currentTime = fileView.findViewById(R.id.audio_current_time);
            TextView totalTime = fileView.findViewById(R.id.audio_total_time);
            View metaContainer = fileView.findViewById(R.id.file_meta_container);

            if (isVoice) {
                if (fileNameClip != null) {
                    fileNameClip.setVisibility(View.GONE);
                }
                fileName.setText("");
                fileName.setVisibility(View.GONE);
                fileExt.setText("");
                fileExt.setVisibility(View.GONE);
            } else {
                if (fileNameClip != null) {
                    fileNameClip.setVisibility(View.VISIBLE);
                }
                fileName.setVisibility(View.VISIBLE);
                fileExt.setVisibility(View.VISIBLE);
                fileName.setText(name);
                resetFileNameMarquee(fileName);
                fileExt.setText(getFileExtension(mimeType, fileType, name));
            }

            if (isAudio) {
                String nowPlayingTitle = isVoice
                        ? buildVoiceNowPlayingTitle(fileView.getContext(), ownerMessage)
                        : (name != null && !name.trim().isEmpty()
                        ? name
                        : fileView.getContext().getString(R.string.audio_now_playing_unknown));
                String nowPlayingSubtitle = isVoice
                        ? fileView.getContext().getString(R.string.audio_now_playing_voice_subtitle)
                        : getFileExtension(mimeType, fileType, name);
                audioDisplayTitles.put(fileId, nowPlayingTitle);
                audioDisplaySubtitles.put(fileId, nowPlayingSubtitle);
            }

            applyFileState(
                    progressBar,
                    progressText,
                    status,
                    menuButton,
                    fileId,
                    isMyMessage,
                    progress,
                    isDownloaded
            );

            setupFileMenuButton(menuButton, finalEntity, url, mimeType, fileId, fileType);

            if (isAudio) {
                bindAudioFile(
                        fileId,
                        url,
                        mimeType,
                        fileType,
                        finalEntity,
                        fileName,
                        playIcon,
                        albumArt,
                        progressText,
                        audioContainer,
                        metaContainer,
                        seekBar,
                        currentTime,
                        totalTime,
                        isVoice
                );
            } else {
                bindNonAudioFile(
                        playIcon,
                        metaContainer,
                        audioContainer,
                        mimeType,
                        fileType,
                        name,
                        fileId,
                        url,
                        finalEntity,
                        isVideo
                );
            }

            if (!isAudio && !isVideo) {
                View.OnClickListener documentOpenListener = v -> openOrDownloadRegularFile(
                        v,
                        fileId,
                        url,
                        mimeType,
                        fileType,
                        finalEntity
                );

                fileView.setOnClickListener(documentOpenListener);
                if (playIcon != null) {
                    playIcon.setOnClickListener(documentOpenListener);
                }
            }

            filesContainer.addView(fileView);
        }

        @NonNull
        private String buildVoiceNowPlayingTitle(@NonNull Context context, @Nullable Message ownerMessage) {
            String sender = context.getString(R.string.audio_now_playing_interlocutor);

            if (ownerMessage != null && messageController.isMyMessage(ownerMessage)) {
                sender = context.getString(R.string.audio_now_playing_you);
            }

            String time = ownerMessage != null
                    ? MessageController.getFormattedMessageTime(ownerMessage)
                    : "";

            if (time == null || time.trim().isEmpty()) {
                return sender;
            }

            return context.getString(R.string.audio_now_playing_voice_title_format, sender, time);
        }

        private void setupFileMenuButton(
                ImageView menuButton,
                com.example.aichat.model.entities.File finalEntity,
                String url,
                String mimeType,
                UUID fileId,
                FileType fileType
        ) {
            if (menuButton == null) {
                return;
            }

            menuButton.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(v.getContext(), v);
                menu.getMenu().add("Открыть");
                menu.getMenu().add("Открыть через другое приложение");

                menu.setOnMenuItemClickListener(item -> {
                    String title = item.getTitle() != null ? item.getTitle().toString() : "";

                    if ("Открыть".equals(title)) {
                        openOrDownloadRegularFile(
                                v,
                                fileId,
                                url,
                                mimeType,
                                fileType,
                                finalEntity
                        );
                        return true;
                    }

                    if (hasExistingLocalFile(finalEntity)) {
                        openFileExternal(v.getContext(), finalEntity);
                    } else {
                        downloadFileForOpen(v, fileId, url, mimeType, fileType);
                    }

                    return true;
                });

                menu.show();
            });
        }

        private void bindAudioFile(
                UUID fileId,
                String url,
                String mimeType,
                FileType fileType,
                com.example.aichat.model.entities.File finalEntity,
                TextView fileName,
                ImageView playIcon,
                ImageView albumArt,
                TextView progressText,
                View audioContainer,
                View metaContainer,
                SeekBar seekBar,
                TextView currentTime,
                TextView totalTime,
                boolean isVoice
        ) {
            audioContainer.setVisibility(View.VISIBLE);
            ViewGroup.MarginLayoutParams audioParams = audioContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams
                    ? (ViewGroup.MarginLayoutParams) audioContainer.getLayoutParams()
                    : null;
            if (audioParams != null) {
                audioParams.topMargin = isVoice ? 0 : dp(audioContainer, 6);
                audioContainer.setLayoutParams(audioParams);
            }
            metaContainer.setVisibility(View.GONE);

            bindAudioArtwork(fileId, finalEntity, albumArt, isVoice);

            boolean isPlaying = audioPlayerManager != null && audioPlayerManager.isPlaying(fileId);
            boolean isPreparing = audioPlayerManager != null && audioPlayerManager.isPreparing(fileId);

            if (isVoice) {
                stopFileNameMarquee(fileName);
                fileName.setText("");
                fileName.setVisibility(View.GONE);
            } else {
                fileName.setVisibility(View.VISIBLE);
                boolean shouldMarquee = fileId.equals(activeAudioFileId);

                if (shouldMarquee) {
                    startFileNameMarquee(fileName);
                } else {
                    stopFileNameMarquee(fileName);
                }
            }

            playIcon.setVisibility(View.VISIBLE);
            playIcon.setImageResource((isPlaying || isPreparing)
                    ? R.drawable.ic_pause
                    : R.drawable.ic_play_circle
            );

            int duration = audioPlayerManager != null ? audioPlayerManager.getDuration(fileId) : 0;

            if (duration <= 0) {
                duration = getKnownLocalMediaDuration(fileId, finalEntity);
            }

            int audioPosition = audioPlayerManager != null ? audioPlayerManager.getCurrentPosition(fileId) : 0;
            prepareAudioSeekControl(seekBar, isVoice);
            AudioWaveformSeekView voiceWaveform = prepareVoiceWaveformView(
                    seekBar,
                    fileId,
                    finalEntity,
                    isVoice
            );

            updateAudioUi(seekBar, voiceWaveform, currentTime, totalTime, duration, audioPosition);
            seekBar.setOnSeekBarChangeListener(isVoice ? null : createAudioSeekListener(fileId));

            if (voiceWaveform != null) {
                voiceWaveform.setOnSeekListener(progress -> {
                    if (audioPlayerManager == null) {
                        return;
                    }

                    int knownDuration = audioPlayerManager.getDuration(fileId);

                    if (knownDuration <= 0) {
                        knownDuration = getKnownLocalMediaDuration(fileId, finalEntity);
                    }

                    if (knownDuration <= 0) {
                        return;
                    }

                    int newPosition = Math.max(0, Math.min(knownDuration, (int) (knownDuration * progress)));

                    try {
                        if (audioPlayerManager.isPlaying(fileId) || audioPlayerManager.isPaused(fileId)) {
                            audioPlayerManager.seekTo(newPosition);
                            updateAudioMiniPlayerProgress(fileId);
                            notifyFileChanged(fileId);
                        }
                    } catch (Exception exception) {
                        Log.e(TAG, "Failed to seek voice waveform", exception);
                    }
                });
            }

            playIcon.setOnClickListener(
                    v -> handleAudioClick(
                            v,
                            fileId,
                            url,
                            mimeType,
                            fileType,
                            finalEntity,
                            progressText
                    )
            );
        }

        private void prepareAudioSeekControl(@Nullable SeekBar seekBar, boolean isVoice) {
            if (seekBar == null) {
                return;
            }

            seekBar.setMax(100);
            seekBar.setPadding(0, 0, 0, 0);
            seekBar.setMinHeight(dp(seekBar, isVoice ? 30 : 32));
            seekBar.setMinimumHeight(dp(seekBar, isVoice ? 30 : 32));
            seekBar.setScaleY(isVoice ? 0.92f : 1.35f);
        }

        private void updateAudioUi(
                SeekBar seekBar,
                @Nullable AudioWaveformSeekView voiceWaveform,
                TextView currentTime,
                TextView totalTime,
                int duration,
                int audioPosition
        ) {
            if (duration > 0) {
                int progress = (int) (audioPosition * 100f / duration);

                if (seekBar != null) {
                    seekBar.setProgress(progress);
                }

                if (voiceWaveform != null) {
                    voiceWaveform.setProgress(audioPosition / (float) duration);
                }

                currentTime.setText(formatTime(audioPosition));
                totalTime.setText(formatTime(duration));
            } else {
                if (seekBar != null) {
                    seekBar.setProgress(0);
                }

                if (voiceWaveform != null) {
                    voiceWaveform.setProgress(0f);
                }

                String zeroTime = formatTime(0);
                currentTime.setText(zeroTime);
                totalTime.setText(zeroTime);
            }
        }

        @Nullable
        private AudioWaveformSeekView prepareVoiceWaveformView(
                SeekBar seekBar,
                UUID fileId,
                com.example.aichat.model.entities.File finalEntity,
                boolean isVoice
        ) {
            if (seekBar == null) {
                return null;
            }

            AudioWaveformSeekView existing = findVoiceWaveformView((View) seekBar.getParent());

            if (!isVoice) {
                seekBar.setVisibility(View.VISIBLE);

                if (existing != null) {
                    existing.setVisibility(View.GONE);
                }

                return null;
            }

            seekBar.setVisibility(View.GONE);

            AudioWaveformSeekView waveform = existing;

            if (waveform == null) {
                waveform = new AudioWaveformSeekView(seekBar.getContext());
                waveform.setTag(AudioWaveformSeekView.VOICE_WAVEFORM_TAG);

                ViewGroup parent = seekBar.getParent() instanceof ViewGroup
                        ? (ViewGroup) seekBar.getParent()
                        : null;

                if (parent == null) {
                    return null;
                }

                int index = parent.indexOfChild(seekBar);
                ViewGroup.LayoutParams params = cloneWaveformLayoutParams(seekBar);
                parent.addView(waveform, Math.max(0, index + 1), params);
            }

            int active = resolveThemeColor(seekBar, com.google.android.material.R.attr.colorPrimary, Color.rgb(32, 163, 154));
            int inactive = resolveThemeColor(seekBar, com.google.android.material.R.attr.colorOnSurfaceVariant, 0x66808080);
            waveform.setColors(active, inactive);
            waveform.setLevels(getVoiceWaveformLevels(fileId, finalEntity));
            waveform.setVisibility(View.VISIBLE);

            return waveform;
        }

        @Nullable
        private AudioWaveformSeekView findVoiceWaveformView(@Nullable View root) {
            if (root == null) {
                return null;
            }

            if (root instanceof AudioWaveformSeekView
                    && AudioWaveformSeekView.VOICE_WAVEFORM_TAG.equals(root.getTag())) {
                return (AudioWaveformSeekView) root;
            }

            if (!(root instanceof ViewGroup)) {
                return null;
            }

            ViewGroup group = (ViewGroup) root;

            for (int i = 0; i < group.getChildCount(); i++) {
                AudioWaveformSeekView result = findVoiceWaveformView(group.getChildAt(i));

                if (result != null) {
                    return result;
                }
            }

            return null;
        }

        private ViewGroup.LayoutParams cloneWaveformLayoutParams(SeekBar seekBar) {
            ViewGroup.LayoutParams original = seekBar.getLayoutParams();
            int height = Math.max(dp(seekBar, 38), original != null ? original.height : dp(seekBar, 38));

            if (original instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams source = (LinearLayout.LayoutParams) original;
                LinearLayout.LayoutParams copy = new LinearLayout.LayoutParams(source.width, height, source.weight);
                copy.setMargins(source.leftMargin, source.topMargin, source.rightMargin, source.bottomMargin);
                return copy;
            }

            if (original instanceof androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams source =
                        (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) original;
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams copy =
                        new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(source);
                copy.height = height;
                return copy;
            }

            if (original instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams source = (FrameLayout.LayoutParams) original;
                FrameLayout.LayoutParams copy = new FrameLayout.LayoutParams(source.width, height, source.gravity);
                copy.setMargins(source.leftMargin, source.topMargin, source.rightMargin, source.bottomMargin);
                return copy;
            }

            return new ViewGroup.LayoutParams(
                    original != null ? original.width : ViewGroup.LayoutParams.MATCH_PARENT,
                    height
            );
        }

        private float[] getVoiceWaveformLevels(UUID fileId, com.example.aichat.model.entities.File entity) {
            float[] cached = voiceWaveformCache.get(fileId);

            if (cached != null) {
                return cached;
            }

            long seed = fileId != null ? (fileId.getMostSignificantBits() ^ fileId.getLeastSignificantBits()) : 7L;
            long size = entity != null ? Math.max(1L, entity.size) : 1L;
            int count = 52;
            float[] levels = new float[count];
            long value = seed ^ size;

            for (int i = 0; i < count; i++) {
                value = value * 6364136223846793005L + 1442695040888963407L;
                float random = ((value >>> 40) & 0xFFFF) / 65535f;
                float wave = (float) Math.abs(Math.sin((i + 1) * 0.42f + (seed & 15)));
                levels[i] = Math.max(0.12f, Math.min(1f, 0.18f + random * 0.48f + wave * 0.34f));
            }

            putBoundedCache(voiceWaveformCache, fileId, levels, MAX_VOICE_WAVEFORM_CACHE);
            return levels;
        }

        private SeekBar.OnSeekBarChangeListener createAudioSeekListener(UUID fileId) {
            return new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser || audioPlayerManager == null) {
                        return;
                    }

                    if (!audioPlayerManager.isPlaying(fileId) && !audioPlayerManager.isPaused(fileId)) {
                        return;
                    }

                    int duration = audioPlayerManager.getDuration(fileId);
                    if (duration <= 0) {
                        com.example.aichat.model.entities.File entity = fileCache.get(fileId);
                        duration = getKnownLocalMediaDuration(fileId, entity);
                    }

                    if (duration <= 0) {
                        return;
                    }

                    int newPosition = Math.max(0, Math.min(duration, (int) (duration * (progress / 100f))));

                    try {
                        audioPlayerManager.seekTo(newPosition);
                        updateAudioMiniPlayerProgress(fileId);
                    } catch (Exception exception) {
                        Log.e(TAG, "Failed to seek audio", exception);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            };
        }

        private void handleAudioClick(
                View v,
                UUID fileId,
                String url,
                String mimeType,
                FileType fileType,
                com.example.aichat.model.entities.File finalEntity,
                TextView progressText
        ) {
            if (audioPlayerManager == null) {
                return;
            }

            if (audioPlayerManager.isPlaying(fileId)) {
                audioPlayerManager.pause();
                updateAudioMiniPlayerProgress(fileId);
                notifyFileChanged(fileId);
                return;
            }

            if (audioPlayerManager.isPaused(fileId)) {
                activeAudioFileId = fileId;
                audioPlayerManager.resume();
                showAudioMiniPlayer(fileId);
                notifyFileChanged(fileId);
                return;
            }

            UUID previous = activeAudioFileId;

            if (tryPlayLocalAudio(v, fileId, finalEntity)) {
                if (previous != null && !previous.equals(fileId)) {
                    notifyFileChanged(previous);
                }

                return;
            }

            if (audioPlayerManager.shouldAutoPlay(fileId)) {
                return;
            }

            activeAudioFileId = fileId;
            audioPlayerManager.requestPlayAfterDownload(fileId);

            if (previous != null && !previous.equals(fileId)) {
                notifyFileChanged(previous);
            }

            if (progressManager != null) {
                progressManager.updateProgress(fileId, 0);
            }

            if (fileDownloadListener != null) {
                fileDownloadListener.onFileDownloadRequired(
                        url,
                        mimeType,
                        fileId,
                        fileType,
                        false
                );
            }

            progressText.setVisibility(View.VISIBLE);
            progressText.setText(formatPercent(0));

            notifyFileChanged(fileId);
        }

        private boolean tryPlayLocalAudio(
                View v,
                UUID fileId,
                com.example.aichat.model.entities.File finalEntity
        ) {
            if (!hasExistingLocalFile(finalEntity)) {
                return false;
            }

            activeAudioFileId = fileId;
            audioPlayerManager.clearPendingPlay();

            prepareAudioQueueForPlayback(v.getContext(), fileId);

            audioPlayerManager.playLocal(
                    v.getContext(),
                    finalEntity.localPath,
                    fileId,
                    resolveAudioTitle(fileId, finalEntity),
                    resolveAudioSubtitle(fileId, finalEntity),
                    isVoiceAudioFile(fileId)
            );
            showAudioMiniPlayer(fileId);
            notifyFileChanged(fileId);

            return true;
        }

        private void bindNonAudioFile(
                ImageView playIcon,
                View metaContainer,
                View audioContainer,
                String mimeType,
                FileType fileType,
                String name,
                UUID fileId,
                String url,
                com.example.aichat.model.entities.File finalEntity,
                boolean isVideo
        ) {
            audioContainer.setVisibility(View.GONE);
            metaContainer.setVisibility(View.VISIBLE);

            playIcon.setVisibility(View.VISIBLE);

            if (isVideo) {
                playIcon.setImageTintList(null);
                playIcon.clearColorFilter();
                playIcon.setImageResource(R.drawable.ic_play_circle);
                playIcon.setOnClickListener(v -> openOrDownloadVideo(
                        v,
                        fileId,
                        url,
                        mimeType,
                        fileType,
                        finalEntity,
                        isMyMessage
                ));
                return;
            }

            playIcon.setOnClickListener(null);
            playIcon.setImageTintList(null);
            playIcon.clearColorFilter();
            playIcon.setImageResource(getFileIconResource(mimeType, fileType, name));
        }

        private boolean isDownloadInProgress(UUID fileId) {
            if (progressManager == null || fileId == null) {
                return false;
            }

            Integer currentProgress = progressManager.getProgress(fileId);

            return currentProgress != null
                    && currentProgress >= 0
                    && currentProgress < 100;
        }

        private boolean isUploadInProgress(UUID fileId) {
            if (uploadProgressManager == null || fileId == null) {
                return false;
            }

            Integer currentProgress = uploadProgressManager.getProgress(fileId);

            return currentProgress != null
                    && currentProgress >= 0
                    && currentProgress < 100;
        }

        private boolean isTransferInProgress(UUID fileId) {
            return isUploadInProgress(fileId) || isDownloadInProgress(fileId);
        }

        private void openOrDownloadRegularFile(
                @NonNull View anchor,
                @NonNull UUID fileId,
                @NonNull String url,
                @Nullable String mimeType,
                @Nullable FileType fileType,
                @Nullable com.example.aichat.model.entities.File finalEntity
        ) {
            if (hasExistingLocalFile(finalEntity)) {
                openFileExternal(anchor.getContext(), finalEntity);
                return;
            }

            com.example.aichat.model.entities.File cachedEntity = fileCache.get(fileId);

            if (hasExistingLocalFile(cachedEntity)) {
                openFileExternal(anchor.getContext(), cachedEntity);
                return;
            }

            if (isUploadInProgress(fileId)) {
                android.widget.Toast.makeText(
                        anchor.getContext(),
                        "Файл ещё отправляется",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
                return;
            }

            loadLocalRegularFileFromDbOrDownload(anchor, fileId, url, mimeType, fileType);
        }

        private void loadLocalRegularFileFromDbOrDownload(
                @NonNull View anchor,
                @NonNull UUID fileId,
                @NonNull String url,
                @Nullable String mimeType,
                @Nullable FileType fileType
        ) {
            new Thread(() -> {
                com.example.aichat.model.entities.File dbEntity = null;

                try {
                    dbEntity = DatabaseManager.getDatabase().fileDao().getById(fileId);
                } catch (Exception e) {
                    Log.e(TAG, "Cannot read local file from DB", e);
                }

                final com.example.aichat.model.entities.File finalDbEntity = dbEntity;

                handler.post(() -> {
                    if (hasExistingLocalFile(finalDbEntity)) {
                        fileCache.put(fileId, finalDbEntity);
                        openFileExternal(anchor.getContext(), finalDbEntity);
                        return;
                    }

                    downloadFileForOpen(anchor, fileId, url, mimeType, fileType);
                });
            }).start();
        }


        private void downloadFileForOpen(
                @NonNull View anchor,
                @NonNull UUID fileId,
                @NonNull String url,
                @Nullable String mimeType,
                @Nullable FileType fileType
        ) {
            if (isUploadInProgress(fileId)) {
                android.widget.Toast.makeText(
                        anchor.getContext(),
                        "Файл ещё отправляется",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (progressManager != null && progressManager.getProgress(fileId) == null) {
                progressManager.updateProgress(fileId, 0);
            }

            if (fileDownloadListener != null) {

                fileDownloadListener.onFileDownloadRequired(
                        url,
                        mimeType,
                        fileId,
                        fileType,
                        true
                );
            }
        }

        private void openOrDownloadVideo(
                View v,
                UUID fileId,
                String url,
                String mimeType,
                FileType fileType,
                com.example.aichat.model.entities.File finalEntity,
                boolean ownerIsCurrentUser
        ) {
            if (v == null || fileId == null) {
                return;
            }

            Context context = v.getContext();

            if (hasExistingLocalFile(finalEntity)) {
                openVideoPlayerInternal(context, finalEntity, null, mimeType);
                return;
            }

            com.example.aichat.model.entities.File cachedEntity = fileCache.get(fileId);

            if (hasExistingLocalFile(cachedEntity)) {
                openVideoPlayerInternal(context, cachedEntity, null, mimeType);
                return;
            }

            if (ownerIsCurrentUser) {
                android.widget.Toast.makeText(
                        context,
                        isUploadInProgress(fileId) ? "Видео ещё отправляется" : "Видео ещё подготавливается",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (isUploadInProgress(fileId)) {
                android.widget.Toast.makeText(
                        context,
                        "Видео ещё отправляется",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
                return;
            }

            loadLocalVideoFromDbOrDownload(v, fileId, url, mimeType, fileType);
        }

        private void loadLocalVideoFromDbOrDownload(
                @NonNull View anchor,
                @NonNull UUID fileId,
                @NonNull String url,
                @Nullable String mimeType,
                @Nullable FileType fileType
        ) {
            new Thread(() -> {
                com.example.aichat.model.entities.File dbEntity = null;

                try {
                    dbEntity = DatabaseManager.getDatabase().fileDao().getById(fileId);
                } catch (Exception e) {
                    Log.e(TAG, "Cannot read downloaded video from DB", e);
                }

                final com.example.aichat.model.entities.File finalDbEntity = dbEntity;

                handler.post(() -> {
                    Context context = anchor.getContext();

                    if (hasExistingLocalFile(finalDbEntity)) {
                        fileCache.put(fileId, finalDbEntity);
                        openVideoPlayerInternal(context, finalDbEntity, null, mimeType);
                        return;
                    }

                    if (progressManager != null && progressManager.getProgress(fileId) == null) {
                        progressManager.updateProgress(fileId, 0);
                    }

                    if (fileDownloadListener != null) {
                        fileDownloadListener.onFileDownloadRequired(
                                url,
                                mimeType,
                                fileId,
                                fileType,
                                true
                        );
                    }
                });
            }).start();
        }


        private void startBackgroundDownloadIfNeeded(
                @NonNull UUID fileId,
                @NonNull String url,
                @Nullable String mimeType,
                @Nullable FileType fileType
        ) {
            if (fileDownloadListener == null || isUploadInProgress(fileId)) {
                return;
            }

            if (runningVisibleDownloads.contains(fileId) || queuedVisibleDownloads.contains(fileId)) {
                return;
            }

            if (progressManager != null && progressManager.getProgress(fileId) == null) {
                progressManager.updateProgress(fileId, 0);
            }

            fileDownloadListener.onFileDownloadRequired(
                    url,
                    mimeType,
                    fileId,
                    fileType,
                    false
            );
        }

        private void openVideoPlayerInternal(
                Context context,
                com.example.aichat.model.entities.File file,
                String remoteUrl,
                String mimeType
        ) {
            if (context == null) {
                return;
            }

            if (!hasExistingLocalFile(file)) {
                android.widget.Toast.makeText(
                        context,
                        "Видео ещё загружается",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
                return;
            }

            try {
                Intent intent = new Intent(context, VideoPlayerActivity.class);

                intent.putExtra(VideoPlayerActivity.EXTRA_LOCAL_PATH, file.localPath);

                String title = file.fileName != null && !file.fileName.trim().isEmpty()
                        ? file.fileName
                        : "Видео";

                intent.putExtra(VideoPlayerActivity.EXTRA_TITLE, title);
                intent.putExtra(
                        VideoPlayerActivity.EXTRA_MIME_TYPE,
                        mimeType != null && !mimeType.trim().isEmpty() ? mimeType : "video/mp4"
                );
                intent.putExtra(VideoPlayerActivity.EXTRA_ENGINE, VideoPlayerActivity.ENGINE_EXO_PLAYER);

                context.startActivity(intent);
            } catch (Exception exception) {
                android.widget.Toast.makeText(
                        context,
                        "Не удалось открыть видеоплеер",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            }
        }

        private boolean isImageFile(String name) {
            return looksLikeImageFile(name);
        }

        private void openFileExternal(Context context, com.example.aichat.model.entities.File file) {
            if (context == null || !hasExistingLocalFile(file)) {
                return;
            }

            File localFile = new File(file.localPath);

            android.net.Uri uri =
                    androidx.core.content.FileProvider.getUriForFile(
                            context,
                            context.getPackageName() + ".fileprovider",
                            localFile
                    );

            String mime = file.mimeType != null && !file.mimeType.trim().isEmpty()
                    ? file.mimeType
                    : "*/*";

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                context.startActivity(intent);
            } catch (Exception directOpenError) {
                try {
                    Intent chooser = Intent.createChooser(intent, "Открыть через");
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(chooser);
                } catch (Exception chooserError) {
                    android.widget.Toast.makeText(
                            context,
                            "Нет приложения для открытия файла",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                }
            }
        }

        private String formatTime(long millis) {
            if (millis <= 0) {
                return "0:00";
            }

            int totalSeconds = (int) (millis / 1000);
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;

            return String.format(Locale.US, "%d:%02d", minutes, seconds);
        }

        void updateFileProgress(UUID fileId) {
            for (int i = 0; i < filesContainer.getChildCount(); i++) {
                View fileView = filesContainer.getChildAt(i);

                if (!fileId.equals(fileView.getTag())) {
                    continue;
                }

                ProgressBar progressBar = fileView.findViewById(R.id.file_progress_bar);
                TextView progressText = fileView.findViewById(R.id.file_progress_text);
                ImageView status = fileView.findViewById(R.id.file_status);
                ImageView menuButton = fileView.findViewById(R.id.file_menu);

                Integer downloadProgress = progressManager != null
                        ? progressManager.getProgress(fileId)
                        : null;

                com.example.aichat.model.entities.File entity = fileCache.get(fileId);

                if (hasLocalPath(entity) && !new File(entity.localPath).exists()) {
                    fileCache.remove(fileId);
                    entity = null;
                }

                boolean isDownloaded = hasExistingLocalFile(entity);

                applyFileState(
                        progressBar,
                        progressText,
                        status,
                        menuButton,
                        fileId,
                        isMyMessage,
                        downloadProgress,
                        isDownloaded
                );

                break;
            }
        }

        void updateAudioProgress(UUID fileId) {
            for (int i = 0; i < filesContainer.getChildCount(); i++) {
                View fileView = filesContainer.getChildAt(i);

                if (!fileId.equals(fileView.getTag())) {
                    continue;
                }

                SeekBar seekBar = fileView.findViewById(R.id.audio_seekbar);
                TextView currentTime = fileView.findViewById(R.id.audio_current_time);
                TextView totalTime = fileView.findViewById(R.id.audio_total_time);
                ImageView playIcon = fileView.findViewById(R.id.play_icon);

                if (seekBar == null || currentTime == null || totalTime == null || playIcon == null) {
                    return;
                }

                if (audioPlayerManager == null) {
                    return;
                }

                boolean isPlaying = audioPlayerManager.isPlaying(fileId);
                boolean isPreparing = audioPlayerManager.isPreparing(fileId);

                playIcon.setImageResource(
                        (isPlaying || isPreparing)
                                ? R.drawable.ic_pause
                                : R.drawable.ic_play_circle
                );

                int duration = audioPlayerManager.getDuration(fileId);
                int position = audioPlayerManager.getCurrentPosition(fileId);

                AudioWaveformSeekView voiceWaveform = findVoiceWaveformView(fileView);

                if (duration <= 0) {
                    com.example.aichat.model.entities.File entity = fileCache.get(fileId);
                    duration = getKnownLocalMediaDuration(fileId, entity);
                }

                if (duration > 0) {
                    int progress = (int) (position * 100f / duration);

                    if (!seekBar.isPressed()) {
                        seekBar.setProgress(progress);
                    }

                    if (voiceWaveform != null && !voiceWaveform.isUserScrubbing()) {
                        voiceWaveform.setProgress(position / (float) duration);
                    }

                    currentTime.setText(formatTime(position));
                    totalTime.setText(formatTime(duration));
                } else {
                    seekBar.setProgress(0);

                    if (voiceWaveform != null && !voiceWaveform.isUserScrubbing()) {
                        voiceWaveform.setProgress(0f);
                    }

                    String zeroTime = formatTime(0);
                    currentTime.setText(zeroTime);
                    totalTime.setText(zeroTime);
                }

                break;
            }
        }

        void updateAudioArtwork(UUID fileId) {
            for (int i = 0; i < filesContainer.getChildCount(); i++) {
                View fileView = filesContainer.getChildAt(i);

                if (!fileId.equals(fileView.getTag())) {
                    continue;
                }

                ImageView albumArt = fileView.findViewById(R.id.file_album_art);
                if (albumArt == null) {
                    return;
                }

                com.example.aichat.model.entities.File entity = fileCache.get(fileId);
                bindAudioArtwork(fileId, entity, albumArt, isVoiceAudioFile(fileId));
                return;
            }
        }

        void updateFileNameMarquee(UUID fileId) {
            for (int i = 0; i < filesContainer.getChildCount(); i++) {
                View fileView = filesContainer.getChildAt(i);

                if (!fileId.equals(fileView.getTag())) {
                    continue;
                }

                TextView fileName = fileView.findViewById(R.id.file_name);

                if (fileName == null) {
                    return;
                }

                boolean shouldMarquee =
                        audioPlayerManager != null
                                && fileId.equals(activeAudioFileId);

                if (shouldMarquee) {
                    startFileNameMarquee(fileName);
                } else {
                    stopFileNameMarquee(fileName);
                }

                break;
            }
        }

        private String getFileExtension(String mimeType, FileType fileType, String fileName) {
            if (mimeType != null) {
                if (mimeType.contains("image")) {
                    return "IMG";
                }

                if (mimeType.contains("video")) {
                    return "VID";
                }

                if (mimeType.contains("pdf")) {
                    return "PDF";
                }

                if (mimeType.contains("word")) {
                    return "DOC";
                }

                if (mimeType.contains("excel")) {
                    return "XLS";
                }

                if (mimeType.contains("zip")) {
                    return "ZIP";
                }

                if (mimeType.contains("audio")) {
                    return "AUDIO";
                }

                if (mimeType.contains("text")) {
                    return "TXT";
                }
            }

            if (fileName != null && fileName.contains(".")) {
                String extension = fileName
                        .substring(fileName.lastIndexOf('.') + 1)
                        .toLowerCase(Locale.US);

                switch (extension) {
                    case "jpg":
                    case "jpeg":
                    case "png":
                    case "webp":
                        return "IMG";

                    case "mp4":
                    case "mkv":
                    case "webm":
                        return "VID";

                    case "pdf":
                        return "PDF";

                    case "doc":
                    case "docx":
                        return "DOC";

                    case "xls":
                    case "xlsx":
                        return "XLS";

                    case "zip":
                    case "rar":
                        return "ZIP";

                    case "mp3":
                    case "wav":
                        return "AUDIO";

                    case "txt":
                        return "TXT";

                    default:
                        break;
                }
            }

            if (fileType != null) {
                switch (fileType) {
                    case MessageImage:
                        return "IMG";

                    case VideoMessage:
                        return "VID";

                    case VoiceMessage:
                        return "AUDIO";

                    default:
                        return "FILE";
                }
            }

            return "FILE";
        }

        private int getFileIconResource(String mimeType, FileType fileType, String fileName) {
            if (mimeType != null) {
                if (mimeType.contains("pdf")) {
                    return R.drawable.ic_pdf;
                }

                if (mimeType.contains("word") || mimeType.contains("document")) {
                    return R.drawable.ic_doc;
                }

                if (mimeType.contains("excel") || mimeType.contains("sheet")) {
                    return R.drawable.ic_excel;
                }

                if (mimeType.contains("zip") || mimeType.contains("rar")) {
                    return R.drawable.ic_zip;
                }

                if (mimeType.contains("audio")) {
                    return R.drawable.ic_audio;
                }

                if (mimeType.contains("video")) {
                    return R.drawable.ic_video;
                }

                if (mimeType.contains("text")) {
                    return R.drawable.ic_txt;
                }
            }

            if (fileName != null && fileName.contains(".")) {
                String extension = fileName
                        .substring(fileName.lastIndexOf('.') + 1)
                        .toLowerCase(Locale.US);

                switch (extension) {
                    case "jpg":
                    case "jpeg":
                    case "png":
                    case "webp":
                        return R.drawable.ic_image;

                    case "mp4":
                    case "mkv":
                    case "webm":
                        return R.drawable.ic_video;

                    case "pdf":
                        return R.drawable.ic_pdf;

                    case "doc":
                    case "docx":
                        return R.drawable.ic_doc;

                    case "xls":
                    case "xlsx":
                        return R.drawable.ic_excel;

                    case "zip":
                    case "rar":
                        return R.drawable.ic_zip;

                    case "mp3":
                    case "wav":
                        return R.drawable.ic_audio;

                    case "txt":
                        return R.drawable.ic_txt;

                    default:
                        break;
                }
            }

            if (fileType != null) {
                switch (fileType) {
                    case MessageImage:
                        return R.drawable.ic_image;

                    case VideoMessage:
                        return R.drawable.ic_video;

                    case VoiceMessage:
                        return R.drawable.ic_audio;

                    default:
                        return R.drawable.ic_file;
                }
            }

            return R.drawable.ic_file;
        }

        void updateStatus() {
            if (isMyMessage && statusIcon != null && message != null) {
                MessageStatus max = MessageController.getMaxStatus(message.getStatuses());
                int iconRes = MessageController.getStatusIconRes(max);

                if (isValidDrawableResourceId(iconRes)) {
                    statusIcon.setVisibility(View.VISIBLE);
                    statusIcon.setImageResource(iconRes);
                } else {
                    statusIcon.setVisibility(View.INVISIBLE);
                    statusIcon.setImageDrawable(null);
                }
            }
        }
    }
}
