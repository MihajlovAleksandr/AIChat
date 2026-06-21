package com.example.aichat.dto.request;

import androidx.annotation.Nullable;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class RemoveUserFromChatRequest {
    public final @Nullable UUID userId;
    public final UUID chatId;
    public RemoveUserFromChatRequest(@JsonProperty("userId") @Nullable UUID userId,
    @JsonProperty("chatId") UUID chatId){
        this.chatId = chatId;
        this.userId = userId;
    }
}
