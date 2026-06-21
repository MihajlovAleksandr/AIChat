package com.example.aichat.view.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.aichat.model.LocaleManager;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.model.utils.theme.ThemeManager;
import com.example.aichat.model.utils.theme.ThemeModel;
import com.example.aichat.model.utils.theme.ThemeStorage;
import com.example.aichat.R;
import com.example.aichat.view.helpers.KeyboardTransitionHelper;

public class BaseActivity extends AppCompatActivity {

    private static final long CLEAN_ACTIVITY_START_DELAY_MS = 220;

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = LocaleManager.wrap(newBase);
        super.attachBaseContext(context != null ? context : newBase);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE);
        String theme = prefs.getString("app_theme", "system");

        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                setTheme(R.style.Theme_AIChat);
                break;

            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                setTheme(R.style.Theme_AIChat);
                break;

            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                setTheme(R.style.Theme_AIChat);
                break;
        }

        super.onCreate(savedInstanceState);

        applyRuntimeTheme();
        applyCustomTheme();
    }

    @Override
    public void startActivity(Intent intent) {
        KeyboardTransitionHelper.hideKeyboard(this);
        super.startActivity(intent);
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        KeyboardTransitionHelper.hideKeyboard(this);
        super.startActivity(intent, options);
    }

    @Override
    public void finish() {
        KeyboardTransitionHelper.hideKeyboard(this);
        super.finish();
    }

    @Override
    protected void onPause() {
        KeyboardTransitionHelper.hideKeyboard(this);
        super.onPause();
    }

    public void startActivityClean(Intent intent, boolean finishCurrent) {
        KeyboardTransitionHelper.hideKeyboard(this);

        View decorView = getWindow() != null ? getWindow().getDecorView() : null;

        Runnable action = () -> {
            if (isFinishing() || isDestroyed()) return;

            BaseActivity.super.startActivity(intent);

            if (finishCurrent) {
                BaseActivity.super.finish();
            }

            overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            );
        };

        if (decorView != null) {
            decorView.postDelayed(action, CLEAN_ACTIVITY_START_DELAY_MS);
        } else {
            action.run();
        }
    }

    private void applyRuntimeTheme() {
        Log.d("RuntimeTheme", "applyRuntimeTheme()");

        SharedPreferences preferences = getSharedPreferences("settings_prefs", MODE_PRIVATE);
        String customThemeId = preferences.getString("selected_custom_theme", null);

        Log.d("RuntimeTheme", "selected_custom_theme = " + customThemeId);

        if (customThemeId == null || customThemeId.isEmpty()) {
            Log.d("RuntimeTheme", "No custom theme selected");
            ThemeAttrResolver.clear();
            return;
        }

        ThemeStorage storage = new ThemeStorage(this);
        ThemeModel customTheme = storage.getThemeById(customThemeId);

        if (customTheme == null) {
            Log.d("RuntimeTheme", "Theme not found in storage");
            ThemeAttrResolver.clear();
            return;
        }

        Log.d("RuntimeTheme", "Theme loaded: " + customTheme.getName());
        Log.d("RuntimeTheme", "Applying runtime theme...");

        ThemeAttrResolver.applyTheme(customTheme);
    }

    private void applyCustomTheme() {
        SharedPreferences prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE);
        String themeId = prefs.getString("selected_custom_theme", null);

        if (themeId == null) return;

        getWindow().getDecorView().post(() -> {
            ThemeManager manager = new ThemeManager(this);
            manager.apply(themeId);
        });
    }

    public void restartAppTo(Class<?> activityClass) {
        KeyboardTransitionHelper.hideKeyboard(this);

        Intent intent = new Intent(this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivityClean(intent, true);
    }
}
