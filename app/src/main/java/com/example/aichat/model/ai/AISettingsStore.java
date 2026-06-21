package com.example.aichat.model.ai;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.utils.theme.ThemeSelectionCoordinator;
import java.util.UUID;

public final class AISettingsStore {

    public static final String KEY_SELECTED_CHAT_AI_MODEL = "selected_chat_ai_model";
    public static final String ACTION_CHAT_AI_MODEL_CHANGED = "com.example.aichat.ACTION_CHAT_AI_MODEL_CHANGED";
    public static final String EXTRA_CHAT_ID = "chat_id";
    public static final String EXTRA_AI_MODEL = "ai_model";
    private static final String KEY_SELECTED_CHAT_AI_MODEL_BY_CHAT_PREFIX = "selected_chat_ai_model_";

    private AISettingsStore() {
    }

    @NonNull
    public static AIModel getSelectedChatModel(@Nullable Context context) {
        if (context == null) {
            return AIModel.Default;
        }

        SharedPreferences preferences = getPreferences(context);

        return AIModel.fromServerValue(
                preferences.getString(
                        KEY_SELECTED_CHAT_AI_MODEL,
                        AIModel.Default.getServerValue()
                )
        );
    }

    @NonNull
    public static AIModel getSelectedChatModel(
            @Nullable Context context,
            @Nullable UUID chatId
    ) {
        if (context == null) {
            return AIModel.Default;
        }

        SharedPreferences preferences = getPreferences(context);

        if (chatId != null) {
            String perChatValue = preferences.getString(
                    KEY_SELECTED_CHAT_AI_MODEL_BY_CHAT_PREFIX + chatId,
                    null
            );

            if (perChatValue != null && !perChatValue.trim().isEmpty()) {
                return AIModel.fromServerValue(perChatValue);
            }
        }

        return getSelectedChatModel(context);
    }

    public static void saveSelectedChatModel(
            @Nullable Context context,
            @Nullable AIModel model
    ) {
        if (context == null) {
            return;
        }

        AIModel safeModel = model != null ? model : AIModel.Default;

        getPreferences(context)
                .edit()
                .putString(KEY_SELECTED_CHAT_AI_MODEL, safeModel.getServerValue())
                .apply();
    }

    public static void saveSelectedChatModel(
            @Nullable Context context,
            @Nullable UUID chatId,
            @Nullable AIModel model
    ) {
        if (context == null || chatId == null) {
            return;
        }

        AIModel safeModel = model != null ? model : AIModel.Default;

        getPreferences(context)
                .edit()
                .putString(KEY_SELECTED_CHAT_AI_MODEL_BY_CHAT_PREFIX + chatId, safeModel.getServerValue())
                .apply();
    }

    public static void notifyChatModelChanged(
            @Nullable Context context,
            @Nullable UUID chatId,
            @Nullable AIModel model
    ) {
        if (context == null || chatId == null) {
            return;
        }

        AIModel safeModel = model != null ? model : AIModel.Default;

        Intent intent = new Intent(ACTION_CHAT_AI_MODEL_CHANGED);
        intent.setPackage(context.getPackageName());
        intent.putExtra(EXTRA_CHAT_ID, chatId.toString());
        intent.putExtra(EXTRA_AI_MODEL, safeModel.getServerValue());
        context.sendBroadcast(intent);
    }

    @NonNull
    private static SharedPreferences getPreferences(@NonNull Context context) {
        return context.getSharedPreferences(
                ThemeSelectionCoordinator.SETTINGS_PREFS,
                Context.MODE_PRIVATE
        );
    }
}
