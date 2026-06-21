package com.example.aichat.controller;

import com.example.aichat.dto.request.CreateThemeRequest;
import com.example.aichat.dto.response.ThemeResponse;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.entities.HttpCommand;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

public class ThemeController {

    private final ConnectionDispatcher dispatcher;

    public ThemeController(ConnectionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public CompletableFuture<ThemeResponse> sendTheme(
            String themeName,
            String themeContentJson
    ) {
        CreateThemeRequest request = new CreateThemeRequest(
                themeName,
                themeContentJson
        );

        return dispatcher.sendHttpRequestAsync(
                "/api/themes",
                HttpClient.HTTPMethod.POST,
                request,
                false
        ).thenApply(cmd -> {
            if (!cmd.isSuccess()) {
                throw new RuntimeException("Failed to create theme: " + cmd.getCode());
            }

            return cmd.getData(ThemeResponse.class);
        });
    }

    public CompletableFuture<ThemeResponse> updateTheme(
            UUID themeId,
            String themeName,
            String themeContentJson
    ) {
        CreateThemeRequest request = new CreateThemeRequest(
                themeName,
                themeContentJson
        );

        return dispatcher.sendHttpRequestAsync(
                "/api/themes/" + themeId,
                HttpClient.HTTPMethod.PUT,
                request,
                false
        ).thenApply(cmd -> {
            if (!cmd.isSuccess()) {
                throw new RuntimeException("Failed to update theme: " + cmd.getCode());
            }

            return cmd.getData(ThemeResponse.class);
        });
    }

    public CompletableFuture<Boolean> deleteTheme(UUID themeId) {
        return dispatcher.sendHttpRequestAsync(
                "/api/themes/" + themeId,
                HttpClient.HTTPMethod.DELETE,
                null,
                false
        ).thenApply(HttpCommand::isSuccess);
    }

    public CompletableFuture<Void> selectTheme(UUID themeId) {
        return dispatcher.sendHttpRequestAsync(
                "/api/themes/selected/" + themeId,
                HttpClient.HTTPMethod.POST,
                null,
                false
        ).thenApply(cmd -> {
            if (!cmd.isSuccess()) {
                throw new RuntimeException("Failed to select theme: " + cmd.getCode());
            }

            return null;
        });
    }

    public CompletableFuture<ThemeResponse[]> getThemes() {
        return dispatcher.sendHttpRequestAsync(
                "/api/themes/user",
                HttpClient.HTTPMethod.GET,
                null,
                false
        ).thenApply(cmd -> {
            if (!cmd.isSuccess()) {
                throw new RuntimeException("Failed to load themes: " + cmd.getCode());
            }

            return cmd.getData(ThemeResponse[].class);
        });
    }
}
