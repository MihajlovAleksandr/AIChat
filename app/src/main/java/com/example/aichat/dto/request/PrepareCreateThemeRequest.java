package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PrepareCreateThemeRequest {
    public final String name;
    public final UploadSessionFileRequest file;

    @JsonCreator
    public PrepareCreateThemeRequest(
            @JsonProperty("name") String name,
            @JsonProperty("file") UploadSessionFileRequest file) {
        this.name = name;
        this.file = file;
    }
}
