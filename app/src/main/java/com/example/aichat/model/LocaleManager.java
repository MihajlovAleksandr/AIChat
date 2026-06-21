package com.example.aichat.model;

import android.content.Context;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.LocaleList;
import java.util.Locale;

public class LocaleManager {

    private static final String PREF_NAME = "settings_prefs";
    private static final String KEY_LANGUAGE = "app_language";

    public static Context setLocale(
            Context context,
            String language
    ) {
        String normalizedLanguage = normalizeLanguage(language);

        persistLanguage(
                context,
                normalizedLanguage
        );

        return updateResources(
                context,
                normalizedLanguage
        );
    }

    public static Context wrap(Context context) {
        if (context == null) {
            return null;
        }

        SharedPreferences prefs = getPreferences(context);

        String language = normalizeLanguage(
                prefs.getString(
                        KEY_LANGUAGE,
                        "en"
                )
        );

        return updateResources(context, language);
    }

    public static Locale getLocale(Context context) {
        SharedPreferences prefs = getPreferences(context);

        String language = normalizeLanguage(
                prefs.getString(
                        KEY_LANGUAGE,
                        "en"
                )
        );

        return new Locale(language);
    }

    public static String getLanguage(Context context) {
        return getLocale(context).getLanguage();
    }

    private static void persistLanguage(
            Context context,
            String language
    ) {
        getPreferences(context)
                .edit()
                .putString(
                        KEY_LANGUAGE,
                        normalizeLanguage(language)
                )
                .apply();
    }

    public static String normalizeLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return "en";
        }

        String normalized = language.trim()
                .replace('-', '_')
                .toLowerCase(Locale.US);

        if (normalized.startsWith("ua")) {
            return "uk";
        }

        if (normalized.startsWith("uk")) {
            return "uk";
        }

        if (normalized.startsWith("pl")) {
            return "pl";
        }

        if (normalized.startsWith("ru")) {
            return "ru";
        }

        if (normalized.startsWith("es")) {
            return "es";
        }

        if (normalized.startsWith("be")) {
            return "be";
        }

        return "en";
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    private static Context updateResources(
            Context context,
            String language
    ) {
        String normalizedLanguage = normalizeLanguage(language);
        Locale locale = new Locale(normalizedLanguage);

        Locale.setDefault(locale);

        Configuration configuration = new Configuration(
                context.getResources().getConfiguration()
        );

        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);
        }

        Context localizedContext = context.createConfigurationContext(configuration);

        context.getResources().updateConfiguration(
                configuration,
                context.getResources().getDisplayMetrics()
        );

        localizedContext.getResources().updateConfiguration(
                configuration,
                localizedContext.getResources().getDisplayMetrics()
        );

        return localizedContext;
    }
}
