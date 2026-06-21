package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public class PrepareSendMessageRequest {
    public final UUID chatId;
    public final int textLength;
    public final List<UploadSessionFileRequest> files;
    public final int repliesCount;

    @JsonCreator
    public PrepareSendMessageRequest(
            @JsonProperty("chatId")  UUID chatId,
            @JsonProperty("textLength") int textLength,
            @JsonProperty("files") List<UploadSessionFileRequest> files,
            @JsonProperty("repliesCount") int repliesCount
    )
    {
        this.chatId = chatId;
        this.textLength = textLength;
        this.files = files;
        this.repliesCount = repliesCount;
    }
}
