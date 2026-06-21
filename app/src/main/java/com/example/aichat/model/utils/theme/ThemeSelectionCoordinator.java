package com.example.aichat.model.utils.theme;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeSelectionCoordinator {

    public static final String SETTINGS_PREFS =
            "settings_prefs";

    public static final String KEY_APP_THEME =
            "app_theme";

    public static final String KEY_SELECTED_CUSTOM_THEME =
            "selected_custom_theme";

    public static final String THEME_LIGHT =
            "light";

    public static final String THEME_DARK =
            "dark";

    public static final String THEME_SYSTEM =
            "system";

    private static final String LEGACY_STANDARD_LIGHT_THEME_ID =
            "00000000-0000-0000-0000-000000000101";

    private static final String LEGACY_STANDARD_DARK_THEME_ID =
            "00000000-0000-0000-0000-000000000102";

    private static volatile long lastRestartTimeMs = 0L;
    private static final long RESTART_DEBOUNCE_MS = 1200L;

    private ThemeSelectionCoordinator() {
    }


    public static void selectDefaultTheme(
            Context context,
            String mode
    ) {
        String safeMode =
                saveDefaultThemeOnly(
                        context,
                        mode
                );

        applyNightMode(
                safeMode
        );
    }

    public static String saveDefaultThemeOnly(
            Context context,
            String mode
    ) {
        if (context == null) {
            return THEME_SYSTEM;
        }

        String safeMode =
                isDefaultThemeMode(mode)
                        ? mode
                        : THEME_SYSTEM;

        SharedPreferences preferences =
                context.getSharedPreferences(
                        SETTINGS_PREFS,
                        Context.MODE_PRIVATE
                );

        preferences.edit()
                .putString(
                        KEY_APP_THEME,
                        safeMode
                )
                .remove(
                        KEY_SELECTED_CUSTOM_THEME
                )
                .apply();

        ThemeStorage themeStorage =
                new ThemeStorage(
                        context
                );

        themeStorage.clearSelectedThemeId();

        ThemeAttrResolver.clear();

        return safeMode;
    }


    public static ThemeModel selectCustomTheme(
            Context context,
            String themeId
    ) {
        ThemeModel theme =
                saveCustomThemeOnly(
                        context,
                        themeId
                );

        if (theme == null) {
            return null;
        }

        applyNightMode(
                THEME_LIGHT
        );

        return theme;
    }


    public static ThemeModel saveCustomThemeOnly(
            Context context,
            String themeId
    ) {
        if (context == null
                || themeId == null
                || themeId.trim().isEmpty()) {

            return null;
        }

        ThemeStorage themeStorage =
                new ThemeStorage(
                        context
                );

        ThemeModel theme =
                themeStorage.getThemeById(
                        themeId
                );

        if (theme == null) {
            return null;
        }

        SharedPreferences preferences =
                context.getSharedPreferences(
                        SETTINGS_PREFS,
                        Context.MODE_PRIVATE
                );

        preferences.edit()
                .putString(
                        KEY_APP_THEME,
                        THEME_LIGHT
                )
                .putString(
                        KEY_SELECTED_CUSTOM_THEME,
                        themeId
                )
                .apply();

        themeStorage.setSelectedThemeId(
                themeId
        );

        ThemeAttrResolver.applyTheme(
                theme
        );

        return theme;
    }


    public static void migrateLegacyStandardThemeSelection(
            Context context
    ) {
        if (context == null) {
            return;
        }

        SharedPreferences preferences =
                context.getSharedPreferences(
                        SETTINGS_PREFS,
                        Context.MODE_PRIVATE
                );

        String selectedCustomTheme =
                preferences.getString(
                        KEY_SELECTED_CUSTOM_THEME,
                        null
                );

        if (LEGACY_STANDARD_LIGHT_THEME_ID.equals(selectedCustomTheme)) {
            preferences.edit()
                    .putString(KEY_APP_THEME, THEME_LIGHT)
                    .remove(KEY_SELECTED_CUSTOM_THEME)
                    .apply();
            return;
        }

        if (LEGACY_STANDARD_DARK_THEME_ID.equals(selectedCustomTheme)) {
            preferences.edit()
                    .putString(KEY_APP_THEME, THEME_DARK)
                    .remove(KEY_SELECTED_CUSTOM_THEME)
                    .apply();
            return;
        }

        String appTheme =
                preferences.getString(
                        KEY_APP_THEME,
                        THEME_SYSTEM
                );

        if (LEGACY_STANDARD_LIGHT_THEME_ID.equals(appTheme)) {
            preferences.edit()
                    .putString(KEY_APP_THEME, THEME_LIGHT)
                    .remove(KEY_SELECTED_CUSTOM_THEME)
                    .apply();
            return;
        }

        if (LEGACY_STANDARD_DARK_THEME_ID.equals(appTheme)) {
            preferences.edit()
                    .putString(KEY_APP_THEME, THEME_DARK)
                    .remove(KEY_SELECTED_CUSTOM_THEME)
                    .apply();
        }
    }

    public static String getSelectedCustomThemeId(
            Context context
    ) {
        if (context == null) {
            return null;
        }

        migrateLegacyStandardThemeSelection(context);

        SharedPreferences preferences =
                context.getSharedPreferences(
                        SETTINGS_PREFS,
                        Context.MODE_PRIVATE
                );

        return preferences.getString(
                KEY_SELECTED_CUSTOM_THEME,
                null
        );
    }

    public static boolean isDefaultThemeMode(
            String mode
    ) {
        return THEME_LIGHT.equals(mode)
                || THEME_DARK.equals(mode)
                || THEME_SYSTEM.equals(mode);
    }

    public static boolean isLegacyStandardThemeId(
            String themeId
    ) {
        if (themeId == null) {
            return false;
        }

        String normalizedThemeId =
                themeId.trim();

        return LEGACY_STANDARD_LIGHT_THEME_ID.equals(normalizedThemeId)
                || LEGACY_STANDARD_DARK_THEME_ID.equals(normalizedThemeId);
    }

    public static void applyNightMode(
            String mode
    ) {
        int targetMode =
                resolveNightMode(
                        mode
                );

        int currentMode =
                AppCompatDelegate.getDefaultNightMode();

        if (currentMode == targetMode) {
            return;
        }

        AppCompatDelegate.setDefaultNightMode(
                targetMode
        );
    }

    private static int resolveNightMode(
            String mode
    ) {
        if (THEME_LIGHT.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }

        if (THEME_DARK.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }

        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    public static void restartApp(
            Activity activity,
            Class<?> targetActivity
    ) {
        if (activity == null
                || targetActivity == null) {

            return;
        }

        long now =
                System.currentTimeMillis();

        if (now - lastRestartTimeMs < RESTART_DEBOUNCE_MS) {
            return;
        }

        lastRestartTimeMs =
                now;

        Intent intent =
                new Intent(
                        activity,
                        targetActivity
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        activity.startActivity(
                intent
        );

        activity.overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );

        activity.finish();
    }
}
