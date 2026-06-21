package com.example.aichat.model.connection;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.BuildConfig;
import com.example.aichat.model.connection.files.ProgressRequestBody;
import com.example.aichat.model.connection.files.UploadProgressListener;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.database.PendingCommandDao;
import com.example.aichat.model.entities.CommandOperation;
import com.example.aichat.model.entities.HttpCommand;
import com.example.aichat.model.entities.PendingCommand;
import com.example.aichat.model.utils.JsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpClient {

    public enum HTTPMethod {
        GET, POST, PUT, DELETE
    }

    public interface UnauthorizedHandler {
        void onUnauthorized();
    }

    private static final String TAG = "HttpClient";

    @Nullable
    private String token;

    private final PendingCommandDao database =
            DatabaseManager.getDatabase().pendingCommandDao();

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    private static final ObjectMapper mapper =
            new ObjectMapper();

    private static final long DEFAULT_CONNECT_TIMEOUT_SECONDS = 30L;
    private static final long DEFAULT_READ_TIMEOUT_SECONDS = 30L;
    private static final long DEFAULT_WRITE_TIMEOUT_SECONDS = 30L;

    private static final long UPLOAD_TIMEOUT_SECONDS = 90L;

    private final OkHttpClient client;
    private final OkHttpClient uploadClient;

    @Nullable
    private UnauthorizedHandler unauthorizedHandler;

    public HttpClient(@Nullable String token) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        this.uploadClient = this.client.newBuilder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(UPLOAD_TIMEOUT_SECONDS + DEFAULT_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        this.token = token;
    }

    public void setUnauthorizedHandler(@Nullable UnauthorizedHandler handler) {
        this.unauthorizedHandler = handler;
    }

    public CompletableFuture<HttpCommand> fetchAsync(
            String url,
            HTTPMethod method,
            @Nullable Object body,
            boolean isCritical
    ) {
        return fetchAsync(url, method, body, isCritical, true);
    }

    public CompletableFuture<HttpCommand> fetchAsync(
            String url,
            HTTPMethod method,
            @Nullable Object body,
            boolean isCritical,
            boolean triggerUnauthorizedHandler
    ) {
        CompletableFuture<HttpCommand> future =
                new CompletableFuture<>();

        if (url == null || url.trim().isEmpty()) {
            JsonNode err = mapper.createObjectNode()
                    .put("error", true)
                    .put("message", "URL is empty");

            future.complete(new HttpCommand(CommandOperation.BAD_REQUEST, err));
            return future;
        }

        String jsonBody =
                body != null ? JsonHelper.Serialize(body) : null;

        String serverUrl =
                BuildConfig.SERVER_URL;

        Request request =
                buildRequest(serverUrl + url, method, jsonBody);

        client.newCall(request)
                .enqueue(createCallback(
                        future,
                        url,
                        method,
                        body,
                        isCritical,
                        triggerUnauthorizedHandler
                ));

        return future;
    }

    private String guessMimeType(File file) {
        String name =
                file.getName().toLowerCase();

        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";

        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".txt")) return "text/plain";
        if (name.endsWith(".doc")) return "application/msword";
        if (name.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (name.endsWith(".xls")) return "application/vnd.ms-excel";
        if (name.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (name.endsWith(".zip")) return "application/zip";
        if (name.endsWith(".rar")) return "application/vnd.rar";

        if (name.endsWith(".mp4")) return "video/mp4";
        if (name.endsWith(".mkv")) return "video/x-matroska";
        if (name.endsWith(".webm")) return "video/webm";

        if (name.endsWith(".mp3")) return "audio/mpeg";
        if (name.endsWith(".wav")) return "audio/wav";
        if (name.endsWith(".ogg")) return "audio/ogg";
        if (name.endsWith(".m4a")) return "audio/mp4";

        return "application/octet-stream";
    }

    public CompletableFuture<HttpCommand> uploadAsync(
            String url,
            @Nullable Object body,
            @NonNull File file,
            @NonNull UUID fileId,
            @Nullable String uploadFileName,
            @Nullable UploadProgressListener listener
    ) {
        CompletableFuture<HttpCommand> future =
                new CompletableFuture<>();

        try {
            String serverUrl =
                    BuildConfig.SERVER_URL;

            MultipartBody.Builder multipartBuilder =
                    new MultipartBody.Builder()
                            .setType(MultipartBody.FORM);

            if (body != null) {
                JsonNode node =
                        mapper.valueToTree(body);

                Iterator<Map.Entry<String, JsonNode>> fields =
                        node.fields();

                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry =
                            fields.next();

                    multipartBuilder.addFormDataPart(
                            entry.getKey(),
                            entry.getValue().asText()
                    );
                }
            }

            String mimeType =
                    guessMimeType(file);

            ProgressRequestBody fileBody =
                    new ProgressRequestBody(
                            file,
                            mimeType,
                            fileId,
                            listener
                    );

            String multipartFileName =
                    resolveMultipartFileName(
                            uploadFileName,
                            file
                    );

            multipartBuilder.addFormDataPart(
                    "file",
                    multipartFileName,
                    fileBody
            );

            Request request =
                    buildMultipartRequest(
                            serverUrl + url,
                            multipartBuilder.build()
                    );

            uploadClient.newCall(request)
                    .enqueue(createCallback(
                            future,
                            url,
                            HTTPMethod.POST,
                            null,
                            false,
                            true
                    ));

        } catch (Exception ex) {
            JsonNode errorNode =
                    mapper.createObjectNode()
                            .put("error", true)
                            .put("message", ex.getMessage());

            future.complete(
                    new HttpCommand(
                            CommandOperation.INTERNAL_SERVER_ERROR,
                            errorNode
                    )
            );
        }

        return future;
    }

    public CompletableFuture<File> downloadFileAsync(
            String url,
            UUID fileId,
            @Nullable UploadProgressListener listener
    ) {
        CompletableFuture<File> future =
                new CompletableFuture<>();

        if (url == null || url.trim().isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("URL is empty"));
            return future;
        }

        String serverUrl =
                BuildConfig.SERVER_URL;

        Request.Builder requestBuilder =
                new Request.Builder()
                        .url(serverUrl + url)
                        .addHeader("Accept", "*/*")
                        .addHeader("Accept-Language", getAcceptLanguage())
                        .addHeader("device", Build.MANUFACTURER + " " + Build.MODEL);

        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }

        Request request =
                requestBuilder.get().build();

        Log.d(TAG, "=== DOWNLOAD FILE REQUEST ===");
        Log.d(TAG, "URL: " + (serverUrl + url));
        Log.d(TAG, "FileId: " + fileId);
        Log.d(TAG, "============================");

        client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e(TAG, "Download failed for URL: " + url + " - " + e.getMessage());
                        future.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        int httpCode =
                                response.code();

                        if (httpCode == 401 && unauthorizedHandler != null) {
                            unauthorizedHandler.onUnauthorized();
                            future.completeExceptionally(new IOException("Unauthorized"));
                            return;
                        }

                        if (httpCode != 200) {
                            future.completeExceptionally(
                                    new IOException("Server returned code: " + httpCode)
                            );
                            return;
                        }

                        String contentDisposition =
                                response.header("Content-Disposition");

                        String fileName =
                                extractFileNameFromContentDisposition(contentDisposition);

                        if (fileName == null) {
                            fileName = fileId.toString();
                        }

                        Context context =
                                ConnectionSingleton.getInstance()
                                        .getConnectionDispatcher()
                                        .getTokenStorage()
                                        .getContext();

                        File outputFile =
                                new File(
                                        context.getCacheDir(),
                                        "downloaded_" + System.currentTimeMillis() + "_" + fileName
                                );

                        android.os.Handler mainHandler =
                                new android.os.Handler(android.os.Looper.getMainLooper());

                        try (okhttp3.ResponseBody responseBody = response.body();
                             java.io.InputStream inputStream = responseBody != null ? responseBody.byteStream() : null;
                             java.io.FileOutputStream outputStream = new java.io.FileOutputStream(outputFile)) {

                            if (inputStream == null) {
                                throw new IOException("Response body is null");
                            }

                            long totalBytes =
                                    responseBody != null ? responseBody.contentLength() : -1;

                            long downloadedBytes =
                                    0;

                            byte[] buffer =
                                    new byte[8192];

                            int bytesRead;

                            if (listener != null) {
                                long safeTotal =
                                        totalBytes > 0 ? totalBytes : 1;

                                listener.onProgress(
                                        new com.example.aichat.model.connection.files.UploadProgress(
                                                fileId,
                                                safeTotal,
                                                0
                                        )
                                );
                            }

                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                                downloadedBytes += bytesRead;

                                if (listener != null) {
                                    long finalDownloaded =
                                            downloadedBytes;

                                    long finalTotal =
                                            totalBytes > 0 ? totalBytes : finalDownloaded;

                                    mainHandler.post(
                                            () -> listener.onProgress(
                                                    new com.example.aichat.model.connection.files.UploadProgress(
                                                            fileId,
                                                            finalTotal,
                                                            finalDownloaded
                                                    )
                                            )
                                    );
                                }
                            }

                            outputStream.flush();

                            File finalFile =
                                    outputFile;

                            future.complete(finalFile);

                        } catch (Exception e) {
                            if (outputFile.exists()) {
                                outputFile.delete();
                            }

                            future.completeExceptionally(e);
                        }
                    }
                });

        return future;
    }

    public void setToken(@Nullable String token) {
        this.token = token;
    }

    private Callback createCallback(
            CompletableFuture<HttpCommand> future,
            String url,
            HTTPMethod method,
            @Nullable Object body,
            boolean isCritical,
            boolean triggerUnauthorizedHandler
    ) {
        return new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (isNetworkError(e) && isCritical) {
                    database.insertCommand(new PendingCommand(url, method, body));
                }

                Log.e(TAG, "Request failed for URL: " + url + " - " + e.getMessage());

                JsonNode errorNode =
                        mapper.createObjectNode()
                                .put("error", true)
                                .put("message", e.getMessage() != null ? e.getMessage() : "Network error");

                future.complete(
                        new HttpCommand(
                                CommandOperation.SERVICE_UNAVAILABLE,
                                errorNode
                        )
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                int httpCode =
                        response.code();

                if (httpCode == 401 && unauthorizedHandler != null && triggerUnauthorizedHandler) {
                    Log.e(TAG, "Received 401 Unauthorized - triggering logout");
                    unauthorizedHandler.onUnauthorized();
                }

                String raw =
                        response.body() != null ? response.body().string() : "";

                Log.e(TAG, "=== HTTP RESPONSE ===");
                Log.e(TAG, "URL: " + url);
                Log.e(TAG, "Code: " + httpCode);
                Log.e(TAG, "Raw response: " + (raw.trim().isEmpty() ? "[EMPTY]" : raw));
                Log.e(TAG, "====================");

                if (raw.trim().isEmpty()) {
                    JsonNode emptyNode =
                            mapper.createObjectNode()
                                    .put("empty", true)
                                    .put("httpCode", httpCode);

                    future.complete(
                            new HttpCommand(
                                    httpCode == 204
                                            ? CommandOperation.NO_CONTENT
                                            : CommandOperation.valueOfCode(httpCode),
                                    emptyNode
                            )
                    );
                    return;
                }

                try {
                    JsonNode dataNode =
                            mapper.readTree(raw);

                    Log.e(TAG, "Parsed JSON: " + dataNode.toString());

                    future.complete(
                            new HttpCommand(
                                    CommandOperation.valueOfCode(httpCode),
                                    dataNode
                            )
                    );

                } catch (Exception parseEx) {
                    Log.e(TAG, "Failed to parse response: " + parseEx.getMessage());

                    JsonNode fallback =
                            mapper.createObjectNode()
                                    .put("httpCode", httpCode)
                                    .put("raw", raw)
                                    .put("parseError", true);

                    future.complete(
                            new HttpCommand(
                                    CommandOperation.UNPROCESSABLE_ENTITY,
                                    fallback
                            )
                    );
                }
            }
        };
    }

    private String resolveMultipartFileName(
            @Nullable String uploadFileName,
            @NonNull File file
    ) {
        if (uploadFileName != null && !uploadFileName.trim().isEmpty()) {
            return uploadFileName.trim();
        }

        String localName =
                file.getName();

        if (localName != null && !localName.trim().isEmpty()) {
            return localName.trim();
        }

        return file.getAbsolutePath();
    }

    private Request buildMultipartRequest(String url, MultipartBody body) {
        String deviceModel =
                Build.MANUFACTURER + " " + Build.MODEL;

        String language =
                getAcceptLanguage();

        Request.Builder builder =
                new Request.Builder()
                        .url(url)
                        .addHeader("Accept", "application/json")
                        .addHeader("Accept-Language", language)
                        .addHeader("device", deviceModel)
                        .post(body);

        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        return builder.build();
    }

    private boolean isNetworkError(IOException e) {
        if (e == null) return false;

        String msg =
                e.getMessage();

        if (msg == null) return true;

        msg =
                msg.toLowerCase();

        return msg.contains("failed to connect")
                || msg.contains("timeout")
                || msg.contains("unable to resolve host")
                || msg.contains("connection refused")
                || msg.contains("socket closed")
                || msg.contains("network is unreachable");
    }

    private Request buildRequest(
            String url,
            HTTPMethod method,
            @Nullable String jsonBody
    ) {
        String deviceModel =
                Build.MANUFACTURER + " " + Build.MODEL;

        String language =
                getAcceptLanguage();

        Request.Builder builder =
                new Request.Builder()
                        .url(url)
                        .addHeader("Accept", "application/json")
                        .addHeader("Accept-Language", language)
                        .addHeader("device", deviceModel);

        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        Log.d(TAG, "=== HTTP REQUEST ===");
        Log.d(TAG, "URL: " + url);
        Log.d(TAG, "Method: " + method);
        Log.d(TAG, "Body: " + (jsonBody != null ? jsonBody : "null"));
        Log.d(TAG, "===================");

        switch (method) {
            case GET:
                builder.get();
                break;

            case POST:
                builder.post(
                        okhttp3.RequestBody.create(
                                jsonBody != null ? jsonBody : "{}",
                                JSON_MEDIA_TYPE
                        )
                );
                builder.addHeader("Content-Type", "application/json");
                break;

            case PUT:
                builder.put(
                        okhttp3.RequestBody.create(
                                jsonBody != null ? jsonBody : "{}",
                                JSON_MEDIA_TYPE
                        )
                );
                builder.addHeader("Content-Type", "application/json");
                break;

            case DELETE:
                if (jsonBody != null && !jsonBody.trim().isEmpty()) {
                    builder.delete(
                            okhttp3.RequestBody.create(
                                    jsonBody,
                                    JSON_MEDIA_TYPE
                            )
                    );
                    builder.addHeader("Content-Type", "application/json");
                } else {
                    builder.delete();
                }
                break;
        }

        return builder.build();
    }

    public void onConnected() {
        List<PendingCommand> commandList =
                database.getAllCommands();

        for (PendingCommand command : commandList) {
            Object body =
                    command.getCommandFormat(Object.class);

            fetchAsync(command.getUrl(), command.getMethod(), body, true)
                    .thenAccept(cmd -> {
                        if (!cmd.isSuccess()) {
                            Log.e(TAG, "error sending command after reconnect");
                        }
                    });
        }
    }

    private String getAcceptLanguage() {
        Locale locale =
                Locale.getDefault();

        String language = locale.getLanguage();
        String country = locale.getCountry();

        if (language == null || language.trim().isEmpty()) {
            return "en";
        }

        if (country == null || country.trim().isEmpty()) {
            return language;
        }

        return language + "-" + country;
    }

    private String extractFileNameFromContentDisposition(String contentDisposition) {
        if (contentDisposition == null) return null;

        int utf8Start =
                contentDisposition.indexOf("filename*=UTF-8''");

        if (utf8Start != -1) {
            int start =
                    utf8Start + "filename*=UTF-8''".length();

            int end =
                    contentDisposition.indexOf(";", start);

            if (end == -1) {
                end = contentDisposition.length();
            }

            String fileName =
                    contentDisposition.substring(start, end);

            try {
                return java.net.URLDecoder.decode(fileName, "UTF-8");
            } catch (Exception e) {
                return fileName;
            }
        }

        int filenameStart =
                contentDisposition.indexOf("filename=");

        if (filenameStart != -1) {
            int start =
                    filenameStart + 9;

            if (contentDisposition.charAt(start) == '"') {
                start++;

                int end =
                        contentDisposition.indexOf("\"", start);

                if (end != -1) {
                    return contentDisposition.substring(start, end);
                }
            } else {
                int end =
                        contentDisposition.indexOf(";", start);

                if (end == -1) {
                    end = contentDisposition.length();
                }

                return contentDisposition.substring(start, end).trim();
            }
        }

        return null;
    }
}
