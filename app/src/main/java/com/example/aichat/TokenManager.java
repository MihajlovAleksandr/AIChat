package com.example.aichat;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenManager {

    private static final String SHARED_PREFS_FILE = "secret_shared_prefs";
    private static final String TOKEN_KEY = "auth_token";

    public static void saveToken(Context context, String token) {
        try {
            // Create or get the Master Key using MasterKey.Builder
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // Create EncryptedSharedPreferences
            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    SHARED_PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            // Save token
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(TOKEN_KEY, token);
            editor.apply();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            // Handle the error here according to your needs
        }
    }

    public static String getToken(Context context) {
        try {
            // Create or get the Master Key using MasterKey.Builder
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // Create EncryptedSharedPreferences
            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    SHARED_PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            // Get token
            return sharedPreferences.getString(TOKEN_KEY, null);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            // Handle the error here according to your needs
            return null;
        }
    }
}
