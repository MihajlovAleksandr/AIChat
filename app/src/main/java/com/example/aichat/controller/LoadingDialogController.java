package com.example.aichat.controller;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LoadingDialogController {

    private final Activity activity;
    private AlertDialog dialog;

    public LoadingDialogController(@NonNull Activity activity) {
        this.activity = activity;
    }

    public void show(String title, String message) {
        activity.runOnUiThread(() -> showInternal(title, message));
    }

    private void showInternal(String title, String message) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        hideKeyboardAndClearFocus();
        dismissInternal();

        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(dp(24), dp(16), dp(24), dp(8));
        container.setFocusable(true);
        container.setFocusableInTouchMode(true);

        ProgressBar progressBar = new ProgressBar(activity);
        progressBar.setIndeterminate(true);

        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        progressParams.bottomMargin = dp(16);
        container.addView(progressBar, progressParams);

        TextView messageView = new TextView(activity);
        messageView.setText(message);
        messageView.setGravity(Gravity.CENTER);
        messageView.setTextSize(15f);

        container.addView(
                messageView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setView(container)
                .create();

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnShowListener(d -> {
            hideKeyboardAndClearFocus();

            Window window = dialog.getWindow();

            if (window != null) {
                window.setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                );
            }
        });

        dialog.show();

        container.requestFocus();

        Window window = dialog.getWindow();

        if (window != null) {
            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            );
        }
    }

    public void dismiss() {
        activity.runOnUiThread(this::dismissInternal);
    }

    private void dismissInternal() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = null;
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    private void hideKeyboardAndClearFocus() {
        try {
            View focusedView = activity.getCurrentFocus();

            if (focusedView == null) {
                focusedView = activity.getWindow() != null
                        ? activity.getWindow().getDecorView()
                        : null;
            }

            if (focusedView == null) return;

            InputMethodManager inputMethodManager =
                    (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(
                        focusedView.getWindowToken(),
                        0
                );
            }

            focusedView.clearFocus();

        } catch (Exception ignored) {
        }
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
