package com.example.aichat.view.main.chat;

import com.example.aichat.model.utils.mappers.MessageMapper;
import com.example.aichat.model.utils.media.ChatMediaMarkers;
import com.example.aichat.model.utils.time.TimeConverter;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.controller.main.chat.actions.ChatMembersActions;
import com.example.aichat.controller.main.chat.actions.ChatMessageActions;
import com.example.aichat.controller.main.chat.actions.ChatStateActions;
import com.example.aichat.controller.main.chat.actions.SendMessageController;
import com.example.aichat.dto.request.TypingRequest;
import com.example.aichat.dto.response.AISettingsResponse;
import com.example.aichat.dto.response.ChatEndedResponse;
import com.example.aichat.dto.response.ChatNameUpdatedResponse;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.dto.response.TypingResponse;
import com.example.aichat.dto.response.UpdateMessageStatusResponse;
import com.example.aichat.dto.response.UserOnlineChangesResponse;
import com.example.aichat.model.ai.AIModel;
import com.example.aichat.model.ai.AISettingsStore;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.files.UploadProgressListener;
import com.example.aichat.model.connection.TokenStorage;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.AiRole;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.ChatType;
import com.example.aichat.model.entities.FileType;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageInfo;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.model.entities.User;
import com.example.aichat.model.exceptions.SignalRNotConnectedException;
import com.example.aichat.model.utils.media.audio.AudioPlayerManager;
import com.example.aichat.model.utils.media.recording.RecordedChatMedia;
import com.example.aichat.model.utils.export.ChatPdfExporter;
import com.example.aichat.model.utils.files.FileDownloadProgressManager;
import com.example.aichat.model.utils.files.FileManager;
import com.example.aichat.model.utils.files.FileManagerHolder;
import com.example.aichat.model.utils.files.FileUploadProgressManager;
import com.example.aichat.model.utils.files.InternalFileAdapter;
import com.example.aichat.model.utils.files.ProgressManagerHolder;
import com.example.aichat.R;
import com.example.aichat.view.main.chat.helpers.ChatMediaInputController;
import com.example.aichat.view.main.chat.helpers.ChatMembersUi;
import com.example.aichat.view.main.chat.helpers.ChatMessagesUi;
import com.example.aichat.view.main.chat.helpers.ChatSearchUi;
import com.example.aichat.view.main.chat.helpers.ChatUi;
import com.example.aichat.view.main.chat.helpers.VideoCircleRecorderDialogFragment;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.view.theme.binders.ChatFragmentThemeBinder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@androidx.media3.common.util.UnstableApi
public class ChatFragment extends Fragment {

    private UUID chatId;
    private UUID currentUserId;

    private SendMessageController sendMessageController;
    private ChatMessageActions messageActions;
    private ChatMembersActions membersActions;
    private ChatStateActions stateActions;

    private ActivityResultLauncher<String[]> filePickerLauncher;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ChatMediaInputController mediaInputController;
    private boolean isTimerRunning = false;

    private static final AudioPlayerManager SHARED_AUDIO_PLAYER_MANAGER = new AudioPlayerManager();
    private AudioPlayerManager audioPlayerManager = SHARED_AUDIO_PLAYER_MANAGER;

    private ChatUi ui;
    private ChatMessagesUi messagesUi;
    private ChatMembersUi membersUi;
    private ChatSearchUi searchUi;

    private Message editingMessage = null;
    private Message replyingToMessage = null;
    private String pendingQuoteText = null;

    private Chat chat;

    private final List<Uri> selectedUris = new ArrayList<>();

    private FileType selectedFileType = null;
    private FileManager fileManager;
    private FileUploadProgressManager fileUploadProgressManager = new FileUploadProgressManager();

    private static final String TAG = "ChatFragment";

    private static final int TYPING_IDLE_DELAY_MS = 1000;
    private static final int REMOTE_TYPING_HIDE_DELAY_MS = 2500;

    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private final Handler remoteTypingHandler = new Handler(Looper.getMainLooper());

    private boolean isCurrentlyTyping = false;
    private String lastLoggedText = "";

    private Runnable remoteTypingHideRunnable;

    private TextView fileCounterTextView;

    private List<Uri> pendingNewUris = null;
    private FileType pendingNewType = null;
    private LinearLayout timerPanel;
    private TextView timerTextView;
    private View rootView;

    private final Map<UUID, Long> fileSizeCache = new HashMap<>();
    private final Map<UUID, UUID> pendingUploadProgressMap = new HashMap<>();
    private final Object uploadProgressLock = new Object();
    private final ArrayDeque<UUID> pendingUploadUiFileOrder = new ArrayDeque<>();

    private boolean firstMessagesRender = true;

    public static ChatFragment newInstance(@Nullable UUID chatId, @NonNull UUID currentUserId) {
        ChatFragment fragment = new ChatFragment();
        Bundle bundle = new Bundle();

        if (chatId != null) {
            bundle.putString("chatId", chatId.toString());
        }

        bundle.putString("currentUserId", currentUserId.toString());
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);

        Bundle arguments = getArguments();

        if (arguments != null) {
            String id = arguments.getString("chatId");
            chatId = id != null ? UUID.fromString(id) : null;
            currentUserId = UUID.fromString(arguments.getString("currentUserId"));
        }

        initFilePickers();
    }

    private void initializeTimerPanel(@Nullable Chat sourceChat) {
        if (timerPanel == null) return;

        if (sourceChat != null
                && sourceChat.getType() == ChatType.Random
                && sourceChat.isActive()) {
            timerPanel.setVisibility(View.VISIBLE);
            startTimer(sourceChat.getEndTimeFormat());
        } else {
            timerPanel.setVisibility(View.GONE);
        }
    }

    private void startTimer(LocalDateTime endTime){
        Activity activity = getActivity();
        if (activity == null) return;

        isTimerRunning = true;

        Thread timerThread = new Thread(() -> {
            while (isTimerRunning) {
                String time = getTime(endTime);
                if (time == null) {
                    activity.runOnUiThread(() -> {
                        timerPanel.setVisibility(View.GONE);
                        showChatEnded();
                    });
                    break;
                }
                activity.runOnUiThread(() -> {
                    timerTextView.setText(time);
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        timerThread.start();
    }

    private @Nullable String getTime(LocalDateTime targetDateTime){
        LocalDateTime now = LocalDateTime.now();
        if (targetDateTime.isBefore(now) || targetDateTime.equals(now)) {
            Log.d(TAG, "getTime: targetTime is before or equals");
            return null;
        }

        Duration duration = Duration.between(now, targetDateTime);

        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        Log.d(TAG, "getTime: "+ time);

        if (hours > 23 || (hours == 23 && (minutes > 59 || seconds > 59))) {
            return "23:59:59";
        }

        return time;
    }


    private void initFilePickers() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        addSelectedFiles(uris, FileType.MessageFile);
                    }
                }
        );

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        addSelectedFiles(uris, FileType.MessageImage);
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_chat, container, false);

        ChatFragmentThemeBinder.apply(this, rootView);

        FileDownloadProgressManager progressManager = ProgressManagerHolder.INSTANCE;

        fileManager = FileManagerHolder.get(requireContext(), progressManager);
        fileManager.setAudioDownloadListener(this::onAudioDownloaded);

        ui = new ChatUi(rootView, this);
        messagesUi = new ChatMessagesUi(rootView, this);
        membersUi = new ChatMembersUi(rootView, this);
        searchUi = new ChatSearchUi(rootView, this);

        timerPanel = rootView.findViewById(R.id.timer_panel);
        timerTextView = rootView.findViewById(R.id.timer_text);

        messagesUi.setProgressManager(progressManager);

        initFileCounter(rootView);
        initMediaInputController(rootView);

        if (chatId != null) {
            initActions(chatId);
        }

        initTypingTracking();
        loadChatHistory();

        return rootView;
    }

    private void initMediaInputController(@NonNull View root) {
        mediaInputController = new ChatMediaInputController(
                this,
                new ChatMediaInputController.Listener() {
                    @Override
                    public void onVoiceRecorded(@NonNull RecordedChatMedia media) {
                        if (!canUseMultimediaInput()) {
                            showMediaUnsupportedToast();
                            return;
                        }

                        sendRecordedMedia(media);
                    }

                    @Override
                    public void onVideoCircleRequested() {
                        if (!canUseMultimediaInput()) {
                            showMediaUnsupportedToast();
                            return;
                        }

                        launchVideoCircleRecorder();
                    }

                    @Override
                    public void onRecordingStarted() {
                        android.widget.Toast.makeText(
                                getContext(),
                                "Идёт запись. Отпустите, чтобы отправить",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onRecordingCanceled() {
                        android.widget.Toast.makeText(
                                getContext(),
                                "Запись отменена",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onRecordingTooShort() {
                        android.widget.Toast.makeText(
                                getContext(),
                                "Слишком короткая запись",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        mediaInputController.bind(root);
    }

    private void launchVideoCircleRecorder() {
        if (!isAdded() || getContext() == null || getChildFragmentManager().isStateSaved()) {
            return;
        }

        if (!canUseMultimediaInput()) {
            showMediaUnsupportedToast();
            return;
        }

        VideoCircleRecorderDialogFragment dialog = new VideoCircleRecorderDialogFragment();
        dialog.setCallback(new VideoCircleRecorderDialogFragment.Callback() {
            @Override
            public void onVideoCircleRecorded(@NonNull RecordedChatMedia media) {
                if (!canUseMultimediaInput()) {
                    showMediaUnsupportedToast();
                    return;
                }

                sendRecordedMedia(media);
            }

            @Override
            public void onVideoCircleCanceled() {
                android.widget.Toast.makeText(
                        getContext(),
                        "Кружок отменён",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            }
        });

        dialog.show(getChildFragmentManager(), "video_circle_recorder");
    }

    private void initTypingTracking() {
        if (ui == null || ui.getMessageInput() == null) return;

        ui.getMessageInput().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String currentText = s != null ? s.toString() : "";

                if (currentText.equals(lastLoggedText)) return;

                lastLoggedText = currentText;

                if (currentText.trim().isEmpty()) {
                    typingHandler.removeCallbacksAndMessages(null);

                    if (isCurrentlyTyping) {
                        stopTypingSignal();
                    }

                    return;
                }

                if (!isCurrentlyTyping) {
                    startTypingSignal();
                }

                resetTypingIdleTimer(currentText);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ui.getMessageInput().setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && isCurrentlyTyping) {
                typingHandler.removeCallbacksAndMessages(null);
                stopTypingSignal();
            }
        });
    }

    public ChatType getCurrentChatType() {
        if (chat == null) {
            return ChatType.Human;
        }

        return chat.getType();
    }

    private void applyChatInputMode() {
        if (ui == null) return;

        ui.applyChatInputMode(chat != null ? chat.getType() : null);
    }

    private boolean canUseMultimediaInput() {
        ChatType type = getCurrentChatType();
        return type != ChatType.AI && type != ChatType.Random;
    }

    private void showMediaUnsupportedToast() {
        if (!isAdded() || getContext() == null) return;

        Toast.makeText(
                getContext(),
                R.string.chat_media_not_supported,
                Toast.LENGTH_SHORT
        ).show();
    }
    private void resetTypingIdleTimer(final String currentText) {
        typingHandler.removeCallbacksAndMessages(null);

        typingHandler.postDelayed(() -> {
            if (isCurrentlyTyping) {
                stopTypingSignal();
                Log.d(TAG, "User stopped typing by idle timeout. Text length: " + currentText.length());
            }
        }, TYPING_IDLE_DELAY_MS);
    }

    private void startTypingSignal() {
        isCurrentlyTyping = true;
        Log.d(TAG, "User started typing");
        sendTypingState(true);
    }

    private void stopTypingSignal() {
        isCurrentlyTyping = false;
        Log.d(TAG, "User stopped typing");
        sendTypingState(false);
    }

    private void sendTypingState(boolean isTyping) {
        if (chatId == null) return;

        ConnectionDispatcher dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();

        if (dispatcher == null) return;

        try {
            dispatcher.sendSignalRRequestAsync("Typing", new TypingRequest(chatId, isTyping));
        } catch (SignalRNotConnectedException e) {
            Log.w(TAG, "Typing signal was not sent: SignalR is not connected");
        } catch (Exception e) {
            Log.e(TAG, "Typing signal error", e);
        }
    }

    private void cleanupTypingTracking() {
        typingHandler.removeCallbacksAndMessages(null);
        remoteTypingHandler.removeCallbacksAndMessages(null);

        if (isCurrentlyTyping) {
            stopTypingSignal();
        }

        if (ui != null) {
            ui.hideTypingStatus();
        }
    }

    private void registerTypingListener(ConnectionDispatcher dispatcher) {
        if (dispatcher == null) return;

        dispatcher.addEventListener("UserTyping", TypingResponse.class, command -> {
            TypingResponse response = command.getPayload();

            if (response == null) return;
            if (chatId == null || !chatId.equals(response.chatId)) return;
            if (currentUserId != null && currentUserId.equals(response.userId)) return;
            if (!isAdded() || getActivity() == null) return;

            requireActivity().runOnUiThread(() -> handleRemoteTypingResponse(response));
        });
    }

    private void handleRemoteTypingResponse(TypingResponse response) {
        if (ui == null || response == null) return;

        remoteTypingHandler.removeCallbacksAndMessages(null);

        if (response.isTyping) {
            ui.showTypingStatus("печатает...");

            remoteTypingHideRunnable = () -> {
                if (ui != null) {
                    ui.hideTypingStatus();
                }
            };

            remoteTypingHandler.postDelayed(remoteTypingHideRunnable, REMOTE_TYPING_HIDE_DELAY_MS);
        } else {
            ui.hideTypingStatus();
        }
    }

    private void initFileCounter(View root) {
        fileCounterTextView = root.findViewById(R.id.file_counter);

        if (fileCounterTextView != null) {
            updateFileCounter();
            fileCounterTextView.setOnClickListener(v -> showSelectedFilesDialog());
        }
    }

    private void updateFileCounter() {
        if (fileCounterTextView == null) return;

        int count = selectedUris.size();

        if (count > 0) {
            fileCounterTextView.setVisibility(View.VISIBLE);
            fileCounterTextView.setText(String.valueOf(count));
        } else {
            fileCounterTextView.setVisibility(View.GONE);
            selectedFileType = null;
        }
    }

    private boolean canAddMoreFiles() {
        return selectedUris.size() < 10;
    }

    private void addSelectedFiles(List<Uri> newUris, FileType fileType) {
        if (!canUseMultimediaInput()) {
            clearSelectedFiles();
            showMediaUnsupportedToast();
            return;
        }

        if (selectedFileType != null && selectedFileType != fileType) {
            pendingNewUris = new ArrayList<>(newUris);
            pendingNewType = fileType;
            showTypeConflictDialog(fileType);
            return;
        }

        if (!canAddMoreFiles()) {
            showMaxFilesWarning();
            return;
        }

        selectedFileType = fileType;

        Set<Uri> uniqueUris = new HashSet<>(selectedUris);
        int added = 0;

        for (Uri uri : newUris) {
            if (selectedUris.size() >= 10) break;

            if (!uniqueUris.contains(uri)) {
                selectedUris.add(uri);
                uniqueUris.add(uri);
                added++;
            }
        }

        if (added > 0) {
            updateFileCounter();
            Log.d(TAG, "Added " + added + " files. Total: " + selectedUris.size());
        }

        if (added < newUris.size()) {
            showDuplicatesWarning();
        }
    }

    private void showTypeConflictDialog(FileType newType) {
        String currentType = selectedFileType == FileType.MessageFile ? "файлы" : "изображения";
        String newTypeString = newType == FileType.MessageFile ? "файлы" : "изображения";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Смена типа файлов")
                .setMessage("Сейчас выбраны " + currentType + ". Заменить их на " + newTypeString + "?")
                .setPositiveButton("Заменить", (dialog, which) -> {
                    clearSelectedFiles();
                    selectedFileType = pendingNewType;

                    if (pendingNewUris != null && !pendingNewUris.isEmpty()) {
                        List<Uri> urisToAdd = new ArrayList<>(pendingNewUris);
                        FileType typeToAdd = pendingNewType;

                        pendingNewUris = null;
                        pendingNewType = null;

                        addSelectedFiles(urisToAdd, typeToAdd);
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> {
                    pendingNewUris = null;
                    pendingNewType = null;
                })
                .show();
    }

    private void showDuplicatesWarning() {
        android.widget.Toast.makeText(
                getContext(),
                "Некоторые файлы уже были выбраны",
                android.widget.Toast.LENGTH_SHORT
        ).show();
    }

    private void showSelectedFilesDialog() {
        if (selectedUris.isEmpty()) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_selected_files, null);

        RecyclerView recyclerView = dialogView.findViewById(R.id.rv_selected_files);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        InternalFileAdapter adapter = new InternalFileAdapter(
                selectedUris,
                selectedFileType == FileType.MessageImage,
                new InternalFileAdapter.OnFileInteractionListener() {
                    @Override
                    public void onFileClick(Uri uri) {
                        fileManager.openLocalFile(uri);
                    }

                    @Override
                    public void onDeleteClick(Uri uri) {
                        selectedUris.remove(uri);
                        updateFileCounter();

                        if (selectedUris.isEmpty()) {
                            selectedFileType = null;
                        }

                        if (recyclerView.getAdapter() != null) {
                            recyclerView.getAdapter().notifyDataSetChanged();
                        }

                        AlertDialog dialog = (AlertDialog) recyclerView.getTag();

                        if (dialog != null) {
                            dialog.setTitle("Выбранные файлы (" + selectedUris.size() + "/10)");
                        }

                        if (selectedUris.isEmpty() && dialog != null) {
                            dialog.dismiss();
                        }
                    }
                },
                requireContext()
        );

        recyclerView.setAdapter(adapter);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Выбранные файлы (" + selectedUris.size() + "/10)")
                .setView(dialogView)
                .setPositiveButton("Очистить все", (d, which) -> {
                    clearSelectedFiles();
                    d.dismiss();
                })
                .setNegativeButton("Закрыть", null)
                .create();

        recyclerView.setTag(dialog);
        dialog.show();
    }

    public void downloadFile(
            String url,
            String mimeType,
            UUID fileId,
            FileType fileType,
            boolean openAfterDownload
    ) {
        String token = getAuthToken();

        boolean isVideo = fileType == FileType.VideoMessage
                || (mimeType != null && mimeType.toLowerCase().startsWith("video"));

        if (openAfterDownload && isVideo) {
            fileManager.downloadFile(url, mimeType, fileId, fileType, token)
                    .thenAccept(file -> {
                        if (file == null || !file.exists()) {
                            return;
                        }

                        runOnUiThreadSafe(() -> openDownloadedVideo(file, mimeType));
                    });

            return;
        }

        if (openAfterDownload) {
            fileManager.downloadAndOpenFile(url, mimeType, fileId, fileType, token);
            return;
        }

        fileManager.downloadFile(url, mimeType, fileId, fileType, token);
    }

    private void openDownloadedVideo(@NonNull File file, @Nullable String mimeType) {
        if (!isAdded() || getContext() == null || !file.exists()) {
            return;
        }

        try {
            android.content.Intent intent = new android.content.Intent(
                    requireContext(),
                    VideoPlayerActivity.class
            );

            intent.putExtra(VideoPlayerActivity.EXTRA_LOCAL_PATH, file.getAbsolutePath());
            intent.putExtra(VideoPlayerActivity.EXTRA_TITLE, file.getName());
            intent.putExtra(
                    VideoPlayerActivity.EXTRA_MIME_TYPE,
                    mimeType != null && !mimeType.trim().isEmpty() ? mimeType : "video/mp4"
            );
            intent.putExtra(VideoPlayerActivity.EXTRA_ENGINE, VideoPlayerActivity.ENGINE_EXO_PLAYER);

            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Cannot open downloaded video", e);

            android.widget.Toast.makeText(
                    getContext(),
                    "Не удалось открыть видео",
                    android.widget.Toast.LENGTH_SHORT
            ).show();
        }
    }

    private String getAuthToken() {
        try {
            TokenStorage tokenStorage = new TokenStorage(requireContext());
            return tokenStorage.getToken();
        } catch (Exception e) {
            Log.e(TAG, "Error getting auth token", e);
            return null;
        }
    }

    private void registerPendingUploadProgressTarget(@NonNull UUID localFileId) {
        synchronized (uploadProgressLock) {
            pendingUploadProgressMap.put(localFileId, localFileId);

            if (!pendingUploadUiFileOrder.contains(localFileId)) {
                pendingUploadUiFileOrder.offer(localFileId);
            }
        }
    }

    private UUID resolveUploadProgressUiFileId(@Nullable UUID progressFileId, int percent) {
        if (progressFileId == null) {
            synchronized (uploadProgressLock) {
                UUID fallback = pendingUploadUiFileOrder.peekFirst();
                return fallback != null ? fallback : UUID.randomUUID();
            }
        }

        synchronized (uploadProgressLock) {
            UUID mapped = pendingUploadProgressMap.get(progressFileId);

            if (mapped == null) {
                mapped = pendingUploadUiFileOrder.peekFirst();

                if (mapped == null) {
                    mapped = progressFileId;
                }

                pendingUploadProgressMap.put(progressFileId, mapped);
            }

            if (percent >= 100) {
                pendingUploadUiFileOrder.remove(mapped);
            }

            return mapped;
        }
    }

    private void clearPendingUploadProgressTarget(@Nullable UUID id) {
        if (id == null) {
            return;
        }

        synchronized (uploadProgressLock) {
            pendingUploadProgressMap.remove(id);

            List<UUID> keysToRemove = new ArrayList<>();
            for (Map.Entry<UUID, UUID> entry : pendingUploadProgressMap.entrySet()) {
                if (id.equals(entry.getValue())) {
                    keysToRemove.add(entry.getKey());
                }
            }

            for (UUID key : keysToRemove) {
                pendingUploadProgressMap.remove(key);
            }

            pendingUploadUiFileOrder.remove(id);
        }
    }

    private void clearPendingUploadProgressTargets(@NonNull List<UUID> ids) {
        for (UUID id : ids) {
            clearPendingUploadProgressTarget(id);
        }
    }

    private void initActions(UUID id) {
        messageActions = new ChatMessageActions(this, id, currentUserId);
        membersActions = new ChatMembersActions(this, id, currentUserId);
        stateActions = new ChatStateActions(this, chatId, currentUserId);

        ConnectionDispatcher dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();

        if (dispatcher != null) {
            dispatcher.addEventListener("MessageSent", MessageResponse.class, command -> {
                MessageResponse response = command.getPayload();

                if (response != null && chatId != null && chatId.equals(response.chatId) && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        messageActions.onMessageReceived(response);


                        try {
                            Message directMessage = new com.example.aichat.model.utils.mappers.MessageMapper(currentUserId)
                                    .ToModel(response);

                            if (directMessage != null) {
                                addOrUpdateMessage(directMessage);
                            }
                        } catch (Exception mappingError) {
                            Log.e(TAG, "Cannot map MessageSent directly", mappingError);
                        }

                        MessageAdapter adapter = messagesUi != null ? messagesUi.getAdapter() : null;
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();

                            try {
                                Message directMessage = new com.example.aichat.model.utils.mappers.MessageMapper(currentUserId)
                                        .ToModel(response);

                                adapter.prefetchMessageFiles(directMessage);
                            } catch (Exception prefetchError) {
                                Log.e(TAG, "Cannot start immediate MessageSent file prefetch", prefetchError);
                            }

                            adapter.scheduleVisibleFilePrefetch();
                        }
                    });
                }
            });

            dispatcher.addEventListener("MessageStatusUpdated", UpdateMessageStatusResponse.class, command -> {
                UpdateMessageStatusResponse response = command.getPayload();

                if (response != null && chatId != null && chatId.equals(response.chatId) && isAdded()) {
                    requireActivity().runOnUiThread(() -> messageActions.onMessageStatusUpdated(response));
                }
            });

            dispatcher.addEventListener("ChatEnded", ChatEndedResponse.class, command -> {
                ChatEndedResponse response = command.getPayload();

                if (response != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> onChatEnded(response.chatId));
                }
            });

            dispatcher.addEventListener("OnlineStatusChanged", UserOnlineChangesResponse.class, command -> {
                UserOnlineChangesResponse response = command.getPayload();

                if (response != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> updateOnline(response.userId, response.lastOnline));
                }
            });

            dispatcher.addEventListener("ChatNameUpdated", ChatNameUpdatedResponse.class, command -> {
                ChatNameUpdatedResponse response = command.getPayload();

                if (response != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> onChatNameUpdated(response.chatId, response.name));
                }
            });

            registerTypingListener(dispatcher);
        }

        UploadProgressListener uploadListener = progress -> {
            UUID progressFileId = progress.getFileId();
            int percent = Math.max(1, Math.min(99, progress.getPercent()));
            UUID uiFileId = resolveUploadProgressUiFileId(progressFileId, percent);

            fileUploadProgressManager.updateProgress(uiFileId, percent);
        };

        sendMessageController = new SendMessageController(requireContext(), dispatcher, uploadListener);

        ui.enableSendButton();
    }

    public void onChatEnded(UUID endedChatId) {
        if (!isAdded() || getActivity() == null) return;

        if (chatId != null && chatId.equals(endedChatId)) {
            Log.d(TAG, "onChatEnded: chat " + endedChatId + " has been ended");

            if (chat != null && chat.getEndTime() == null) {
                chat.setEndTime(
                        com.example.aichat.model.utils.time.TimeConverter.getString(
                                java.time.LocalDateTime.now()
                        )
                );
            }

            requireActivity().runOnUiThread(() -> {
                showChatEnded();
                ui.disableMessageInput();
                ui.clearMessageInput();

                android.widget.Toast.makeText(
                        getContext(),
                        "Чат завершён",
                        android.widget.Toast.LENGTH_SHORT
                ).show();

                Log.d(TAG, "Chat ended UI updated");
            });
        }
    }

    public void showAttachBottomSheet() {
        if (!canUseMultimediaInput()) {
            clearSelectedFiles();
            showMediaUnsupportedToast();
            return;
        }

        showFileTypeChoiceDialog();
    }

    private void showFileTypeChoiceDialog() {
        if (!isAdded() || getContext() == null) return;

        if (!canUseMultimediaInput()) {
            clearSelectedFiles();
            showMediaUnsupportedToast();
            return;
        }

        if (!canAddMoreFiles()) {
            showMaxFilesWarning();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Прикрепить")
                .setItems(
                        new String[]{
                                "Изображения",
                                "Файлы"
                        },
                        (dialog, which) -> {
                            if (!canAddMoreFiles()) {
                                showMaxFilesWarning();
                                return;
                            }

                            if (which == 0) {
                                imagePickerLauncher.launch("image/*");
                            } else {
                                filePickerLauncher.launch(
                                        new String[]{
                                                "application/*",
                                                "text/*",
                                                "audio/*",
                                                "video/*"
                                        }
                                );
                            }
                        }
                )
                .show();
    }

    private void showMaxFilesWarning() {
        android.widget.Toast.makeText(
                getContext(),
                "Максимум 10 файлов можно прикрепить к одному сообщению",
                android.widget.Toast.LENGTH_SHORT
        ).show();
    }

    private File getFileFromUri(Uri uri) {
        try {
            String fileName = getFileNameFromUri(uri);
            File file = new File(requireContext().getCacheDir(), fileName);

            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[8192];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            return file;
        } catch (Exception e) {
            Log.e(TAG, "Error converting Uri to File", e);
            return null;
        }
    }

    public void setFileSize(UUID fileId, long size) {
        if (size > 0) {
            fileSizeCache.put(fileId, size);
        }
    }

    @NonNull
    private HashMap<UUID, MessageStatus> createLocalSendingStatuses() {
        HashMap<UUID, MessageStatus> statuses = new HashMap<>();

        if (currentUserId != null) {
            statuses.put(
                    currentUserId,
                    MessageStatus.SENDING
            );
        }

        return statuses;
    }

    private void sendRecordedMedia(@NonNull RecordedChatMedia media) {
        if (!canUseMultimediaInput()) {
            showMediaUnsupportedToast();
            return;
        }

        if (chatId == null || currentUserId == null || sendMessageController == null) {
            return;
        }

        File uploadFile = media.getFile();

        if (uploadFile == null || !uploadFile.exists() || uploadFile.length() <= 0) {
            android.widget.Toast.makeText(
                    getContext(),
                    "Файл записи не найден",
                    android.widget.Toast.LENGTH_SHORT
            ).show();
            return;
        }

        UUID messageId = UUID.randomUUID();
        UUID localFileId = UUID.randomUUID();
        String originalName = media.getFileName();

        if (originalName == null || originalName.trim().isEmpty()) {
            originalName = uploadFile.getName();
        }

        File localCacheFile = copyFileToPersistentSentCache(uploadFile, localFileId, originalName);

        if (localCacheFile == null || !localCacheFile.exists()) {
            localCacheFile = uploadFile;
        }

        String mimeType = media.getMimeType();
        FileType localFileType = media.getFileType();
        FileType uploadFileType = resolveServerUploadFileType(localFileType, mimeType, originalName);

        HashMap<UUID, FileType> localFileTypes = new HashMap<>();
        HashMap<UUID, String> localMimeTypes = new HashMap<>();
        List<UUID> localFileIds = new ArrayList<>();
        List<File> files = new ArrayList<>();
        List<FileType> types = new ArrayList<>();

        localFileIds.add(localFileId);
        files.add(uploadFile);
        types.add(uploadFileType);
        localFileTypes.put(localFileId, localFileType);
        localMimeTypes.put(localFileId, mimeType);

        String textToSend = com.example.aichat.model.utils.media.ChatMediaMarkers.buildFileMarker(localFileId, originalName);
        final String finalTextToSend = textToSend;
        final String finalOriginalName = originalName;

        savePendingFileToDatabase(localFileId, localCacheFile, mimeType, localFileType, originalName);
        cachePendingFileInAdapter(localFileId, localCacheFile, mimeType, localFileType, originalName);

        registerPendingUploadProgressTarget(localFileId);
        synchronized (uploadProgressLock) {
            pendingUploadProgressMap.put(messageId, localFileId);
        }
        fileUploadProgressManager.updateProgress(localFileId, 1);

        MessageInfo info = new MessageInfo(
                messageId,
                chatId,
                textToSend,
                new ArrayList<>(),
                types,
                files,
                localFileIds,
                java.util.Collections.singletonList(finalOriginalName)
        );

        Message localMessage = new Message(
                messageId,
                textToSend,
                currentUserId,
                chatId,
                com.example.aichat.model.utils.time.TimeConverter.getString(java.time.LocalDateTime.now()),
                com.example.aichat.model.utils.time.TimeConverter.getString(java.time.LocalDateTime.now()),
                new ArrayList<>(),
                createLocalSendingStatuses(),
                localFileIds,
                localMimeTypes,
                localFileTypes
        );

        addOrUpdateMessage(localMessage);

        new Thread(() -> DatabaseManager.getDatabase().messageDao().upsertMessage(localMessage)).start();

        sendRecordedMediaAttempt(
                info,
                localFileId,
                messageId,
                localFileIds,
                java.util.Collections.singletonList(localCacheFile),
                localMimeTypes,
                localFileTypes,
                java.util.Collections.singletonList(finalOriginalName),
                finalTextToSend,
                finalOriginalName,
                localFileType,
                0
        );
    }


    private void sendRecordedMediaAttempt(
            @NonNull MessageInfo info,
            @NonNull UUID localFileId,
            @NonNull UUID messageId,
            @NonNull List<UUID> localFileIds,
            @NonNull List<File> localFiles,
            @NonNull Map<UUID, String> localMimeTypes,
            @NonNull Map<UUID, FileType> localFileTypes,
            @NonNull List<String> originalNames,
            @NonNull String finalTextToSend,
            @NonNull String finalOriginalName,
            @NonNull FileType localFileType,
            int attempt
    ) {
        sendMessageController.sendMessage(info)
                .thenAccept(cmd -> {
                    new Thread(() -> {
                        if (cmd == null || !cmd.isSuccess()) {
                            if (attempt < 1 && canTouchChatUi()) {
                                requireActivity().runOnUiThread(() -> {
                                    fileUploadProgressManager.updateProgress(localFileId, 1);
                                    new Handler(Looper.getMainLooper()).postDelayed(
                                            () -> sendRecordedMediaAttempt(
                                                    info,
                                                    localFileId,
                                                    messageId,
                                                    localFileIds,
                                                    localFiles,
                                                    localMimeTypes,
                                                    localFileTypes,
                                                    originalNames,
                                                    finalTextToSend,
                                                    finalOriginalName,
                                                    localFileType,
                                                    attempt + 1
                                            ),
                                            900L
                                    );
                                });
                                return;
                            }

                            postUploadProgressError(localFileId);

                            if (canTouchChatUi()) {
                                requireActivity().runOnUiThread(() ->
                                        android.widget.Toast.makeText(
                                                getContext(),
                                                "Ошибка отправки записи",
                                                android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                );
                            }

                            return;
                        }

                        try {
                            MessageResponse response = cmd.getData(MessageResponse.class);

                            if (response == null) {
                                throw new IllegalStateException("Empty message response");
                            }

                            Message updated = new com.example.aichat.model.utils.mappers.MessageMapper(currentUserId)
                                    .ToModel(response);

                            UUID serverFileId = null;

                            if (response.files != null && !response.files.isEmpty()) {
                                saveServerFileAliasesFromLocalFiles(
                                        localFileIds,
                                        response.files,
                                        localFiles,
                                        localMimeTypes,
                                        localFileTypes,
                                        originalNames
                                );
                                bindUploadProgressAliases(localFileIds, response.files);
                                serverFileId = response.files.get(0);
                            }

                            if (updated != null && !finalTextToSend.isEmpty()) {
                                updated.setText(
                                        buildServerTextFromLocalMarkers(
                                                finalTextToSend,
                                                localFileIds,
                                                response.files
                                        )
                                );
                            }

                            if (updated != null) {
                                DatabaseManager.getDatabase().messageDao().upsertMessage(updated);
                            }


                            final UUID finalServerFileId = serverFileId;
                            final com.example.aichat.model.entities.File finalServerAlias =
                                    finalServerFileId != null
                                            ? DatabaseManager.getDatabase().fileDao().getById(finalServerFileId)
                                            : null;

                            if (!canTouchChatUi()) {
                                postUploadProgressCompletion(messageId, localFileIds, response.files);
                                return;
                            }

                            requireActivity().runOnUiThread(() -> {
                                if (finalServerFileId != null) {
                                    fileUploadProgressManager.bindAlias(localFileId, finalServerFileId);
                                    fileUploadProgressManager.complete(finalServerFileId);
                                }
                                fileUploadProgressManager.complete(localFileId);

                                MessageAdapter adapter = messagesUi != null ? messagesUi.getAdapter() : null;

                                if (adapter != null && finalServerFileId != null) {
                                    adapter.clearFailedRemoteLoads();
                                    adapter.setLocalFileName(finalServerFileId, finalOriginalName);

                                    if (finalServerAlias != null
                                            && finalServerAlias.localPath != null
                                            && !finalServerAlias.localPath.trim().isEmpty()) {
                                        java.io.File cachedFile = new java.io.File(finalServerAlias.localPath);

                                        if (cachedFile.exists()) {
                                            adapter.cacheLocalFile(
                                                    finalServerFileId,
                                                    finalServerAlias.localPath,
                                                    finalServerAlias.mimeType,
                                                    localFileType,
                                                    cachedFile.length(),
                                                    finalOriginalName
                                            );
                                        }
                                    }
                                }

                                if (updated != null) {
                                    if (adapter != null) {
                                        adapter.replaceLocalMessageWithServerMessage(messageId, updated);
                                    } else {
                                        addOrUpdateMessage(updated);
                                    }
                                }

                                clearPendingUploadProgressTarget(localFileId);
                                synchronized (uploadProgressLock) {
                                    pendingUploadProgressMap.remove(messageId);
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to process recorded media response", e);

                            postUploadProgressError(localFileId);

                            if (canTouchChatUi()) {
                                requireActivity().runOnUiThread(() ->
                                        android.widget.Toast.makeText(
                                                getContext(),
                                                "Ошибка обработки отправленной записи",
                                                android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                );
                            }
                        }
                    }).start();
                })
                .exceptionally(e -> {
                    Log.e(TAG, "Failed to send recorded media, attempt=" + attempt, e);

                    if (canTouchChatUi() && attempt < 1) {
                        requireActivity().runOnUiThread(() -> {
                            fileUploadProgressManager.updateProgress(localFileId, 1);
                            new Handler(Looper.getMainLooper()).postDelayed(
                                    () -> sendRecordedMediaAttempt(
                                            info,
                                            localFileId,
                                            messageId,
                                            localFileIds,
                                            localFiles,
                                            localMimeTypes,
                                            localFileTypes,
                                            originalNames,
                                            finalTextToSend,
                                            finalOriginalName,
                                            localFileType,
                                            attempt + 1
                                    ),
                                    900L
                            );
                        });
                        return null;
                    }

                    postUploadProgressError(localFileId);

                    if (canTouchChatUi()) {
                        requireActivity().runOnUiThread(() ->
                                android.widget.Toast.makeText(
                                        getContext(),
                                        "Ошибка отправки записи",
                                        android.widget.Toast.LENGTH_SHORT
                                ).show()
                        );
                    }

                    return null;
                });
    }

    private void sendAllStoredFilesWithText(String textMessage) {
        if (selectedUris == null || selectedUris.isEmpty()) return;

        if (selectedFileType == null) {
            android.widget.Toast.makeText(
                    getContext(),
                    "Не выбран тип файлов",
                    android.widget.Toast.LENGTH_SHORT
            ).show();

            return;
        }

        final List<Uri> urisToSend = new ArrayList<>(selectedUris);
        final FileType selectedTypeToSend = selectedFileType;
        clearSelectedFiles();

        UUID messageId = UUID.randomUUID();

        List<File> allFiles = new ArrayList<>();
        List<File> localCacheFiles = new ArrayList<>();
        List<String> originalNames = new ArrayList<>();
        List<UUID> localFileIds = new ArrayList<>();
        HashMap<UUID, FileType> localFileTypes = new HashMap<>();
        HashMap<UUID, String> localMimeTypes = new HashMap<>();
        StringBuilder fileTagsBuilder = new StringBuilder();

        for (int i = 0; i < urisToSend.size(); i++) {
            Uri uri = urisToSend.get(i);
            UUID localFileId = i == 0 ? messageId : UUID.randomUUID();

            File file = fileManager.copyUriToUploadCache(uri, localFileId);

            if (file == null || !file.exists()) continue;

            String originalName = getFileNameFromUri(uri);

            if (originalName == null || originalName.trim().isEmpty()) {
                originalName = file.getName();
            }

            String mimeType = fileManager.getMimeTypeFromFile(file);
            FileType fileType = resolveFileType(mimeType, originalName, selectedTypeToSend);

            File localCacheFile = copyFileToPersistentSentCache(file, localFileId, originalName);

            if (localCacheFile == null || !localCacheFile.exists()) {
                localCacheFile = file;
            }

            allFiles.add(file);
            localCacheFiles.add(localCacheFile);
            originalNames.add(originalName);
            localFileIds.add(localFileId);
            localFileTypes.put(localFileId, fileType);
            localMimeTypes.put(localFileId, mimeType);

            fileTagsBuilder
                    .append(ChatMediaMarkers.buildFileMarker(localFileId, originalName))
                    .append(" ");

            savePendingFileToDatabase(localFileId, localCacheFile, mimeType, fileType, originalName);
            cachePendingFileInAdapter(localFileId, localCacheFile, mimeType, fileType, originalName);

            registerPendingUploadProgressTarget(localFileId);
            fileUploadProgressManager.updateProgress(localFileId, 1);
        }

        if (allFiles.isEmpty()) return;

        if (!localFileIds.isEmpty()) {
            synchronized (uploadProgressLock) {
                pendingUploadProgressMap.put(messageId, localFileIds.get(0));
            }
        }

        String textToSend = fileTagsBuilder.toString();
        String cleanUserText = textMessage != null ? textMessage.trim() : "";

        if (!cleanUserText.isEmpty()) {
            textToSend = cleanUserText + " " + textToSend;
        }

        final String finalTextToSend = textToSend;
        final List<String> finalOriginalNames = new ArrayList<>(originalNames);

        List<FileType> types = new ArrayList<>(allFiles.size());

        for (UUID fileId : localFileIds) {
            FileType type = localFileTypes.get(fileId);
            types.add(type != null ? type : selectedTypeToSend);
        }

        MessageInfo info = new MessageInfo(
                messageId,
                chatId,
                finalTextToSend,
                new ArrayList<>(),
                types,
                allFiles,
                localFileIds,
                finalOriginalNames
        );

        Message localMessage = new Message(
                messageId,
                finalTextToSend,
                currentUserId,
                chatId,
                com.example.aichat.model.utils.time.TimeConverter.getString(java.time.LocalDateTime.now()),
                com.example.aichat.model.utils.time.TimeConverter.getString(java.time.LocalDateTime.now()),
                new ArrayList<>(),
                createLocalSendingStatuses(),
                localFileIds,
                localMimeTypes,
                localFileTypes
        );

        addOrUpdateMessage(localMessage);

        new Thread(() -> DatabaseManager.getDatabase().messageDao().upsertMessage(localMessage)).start();

        sendMessageController.sendMessage(info)
                .thenAccept(cmd -> {
                    new Thread(() -> {
                        if (cmd == null || !cmd.isSuccess()) {
                            postUploadProgressError(localFileIds);

                            if (canTouchChatUi()) {
                                requireActivity().runOnUiThread(() ->
                                        android.widget.Toast.makeText(
                                                getContext(),
                                                "Ошибка отправки файлов",
                                                android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                );
                            }

                            return;
                        }

                        try {
                            MessageResponse response = cmd.getData(MessageResponse.class);

                            Message updated = new com.example.aichat.model.utils.mappers.MessageMapper(currentUserId)
                                    .ToModel(response);

                            if (updated != null && !finalTextToSend.isEmpty()) {
                                updated.setText(
                                        buildServerTextFromLocalMarkers(
                                                finalTextToSend,
                                                localFileIds,
                                                response.files
                                        )
                                );
                            }

                            saveServerFileAliasesFromLocalFiles(
                                    localFileIds,
                                    response.files,
                                    localCacheFiles,
                                    localMimeTypes,
                                    localFileTypes,
                                    finalOriginalNames
                            );
                            bindUploadProgressAliases(localFileIds, response.files);
                            DatabaseManager.getDatabase().messageDao().upsertMessage(updated);

                            final List<com.example.aichat.model.entities.File> serverAliases = new ArrayList<>();

                            if (response.files != null) {
                                for (UUID serverFileId : response.files) {
                                    com.example.aichat.model.entities.File alias =
                                            DatabaseManager.getDatabase().fileDao().getById(serverFileId);
                                    serverAliases.add(alias);
                                }
                            }

                            if (!canTouchChatUi()) {
                                postUploadProgressCompletion(messageId, localFileIds, response.files);
                                return;
                            }

                            requireActivity().runOnUiThread(() -> {
                                if (response.files != null) {
                                    int aliasCount = Math.min(localFileIds.size(), response.files.size());
                                    for (int i = 0; i < aliasCount; i++) {
                                        UUID localId = localFileIds.get(i);
                                        UUID serverId = response.files.get(i);
                                        fileUploadProgressManager.bindAlias(localId, serverId);
                                        fileUploadProgressManager.complete(serverId);
                                    }
                                }

                                for (UUID fileId : localFileIds) {
                                    fileUploadProgressManager.complete(fileId);
                                }

                                MessageAdapter adapter = messagesUi.getAdapter();

                                if (adapter != null && response.files != null) {
                                    adapter.clearFailedRemoteLoads();

                                    for (int i = 0; i < response.files.size(); i++) {
                                        if (i < finalOriginalNames.size()) {
                                            UUID serverFileId = response.files.get(i);
                                            String originalName = finalOriginalNames.get(i);

                                            adapter.setLocalFileName(
                                                    serverFileId,
                                                    originalName
                                            );

                                            if (i < serverAliases.size()) {
                                                com.example.aichat.model.entities.File alias = serverAliases.get(i);

                                                if (alias != null
                                                        && alias.localPath != null
                                                        && !alias.localPath.trim().isEmpty()) {
                                                    File cachedFile = new File(alias.localPath);

                                                    if (cachedFile.exists()) {
                                                        adapter.cacheLocalFile(
                                                                serverFileId,
                                                                alias.localPath,
                                                                alias.mimeType,
                                                                localFileTypes.get(i < localFileIds.size() ? localFileIds.get(i) : null),
                                                                cachedFile.length(),
                                                                originalName
                                                        );
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (adapter != null) {
                                    adapter.replaceLocalMessageWithServerMessage(messageId, updated);
                                } else {
                                    addOrUpdateMessage(updated);
                                }

                                clearPendingUploadProgressTargets(localFileIds);

                                synchronized (uploadProgressLock) {
                                    pendingUploadProgressMap.remove(messageId);
                                }
                                clearSelectedFiles();
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to process uploaded files response", e);
                            postUploadProgressError(localFileIds);
                        }
                    }).start();
                })
                .exceptionally(e -> {
                    e.printStackTrace();

                    postUploadProgressError(localFileIds);

                    if (canTouchChatUi()) {
                        requireActivity().runOnUiThread(() ->
                                android.widget.Toast.makeText(
                                        getContext(),
                                        "Ошибка отправки файлов",
                                        android.widget.Toast.LENGTH_SHORT
                                ).show()
                        );
                    }

                    return null;
                });
    }


    private boolean canTouchChatUi() {
        return isAdded() && getActivity() != null && getContext() != null;
    }

    private void postUploadProgressError(@NonNull List<UUID> fileIds) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (UUID fileId : fileIds) {
                fileUploadProgressManager.error(fileId);
            }
        });
    }

    private void postUploadProgressError(@NonNull UUID fileId) {
        new Handler(Looper.getMainLooper()).post(() -> fileUploadProgressManager.error(fileId));
    }

    private void postUploadProgressCompletion(
            @Nullable UUID messageId,
            @NonNull List<UUID> localFileIds,
            @Nullable List<UUID> serverFileIds
    ) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (serverFileIds != null) {
                int aliasCount = Math.min(localFileIds.size(), serverFileIds.size());

                for (int i = 0; i < aliasCount; i++) {
                    UUID localId = localFileIds.get(i);
                    UUID serverId = serverFileIds.get(i);

                    fileUploadProgressManager.bindAlias(localId, serverId);
                    fileUploadProgressManager.complete(serverId);
                }
            }

            for (UUID fileId : localFileIds) {
                fileUploadProgressManager.complete(fileId);
            }

            clearPendingUploadProgressTargets(localFileIds);

            if (messageId != null) {
                synchronized (uploadProgressLock) {
                    pendingUploadProgressMap.remove(messageId);
                }
            }
        });
    }


    @Nullable
    private File copyFileToPersistentSentCache(
            @NonNull File source,
            @NonNull UUID fileId,
            @Nullable String originalName
    ) {
        if (!source.exists() || source.length() <= 0) {
            return null;
        }

        Context context = getContext();

        if (context == null) {
            return null;
        }

        String safeName = sanitizeCacheFileName(
                originalName != null && !originalName.trim().isEmpty()
                        ? originalName
                        : source.getName()
        );

        File sentDir = new File(context.getCacheDir(), "sent_files");

        if (!sentDir.exists() && !sentDir.mkdirs()) {
            Log.e(TAG, "Cannot create sent files cache directory: " + sentDir.getAbsolutePath());
            return null;
        }

        File target = new File(sentDir, fileId + "_" + safeName);

        try (InputStream input = new FileInputStream(source);
             OutputStream output = new FileOutputStream(target, false)) {

            byte[] buffer = new byte[64 * 1024];
            int length;

            while ((length = input.read(buffer)) != -1) {
                if (length > 0) {
                    output.write(buffer, 0, length);
                }
            }

            output.flush();
        } catch (Exception e) {
            Log.e(TAG, "copyFileToPersistentSentCache error", e);
            target.delete();
            return null;
        }

        if (!target.exists() || target.length() <= 0) {
            target.delete();
            return null;
        }

        return target;
    }

    @NonNull
    private String sanitizeCacheFileName(@NonNull String fileName) {
        String sanitized = fileName
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ")
                .trim();

        if (sanitized.isEmpty()) {
            return "file_" + System.currentTimeMillis();
        }

        return sanitized;
    }


    private void cachePendingFileInAdapter(
            UUID fileId,
            File file,
            String mimeType,
            FileType fileType,
            String fileName
    ) {
        if (fileId == null || file == null || !file.exists() || messagesUi == null) {
            return;
        }

        MessageAdapter adapter = messagesUi.getAdapter();

        if (adapter == null) {
            return;
        }

        adapter.cacheLocalFile(
                fileId,
                file.getAbsolutePath(),
                mimeType,
                fileType,
                file.length(),
                fileName
        );
    }

    private void bindUploadProgressAliases(List<UUID> localFileIds, List<UUID> serverFileIds) {
        if (localFileIds == null || serverFileIds == null) {
            return;
        }

        int count = Math.min(localFileIds.size(), serverFileIds.size());

        for (int i = 0; i < count; i++) {
            UUID localFileId = localFileIds.get(i);
            UUID serverFileId = serverFileIds.get(i);

            if (localFileId == null || serverFileId == null) {
                continue;
            }

            fileUploadProgressManager.bindAlias(localFileId, serverFileId);
        }
    }

    @NonNull
    private String buildServerTextFromLocalMarkers(
            @Nullable String text,
            @Nullable List<UUID> localFileIds,
            @Nullable List<UUID> serverFileIds
    ) {
        if (text == null || text.isEmpty() || localFileIds == null || serverFileIds == null) {
            return text == null ? "" : text;
        }

        int count = Math.min(localFileIds.size(), serverFileIds.size());
        Map<UUID, UUID> idMap = new HashMap<>();

        for (int i = 0; i < count; i++) {
            UUID localId = localFileIds.get(i);
            UUID serverId = serverFileIds.get(i);

            if (localId != null && serverId != null) {
                idMap.put(localId, serverId);
            }
        }

        return ChatMediaMarkers.replaceFileMarkerIds(
                text,
                idMap
        );
    }

    private void saveServerFileAliasesFromLocalFiles(
            List<UUID> localFileIds,
            List<UUID> serverFileIds,
            List<File> localFiles,
            Map<UUID, String> localMimeTypes,
            Map<UUID, FileType> localFileTypes,
            List<String> originalNames
    ) {
        if (localFileIds == null || serverFileIds == null || localFiles == null) {
            return;
        }

        int count = Math.min(Math.min(localFileIds.size(), serverFileIds.size()), localFiles.size());
        long now = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            UUID localFileId = localFileIds.get(i);
            UUID serverFileId = serverFileIds.get(i);
            File localFile = localFiles.get(i);

            if (localFileId == null || serverFileId == null || localFile == null || !localFile.exists()) {
                continue;
            }

            String mimeType = localMimeTypes != null ? localMimeTypes.get(localFileId) : null;
            FileType fileType = localFileTypes != null ? localFileTypes.get(localFileId) : null;
            String fileName = originalNames != null && i < originalNames.size()
                    ? originalNames.get(i)
                    : localFile.getName();

            DatabaseManager.getDatabase()
                    .fileDao()
                    .insert(
                            new com.example.aichat.model.entities.File(
                                    serverFileId,
                                    localFile.getAbsolutePath(),
                                    mimeType,
                                    fileType != null ? fileType.name() : null,
                                    localFile.length(),
                                    now,
                                    now,
                                    fileName
                            )
                    );
        }
    }

    private void saveServerFileAliases(List<UUID> localFileIds, List<UUID> serverFileIds) {
        if (localFileIds == null || serverFileIds == null) {
            return;
        }

        int count = Math.min(localFileIds.size(), serverFileIds.size());

        for (int i = 0; i < count; i++) {
            UUID localFileId = localFileIds.get(i);
            UUID serverFileId = serverFileIds.get(i);

            if (localFileId == null || serverFileId == null) {
                continue;
            }

            com.example.aichat.model.entities.File localFile =
                    DatabaseManager.getDatabase().fileDao().getById(localFileId);

            if (localFile == null
                    || localFile.localPath == null
                    || localFile.localPath.trim().isEmpty()) {
                continue;
            }

            File actualFile = new File(localFile.localPath);

            if (!actualFile.exists()) {
                continue;
            }

            long now = System.currentTimeMillis();

            DatabaseManager.getDatabase()
                    .fileDao()
                    .insert(
                            new com.example.aichat.model.entities.File(
                                    serverFileId,
                                    localFile.localPath,
                                    localFile.mimeType,
                                    localFile.fileType,
                                    actualFile.length(),
                                    now,
                                    now,
                                    localFile.fileName
                            )
                    );
        }
    }

    private void savePendingFileToDatabase(UUID fileId, File file, String mimeType, FileType fileType, String fileName) {
        new Thread(() -> {
            long now = System.currentTimeMillis();

            DatabaseManager.getDatabase()
                    .fileDao()
                    .insert(
                            new com.example.aichat.model.entities.File(
                                    fileId,
                                    file.getAbsolutePath(),
                                    mimeType,
                                    fileType != null ? fileType.name() : null,
                                    file.length(),
                                    now,
                                    now,
                                    fileName
                            )
                    );
        }).start();
    }


    private boolean isVideoFile(String fileName) {
        if (fileName == null) {
            return false;
        }

        String lower = fileName.trim().toLowerCase();

        return lower.endsWith(".mp4")
                || lower.endsWith(".m4v")
                || lower.endsWith(".mov")
                || lower.endsWith(".mkv")
                || lower.endsWith(".webm")
                || lower.endsWith(".avi")
                || lower.endsWith(".3gp")
                || lower.endsWith(".3gpp");
    }

    private FileType resolveServerUploadFileType(FileType localFileType, String mimeType, String fileName) {
        if (localFileType == FileType.MessageImage) {
            return FileType.MessageImage;
        }


        if (localFileType == FileType.VoiceMessage) {
            return FileType.VoiceMessage;
        }

        if (localFileType == FileType.VideoMessage
                || (mimeType != null && mimeType.startsWith("video"))
                || isVideoFile(fileName)) {
            return FileType.VideoMessage;
        }

        return FileType.MessageFile;
    }

    private FileType resolveFileType(String mimeType, String fileName, FileType fallback) {
        if (mimeType != null) {
            if (mimeType.startsWith("image")) return FileType.MessageImage;

            if (mimeType.startsWith("audio")) return FileType.MessageFile;

            if (mimeType.startsWith("video")) return FileType.VideoMessage;
        }

        String lower = fileName != null ? fileName.toLowerCase() : "";

        if (lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp")
                || lower.endsWith(".gif")) {
            return FileType.MessageImage;
        }

        if (lower.endsWith(".mp3")
                || lower.endsWith(".wav")
                || lower.endsWith(".ogg")
                || lower.endsWith(".m4a")
                || lower.endsWith(".aac")
                || lower.endsWith(".flac")) {
            return FileType.MessageFile;
        }

        if (lower.endsWith(".mp4")
                || lower.endsWith(".mkv")
                || lower.endsWith(".webm")
                || lower.endsWith(".mov")
                || lower.endsWith(".avi")
                || lower.endsWith(".3gp")
                || lower.endsWith(".3gpp")) {
            return FileType.VideoMessage;
        }

        return fallback != null ? fallback : FileType.MessageFile;
    }

    public void clearSelectedFiles() {
        selectedUris.clear();
        selectedFileType = null;
        updateFileCounter();

        Log.d(TAG, "Selected files cleared");
    }

    private void loadChatHistory() {
        new Thread(() -> {
            Chat loadedChat = DatabaseManager.getDatabase().chatDao().getChatById(chatId);
            List<Message> messages = DatabaseManager.getDatabase().messageDao().getMessagesByChatId(chatId);

            chat = loadedChat;

            if (loadedChat != null) {
                loadChatMembersFromChat(loadedChat);
            }

            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                chat = loadedChat;
                applyChatInputMode();
                initializeTimerPanel(loadedChat);

                if (loadedChat != null) {
                    ui.setChatTitle(loadedChat.getName());

                    if (!loadedChat.isActive()) {
                        showChatEnded();
                    }
                }

                messagesUi.clearFileCache();
                messagesUi.initAdapter(messages);

                MessageAdapter adapter = messagesUi.getAdapter();

                if (adapter != null) {
                    adapter.setAudioPlayerManager(audioPlayerManager);
                    adapter.setUploadProgressManager(fileUploadProgressManager);
                    adapter.scheduleVisibleFilePrefetch();
                }

                messagesUi.scrollToBottomImmediately();
                firstMessagesRender = false;

                if (ui != null) {
                    ui.hideUnreadJumpButton();
                }

                if (rootView != null) {
                    rootView.postDelayed(() -> {
                        if (messagesUi != null && messageActions != null) {
                            messagesUi.checkVisibleMessages(messageActions);
                        }

                        updateUnreadJumpButton();
                    }, 250);
                }
            });
        }).start();
    }

    private void refreshChatMembersFromDatabase() {
        if (chatId == null || membersActions == null) return;

        new Thread(() -> {
            Chat freshChat = DatabaseManager.getDatabase().chatDao().getChatById(chatId);

            if (freshChat == null) return;

            chat = freshChat;
            loadChatMembersFromChat(freshChat);
        }).start();
    }

    private void refreshMessagesFromDatabaseWithoutRecreatingAdapter() {
        if (chatId == null || messagesUi == null) {
            return;
        }

        new Thread(() -> {
            List<Message> freshMessages = DatabaseManager.getDatabase()
                    .messageDao()
                    .getMessagesByChatId(chatId);

            if (!isAdded() || getActivity() == null) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                if (messagesUi == null) {
                    return;
                }

                MessageAdapter adapter = messagesUi.getAdapter();

                if (adapter == null) {
                    messagesUi.initAdapter(freshMessages);
                    adapter = messagesUi.getAdapter();

                    if (adapter != null) {
                        adapter.setAudioPlayerManager(audioPlayerManager);
                        adapter.setUploadProgressManager(fileUploadProgressManager);
                    }
                } else {
                    adapter.setMessages(freshMessages);
                    adapter.notifyDataSetChanged();
                }

                if (adapter != null) {
                    adapter.scheduleVisibleFilePrefetch();
                }

                updateUnreadJumpButton();
            });
        }).start();
    }

    private void loadChatMembersFromChat(Chat sourceChat) {
        if (sourceChat == null || sourceChat.getUsers() == null || sourceChat.getUsers().isEmpty()) {
            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> showUsers(new ArrayList<>()));
            }

            return;
        }

        if (membersActions == null) return;

        membersActions.loadUsers(sourceChat.getUsers())
                .thenAccept(users -> {
                    if (!isAdded() || getActivity() == null) return;

                    requireActivity().runOnUiThread(() -> showUsers(users));
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to load chat members", throwable);
                    return null;
                });
    }

    public void onExportChatClick() {
        if (chat == null) return;

        new Thread(() -> {
            try {
                List<Message> messages = DatabaseManager.getDatabase()
                        .messageDao()
                        .getMessagesByChatId(chat.getId());

                if (messages == null || messages.isEmpty()) return;

                ChatPdfExporter exporter = new ChatPdfExporter(requireActivity(), currentUserId);
                exporter.exportChat(messages, chat.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private AlertDialog customizeAiDialog;

    private void setupDialogCustomizeAi(AISettingsResponse currentSettings) {
        if (getContext() == null) return;

        List<AIModel> selectableModels = buildSelectableAiModels(currentSettings);

        if (selectableModels.isEmpty()) {
            Toast.makeText(getContext(), R.string.ai_settings_empty_models, Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_customize_ai, null);
        applyAiCustomizeDialogTheme(dialogView);

        Spinner modelSpinner = dialogView.findViewById(R.id.modelSpinner);
        EditText promptInput = dialogView.findViewById(R.id.promptInput);
        TextView promptHelpText = dialogView.findViewById(R.id.promptHelpText);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button doneButton = dialogView.findViewById(R.id.doneButton);
        LinearLayout mainPanel = dialogView.findViewById(R.id.mainPanel);
        LinearLayout loadingPanel = dialogView.findViewById(R.id.loadingPanel);

        ArrayAdapter<AIModel> adapter = createAiModelAdapter(selectableModels);
        modelSpinner.setAdapter(adapter);

        AIModel initialSelectedModel = currentSettings != null && currentSettings.aiModel != null
                ? currentSettings.aiModel
                : AIModel.Default;

        int position = selectableModels.indexOf(initialSelectedModel);
        if (position < 0) {
            position = selectableModels.indexOf(AIModel.Default);
        }
        if (position >= 0) {
            modelSpinner.setSelection(position);
        }

        updateAiPromptHelpText(
                promptHelpText,
                position >= 0 ? selectableModels.get(position) : initialSelectedModel
        );

        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                updateAiPromptHelpText(
                        promptHelpText,
                        item instanceof AIModel ? (AIModel) item : null
                );
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateAiPromptHelpText(promptHelpText, null);
            }
        });

        promptInput.setText(currentSettings != null && currentSettings.prompt != null
                ? currentSettings.prompt
                : "");

        customizeAiDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        cancelButton.setOnClickListener(v -> {
            if (customizeAiDialog != null) {
                customizeAiDialog.dismiss();
            }
        });

        doneButton.setOnClickListener(v -> {
            hideKeyboard(promptInput);
            promptInput.clearFocus();

            mainPanel.setVisibility(View.GONE);
            loadingPanel.setVisibility(View.VISIBLE);

            AIModel selectedModel = (AIModel) modelSpinner.getSelectedItem();
            String prompt = promptInput.getText() != null
                    ? promptInput.getText().toString().trim()
                    : "";

            if (prompt.isEmpty()) {
                prompt = null;
            }

            stateActions.setAISettings(chat.getId(), selectedModel, prompt)
                    .thenAccept(result -> runOnUiThreadSafe(() -> {
                        AIModel savedModel = result != null && result.aiModel != null
                                ? result.aiModel
                                : selectedModel;
                        AISettingsStore.saveSelectedChatModel(getContext(), chat.getId(), savedModel);
                        AISettingsStore.notifyChatModelChanged(getContext(), chat.getId(), savedModel);

                        if (customizeAiDialog != null) {
                            customizeAiDialog.dismiss();
                        }
                    }))
                    .exceptionally(throwable -> {
                        runOnUiThreadSafe(() -> {
                            if (customizeAiDialog != null) {
                                customizeAiDialog.dismiss();
                            }

                        });
                        return null;
                    });
        });

        customizeAiDialog.setOnShowListener(dialog -> {
            if (customizeAiDialog != null && customizeAiDialog.getWindow() != null) {
                customizeAiDialog.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
                );
            }
        });

        customizeAiDialog.show();
    }

    private void updateAiPromptHelpText(
            @Nullable TextView promptHelpText,
            @Nullable AIModel model
    ) {
        if (promptHelpText == null) {
            return;
        }

        if (model != null
                && model != AIModel.Default
                && model != AIModel.DeepSeekChat) {

            promptHelpText.setText(
                    R.string.ai_prompt_english_only_help
            );

        } else {

            promptHelpText.setText(
                    R.string.ai_prompt_help
            );
        }
    }

    private void hideKeyboard(@Nullable View view) {
        if (view == null || getContext() == null) {
            return;
        }

        InputMethodManager inputMethodManager =
                (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);

        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(
                    view.getWindowToken(),
                    0
            );
        }
    }

    private List<AIModel> buildSelectableAiModels(@Nullable AISettingsResponse settings) {
        LinkedHashSet<AIModel> result = new LinkedHashSet<>();

        if (settings != null && settings.aiModel != null) {
            result.add(settings.aiModel);
        }

        if (settings != null && settings.models != null) {
            for (AIModel model : settings.models) {
                if (model != null) {
                    result.add(model);
                }
            }
        }

        for (AIModel model : AIModel.chatSelectableModels()) {
            if (model != null) {
                result.add(model);
            }
        }

        return new ArrayList<>(result);
    }

    private ArrayAdapter<AIModel> createAiModelAdapter(@NonNull List<AIModel> models) {
        final int textColor = resolveReadableAiDialogTextColor();
        final int dropdownTextColor = resolveReadableAiDialogTextColor();
        final int dropdownSurface = resolveAiColor(R.color.ai_dialog_dropdown_surface);
        return new ArrayAdapter<AIModel>(requireContext(), R.layout.item_ai_model_spinner, models) {
            {
                setDropDownViewResource(R.layout.item_ai_model_spinner_dropdown);
            }

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                bindAiModelItem(view, position, textColor, Color.TRANSPARENT);
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                bindAiModelItem(view, position, dropdownTextColor, dropdownSurface);
                return view;
            }

            private void bindAiModelItem(@NonNull View view, int position, int color, int backgroundColor) {
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    AIModel model = getItem(position);
                    textView.setText(getAiModelDisplayName(model));
                    textView.setTextColor(color);
                    textView.setBackgroundColor(backgroundColor);
                }
            }
        };
    }

    private String getAiModelDisplayName(@Nullable AIModel model) {
        AIModel safeModel = model != null ? model : AIModel.Default;

        switch (safeModel) {
            case DeepSeekChat:
                return getString(R.string.ai_model_deepseek_chat_display);
            case OllamaQwen3_4B:
                return getString(R.string.ai_model_ollama_qwen3_4b_display);
            case OllamaLlama3:
                return getString(R.string.ai_model_ollama_llama3_display);
            case OllamaMistral:
                return getString(R.string.ai_model_ollama_mistral_display);
            case OllamaGemma4e:
                return getString(R.string.ai_model_ollama_gemma4e_display);
            case Default:
            default:
                return getString(R.string.ai_model_default_display);
        }
    }

    private void applyAiCustomizeDialogTheme(@Nullable View dialogView) {
        if (dialogView == null || getContext() == null) return;

        int primary = resolveThemeColor(R.attr.colorPrimary, 0xFF42969E);
        int onPrimary = resolveThemeColor(R.attr.colorOnPrimary, isNightMode() ? Color.WHITE : Color.BLACK);
        int surface = resolveAiColor(R.color.ai_dialog_surface);
        int field = resolveAiColor(R.color.ai_dialog_field);
        int stroke = resolveAiColor(R.color.ai_dialog_stroke);
        int softButton = resolveAiColor(R.color.ai_dialog_soft_button);
        int onSurface = resolveReadableAiDialogTextColor();
        int onSurfaceVariant = withAlpha(onSurface, isNightMode() ? 205 : 190);

        View mainPanel = dialogView.findViewById(R.id.mainPanel);
        View loadingPanel = dialogView.findViewById(R.id.loadingPanel);
        View spinner = dialogView.findViewById(R.id.modelSpinner);
        View promptInput = dialogView.findViewById(R.id.promptInput);

        setRoundedBackground(mainPanel, surface, dp(28), stroke);
        setRoundedBackground(loadingPanel, surface, dp(28), stroke);
        setRoundedBackground(spinner, field, dp(18), stroke);
        setRoundedBackground(promptInput, field, dp(18), stroke);

        setTextColor(dialogView, R.id.titleText, onSurface);
        setTextColor(dialogView, R.id.subtitleText, onSurfaceVariant);
        setTextColor(dialogView, R.id.modelLabel, onSurfaceVariant);
        setTextColor(dialogView, R.id.promptLabel, onSurfaceVariant);
        setTextColor(dialogView, R.id.promptHelpText, onSurfaceVariant);

        EditText input = dialogView.findViewById(R.id.promptInput);
        if (input != null) {
            input.setTextColor(onSurface);
            input.setHintTextColor(onSurfaceVariant);
            input.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        }

        Spinner modelSpinner = dialogView.findViewById(R.id.modelSpinner);
        if (modelSpinner != null) {
            modelSpinner.setPopupBackgroundDrawable(
                    createRoundedDrawable(resolveAiColor(R.color.ai_dialog_dropdown_surface), dp(16), stroke)
            );
        }

        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        if (cancelButton != null) {
            cancelButton.setTextColor(primary);
            cancelButton.setBackgroundTintList(ColorStateList.valueOf(softButton));
        }

        Button cancelLoadingButton = dialogView.findViewById(R.id.cancelLoadingButton);
        if (cancelLoadingButton != null) {
            cancelLoadingButton.setTextColor(primary);
            cancelLoadingButton.setBackgroundTintList(ColorStateList.valueOf(softButton));
        }

        Button doneButton = dialogView.findViewById(R.id.doneButton);
        if (doneButton != null) {
            doneButton.setTextColor(onPrimary);
            doneButton.setBackgroundTintList(ColorStateList.valueOf(primary));
        }
    }

    private void setTextColor(@Nullable View root, int id, int color) {
        if (root == null) return;

        TextView view = root.findViewById(id);
        if (view != null) {
            view.setTextColor(color);
        }
    }

    private void setRoundedBackground(@Nullable View view, int color, int radiusPx, int strokeColor) {
        if (view == null) return;

        view.setBackground(createRoundedDrawable(color, radiusPx, strokeColor));
    }

    private GradientDrawable createRoundedDrawable(int color, int radiusPx, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(forceOpaque(color));
        drawable.setCornerRadius(radiusPx);
        drawable.setStroke(dp(1), forceOpaque(strokeColor));
        return drawable;
    }

    private int resolveReadableAiDialogTextColor() {
        int surface = resolveAiColor(R.color.ai_dialog_surface);
        int themedText = resolveThemeColor(R.attr.colorOnSurface, isNightMode() ? Color.WHITE : 0xFF1D1B20);
        themedText = forceOpaque(themedText);

        if (Color.alpha(themedText) >= 220 && contrastRatio(themedText, surface) >= 4.5) {
            return themedText;
        }

        return colorLuminance(surface) < 0.45 ? Color.WHITE : 0xFF1D1B20;
    }

    private int resolveAiColor(int colorRes) {
        return forceOpaque(ContextCompat.getColor(requireContext(), colorRes));
    }

    private int resolveThemeColor(int attr, int fallback) {
        try {
            android.util.TypedValue value = new android.util.TypedValue();
            boolean resolved = requireContext().getTheme().resolveAttribute(attr, value, true);
            if (resolved) {
                if (value.resourceId != 0) {
                    return forceOpaque(ContextCompat.getColor(requireContext(), value.resourceId));
                }

                if (value.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT
                        && value.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) {
                    return forceOpaque(value.data);
                }
            }
        } catch (Exception ignored) {
        }

        return fallback;
    }

    private boolean isNightMode() {
        if (getContext() == null) return false;

        int nightMode = getContext().getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private double contrastRatio(int foreground, int background) {
        double l1 = colorLuminance(foreground) + 0.05;
        double l2 = colorLuminance(background) + 0.05;
        return Math.max(l1, l2) / Math.min(l1, l2);
    }

    private double colorLuminance(int color) {
        double red = channelLuminance(Color.red(color) / 255.0);
        double green = channelLuminance(Color.green(color) / 255.0);
        double blue = channelLuminance(Color.blue(color) / 255.0);
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private double channelLuminance(double channel) {
        return channel <= 0.03928
                ? channel / 12.92
                : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private int forceOpaque(int color) {
        return (color & 0x00FFFFFF) | 0xFF000000;
    }

    private int withAlpha(int color, int alpha) {
        int safeAlpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (safeAlpha << 24);
    }

    private int blendColors(int baseColor, int overlayColor, float ratio) {
        float safeRatio = Math.max(0f, Math.min(1f, ratio));
        float inverse = 1f - safeRatio;

        int red = Math.round(Color.red(baseColor) * inverse + Color.red(overlayColor) * safeRatio);
        int green = Math.round(Color.green(baseColor) * inverse + Color.green(overlayColor) * safeRatio);
        int blue = Math.round(Color.blue(baseColor) * inverse + Color.blue(overlayColor) * safeRatio);

        return Color.rgb(red, green, blue);
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    public void onSelectModelClick() {
        if (chat == null || chat.getId() == null || getContext() == null) return;

        View loadingView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_customize_ai, null);
        applyAiCustomizeDialogTheme(loadingView);
        LinearLayout mainPanel = loadingView.findViewById(R.id.mainPanel);
        LinearLayout loadingPanel = loadingView.findViewById(R.id.loadingPanel);
        Button cancelLoadingButton = loadingView.findViewById(R.id.cancelLoadingButton);

        mainPanel.setVisibility(View.GONE);
        loadingPanel.setVisibility(View.VISIBLE);

        AlertDialog loadingDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(loadingView)
                .setCancelable(false)
                .create();

        loadingDialog.setOnShowListener(dialog -> {
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
                );
            }
        });

        final boolean[] loadingCancelled = {false};

        cancelLoadingButton.setOnClickListener(v -> {
            loadingCancelled[0] = true;
            loadingDialog.dismiss();
            Toast.makeText(getContext(), R.string.ai_settings_loading_cancelled, Toast.LENGTH_SHORT).show();
        });

        loadingDialog.show();

        stateActions.getAISettings(chat.getId())
                .thenAccept(aiSettings -> runOnUiThreadSafe(() -> {
                    if (loadingCancelled[0]) return;

                    loadingDialog.dismiss();
                    setupDialogCustomizeAi(aiSettings);
                }))
                .exceptionally(throwable -> {
                    runOnUiThreadSafe(() -> {
                        if (loadingCancelled[0]) return;

                        loadingDialog.dismiss();
                        Toast.makeText(
                                getContext(),
                                R.string.ai_settings_load_error,
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                    return null;
                });
    }

    public void onSendMessageClick() {
        String text = ui.getMessageText();
        boolean hasQuote = pendingQuoteText != null && !pendingQuoteText.trim().isEmpty();

        if ((text == null || text.isEmpty()) && selectedUris.isEmpty() && !hasQuote) return;

        if (ui.isEditMode() && editingMessage != null) {
            messageActions.editMessage(editingMessage, text);
            ui.hideEditPanel();

            editingMessage = null;
            pendingQuoteText = null;

            ui.clearMessageInput();
            clearSelectedFiles();

            return;
        }

        if (ui.isReplyMode() && replyingToMessage != null) {
            String outgoingText = text != null ? text : "";

            if (hasQuote) {
                outgoingText = MessageAdapter.appendQuoteMarkerToText(outgoingText, pendingQuoteText);
            }

            messageActions.replyToMessage(replyingToMessage, outgoingText);
            ui.hideReplyPanel();

            replyingToMessage = null;
            pendingQuoteText = null;

            ui.clearMessageInput();
            clearSelectedFiles();

            return;
        }

        if (!selectedUris.isEmpty()) {
            sendAllStoredFilesWithText(text);
        } else {
            messageActions.sendMessage(text);
        }

        pendingQuoteText = null;
        ui.clearMessageInput();
    }

    public void onOptionsClick() {
        ui.showOptionsMenu();
    }

    public void onToggleMembersPanel() {
        if (membersUi != null) {
            membersUi.togglePanel();
        }
    }

    public void onShowSearchPanel() {
        ui.showSearchPanel();
    }

    public void onSearchQuery(String query) {
        messageActions.findMessages(query);
    }

    public void onHideSearchPanel() {
        ui.hideSearchPanel();
    }

    public void onHideSearchResults() {
        searchUi.hideResults();
    }

    public void onMemberClick(User user) {
        ui.insertMention(user.getUserData().getName());
    }

    public void onAddMemberClick() {
        membersActions.addUserToChat();
    }

    public void onRemoveMemberClick(UUID id) {
        membersActions.removeUserFromChat(id);
    }

    public void onHighlightMessage(Message message) {
        messagesUi.highlightMessage(message);
    }

    public void onVisibleMessagesChanged() {
        if (messagesUi != null && messageActions != null) {
            messagesUi.checkVisibleMessages(messageActions);
            MessageAdapter adapter = messagesUi.getAdapter();
            if (adapter != null) {
                adapter.scheduleVisibleFilePrefetch();
            }
        }

        updateUnreadJumpButton();
    }

    public void onUnreadJumpClick() {
        if (messagesUi == null || ui == null || currentUserId == null) return;

        ui.hideUnreadJumpButton();

        messagesUi.scrollByUnreadButton(
                currentUserId,
                messageActions,
                this::updateUnreadJumpButton
        );
    }

    private void updateUnreadJumpButton() {
        if (messagesUi == null || ui == null || currentUserId == null) return;

        int unreadCount = messagesUi.getUnreadMessagesCount(currentUserId);

        boolean shouldShowButton = unreadCount > 0 && messagesUi.shouldShowUnreadJump(currentUserId);

        if (shouldShowButton) {
            ui.showUnreadJumpButton(unreadCount);
        } else {
            ui.hideUnreadJumpButton();

            if (unreadCount <= 0) {
                messagesUi.resetUnreadJumpState();
            }
        }
    }

    public void reloadMessagesFromDatabase() {
        new Thread(() -> {
            List<Message> messages = DatabaseManager.getDatabase()
                    .messageDao()
                    .getMessagesByChatId(chatId);

            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> applyMessagesFromDatabaseWithoutReload(messages));
        }).start();
    }

    private void applyMessagesFromDatabaseWithoutReload(List<Message> messages) {
        if (messagesUi == null) return;

        if (messagesUi.getAdapter() == null || firstMessagesRender) {
            showMessages(messages);
            return;
        }

        messagesUi.applyMessagesWithoutReload(messages);

        applyFileSizes(messages);
        updateUnreadJumpButton();
    }

    public void onEndChatClick() {
        Log.d(TAG, "onEndChatClick called");
        stateActions.endChat();
    }

    public void navigateBack() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).backToChats();
        }
    }

    public void setSearchingChatId(@Nullable UUID id) {
        runOnUiThreadSafe(() -> {
            if (membersUi == null) {
                return;
            }

            membersUi.setSearchState(id);
        });
    }

    public void canselSearch() {
        runOnUiThreadSafe(() -> {
            if (membersUi != null) {
                membersUi.setSearchState(null);
            }
        });
    }

    private void runOnUiThreadSafe(Runnable action) {
        if (action == null) {
            return;
        }

        if (!isAdded() || getActivity() == null) {
            return;
        }

        requireActivity().runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null || getView() == null) {
                return;
            }

            action.run();
        });
    }

    public void onCancelSearchClick() {
        membersActions.stopSearchingChat();
    }

    public void onCancelEdit() {
        editingMessage = null;
        ui.hideEditPanel();
    }

    public void onCancelReply() {
        replyingToMessage = null;
        ui.hideReplyPanel();
    }

    public void onMessageEditSelected(Message message) {
        if (message == null || ui == null) return;

        editingMessage = message;
        replyingToMessage = null;
        pendingQuoteText = null;

        ui.showEditPanel(message.getText());

        if (ui.getMessageInput() != null) {
            ui.getMessageInput().setText(message.getText() != null ? message.getText() : "");
            ui.getMessageInput().setSelection(ui.getMessageInput().getText().length());
        }
    }

    public void onMessageDeleteSelected(Message message) {
        if (message == null || messageActions == null) return;

        messageActions.deleteMessage(message);
    }

    public void onMessageReplySelected(Message message) {
        if (message == null || ui == null) return;

        replyingToMessage = message;
        editingMessage = null;
        pendingQuoteText = null;

        ui.showReplyPanel(MessageAdapter.stripEmbeddedQuoteData(message.getText()));
    }

    public void onMessageCopyTextSelected(Message message) {
        if (message == null || getContext() == null) return;

        String pendingQuote = MessageAdapter.consumePendingQuoteSelection(message.getId());

        if (pendingQuote != null && !pendingQuote.trim().isEmpty()) {
            onMessageQuoteSelected(message, pendingQuote);
            return;
        }

        String text = message.getText() != null ? message.getText() : "";

        android.content.ClipboardManager clipboardManager =
                (android.content.ClipboardManager) requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(
                    android.content.ClipData.newPlainText("message", text)
            );

            android.widget.Toast.makeText(
                    getContext(),
                    "Текст скопирован",
                    android.widget.Toast.LENGTH_SHORT
            ).show();
        }
    }

    public void onMessageQuoteSelected(@Nullable Message message, @Nullable String selectedText) {
        if (message == null || ui == null) {
            return;
        }

        String quote = sanitizeQuotedText(selectedText);

        if (quote.isEmpty()) {
            quote = sanitizeQuotedText(message.getText());
        }

        if (quote.isEmpty()) {
            return;
        }

        replyingToMessage = message;
        editingMessage = null;
        pendingQuoteText = quote;

        ui.hideEditPanel();
        ui.showReplyPanel(quote);
    }

    @NonNull
    private String sanitizeQuotedText(@Nullable String value) {
        if (value == null) {
            return "";
        }

        return MessageAdapter.stripEmbeddedQuoteData(value)
                .replaceAll("\\[file:.*?]", "")
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void insertQuoteIntoMessageInput(@NonNull String quote) {
        if (ui == null || ui.getMessageInput() == null) {
            return;
        }

        android.widget.EditText input = ui.getMessageInput();
        String current = input.getText() != null ? input.getText().toString() : "";
        String quoteBlock = buildQuoteBlock(quote);

        if (current.startsWith(quoteBlock)) {
            input.setSelection(input.getText() != null ? input.getText().length() : 0);
            return;
        }

        String nextText = quoteBlock;

        if (!current.trim().isEmpty()) {
            nextText += current;
        }

        input.setText(nextText);
        input.setSelection(input.getText() != null ? input.getText().length() : 0);
        input.requestFocus();
    }

    @NonNull
    private String buildQuoteBlock(@NonNull String quote) {
        StringBuilder builder = new StringBuilder();
        String[] lines = quote.split("\\r?\\n");

        for (String line : lines) {
            String cleanLine = line != null ? line.trim() : "";

            if (!cleanLine.isEmpty()) {
                builder.append("> ").append(cleanLine).append('\n');
            }
        }

        builder.append('\n');
        return builder.toString();
    }

    public void showMessages(List<Message> list) {
        if (messagesUi == null) return;

        boolean isFirstRender = firstMessagesRender || messagesUi.getAdapter() == null;

        if (!isFirstRender) {
            messagesUi.applyMessagesWithoutReload(list);
            applyFileSizes(list);
            updateUnreadJumpButton();

            MessageAdapter adapter = messagesUi.getAdapter();
            if (adapter != null) {
                adapter.scheduleVisibleFilePrefetch();
            }

            return;
        }

        messagesUi.clearFileCache();
        messagesUi.initAdapter(list);

        MessageAdapter adapter = messagesUi.getAdapter();

        if (adapter != null) {
            adapter.setAudioPlayerManager(audioPlayerManager);
            adapter.setUploadProgressManager(fileUploadProgressManager);
            adapter.scheduleVisibleFilePrefetch();
        }

        firstMessagesRender = false;

        messagesUi.scrollToBottomImmediately();

        if (ui != null) {
            ui.hideUnreadJumpButton();
        }

        if (rootView != null) {
            rootView.postDelayed(() -> {
                if (messagesUi != null && messageActions != null) {
                    messagesUi.checkVisibleMessages(messageActions);
                }

                updateUnreadJumpButton();
            }, 250);
        }

        applyFileSizes(list);
    }

    private void applyFileSizes(List<Message> list) {
        if (list == null) return;

        for (Message message : list) {
            if (message.getFiles() != null && !message.getFiles().isEmpty()) {
                for (UUID fileId : message.getFiles()) {
                    setFileSize(fileId, 1024 * 1024);
                }
            }
        }
    }

    public void addOrUpdateIncomingMessageWithoutReload(Message message) {
        if (message == null || messagesUi == null) return;

        messagesUi.addMessageWithoutAutoScroll(message);
        updateUnreadJumpButton();

        MessageAdapter adapter = messagesUi.getAdapter();
        if (adapter != null) {
            adapter.scheduleVisibleFilePrefetch();
        }

        if (message.getFiles() != null && !message.getFiles().isEmpty()) {
            for (UUID fileId : message.getFiles()) {
                setFileSize(fileId, 1024 * 1024);
            }
        }
    }

    public void addOrUpdateMessage(Message message) {
        if (messagesUi == null || message == null) return;

        MessageAdapter adapter = messagesUi.getAdapter();

        if (adapter != null && adapter.getMessagePosition(message.getId()) != -1) {
            adapter.addOrUpdateMessage(message);
        } else {
            messagesUi.addMessage(message);
        }

        updateUnreadJumpButton();

        if (adapter != null) {
            adapter.scheduleVisibleFilePrefetch();
        }

        if (message.getFiles() != null && !message.getFiles().isEmpty()) {
            for (UUID fileId : message.getFiles()) {
                setFileSize(fileId, 1024 * 1024);
            }
        }
    }

    public void updateMessage(Message message) {
        if (messagesUi == null || message == null) return;

        MessageAdapter adapter = messagesUi.getAdapter();
        if (adapter != null) {
            adapter.addOrUpdateMessage(message);
            adapter.scheduleVisibleFilePrefetch();
        } else {
            messagesUi.addMessage(message);
        }

        updateUnreadJumpButton();
    }

    public void removeMessage(UUID id) {
        if (messagesUi == null) return;
        assert getActivity() != null;
        getActivity().runOnUiThread(()-> {
            messagesUi.removeMessage(id);
            updateUnreadJumpButton();
        });
    }

    public void updateMessageStatus(UUID messageId, UUID userId, MessageStatus status) {
        if (messagesUi == null) return;

        messagesUi.updateStatus(messageId, userId, status);
        updateUnreadJumpButton();
    }

    public void showUsers(List<User> users) {
        if (membersUi == null) return;

        List<User> safeUsers = users != null ? users : new ArrayList<>();

        membersUi.updateMembers(safeUsers);

        if (messageActions != null) {
            messageActions.updateUsers(membersUi.getUserIds());
        }
    }

    public void onChatNameUpdated(UUID updatedChatId, String newName) {
        if (!isAdded() || getActivity() == null) return;

        if (chatId != null && chatId.equals(updatedChatId)) {
            Log.d(TAG, "onChatNameUpdated: chat " + updatedChatId + " renamed to '" + newName + "'");

            if (chat != null) {
                chat.setName(newName);
            }

            requireActivity().runOnUiThread(() -> {
                ui.setChatTitle(newName);
                Log.d(TAG, "Chat title updated to: " + newName);
            });
        }
    }

    public void updateOnline(UUID id, @Nullable String lastOnline) {
        membersUi.updateOnline(id, lastOnline);
    }

    public void showChatEnded() {
        if (chat == null || ui == null) return;

        boolean isRandomChatQ = chat.getType() == ChatType.Random && !chat.isActive();
        ui.showChatEnded(isRandomChatQ);
        if (isRandomChatQ && stateActions != null) {
            stateActions.getChatGameResult(chat.getId()).thenAccept(state ->
                    runOnUiThreadSafe(() -> ui.setRandomChatResult(state))
            );
        }
    }

    public void showSearchResults(List<Message> messages, String query) {
        searchUi.showResults(messages, query);
    }

    public void setChatTitle(String name) {
        ui.setChatTitle(name);
    }

    public UUID getCurrentUserId() {
        return currentUserId;
    }

    @Nullable
    public UUID getChatCreatorId() {
        if (chat == null || chat.getUsers() == null || chat.getUsers().isEmpty()) {
            return null;
        }

        return chat.getUsers().get(0);
    }

    public boolean isCurrentUserChatCreator() {
        UUID creatorId = getChatCreatorId();

        return currentUserId != null
                && creatorId != null
                && currentUserId.equals(creatorId);
    }

    public ChatUi getUi() {
        return ui;
    }

    public Chat getChat() {
        return chat;
    }

    public void close() {
        if (mediaInputController != null) {
            mediaInputController.cancelActiveRecordingBecauseChatLeft();
        }

        navigateBack();
    }

    @Override
    public void onPause() {
        if (mediaInputController != null) {
            mediaInputController.cancelActiveRecordingBecauseChatLeft();
        }

        super.onPause();

        if (isCurrentlyTyping) {
            stopTypingSignal();
        }

        typingHandler.removeCallbacksAndMessages(null);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setCurrentChatId(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (rootView != null) {
            ChatFragmentThemeBinder.apply(this, rootView);
        }

        if (messagesUi != null) {
            messagesUi.clearFileCache();
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setCurrentChatId(chatId);
        }

        refreshChatMembersFromDatabase();
        refreshMessagesFromDatabaseWithoutRecreatingAdapter();
    }

    @Override
    public void onDestroyView() {
        cleanupTypingTracking();

        if (mediaInputController != null) {
            mediaInputController.destroy();
            mediaInputController = null;
        }

        super.onDestroyView();

        rootView = null;
        fileCounterTextView = null;
    }

    @Override
    public void onDestroy() {
        if (fileManager != null) {
            fileManager.cleanupTempFiles();
        }
        isTimerRunning = false;

        clearSelectedFiles();

        super.onDestroy();
    }

    public void onAudioDownloaded(UUID fileId, String localPath) {
        if (messagesUi == null) return;

        MessageAdapter adapter = messagesUi.getAdapter();

        if (adapter == null) return;

        adapter.onLocalAudioFileReady(fileId, localPath);

        AudioPlayerManager player = adapter.getAudioPlayerManager();

        if (player == null) return;
        if (!player.shouldAutoPlay(fileId)) return;

        player.clearPendingPlay();

        requireActivity().runOnUiThread(() -> player.playLocal(
                requireContext(),
                localPath,
                fileId
        ));
    }

    public AudioPlayerManager getAudioPlayerManager() {
        return audioPlayerManager;
    }

    private String getFileNameFromUri(Uri uri) {
        Context context = getContext();

        if (context == null) {
            return "file_" + System.currentTimeMillis();
        }

        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);

        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

            if (nameIndex >= 0 && cursor.moveToFirst()) {
                String name = cursor.getString(nameIndex);
                cursor.close();

                return name;
            }

            cursor.close();
        }

        return "file_" + System.currentTimeMillis();
    }
    public void onChatWithPersonClick() {
        stateActions.setChatGameResult(chat.getId(), AiRole.FakeAi).thenAccept(state ->{
            ui.setRandomChatResult(state);
        });
    }

    public void onChatWithAiClick() {
            stateActions.setChatGameResult(chat.getId(), AiRole.RealAi).thenAccept(state ->{
                ui.setRandomChatResult(state);
            });
        }

    public boolean shouldShowEndChat() {
        return chat != null && chat.isActive() && (chat.getType() == ChatType.Human || chat.getType() == ChatType.Group || chat.getType() == ChatType.AI);
    }

    public boolean shouldShowViewMembers() {
        return chat!=null && chat.isActive() && (chat.getType() == ChatType.Group || chat.getType() == ChatType.Human);
    }

    public boolean shouldShowSelectModel() {
        return chat!=null && chat.isActive() && (chat.getType() == ChatType.AI);
    }
}
