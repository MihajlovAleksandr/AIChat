package com.example.aichat.view;

import android.app.Activity;
import android.view.Gravity;
import android.view.Window;

import androidx.appcompat.app.AlertDialog;

public class DialogHelper {

    public static void showBottomDialog(
            Activity activity,
            String title,
            String message,
            String positiveText,
            Runnable onPositive
    ) {
        showBottomDialog(activity, title, message, positiveText, onPositive, null, null);
    }

    public static void showBottomDialog(
            Activity activity,
            String title,
            String message,
            String positiveText,
            Runnable onPositive,
            String negativeText,
            Runnable onNegative
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(positiveText, (d, w) -> {
                    if (onPositive != null) onPositive.run();
                });

        if (negativeText != null) {
            builder.setNegativeButton(negativeText, (d, w) -> {
                if (onNegative != null) onNegative.run();
            });
        }

        AlertDialog dialog = builder.create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            window.getAttributes().y = 100;
            window.getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        }

        dialog.show();
    }
}
