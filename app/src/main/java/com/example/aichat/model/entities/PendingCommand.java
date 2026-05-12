package com.example.aichat.model.entities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.utils.JsonHelper;

@Entity(tableName = "PendingCommands")
public class PendingCommand {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String url;

    private HttpClient.HTTPMethod method;

    @Nullable
    private String data;

    public PendingCommand(String url, HttpClient.HTTPMethod method, @Nullable Object data) {
        this.url = url;
        this.method = method;
        this.data = data != null ? JsonHelper.Serialize(data) : null;
    }

    public PendingCommand() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public HttpClient.HTTPMethod getMethod(){
        return method;
    }

    @Nullable
    public String getData() {
        return data;
    }

    public <T> T getCommandFormat(Class<T> type) {
        if (data == null || data.trim().isEmpty()) return null;
        return JsonHelper.Deserialize(data, type);
    }

    public void setData(@Nullable String data) {
        this.data = data;
    }

    public void setMethod(HttpClient.HTTPMethod method) {
        this.method = method;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

