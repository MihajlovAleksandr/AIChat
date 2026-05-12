package com.example.aichat.controller.main.chat.actions;

import android.content.Context;
import android.os.Build;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class SendMessageController {

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

    public CompletableFuture<HttpCommand> sendMessage(
            MessageInfo info
    ) {

        UUID messageId = info.id;

        if (!info.hasFiles()) {

            MessageRequest request =
                    new MessageRequest(
                            messageId,
                            info.chatId,
                            info.text,
                            info.replies,
                            null
                    );

            return sendMessageRequest(request);
        }

        if (
                info.fileTypes == null ||
                        info.fileTypes.size() != info.files.size()
        ) {

            throw new IllegalStateException(
                    "Invalid fileTypes in MessageInfo"
            );
        }

        AtomicReference<UUID> fileSessionId =
                new AtomicReference<>();

        List<UploadSessionFileRequest> fileRequests =
                getFiles(info.files, info.fileTypes);

        return dispatcher.sendHttpRequestAsync(
                        "/api/messages/prepare",
                        HttpClient.HTTPMethod.POST,
                        new PrepareSendMessageRequest(
                                info.chatId,
                                info.text != null
                                        ? info.text.length()
                                        : 0,
                                fileRequests,
                                info.replies != null
                                        ? info.replies.size()
                                        : 0
                        ),
                        true
                )
                .thenCompose(cmd -> {

                    if (!cmd.isSuccess()) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                            return CompletableFuture.failedFuture(
                                    new RuntimeException(
                                            "Prepare failed"
                                    )
                            );
                        }
                    }

                    UploadSessionResponse response =
                            cmd.getData(
                                    UploadSessionResponse.class
                            );

                    fileSessionId.set(response.id);

                    List<CompletableFuture<HttpCommand>>
                            uploadFutures =
                            new ArrayList<>();

                    for (
                            UploadSessionFileRequest fileRequest
                            : fileRequests
                    ) {

                        for (
                                UploadSessionFileResponse current
                                : response.files
                        ) {

                            if (!fileRequest.verify(current.id)) {
                                continue;
                            }

                            CompletableFuture<HttpCommand>
                                    uploadFuture =
                                    dispatcher.uploadFile(
                                                    "/api/files",
                                                    new UploadFileRequest(
                                                            fileSessionId.get(),
                                                            current.id,
                                                            fileRequest.expectedFileType
                                                    ),
                                                    fileRequest.file,
                                                    current.id,
                                                    listener
                                            )
                                            .thenApply(cmd2 -> {

                                                if (!cmd2.isSuccess()) {

                                                    throw new RuntimeException(
                                                            "Upload failed: "
                                                                    + fileRequest.expectedFileName
                                                    );
                                                }

                                                File permanentFile =
                                                        new File(
                                                                context.getFilesDir(),
                                                                current.id +
                                                                        "_" +
                                                                        fileRequest.file.getName()
                                                        );

                                                if (
                                                        !fileRequest.file.equals(
                                                                permanentFile
                                                        )
                                                ) {

                                                    boolean renamed =
                                                            fileRequest.file.renameTo(
                                                                    permanentFile
                                                            );

                                                    if (!renamed) {
                                                        permanentFile =
                                                                fileRequest.file;
                                                    }
                                                }

                                                com.example.aichat.model.entities.File entity =
                                                        new com.example.aichat.model.entities.File(
                                                                current.id,
                                                                permanentFile.getAbsolutePath(),
                                                                null,
                                                                fileRequest.expectedFileType.name(),
                                                                permanentFile.length(),
                                                                System.currentTimeMillis(),
                                                                System.currentTimeMillis(),
                                                                permanentFile.getName()
                                                        );

                                                DatabaseManager
                                                        .getDatabase()
                                                        .fileDao()
                                                        .insert(entity);

                                                return cmd2;
                                            });

                            uploadFutures.add(uploadFuture);

                            break;
                        }
                    }

                    return CompletableFuture.allOf(
                            uploadFutures.toArray(
                                    new CompletableFuture[0]
                            )
                    );
                })
                .thenCompose(v -> {

                    MessageRequest request =
                            new MessageRequest(
                                    messageId,
                                    info.chatId,
                                    info.text,
                                    info.replies,
                                    fileSessionId.get()
                            );

                    return sendMessageRequest(request);
                });
    }

    private CompletableFuture<HttpCommand>
    sendMessageRequest(
            MessageRequest request
    ) {

        return dispatcher.sendHttpRequestAsync(
                "/api/messages",
                HttpClient.HTTPMethod.POST,
                request,
                true
        );
    }

    private List<UploadSessionFileRequest> getFiles(
            List<File> files,
            List<FileType> types
    ) {

        ArrayList<UploadSessionFileRequest> list =
                new ArrayList<>();

        if (files.isEmpty()) {
            return list;
        }

        for (int i = 0; i < files.size(); i++) {

            list.add(
                    new UploadSessionFileRequest(
                            files.get(i),
                            types.get(i)
                    )
            );
        }

        return list;
    }
}