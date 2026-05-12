package com.example.aichat.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.aichat.R;
import com.example.aichat.model.LocaleManager;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {

        SharedPreferences prefs =
                newBase.getSharedPreferences(
                        "settings_prefs",
                        Context.MODE_PRIVATE
                );

        String language =
                prefs.getString(
                        "app_language",
                        "en"
                );

        Context context =
                LocaleManager.setLocale(
                        newBase,
                        language
                );

        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        SharedPreferences prefs =
                getSharedPreferences(
                        "settings_prefs",
                        MODE_PRIVATE
                );

        String theme =
                prefs.getString(
                        "app_theme",
                        "system"
                );

        switch (theme) {

            case "light":

                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO
                );

                setTheme(R.style.Theme_AIChat);

                break;

            case "dark":

                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_YES
                );

                setTheme(R.style.Theme_AIChat_Dark);

                break;

            default:

                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                );

                setTheme(R.style.Theme_AIChat);

                break;
        }

        super.onCreate(savedInstanceState);
    }

    public void restartAppTo(Class<?> activityClass) {

        Intent intent =
                new Intent(this, activityClass);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();

        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
    }
}