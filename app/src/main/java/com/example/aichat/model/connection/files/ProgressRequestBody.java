package com.example.aichat.model.connection.files;

import androidx.annotation.NonNull;

import com.example.aichat.model.connection.UploadProgress;
import com.example.aichat.model.connection.files.UploadProgressListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class ProgressRequestBody extends RequestBody {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final File file;
    private final String contentType;
    private final UploadProgressListener listener;
    private final UUID fileId;

    public ProgressRequestBody(
            File file,
            String contentType,
            UUID fileId,
            UploadProgressListener listener
    ) {
        this.file = file;
        this.contentType = contentType;
        this.listener = listener;
        this.fileId = fileId;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public void writeTo(@NonNull okio.BufferedSink sink) throws IOException {

        long fileLength = file.length();
        byte[] buffer = new byte[4096];

        try (FileInputStream inputStream = new FileInputStream(file)) {

            long uploaded = 0;
            int read;

            while ((read = inputStream.read(buffer)) != -1) {
                sink.write(buffer, 0, read);
                uploaded += read;

                if (listener != null && fileLength > 0) {
                    long finalUploaded = uploaded;

                    android.os.Handler handler =
                            new android.os.Handler(android.os.Looper.getMainLooper());

                    handler.post(() ->
                            listener.onProgress(
                                    new UploadProgress(fileId, fileLength, finalUploaded)
                            )
                    );
                }
            }
        }
    }
}