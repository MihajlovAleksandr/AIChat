package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class UploadSessionResponse {

    public final UUID id;
    public final Collection<UploadSessionFileResponse> files;

    @JsonCreator
    public UploadSessionResponse(
            @JsonProperty("id") UUID id,
            @JsonProperty("files") Collection<UploadSessionFileResponse> files
    ) {
        this.id = id;
        this.files = files != null
                ? Collections.unmodifiableCollection(files)
                : Collections.emptyList();
    }

    @Override
    public String toString() {
        return "UploadSessionResponse { " +
                "id=" + id +
                ", files=" + files +
                " }";
    }
}
