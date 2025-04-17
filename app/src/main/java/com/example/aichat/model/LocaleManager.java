package com.example.aichat.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import java.util.Locale;

public class LocaleManager {

    private static final String PREF_NAME = "LocaleManager";
    private static final String KEY_LANGUAGE = "language";

    public static Context setLocale(Context context, String language) {
        persistLanguage(context, language);
        return updateResources(context, language);
    }

    public static Locale getLocale(Context context) {
        SharedPreferences prefs = getPreferences(context);
        String lang = prefs.getString(KEY_LANGUAGE, Locale.getDefault().getLanguage());
        return new Locale(lang);
    }

    private static void persistLanguage(Context context, String language) {
        getPreferences(context).edit()
                .putString(KEY_LANGUAGE, language)
                .apply();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration configuration = new Configuration(context.getResources().getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
            context = context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            context.getResources().updateConfiguration(configuration,
                    context.getResources().getDisplayMetrics());
        }

        return context;
    }
}