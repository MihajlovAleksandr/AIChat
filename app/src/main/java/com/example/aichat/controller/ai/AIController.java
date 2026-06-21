package com.example.aichat.controller.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.dto.request.TranslateRequest;
import com.example.aichat.dto.response.AIResultResponse;
import com.example.aichat.dto.response.ApiError;
import com.example.aichat.model.ai.AIModel;
import com.example.aichat.model.ai.TranslateStyle;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.HttpClient;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

public class AIController {

    private final ConnectionDispatcher dispatcher;

    public AIController(@NonNull ConnectionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /*
     * ВАЖНО:
     * Перевод не использует модель, выбранную в настройках для AI-чатов.
     * По требованию модель из настроек распространяется только на чаты.
     */
    @NonNull
    public CompletableFuture<String> translateWithDefaultModel(
            @NonNull UUID chatId,
            @NonNull String langCode,
            @NonNull TranslateStyle style,
            @NonNull String message
    ) {
        return translate(
                chatId,
                AIModel.Default,
                langCode,
                style,
                message
        );
    }

    @NonNull
    public CompletableFuture<String> translate(
            @NonNull UUID chatId,
            @Nullable AIModel model,
            @NonNull String langCode,
            @Nullable TranslateStyle style,
            @NonNull String message
    ) {
        TranslateRequest request = new TranslateRequest(
                chatId,
                model != null ? model : AIModel.Default,
                langCode,
                style != null ? style : TranslateStyle.Neutral,
                message
        );

        return dispatcher.sendHttpRequestAsync(
                        "/api/ai/translate",
                        HttpClient.HTTPMethod.POST,
                        request,
                        false
                )
                .thenApply(command -> {
                    if (command != null && command.isSuccess()) {
                        AIResultResponse response = command.getData(AIResultResponse.class);

                        if (response != null && response.result != null) {
                            return response.result;
                        }

                        return "";
                    }

                    String errorText = "Не удалось выполнить AI-перевод";

                    try {
                        ApiError error = command != null
                                ? command.getData(ApiError.class)
                                : null;

                        if (error != null) {
                            errorText = error.toString();
                        }
                    } catch (Exception ignored) {
                    }

                    throw new RuntimeException(errorText);
                });
    }
}
