package com.example.aichat.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurePreferencesManager {

    private static final String SHARED_PREFS_FILE = "secure_app_prefs";
    private static final String AUTH_TOKEN_KEY = "auth_token";
    private static final String USER_ID_KEY = "user_id";

    // Методы для работы с токеном авторизации
    public static void saveAuthToken(Context context, String token) {
        saveString(context, AUTH_TOKEN_KEY, token);
    }

    public static String getAuthToken(Context context) {
        return getString(context, AUTH_TOKEN_KEY);
    }

    public static void removeAuthToken(Context context) {
        remove(context, AUTH_TOKEN_KEY);
    }

    public static void saveUserId(Context context, int userId) {
        saveInt(context, USER_ID_KEY, userId);
    }

    public static int getUserId(Context context) {
        return getInt(context, USER_ID_KEY, -1);
    }

    public static void removeUserId(Context context) {
        remove(context, USER_ID_KEY);
    }

    // Базовые методы для работы с SharedPreferences
    private static void saveString(Context context, String key, String value) {
        try {
            SharedPreferences.Editor editor = getEncryptedEditor(context);
            editor.putString(key, value);
            editor.apply();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveInt(Context context, String key, int value) {
        try {
            SharedPreferences.Editor editor = getEncryptedEditor(context);
            editor.putInt(key, value);
            editor.apply();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private static String getString(Context context, String key) {
        try {
            SharedPreferences sharedPreferences = getEncryptedPreferences(context);
            return sharedPreferences.getString(key, null);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int getInt(Context context, String key, int defaultValue) {
        try {
            SharedPreferences sharedPreferences = getEncryptedPreferences(context);
            return sharedPreferences.getInt(key, defaultValue);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    private static void remove(Context context, String key) {
        try {
            SharedPreferences.Editor editor = getEncryptedEditor(context);
            editor.remove(key);
            editor.apply();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private static SharedPreferences getEncryptedPreferences(Context context)
            throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                SHARED_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    private static SharedPreferences.Editor getEncryptedEditor(Context context)
            throws GeneralSecurityException, IOException {
        return getEncryptedPreferences(context).edit();
    }
}