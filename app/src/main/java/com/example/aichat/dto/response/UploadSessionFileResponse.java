package com.example.aichat.dto.response;

import com.example.aichat.model.entities.FileType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class UploadSessionFileResponse {

    public final UUID id;
    public final String expectedFileName;
    public final FileType expectedFileType;
    public final long expectedFileSize;

    @JsonCreator
    public UploadSessionFileResponse(
            @JsonProperty("id") UUID id,
            @JsonProperty("expectedFileName") String expectedFileName,
            @JsonProperty("expectedFileType") FileType expectedFileType,
            @JsonProperty("expectedFileSize") long expectedFileSize
    ) {
        this.id = id;
        this.expectedFileName = expectedFileName;
        this.expectedFileType = expectedFileType;
        this.expectedFileSize = expectedFileSize;
    }

    @Override
    public String toString() {
        return "UploadSessionFileResponse { " +
                "id=" + id +
                ", expectedFileName='" + expectedFileName + '\'' +
                ", expectedFileType=" + expectedFileType +
                ", expectedFileSize=" + expectedFileSize +
                " }";
    }
}