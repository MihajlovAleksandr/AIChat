package com.example.aichat.dto.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.ai.AIModel;
import com.example.aichat.model.ai.TranslateStyle;
import java.util.UUID;

public class TranslateRequest {

    public UUID chatId;
    public String model;
    public String langCode;
    public String style;
    public String message;

    public TranslateRequest(
            @NonNull UUID chatId,
            @Nullable AIModel model,
            @Nullable String langCode,
            @Nullable TranslateStyle style,
            @Nullable String message
    ) {
        this.chatId = chatId;
        this.model = (model != null ? model : AIModel.Default).getServerValue();
        this.langCode = langCode != null ? langCode : "";
        this.style = (style != null ? style : TranslateStyle.Neutral).getServerValue();
        this.message = message != null ? message : "";
    }
}
