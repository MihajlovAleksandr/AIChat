package com.example.aichat.dto.request;

import com.example.aichat.model.entities.FileType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class UploadFileRequest {
    public final UUID sessionId;
    public final UUID fileId;
    public final FileType fileType;
    public  UploadFileRequest(
            @JsonProperty("sessionId") UUID sessionId,
            @JsonProperty("fileId") UUID fileId,
            @JsonProperty("fileType") FileType fileType

    )
    {
        this.sessionId = sessionId;
        this.fileId = fileId;
        this.fileType = fileType;
    }
}
