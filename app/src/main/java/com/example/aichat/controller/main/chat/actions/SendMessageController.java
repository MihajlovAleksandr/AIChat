package com.example.aichat.controller.main.chat.actions;

import android.content.Context;
import com.example.aichat.dto.request.MessageRequest;
import com.example.aichat.dto.request.PrepareSendMessageRequest;
import com.example.aichat.dto.request.UploadFileRequest;
import com.example.aichat.dto.request.UploadSessionFileRequest;
import com.example.aichat.dto.response.UploadSessionFileResponse;
import com.example.aichat.dto.response.UploadSessionResponse;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.files.UploadProgressListener;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.FileType;
import com.example.aichat.model.entities.HttpCommand;
import com.example.aichat.model.entities.MessageInfo;
import com.example.aichat.model.utils.media.ChatMediaMarkers;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class SendMessageController {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_BASE_DELAY_MS = 650L;

    private final ConnectionDispatcher dispatcher;
    private final UploadProgressListener listener;
    private final Context context;

    public SendMessageController(
            Context context,
            ConnectionDispatcher dispatcher,
            UploadProgressListener listener
    ) {
        this.context = context.getApplicationContext();
        this.dispatcher = dispatcher;
        this.listener = listener;
    }

    public CompletableFuture<HttpCommand> sendMessage(MessageInfo info) {
        UUID messageId = info.id;

        if (!info.hasFiles()) {
            MessageRequest request = new MessageRequest(
                    messageId,
                    info.chatId,
                    info.text,
                    info.replies,
                    null
            );

            return sendMessageRequest(request);
        }

        if (info.fileTypes == null || info.files == null || info.fileTypes.size() != info.files.size()) {
            return failedFuture(new IllegalStateException("Invalid fileTypes in MessageInfo"));
        }

        AtomicReference<UUID> fileSessionId = new AtomicReference<>();

        List<UploadSessionFileRequest> fileRequests = getFiles(
                info.files,
                info.fileTypes,
                info.uploadFileIds,
                info.fileNames
        );

        final Map<UUID, UUID> localToServerFileIds = new HashMap<>();

        PrepareSendMessageRequest prepareRequest = new PrepareSendMessageRequest(
                info.chatId,
                info.text != null ? info.text.length() : 0,
                fileRequests,
                info.replies != null ? info.replies.size() : 0
        );

        return sendPrepareRequestWithRetry(prepareRequest)
                .thenCompose(cmd -> {
                    if (cmd == null || !cmd.isSuccess()) {
                        return failedFuture(new RuntimeException("Prepare failed"));
                    }

                    UploadSessionResponse response = cmd.getData(UploadSessionResponse.class);

                    if (response == null || response.id == null) {
                        return failedFuture(new RuntimeException("Invalid upload session response"));
                    }

                    fileSessionId.set(response.id);

                    Map<UUID, UploadSessionFileResponse> responseById = new HashMap<>();

                    if (response.files != null) {
                        for (UploadSessionFileResponse current : response.files) {
                            if (current != null && current.id != null) {
                                responseById.put(current.id, current);
                            }
                        }
                    }

                    if (fileRequests.isEmpty()) {
                        return failedFuture(new RuntimeException("No files to upload"));
                    }

                    return uploadFilesSequentially(
                            fileRequests,
                            responseById,
                            fileSessionId.get(),
                            localToServerFileIds
                    );
                })
                .thenCompose(v -> {
                    String serverText = ChatMediaMarkers.replaceFileMarkerIds(
                            info.text,
                            localToServerFileIds
                    );

                    MessageRequest request = new MessageRequest(
                            messageId,
                            info.chatId,
                            serverText,
                            info.replies,
                            fileSessionId.get()
                    );

                    return sendMessageRequest(request);
                });
    }


    private CompletableFuture<Void> uploadFilesSequentially(
            List<UploadSessionFileRequest> fileRequests,
            Map<UUID, UploadSessionFileResponse> responseById,
            UUID sessionId,
            Map<UUID, UUID> localToServerFileIds
    ) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

        for (UploadSessionFileRequest fileRequest : fileRequests) {
            chain = chain.thenCompose(ignored -> uploadSingleFile(
                    fileRequest,
                    responseById,
                    sessionId,
                    localToServerFileIds
            ));
        }

        return chain;
    }

    private CompletableFuture<Void> uploadSingleFile(
            UploadSessionFileRequest fileRequest,
            Map<UUID, UploadSessionFileResponse> responseById,
            UUID sessionId,
            Map<UUID, UUID> localToServerFileIds
    ) {
        return retryVoid(
                () -> uploadSingleFileOnce(
                        fileRequest,
                        responseById,
                        sessionId,
                        localToServerFileIds
                ),
                0
        );
    }

    private CompletableFuture<Void> uploadSingleFileOnce(
            UploadSessionFileRequest fileRequest,
            Map<UUID, UploadSessionFileResponse> responseById,
            UUID sessionId,
            Map<UUID, UUID> localToServerFileIds
    ) {
        UploadSessionFileResponse current = responseById.get(fileRequest.id);

        if (current == null) {
            return failedFuture(new RuntimeException(
                    "Upload session file was not returned: " + fileRequest.expectedFileName
            ));
        }

        return dispatcher.uploadFile(
                        "/api/files",
                        new UploadFileRequest(
                                sessionId,
                                current.id,
                                fileRequest.expectedFileType
                        ),
                        fileRequest.file,
                        fileRequest.id != null ? fileRequest.id : current.id,
                        fileRequest.expectedFileName,
                        listener
                )
                .thenApply(cmd -> {
                    if (cmd == null || !cmd.isSuccess()) {
                        RuntimeException error = new RuntimeException(
                                "Upload failed: " + fileRequest.expectedFileName
                        );

                        if (shouldRetryCommand(cmd)) {
                            throw new RetryableHttpException(error);
                        }

                        throw error;
                    }

                    UUID uploadedFileId = extractUploadedFileId(cmd, current.id);

                    if (fileRequest.id != null && uploadedFileId != null) {
                        localToServerFileIds.put(fileRequest.id, uploadedFileId);
                    }

                    saveUploadedFileToLocalDatabase(
                            fileRequest,
                            uploadedFileId != null ? uploadedFileId : current.id
                    );

                    return null;
                });
    }

    private UUID extractUploadedFileId(
            HttpCommand command,
            UUID fallbackId
    ) {
        if (command == null) {
            return fallbackId;
        }

        try {
            UUID uploadedFileId = command.getData(UUID.class);

            if (uploadedFileId != null) {
                return uploadedFileId;
            }
        } catch (Exception ignored) {
        }

        return fallbackId;
    }

    private void saveUploadedFileToLocalDatabase(
            UploadSessionFileRequest fileRequest,
            UUID uploadedFileId
    ) {
        File permanentFile = new File(
                context.getFilesDir(),
                uploadedFileId + "_" + sanitizeFileName(fileRequest.expectedFileName)
        );

        if (!fileRequest.file.equals(permanentFile)) {
            boolean renamed = fileRequest.file.renameTo(permanentFile);

            if (!renamed) {
                permanentFile = fileRequest.file;
            }
        }

        com.example.aichat.model.entities.File entity =
                new com.example.aichat.model.entities.File(
                        uploadedFileId,
                        permanentFile.getAbsolutePath(),
                        null,
                        fileRequest.expectedFileType.name(),
                        permanentFile.length(),
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        fileRequest.expectedFileName
                );

        DatabaseManager
                .getDatabase()
                .fileDao()
                .insert(entity);
    }

    private CompletableFuture<HttpCommand> sendPrepareRequestWithRetry(PrepareSendMessageRequest request) {
        return retryCommand(
                () -> dispatcher.sendHttpRequestAsync(
                        "/api/messages/prepare",
                        HttpClient.HTTPMethod.POST,
                        request,
                        true
                ),
                0
        );
    }

    private CompletableFuture<HttpCommand> sendMessageRequest(MessageRequest request) {
        return retryCommand(
                () -> dispatcher.sendHttpRequestAsync(
                        "/api/messages",
                        HttpClient.HTTPMethod.POST,
                        request,
                        true
                ),
                0
        );
    }

    private CompletableFuture<HttpCommand> retryCommand(
            FutureSupplier<HttpCommand> supplier,
            int attempt
    ) {
        CompletableFuture<HttpCommand> result = new CompletableFuture<>();

        try {
            supplier.get().whenComplete((command, error) -> {
                boolean shouldRetry = error != null || shouldRetryCommand(command);

                if (shouldRetry && attempt < MAX_RETRY_ATTEMPTS) {
                    delayBeforeRetry(attempt)
                            .thenCompose(ignored -> retryCommand(supplier, attempt + 1))
                            .whenComplete((retryCommand, retryError) -> completeFrom(result, retryCommand, retryError));
                    return;
                }

                completeFrom(result, command, error);
            });
        } catch (Throwable throwable) {
            if (attempt < MAX_RETRY_ATTEMPTS) {
                delayBeforeRetry(attempt)
                        .thenCompose(ignored -> retryCommand(supplier, attempt + 1))
                        .whenComplete((retryCommand, retryError) -> completeFrom(result, retryCommand, retryError));
            } else {
                result.completeExceptionally(throwable);
            }
        }

        return result;
    }

    private CompletableFuture<Void> retryVoid(
            FutureSupplier<Void> supplier,
            int attempt
    ) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        try {
            supplier.get().whenComplete((ignored, error) -> {
                if (error != null && shouldRetryError(error) && attempt < MAX_RETRY_ATTEMPTS) {
                    delayBeforeRetry(attempt)
                            .thenCompose(v -> retryVoid(supplier, attempt + 1))
                            .whenComplete((retryIgnored, retryError) -> {
                                if (retryError != null) {
                                    result.completeExceptionally(retryError);
                                } else {
                                    result.complete(null);
                                }
                            });
                    return;
                }

                if (error != null) {
                    result.completeExceptionally(error);
                } else {
                    result.complete(null);
                }
            });
        } catch (Throwable throwable) {
            if (shouldRetryError(throwable) && attempt < MAX_RETRY_ATTEMPTS) {
                delayBeforeRetry(attempt)
                        .thenCompose(v -> retryVoid(supplier, attempt + 1))
                        .whenComplete((retryIgnored, retryError) -> {
                            if (retryError != null) {
                                result.completeExceptionally(retryError);
                            } else {
                                result.complete(null);
                            }
                        });
            } else {
                result.completeExceptionally(throwable);
            }
        }

        return result;
    }

    private CompletableFuture<Void> delayBeforeRetry(int attempt) {
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(RETRY_BASE_DELAY_MS * (attempt + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void completeFrom(
            CompletableFuture<HttpCommand> target,
            HttpCommand command,
            Throwable error
    ) {
        if (error != null) {
            target.completeExceptionally(error);
        } else {
            target.complete(command);
        }
    }

    private boolean shouldRetryCommand(HttpCommand command) {
        if (command == null) {
            return true;
        }

        int code = command.getCode();
        return code == 502 || code == 503 || code == 504 || code == -1;
    }

    private boolean shouldRetryError(Throwable error) {
        Throwable current = error;

        while (current != null) {
            if (current instanceof RetryableHttpException) {
                return true;
            }

            if (current instanceof java.io.IOException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private interface FutureSupplier<T> {
        CompletableFuture<T> get();
    }

    private static class RetryableHttpException extends RuntimeException {
        RetryableHttpException(Throwable cause) {
            super(cause);
        }
    }

    private List<UploadSessionFileRequest> getFiles(
            List<File> files,
            List<FileType> types,
            List<UUID> uploadFileIds,
            List<String> fileNames
    ) {
        ArrayList<UploadSessionFileRequest> list = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            return list;
        }

        for (int i = 0; i < files.size(); i++) {
            UUID uploadFileId = uploadFileIds != null && i < uploadFileIds.size()
                    ? uploadFileIds.get(i)
                    : null;

            String expectedFileName = fileNames != null && i < fileNames.size()
                    ? fileNames.get(i)
                    : null;

            list.add(new UploadSessionFileRequest(
                    uploadFileId,
                    files.get(i),
                    types.get(i),
                    expectedFileName
            ));
        }

        return list;
    }

    private String sanitizeFileName(String fileName) {
        String safeName = fileName != null ? fileName.trim() : "";

        if (safeName.isEmpty()) {
            safeName = "file";
        }

        return safeName
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static <T> CompletableFuture<T> failedFuture(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }
}
