package com.example.aichat.view.main.chat.helpers;

import android.text.TextUtils;
import android.widget.TextView;

public final class FileNameMarqueeHelper {

    private FileNameMarqueeHelper() {
    }

    public static void start(TextView view) {
        if (view == null) return;

        view.animate().cancel();
        view.setAlpha(1f);
        view.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        view.setSingleLine(true);
        view.setHorizontallyScrolling(true);
        view.setMarqueeRepeatLimit(-1);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setSelected(false);

        view.postDelayed(() -> {
            view.setSelected(true);
            view.requestFocus();
        }, 80);
    }

    public static void stop(TextView view) {
        if (view == null) return;

        view.animate().cancel();

        view.animate()
                .alpha(0.99f)
                .setDuration(140)
                .withEndAction(() -> {
                    view.setSelected(false);
                    view.clearFocus();
                    view.setFocusable(false);
                    view.setFocusableInTouchMode(false);
                    view.setEllipsize(TextUtils.TruncateAt.END);
                    view.setHorizontallyScrolling(false);
                    view.setAlpha(1f);
                })
                .start();
    }

    public static void reset(TextView view) {
        if (view == null) return;

        view.animate().cancel();
        view.setSelected(false);
        view.clearFocus();
        view.setFocusable(false);
        view.setFocusableInTouchMode(false);
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setHorizontallyScrolling(false);
        view.setAlpha(1f);
    }
}
