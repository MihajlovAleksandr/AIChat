package com.example.aichat.model.connection;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.aichat.model.SecurePreferencesManager;

public class TokenStorage {
    private final Context context;

    public TokenStorage(Context context){
        this.context = context;
    }

    public void saveToken(@Nullable String token){
        SecurePreferencesManager.saveAuthToken(context, token);
    }

    public @Nullable String getToken(){
        return SecurePreferencesManager.getAuthToken(context);
    }

    public Context getContext() {
        return context;
    }
}