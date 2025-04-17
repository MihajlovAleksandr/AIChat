package com.example.aichat.model;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.setLocale(
                base,
                LocaleManager.getLocale(base).getLanguage()
        ));
    }
}