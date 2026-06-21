package com.example.aichat.dto.request;

import androidx.annotation.Nullable;
import com.example.aichat.model.entities.FileType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.File;
import java.util.UUID;

public class UploadSessionFileRequest {
    public final UUID id;
    public final String expectedFileName;
    public final FileType expectedFileType;
    public final long expectedFileSize;
    @JsonIgnore
    public File file;

    @JsonIgnore
    public UploadSessionFileRequest(
            File file,
            FileType fileType
    ) {
        this(
                UUID.randomUUID(),
                file,
                fileType
        );
    }

    @JsonIgnore
    public UploadSessionFileRequest(
            @Nullable UUID id,
            File file,
            FileType fileType
    ) {
        this(
                id,
                file,
                fileType,
                file != null ? file.getName() : null
        );
    }

    @JsonIgnore
    public UploadSessionFileRequest(
            @Nullable UUID id,
            File file,
            FileType fileType,
            @Nullable String expectedFileName
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.expectedFileName = resolveExpectedFileName(file, expectedFileName);
        this.expectedFileType = fileType;
        this.expectedFileSize = file != null ? file.length() : 0L;
        this.file = file;
    }

    private static String resolveExpectedFileName(
            @Nullable File file,
            @Nullable String expectedFileName
    ) {
        if (expectedFileName != null && !expectedFileName.trim().isEmpty()) {
            return expectedFileName.trim();
        }

        if (file != null && file.getName() != null && !file.getName().trim().isEmpty()) {
            return file.getName();
        }

        return "file";
    }

    @JsonIgnore
    public boolean verify(UUID id) {
        return this.id.equals(id);
    }
}
