package com.example.aichat.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

public class LocaleManager {

    private static final String PREF_NAME = "settings_prefs";
    private static final String KEY_LANGUAGE = "app_language";

    public static Context setLocale(
            Context context,
            String language
    ) {

        persistLanguage(
                context,
                language
        );

        return updateResources(
                context,
                language
        );
    }

    public static Locale getLocale(Context context) {

        SharedPreferences prefs =
                getPreferences(context);

        String language =
                prefs.getString(
                        KEY_LANGUAGE,
                        "en"
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
                        language
                )
                .apply();
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

        Locale locale =
                new Locale(language);

        Locale.setDefault(locale);

        Configuration configuration =
                new Configuration(
                        context.getResources()
                                .getConfiguration()
                );

        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            Context localizedContext =
                    context.createConfigurationContext(configuration);

            localizedContext.getResources()
                    .updateConfiguration(
                            configuration,
                            localizedContext.getResources()
                                    .getDisplayMetrics()
                    );

            return localizedContext;
        }

        context.getResources()
                .updateConfiguration(
                        configuration,
                        context.getResources()
                                .getDisplayMetrics()
                );

        return context;
    }
}