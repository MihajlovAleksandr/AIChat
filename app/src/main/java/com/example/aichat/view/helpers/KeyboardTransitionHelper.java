package com.example.aichat.view.helpers;

import android.app.Activity;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.view.Window;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public final class KeyboardTransitionHelper {

    private KeyboardTransitionHelper() {
    }

    public static void hideKeyboard(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        try {
            Window window = activity.getWindow();
            if (window == null) return;

            View focusedView = activity.getCurrentFocus();

            if (focusedView == null) {
                focusedView = window.getDecorView();
            }

            if (focusedView != null) {
                InputMethodManager imm =
                        (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

                if (imm != null) {
                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                }

                focusedView.clearFocus();
            }

            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(window, window.getDecorView());

            controller.hide(WindowInsetsCompat.Type.ime());

        } catch (Exception ignored) {
        }
    }
}
