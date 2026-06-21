package com.example.aichat.controller.payment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import com.example.aichat.view.main.MainActivity;

public class PaymentOverlayView {

    private static final long SUCCESS_HIDE_DELAY = 3000L;

    private final Activity activity;

    private final Handler handler;

    private FrameLayout overlay;

    public PaymentOverlayView(
            @NonNull Activity activity
    ) {
        this.activity = activity;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void showSuccess(
            @NonNull String name
    ) {

        show(
                PaymentOverlayType.SUCCESS,
                "Успех",
                "Вам начислено: " + name,
                true
        );

        handler.postDelayed(
                this::closeAndNavigate,
                SUCCESS_HIDE_DELAY
        );
    }

    public void showError(
            @NonNull String reason
    ) {

        show(
                PaymentOverlayType.ERROR,
                "Отклонено",
                reason,
                false
        );
    }

    private void show(
            @NonNull PaymentOverlayType type,
            @NonNull String title,
            @NonNull String message,
            boolean autoClose
    ) {

        hide();

        overlay = new FrameLayout(activity);

        overlay.setLayoutParams(
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        overlay.setClickable(true);

        overlay.setBackgroundColor(
                type == PaymentOverlayType.SUCCESS
                        ? Color.parseColor("#2E7D32")
                        : Color.parseColor("#C62828")
        );

        LinearLayout container =
                new LinearLayout(activity);

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        container.setGravity(Gravity.CENTER);

        container.setLayoutParams(
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        TextView titleView =
                new TextView(activity);

        titleView.setText(title);

        titleView.setTextColor(Color.WHITE);

        titleView.setTextSize(34);

        titleView.setGravity(Gravity.CENTER);

        titleView.setPadding(
                32,
                32,
                32,
                16
        );

        TextView messageView =
                new TextView(activity);

        messageView.setText(message);

        messageView.setTextColor(Color.WHITE);

        messageView.setTextSize(22);

        messageView.setGravity(Gravity.CENTER);

        messageView.setPadding(
                32,
                16,
                32,
                32
        );

        container.addView(titleView);

        container.addView(messageView);

        overlay.addView(container);

        overlay.setOnClickListener(v -> {

            if (autoClose) {
                closeAndNavigate();
                return;
            }

            closeAndNavigate();
        });

        ViewGroup rootView =
                activity.findViewById(
                        android.R.id.content
                );

        rootView.addView(overlay);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void closeAndNavigate() {

        hide();

        Intent intent =
                new Intent(
                        activity,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        activity.finish();
    }

    public void hide() {

        handler.removeCallbacksAndMessages(null);

        if (overlay == null) {
            return;
        }

        ViewGroup parent =
                (ViewGroup) overlay.getParent();

        if (parent != null) {
            parent.removeView(overlay);
        }

        overlay = null;
    }
}
