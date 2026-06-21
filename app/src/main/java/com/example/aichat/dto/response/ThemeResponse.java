package com.example.aichat.dto.response;

import androidx.annotation.Nullable;
import com.example.aichat.model.entities.ThemeType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class ThemeResponse {
    public final UUID id;
    public final String name;
    public final ThemeType type;
    public final String content;
    public final long usageCount;

    @JsonCreator
    public ThemeResponse(@JsonProperty("id") UUID id,
                         @JsonProperty("name") String name,
                         @JsonProperty("type") ThemeType type,
                         @JsonProperty("content") String content,
                         @JsonProperty("usageCount") long usageCount) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.content = content;
        this.usageCount = usageCount;
    }

}
