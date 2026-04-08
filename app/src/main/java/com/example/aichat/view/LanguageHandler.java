package com.example.aichat.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import com.example.aichat.model.LocaleManager;

import java.util.Locale;

public class LanguageHandler {

    private final Context context;

    public LanguageHandler(Context context) {
        this.context = context;
    }

    public void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Configuration config = context.getResources().getConfiguration();
        config.setLocale(locale);

        LocaleManager.setLocale(context, langCode);

        if (context instanceof Activity) {
            ((Activity) context).recreate();
        }
    }
}