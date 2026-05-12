package com.example.aichat.model.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "files")
public class File {

    @PrimaryKey
    @NonNull
    public UUID fileId;

    @NonNull
    public String localPath;

    public String mimeType;

    public String fileType;

    public long size;

    public long downloadedAt;

    public long lastOpenedAt;

    public String fileName;

    public File(@NonNull UUID fileId,
                @NonNull String localPath,
                String mimeType,
                String fileType,
                long size,
                long downloadedAt,
                long lastOpenedAt,
                String fileName) {
        this.fileId = fileId;
        this.localPath = localPath;
        this.mimeType = mimeType;
        this.fileType = fileType;
        this.size = size;
        this.downloadedAt = downloadedAt;
        this.lastOpenedAt = lastOpenedAt;
        this.fileName = fileName;
    }
}