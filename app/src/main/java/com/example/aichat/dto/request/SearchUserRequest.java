package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class SearchUserRequest {
    public final UUID chatId;
    public final String chatMatchPredicate;
    public final int slots;

    @JsonCreator
    public SearchUserRequest(@JsonProperty("chatId") UUID chatId,
                             @JsonProperty("chatMatchPredicate") String chatMatchPredicate) {
        this.chatId = chatId;
        this.chatMatchPredicate = chatMatchPredicate;
        this.slots = 1;
    }

    @Override
    public String toString() {
        return "AddOtherUserToChatRequest  { chatId: " + chatId + ", chatMatchPredicate: "+chatMatchPredicate+" }";
    }
}
