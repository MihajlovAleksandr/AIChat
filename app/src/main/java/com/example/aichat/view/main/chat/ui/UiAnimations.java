package com.example.aichat.view.main.chat.ui;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.recyclerview.widget.RecyclerView;

public class UiAnimations {

    public static void animateNewMessage(View itemView) {
        itemView.setScaleX(0.92f);
        itemView.setScaleY(0.92f);
        itemView.setAlpha(0f);

        itemView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(160)
                .setInterpolator(new OvershootInterpolator(1.15f))
                .start();
    }

    public static void animateChatOpen(View chatContainer, View chatList) {
        chatContainer.setVisibility(View.VISIBLE);
        chatContainer.setAlpha(0f);
        chatContainer.setTranslationX(60f);

        chatContainer.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(180)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        chatList.animate()
                .alpha(0f)
                .translationX(-30f)
                .setDuration(150)
                .withEndAction(() -> chatList.setVisibility(View.GONE))
                .start();
    }

    public static void animateChatClose(View chatContainer, View chatList) {
        chatList.setVisibility(View.VISIBLE);
        chatList.setAlpha(0f);
        chatList.setTranslationX(-30f);

        chatList.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(180)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        chatContainer.animate()
                .alpha(0f)
                .translationX(60f)
                .setDuration(150)
                .withEndAction(() -> chatContainer.setVisibility(View.GONE))
                .start();
    }

    public static void animateChatUpdated(View itemView) {
        itemView.setAlpha(0.5f);
        itemView.animate()
                .alpha(1f)
                .setDuration(200)
                .start();
    }

    public static void animateUnreadBadge(View badge) {
        badge.setScaleX(0.6f);
        badge.setScaleY(0.6f);

        badge.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(140)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();
    }

    public static void animatePress(View v, boolean pressed) {
        if (pressed) {
            v.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(80)
                    .start();
        } else {
            v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(new OvershootInterpolator(2f))
                    .start();
        }
    }
    public static void animateSearchOpen(View panel) {
        panel.setTranslationY(-40f);
        panel.setAlpha(0f);

        panel.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(160)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
    public static void animateChatRemove(View itemView, Runnable onEnd) {
        itemView.animate()
                .alpha(0f)
                .translationX(-itemView.getWidth() * 0.3f)
                .setDuration(150)
                .withEndAction(onEnd)
                .start();
    }

    public static void animateFabAppear(View fab) {
        fab.setScaleX(0f);
        fab.setScaleY(0f);
        fab.setAlpha(0f);

        fab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator(1.3f))
                .start();
    }

    public static void animateInitialMessages(RecyclerView recycler) {
        for (int i = 0; i < recycler.getChildCount(); i++) {
            View child = recycler.getChildAt(i);
            child.setAlpha(0f);
            child.animate()
                    .alpha(1f)
                    .setStartDelay(i * 20)
                    .setDuration(120)
                    .start();
        }
    }
}
