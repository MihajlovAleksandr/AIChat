package com.example.aichat.model.utils.media.recording;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.example.aichat.controller.main.chat.actions.SendMessageController;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.FileType;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageInfo;
import com.example.aichat.model.utils.media.ChatMediaMarkers;
import com.example.aichat.model.utils.files.FileUploadProgressManager;
import com.example.aichat.model.utils.mappers.MessageMapper;
import com.example.aichat.model.utils.time.TimeConverter;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.UUID;

@SuppressWarnings("unused")
public class RecordedMediaSender {

    public interface Callback {
        void onLocalMessage(@NonNull Message message);

        void onServerMessage(@NonNull Message message);

        void onError(@NonNull UUID localFileId);
    }

    private final Context context;
    private final UUID chatId;
    private final UUID currentUserId;
    private final SendMessageController sendMessageController;
    private final FileUploadProgressManager uploadProgressManager;
    private final Callback callback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RecordedMediaSender(
            @NonNull Context context,
            @NonNull UUID chatId,
            @NonNull UUID currentUserId,
            @NonNull SendMessageController sendMessageController,
            @NonNull FileUploadProgressManager uploadProgressManager,
            @NonNull Callback callback
    ) {
        this.context = context.getApplicationContext();
        this.chatId = chatId;
        this.currentUserId = currentUserId;
        this.sendMessageController = sendMessageController;
        this.uploadProgressManager = uploadProgressManager;
        this.callback = callback;
    }

    public void send(
            @NonNull File sourceFile,
            @NonNull FileType fileType,
            @NonNull String mimeType,
            boolean circleVideo
    ) {
        if (!sourceFile.exists() || sourceFile.length() <= 0) {
            showToast("Файл записи не найден");
            return;
        }

        final UUID messageId = UUID.randomUUID();
        final UUID localFileId = UUID.randomUUID();

        final String originalName = resolveOriginalName(sourceFile, fileType, circleVideo);
        final String textToSend = ChatMediaMarkers.buildFileMarker(localFileId, originalName);

        ArrayList<File> files = new ArrayList<>();
        files.add(sourceFile);

        ArrayList<UUID> localFileIds = new ArrayList<>();
        localFileIds.add(localFileId);

        FileType uploadFileType = resolveServerUploadFileType(fileType, mimeType);

        ArrayList<FileType> requestFileTypes = new ArrayList<>();
        requestFileTypes.add(uploadFileType);

        HashMap<UUID, FileType> localFileTypes = new HashMap<>();
        localFileTypes.put(localFileId, fileType);

        HashMap<UUID, String> localMimeTypes = new HashMap<>();
        localMimeTypes.put(localFileId, mimeType);

        savePendingFileToDatabase(
                localFileId,
                sourceFile,
                mimeType,
                fileType,
                originalName
        );

        uploadProgressManager.updateProgress(localFileId, 0);

        MessageInfo info = new MessageInfo(
                messageId,
                chatId,
                textToSend,
                new ArrayList<>(),
                requestFileTypes,
                files,
                localFileIds,
                Collections.singletonList(originalName)
        );

        Message localMessage = new Message(
                messageId,
                textToSend,
                currentUserId,
                chatId,
                TimeConverter.getString(LocalDateTime.now()),
                TimeConverter.getString(LocalDateTime.now()),
                new ArrayList<>(),
                new HashMap<>(),
                localFileIds,
                localMimeTypes,
                localFileTypes
        );

        callback.onLocalMessage(localMessage);

        new Thread(() ->
                DatabaseManager.getDatabase()
                        .messageDao()
                        .upsertMessage(localMessage)
        ).start();

        sendMessageController.sendMessage(info)
                .thenAccept(cmd -> {
                    if (cmd == null || !cmd.isSuccess()) {
                        uploadProgressManager.error(localFileId);
                        callback.onError(localFileId);
                        showToast("Ошибка отправки медиа");
                        return;
                    }

                    try {
                        MessageResponse response = cmd.getData(MessageResponse.class);

                        if (response == null) {
                            throw new IllegalStateException("Empty message response");
                        }

                        Message updated = new MessageMapper(currentUserId).ToModel(response);

                        if (updated != null) {
                            updated.setText(textToSend);
                        }

                        if (response.files != null && !response.files.isEmpty()) {
                            saveServerFileAlias(
                                    localFileId,
                                    response.files.get(0),
                                    sourceFile,
                                    mimeType,
                                    fileType,
                                    originalName
                            );
                        }

                        if (updated != null) {
                            DatabaseManager.getDatabase()
                                    .messageDao()
                                    .upsertMessage(updated);
                        }

                        uploadProgressManager.complete(localFileId);

                        if (updated != null) {
                            callback.onServerMessage(updated);
                        }
                    } catch (Exception exception) {
                        uploadProgressManager.error(localFileId);
                        callback.onError(localFileId);
                        showToast("Ошибка обработки медиа");
                    }
                })
                .exceptionally(throwable -> {
                    uploadProgressManager.error(localFileId);
                    callback.onError(localFileId);
                    showToast("Ошибка отправки медиа");
                    return null;
                });
    }

    @NonNull
    private FileType resolveServerUploadFileType(@NonNull FileType localFileType, @NonNull String mimeType) {
        if (localFileType == FileType.MessageImage) {
            return FileType.MessageImage;
        }

        if (localFileType == FileType.VoiceMessage || mimeType.startsWith("audio")) {
            return FileType.VoiceMessage;
        }

        if (localFileType == FileType.VideoMessage || mimeType.startsWith("video")) {
            return FileType.VideoMessage;
        }

        return FileType.MessageFile;
    }

    @NonNull
    private String resolveOriginalName(
            @NonNull File sourceFile,
            @NonNull FileType fileType,
            boolean circleVideo
    ) {
        String fileName = sourceFile.getName();

        if (!fileName.trim().isEmpty()) {
            return fileName;
        }

        if (fileType == FileType.VoiceMessage) {
            return ChatMediaMarkers.buildVoiceFileName();
        }

        if (circleVideo) {
            return ChatMediaMarkers.buildCircleVideoFileName();
        }

        return ChatMediaMarkers.buildPlainVideoFileName();
    }

    private void savePendingFileToDatabase(
            UUID fileId,
            File file,
            String mimeType,
            FileType fileType,
            String fileName
    ) {
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

    private void saveServerFileAlias(
            UUID localFileId,
            UUID serverFileId,
            File actualFile,
            String mimeType,
            FileType fileType,
            String fileName
    ) {
        if (localFileId == null
                || serverFileId == null
                || actualFile == null
                || !actualFile.exists()) {
            return;
        }

        long now = System.currentTimeMillis();

        DatabaseManager.getDatabase()
                .fileDao()
                .insert(
                        new com.example.aichat.model.entities.File(
                                serverFileId,
                                actualFile.getAbsolutePath(),
                                mimeType,
                                fileType != null ? fileType.name() : null,
                                actualFile.length(),
                                now,
                                now,
                                fileName
                        )
                );
    }

    private void showToast(@NonNull String message) {
        mainHandler.post(() ->
                Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_SHORT
                ).show()
        );
    }
}
