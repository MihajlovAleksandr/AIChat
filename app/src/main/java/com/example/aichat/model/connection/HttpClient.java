package com.example.aichat.model.connection;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.aichat.model.entities.CommandOperation;
import com.example.aichat.model.entities.HttpCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpClient {

    public enum HTTPMethod {
        GET, POST, PUT, DELETE
    }

    private final String serverUrl = "http://192.168.0.102:5000/";
    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    private static final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient client;

    public HttpClient() {
        this.client = new OkHttpClient();
    }

    public CompletableFuture<HttpCommand> fetchAsync(
            String url,
            HTTPMethod method,
            @Nullable String jsonBody
    ) {
        CompletableFuture<HttpCommand> future = new CompletableFuture<>();

        if (url == null || url.trim().isEmpty()) {
            JsonNode err = mapper.createObjectNode()
                    .put("error", true)
                    .put("message", "URL is empty");

            future.complete(new HttpCommand(CommandOperation.BAD_REQUEST, err));
            return future;
        }

        Log.e("fetchAsync", serverUrl + url);

        Request request = buildRequest(serverUrl + url, method, jsonBody);

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                JsonNode errorNode = mapper.createObjectNode()
                        .put("error", true)
                        .put("message", e.getMessage() != null ? e.getMessage() : "Network error");

                future.complete(new HttpCommand(CommandOperation.SERVICE_UNAVAILABLE, errorNode));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                int httpCode = response.code();
                String raw = response.body() != null ? response.body().string() : "";

                if (raw == null || raw.trim().isEmpty()) {
                    JsonNode emptyNode = mapper.createObjectNode()
                            .put("empty", true)
                            .put("httpCode", httpCode);

                    future.complete(new HttpCommand(
                            httpCode == 204
                                    ? CommandOperation.NO_CONTENT
                                    : CommandOperation.valueOfCode(httpCode),
                            emptyNode
                    ));
                    return;
                }

                try {
                    JsonNode dataNode = mapper.readTree(raw);

                    future.complete(new HttpCommand(
                            CommandOperation.valueOfCode(httpCode),
                            dataNode
                    ));

                } catch (Exception parseEx) {
                    JsonNode fallback = mapper.createObjectNode()
                            .put("httpCode", httpCode)
                            .put("raw", raw)
                            .put("parseError", true);

                    future.complete(new HttpCommand(
                            CommandOperation.UNPROCESSABLE_ENTITY,
                            fallback
                    ));
                }
            }
        });

        return future;
    }

    private Request buildRequest(String url, HTTPMethod method, @Nullable String jsonBody) {
        String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;

        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("device", deviceModel);;

        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1laWRlbnRpZmllciI6ImRkMjUwYjQ2LWQyYTYtNGI4ZC05ZTgzLTQyYmE5MmE2MzBlZCIsInN1YiI6ImRkMjUwYjQ2LWQyYTYtNGI4ZC05ZTgzLTQyYmE5MmE2MzBlZCIsInRva2VuVHlwZSI6IldvcmsiLCJjb25uZWN0aW9uSWQiOiI0NGQ5MTcyNC00NTQzLTRmNWEtYmVjYS0wOTZkMzUxODM5ZTgiLCJqdGkiOiI5ZDNhYWE2YS03YmYwLTQzNTEtYTk3Yy0xZTY5OTk2NzM4NzYiLCJpYXQiOjE3NzUwODg2MjIsImV4cCI6MTc3NzY4MDYyMiwiaXNzIjoiTXlBcHAiLCJhdWQiOiJNeUFwcC5DbGllbnQifQ.IcvpaEKDVwJpcZI9aaGtfJl8WkYuO4VpoAV4c-yXq3I";

        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        switch (method) {
            case GET:
                builder.get();
                break;

            case POST:
                builder.post(makeJsonBody(jsonBody));
                builder.addHeader("Content-Type", "application/json");
                break;

            case PUT:
                builder.put(makeJsonBody(jsonBody));
                builder.addHeader("Content-Type", "application/json");
                break;

            case DELETE:
                if (jsonBody != null && !jsonBody.trim().isEmpty()) {
                    builder.delete(makeJsonBody(jsonBody));
                    builder.addHeader("Content-Type", "application/json");
                } else {
                    builder.delete();
                }
                break;
        }

        return builder.build();
    }

    private RequestBody makeJsonBody(@Nullable String jsonBody) {
        if (jsonBody == null || jsonBody.trim().isEmpty()) {
            jsonBody = "{}";
        }
        return RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
    }
}
