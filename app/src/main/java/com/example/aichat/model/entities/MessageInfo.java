package com.example.aichat.model.entities;

import androidx.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.UUID;

public class MessageInfo {
    public final UUID id;
    public final UUID chatId;
    public final String text;
    public final List<MessageReply> replies;
    @Nullable
    public final List<FileType> fileTypes;
    public final List<File> files;
    @Nullable
    public final List<UUID> uploadFileIds;
    @Nullable
    public final List<String> fileNames;

    public MessageInfo(
            UUID id,
            UUID chatId,
            String text,
            List<MessageReply> replies,
            List<FileType> fileTypes,
            List<File> files
    ) {
        this(
                id,
                chatId,
                text,
                replies,
                fileTypes,
                files,
                null,
                null
        );
    }

    public MessageInfo(
            UUID id,
            UUID chatId,
            String text,
            List<MessageReply> replies,
            List<FileType> fileTypes,
            List<File> files,
            @Nullable List<UUID> uploadFileIds
    ) {
        this(
                id,
                chatId,
                text,
                replies,
                fileTypes,
                files,
                uploadFileIds,
                null
        );
    }

    public MessageInfo(
            UUID id,
            UUID chatId,
            String text,
            List<MessageReply> replies,
            List<FileType> fileTypes,
            List<File> files,
            @Nullable List<UUID> uploadFileIds,
            @Nullable List<String> fileNames
    ) {
        if (files != null && fileTypes != null && files.size() != fileTypes.size()) {
            throw new IllegalArgumentException("files and fileTypes size mismatch");
        }

        if (files != null && uploadFileIds != null && files.size() != uploadFileIds.size()) {
            throw new IllegalArgumentException("files and uploadFileIds size mismatch");
        }

        if (files != null && fileNames != null && files.size() != fileNames.size()) {
            throw new IllegalArgumentException("files and fileNames size mismatch");
        }

        this.id = id;
        this.chatId = chatId;
        this.text = text;
        this.replies = replies;
        this.fileTypes = fileTypes;
        this.files = files;
        this.uploadFileIds = uploadFileIds;
        this.fileNames = fileNames;
    }

    public boolean hasFiles() {
        return files != null && !files.isEmpty();
    }
}
