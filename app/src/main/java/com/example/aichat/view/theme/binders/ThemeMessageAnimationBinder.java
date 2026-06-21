package com.example.aichat.view.theme.binders;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.View;
import java.util.Locale;

public class ThemeMessageAnimationBinder {

    public static final String ANIMATION_DEFAULT = "default";
    public static final String ANIMATION_SMOOTH = "smooth";
    public static final String ANIMATION_SLIDE_UP = "slide_up";
    public static final String ANIMATION_SLIDE_UP_BOUNCE = "slide_up_bounce";
    public static final String ANIMATION_FADE = "fade";
    public static final String ANIMATION_SCALE = "scale";

    private static final String TAG = "MessageAnimation";

    private ThemeMessageAnimationBinder() {
    }

    public static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ANIMATION_DEFAULT;
        }

        String normalized = value.trim()
                .toLowerCase(Locale.US)
                .replace(" ", "_")
                .replace("-", "_");

        if (normalized.equals(ANIMATION_DEFAULT)
                || normalized.contains("default")
                || normalized.contains("стандарт")
                || normalized.contains("умолч")
                || normalized.contains("predeterminado")
                || normalized.contains("domy")
                || normalized.contains("за_замовч")
                || normalized.contains("замовч")) {
            return ANIMATION_DEFAULT;
        }

        if (normalized.equals(ANIMATION_SMOOTH)
                || normalized.contains("smooth")
                || normalized.contains("плав")
                || normalized.contains("suave")
                || normalized.contains("płynn")
                || normalized.contains("plynn")
                || normalized.contains("плавн")) {
            return ANIMATION_SMOOTH;
        }

        if (normalized.equals(ANIMATION_SLIDE_UP_BOUNCE)
                || normalized.contains("slide_up_bounce")
                || normalized.contains("bounce")
                || normalized.contains("пруж")
                || normalized.contains("rebote")
                || normalized.contains("odbici")
                || normalized.contains("відск")
                || normalized.contains("vidsk")) {
            return ANIMATION_SLIDE_UP_BOUNCE;
        }

        if (normalized.equals(ANIMATION_SLIDE_UP)
                || normalized.contains("slide_up")
                || normalized.contains("slide")
                || normalized.contains("сдвиг")
                || normalized.contains("вверх")
                || normalized.contains("deslizar")
                || normalized.contains("przesuni")
                || normalized.contains("w_gór")
                || normalized.contains("w_gor")
                || normalized.contains("зсув")
                || normalized.contains("вгору")) {
            return ANIMATION_SLIDE_UP;
        }

        if (normalized.equals(ANIMATION_FADE)
                || normalized.contains("fade")
                || normalized.contains("появ")
                || normalized.contains("исчез")
                || normalized.contains("desvan")
                || normalized.contains("zanik")
                || normalized.contains("згасан")
                || normalized.contains("затух")) {
            return ANIMATION_FADE;
        }

        if (normalized.equals(ANIMATION_SCALE)
                || normalized.contains("scale")
                || normalized.contains("zoom")
                || normalized.contains("масштаб")
                || normalized.contains("escala")
                || normalized.contains("skala")
                || normalized.contains("масштаб")) {
            return ANIMATION_SCALE;
        }

        return ANIMATION_DEFAULT;
    }

    public static boolean isDefault(String animation) {
        return ANIMATION_DEFAULT.equals(normalize(animation));
    }

    public static void apply(View view, String animation) {
        if (view == null) {
            return;
        }

        String normalized = normalize(animation);

        Log.d(TAG, "apply() animation = " + normalized);

        if (ANIMATION_DEFAULT.equals(normalized)) {
            finish(view);
            return;
        }

        runSafely(view, () -> {
            prepare(view);

            if (ANIMATION_SMOOTH.equals(normalized)) {
                applySmooth(view);
                return;
            }

            if (ANIMATION_SLIDE_UP.equals(normalized)) {
                applySlideUp(view);
                return;
            }

            if (ANIMATION_SLIDE_UP_BOUNCE.equals(normalized)) {
                applySlideUpBounce(view);
                return;
            }

            if (ANIMATION_FADE.equals(normalized)) {
                applyFade(view);
                return;
            }

            if (ANIMATION_SCALE.equals(normalized)) {
                applyScale(view);
                return;
            }

            finish(view);
        });
    }

    private static void runSafely(View view, Runnable runnable) {
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            view.post(runnable);
            return;
        }

        runnable.run();
    }

    private static void prepare(View view) {
        view.animate().setListener(null);
        view.animate().cancel();
        view.clearAnimation();

        resetProperties(view);

        view.setHasTransientState(true);
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private static AnimatorListenerAdapter endResetListener(View view) {
        return new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finish(view);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                finish(view);
            }
        };
    }

    private static void finish(View view) {
        if (view == null) {
            return;
        }

        view.animate().setListener(null);
        resetProperties(view);
        view.setLayerType(View.LAYER_TYPE_NONE, null);
        view.setHasTransientState(false);
    }

    private static void applySmooth(View view) {
        view.setAlpha(0f);
        view.setTranslationY(10f);

        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180L)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(endResetListener(view))
                .start();
    }

    private static void applySlideUp(View view) {
        view.setAlpha(0f);
        view.setTranslationY(22f);

        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180L)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(endResetListener(view))
                .start();
    }

    private static void applySlideUpBounce(View view) {
        view.setAlpha(0f);
        view.setTranslationY(28f);

        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220L)
                .setInterpolator(new OvershootInterpolator(0.55f))
                .setListener(endResetListener(view))
                .start();
    }

    private static void applyFade(View view) {
        view.setAlpha(0f);

        view.animate()
                .alpha(1f)
                .setDuration(160L)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(endResetListener(view))
                .start();
    }

    private static void applyScale(View view) {
        view.setAlpha(0f);
        view.setScaleX(0.96f);
        view.setScaleY(0.96f);

        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180L)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(endResetListener(view))
                .start();
    }

    private static void resetProperties(View view) {
        view.setAlpha(1f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setRotation(0f);
    }
}
