package com.example.aichat.model.connection;

import java.util.UUID;

public class UploadProgress {

    private final UUID fileId;
    private final long totalBytes;
    private final long uploadedBytes;

    public UploadProgress(UUID fileId, long totalBytes, long uploadedBytes) {
        this.fileId = fileId;
        this.totalBytes = totalBytes;
        this.uploadedBytes = uploadedBytes;
    }

    public UUID getFileId() {
        return fileId;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getUploadedBytes() {
        return uploadedBytes;
    }

    public int getPercent() {
        if (totalBytes == 0) return 0;
        return (int) ((uploadedBytes * 100) / totalBytes);
    }
}
