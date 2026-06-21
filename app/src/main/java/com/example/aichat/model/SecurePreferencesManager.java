package com.example.aichat.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

public class SecurePreferencesManager {

    private static final String TAG = "SecurePrefs";

    private static final String SHARED_PREFS_FILE = "secure_app_prefs";
    private static final String AUTH_TOKEN_KEY = "auth_token";
    private static final String NOTIFICATION_TOKEN_KEY = "notification_token";
    private static final String USER_ID_KEY = "user_id";

    private static SharedPreferences cachedPrefs = null;
    private static MasterKey cachedMasterKey = null;

    public static void saveAuthToken(Context context, String token) {
        saveString(context, AUTH_TOKEN_KEY, token);
    }

    public static String getAuthToken(Context context) {
        return getString(context, AUTH_TOKEN_KEY);
    }

    public static void removeAuthToken(Context context) {
        remove(context, AUTH_TOKEN_KEY);
    }

    public static void saveNotificationToken(Context context, String token) {
        saveString(context, NOTIFICATION_TOKEN_KEY, token);
    }

    public static String getNotificationToken(Context context) {
        return getString(context, NOTIFICATION_TOKEN_KEY);
    }

    public static void saveUserId(Context context, UUID userId) {
        if (userId != null) {
            saveString(context, USER_ID_KEY, userId.toString());
        } else {
            removeUserId(context);
        }
    }

    public static UUID getUserId(Context context) {
        String str = getString(context, USER_ID_KEY);
        try {
            return str != null ? UUID.fromString(str) : null;
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Corrupted USER_ID in prefs, clearing", e);
            removeUserId(context);
            return null;
        }
    }

    public static void removeUserId(Context context) {
        remove(context, USER_ID_KEY);
    }

    private static void saveString(Context context, String key, String value) {
        try {
            SharedPreferences.Editor editor = getEncryptedPreferencesSafe(context).edit();
            editor.putString(key, value);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "saveString failed for key=" + key, e);
        }
    }

    private static String getString(Context context, String key) {
        try {
            return getEncryptedPreferencesSafe(context).getString(key, null);
        } catch (Exception e) {
            Log.e(TAG, "getString failed for key=" + key, e);
            return null;
        }
    }

    private static void remove(Context context, String key) {
        try {
            SharedPreferences.Editor editor = getEncryptedPreferencesSafe(context).edit();
            editor.remove(key);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "remove failed for key=" + key, e);
        }
    }

    private static SharedPreferences getEncryptedPreferencesSafe(Context context)
            throws GeneralSecurityException, IOException {

        if (cachedPrefs != null) {
            return cachedPrefs;
        }

        try {
            cachedPrefs = createEncryptedPreferences(context);
            return cachedPrefs;
        } catch (Exception e) {
            Log.w(TAG, "First attempt to create EncryptedSharedPreferences failed, trying recovery", e);

            clearCorruptedStorage(context);

            try {
                cachedPrefs = createEncryptedPreferences(context);
                return cachedPrefs;
            } catch (Exception e2) {
                Log.e(TAG, "Recovery attempt to create EncryptedSharedPreferences failed", e2);
                cachedPrefs = context.getSharedPreferences(SHARED_PREFS_FILE + "_fallback", Context.MODE_PRIVATE);
                return cachedPrefs;
            }
        }
    }

    private static synchronized SharedPreferences createEncryptedPreferences(Context context)
            throws GeneralSecurityException, IOException {

        if (cachedPrefs != null) {
            return cachedPrefs;
        }

        if (cachedMasterKey == null) {
            cachedMasterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
        }

        return EncryptedSharedPreferences.create(
                context,
                SHARED_PREFS_FILE,
                cachedMasterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
    public static void clearAll(Context context) {
        try {
            SharedPreferences.Editor editor = getEncryptedPreferencesSafe(context).edit();
            editor.clear();
            editor.apply();
            Log.d(TAG, "All preferences cleared successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear preferences", e);
        }
    }

    private static void clearCorruptedStorage(Context context) {
        try {
            context.deleteSharedPreferences(SHARED_PREFS_FILE);
        } catch (Exception e) {
            Log.w(TAG, "Failed to delete corrupted shared prefs file", e);
        }

        cachedPrefs = null;
        cachedMasterKey = null;
    }
}
