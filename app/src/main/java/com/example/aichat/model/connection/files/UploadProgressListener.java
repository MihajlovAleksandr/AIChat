package com.example.aichat.model.connection.files;

import com.example.aichat.model.connection.UploadProgress;

public interface UploadProgressListener {
    void onProgress(UploadProgress progress);
}