package com.example.aichat.model.utils;

import android.content.Context;
import android.os.Build;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.aichat.model.connection.TokenStorage;

public final class GlideAuthHelper {

    private GlideAuthHelper() {}

    public static GlideUrl build(String url, Context context) {
        String token = new TokenStorage(context).getToken();
        String device = Build.MANUFACTURER + " " + Build.MODEL;

        return new GlideUrl(
                url,
                new LazyHeaders.Builder()
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("device", device)
                        .build()
        );
    }
}