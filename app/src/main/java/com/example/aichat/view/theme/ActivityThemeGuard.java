
package com.example.aichat.view.theme;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.connection.TokenStorage;

public final class ActivityThemeGuard {

    private ActivityThemeGuard() {
    }

    public static boolean isAuthenticated(@Nullable Context context) {
        if (context == null) {
            return false;
        }

        try {
            TokenStorage tokenStorage =
                    new TokenStorage(
                            context
                    );

            String token =
                    tokenStorage.getToken();

            return token != null
                    && !token.trim().isEmpty();

        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean shouldApplyAccountThemeForEntityEdit(
            @Nullable Activity activity,
            @NonNull String entityExtraName
    ) {
        if (activity == null) {
            return false;
        }

        if (!isAuthenticated(activity)) {
            return false;
        }

        Intent intent =
                activity.getIntent();

        if (intent == null) {
            return false;
        }

        String payload =
                intent.getStringExtra(
                        entityExtraName
                );

        return payload != null
                && !payload.trim().isEmpty();
    }
}
