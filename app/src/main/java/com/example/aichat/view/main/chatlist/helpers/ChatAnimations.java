package com.example.aichat.view.main.chatlist.helpers;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ChatAnimations {

    public static void animateAppear(View itemView) {
        itemView.setAlpha(0f);
        itemView.setTranslationY(20f);
        itemView.setScaleX(0.98f);
        itemView.setScaleY(0.98f);

        itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(140)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    public static void animateLastMessageUpdate(TextView tv) {
        tv.setAlpha(0f);
        tv.animate()
                .alpha(1f)
                .setDuration(120)
                .start();
    }


    public static void animateUnreadBadge(TextView badge) {
        badge.setScaleX(0.6f);
        badge.setScaleY(0.6f);

        badge.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(140)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();
    }

    public static void animatePinAppear(ImageView pin) {
        pin.setScaleX(0.6f);
        pin.setScaleY(0.6f);
        pin.setAlpha(0f);

        pin.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150)
                .setInterpolator(new OvershootInterpolator(1.3f))
                .start();
    }

    public static void animatePinDisappear(ImageView pin) {
        pin.animate()
                .alpha(0f)
                .setDuration(120)
                .withEndAction(() -> pin.setVisibility(View.GONE))
                .start();
    }


    public static void animatePlaceholder(View itemView) {
        itemView.setAlpha(0f);
        itemView.animate()
                .alpha(1f)
                .setDuration(180)
                .start();
    }

    public static void animateBounce(View itemView) {
        itemView.setScaleX(0.9f);
        itemView.setScaleY(0.9f);
        itemView.setAlpha(0f);

        itemView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(160)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }
}
