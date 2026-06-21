package com.example.aichat.model.entities;

import androidx.annotation.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.UUID;

public class MessageReply {

    @JsonProperty
    public UUID replyMessageId;

    @JsonProperty
    @Nullable
    public Integer startIndexQuote;

    @JsonProperty
    @Nullable
    public Integer endIndexQuote;

    public MessageReply(UUID replyMessageId) {
        this.replyMessageId = replyMessageId;
        this.startIndexQuote = null;
        this.endIndexQuote = null;
    }

    @JsonCreator
    public MessageReply(
            @JsonProperty("replyMessageId") UUID replyMessageId,
            @JsonProperty("startIndexQuote") @Nullable Integer startIndexQuote,
            @JsonProperty("endIndexQuote") @Nullable Integer endIndexQuote
    ) {
        this.replyMessageId = replyMessageId;
        this.startIndexQuote = startIndexQuote;
        this.endIndexQuote = endIndexQuote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageReply)) return false;
        MessageReply that = (MessageReply) o;
        return Objects.equals(replyMessageId, that.replyMessageId) &&
                Objects.equals(startIndexQuote, that.startIndexQuote) &&
                Objects.equals(endIndexQuote, that.endIndexQuote);
    }

    @Override
    public int hashCode() {
        return Objects.hash(replyMessageId, startIndexQuote, endIndexQuote);
    }

    @Override
    public String toString() {
        return "MessageReply{" +
                "replyMessageId=" + replyMessageId +
                ", startIndexQuote=" + startIndexQuote +
                ", endIndexQuote=" + endIndexQuote +
                '}';
    }
}
