package com.example.aichat.dto.request;

import androidx.annotation.Nullable;

import com.example.aichat.model.entities.FileType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
            FileType fileType)
    {
        id = UUID.randomUUID();
        this.expectedFileName = file.getName();
        this.expectedFileType = fileType;
        this.expectedFileSize = file.length();
        this.file = file;
    }

    @JsonIgnore
    public boolean verify(UUID id){
        return this.id.equals(id);
    }
}
