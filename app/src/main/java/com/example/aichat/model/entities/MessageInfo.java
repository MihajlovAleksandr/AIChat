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

    public MessageInfo(
            UUID id,
            UUID chatId,
            String text,
            List<MessageReply> replies,
            List<FileType> fileTypes,
            List<File> files)
    {
        if (files != null && fileTypes != null && files.size() != fileTypes.size())
            throw new IllegalArgumentException("files and fileTypes size mismatch");

        this.id = id;
        this.chatId = chatId;
        this.text = text;
        this.replies = replies;
        this.fileTypes = fileTypes;
        this.files = files;
    }

    public boolean hasFiles(){
        return !files.isEmpty();
    }
}
