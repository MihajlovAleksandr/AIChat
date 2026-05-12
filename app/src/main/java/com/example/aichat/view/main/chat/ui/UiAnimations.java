package com.example.aichat.view.main.chat.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

public class UiAnimations {

    // =========================================================
    // 1. FADE IN / FADE OUT
    // =========================================================

    public static void fadeIn(View view) {
        if (view == null) return;

        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationY(20f);

        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    public static void fadeOut(View view) {
        if (view == null) return;

        view.animate()
                .alpha(0f)
                .translationY(20f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> view.setVisibility(View.GONE))
                .start();
    }

    // =========================================================
    // 2. ДИАЛОГ ВЫБОРА ЯЗЫКА - ОТКРЫТИЕ / ЗАКРЫТИЕ
    // =========================================================

    /**
     * Анимация открытия диалога выбора языка
     * @param dialog Диалог для анимации
     */
    public static void animateLanguageDialogOpen(AlertDialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return;

        View decor = dialog.getWindow().getDecorView();
        decor.animate().cancel();
        decor.clearAnimation();

        decor.setAlpha(0f);
        decor.setScaleX(0.85f);
        decor.setScaleY(0.85f);

        decor.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    /**
     * Анимация закрытия диалога выбора языка (зеркальная анимации открытия)
     * @param dialog Диалог для анимации
     * @param onEnd Действие после завершения анимации
     */
    public static void animateLanguageDialogClose(AlertDialog dialog, Runnable onEnd) {
        if (dialog == null || dialog.getWindow() == null) {
            if (dialog != null) dialog.dismiss();
            if (onEnd != null) onEnd.run();
            return;
        }

        View decor = dialog.getWindow().getDecorView();
        decor.animate().cancel();

        decor.animate()
                .alpha(0f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(220)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    dialog.dismiss();
                    if (onEnd != null) onEnd.run();
                })
                .start();
    }

    /**
     * Настройка анимаций для диалога выбора языка (открытие и закрытие при любых событиях)
     * @param dialog Диалог для настройки
     * @param onLanguageSelected Действие при выборе языка
     */
    public static void setupLanguageDialogAnimations(AlertDialog dialog, java.util.function.Consumer<Integer> onLanguageSelected) {
        if (dialog == null) return;

        // Отключаем стандартные анимации
        if (dialog.getWindow() != null) {
            dialog.getWindow().setWindowAnimations(0);
        }

        // Анимация открытия
        dialog.setOnShowListener(d -> animateLanguageDialogOpen(dialog));

        // Перехватываем закрытие
        final boolean[] isClosing = {false};

        // 1. Перехватываем нажатие на кнопку "Назад"
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK &&
                    event.getAction() == android.view.MotionEvent.ACTION_UP &&
                    !isClosing[0]) {
                isClosing[0] = true;
                animateLanguageDialogClose(dialog, null);
                return true;
            }
            return false;
        });

        // 2. Перехватываем нажатие вне диалога
        if (dialog.getWindow() != null) {
            dialog.getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            );

            dialog.getWindow().getDecorView().setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == android.view.MotionEvent.ACTION_OUTSIDE && !isClosing[0]) {
                    isClosing[0] = true;
                    animateLanguageDialogClose(dialog, null);
                    return true;
                }
                if (motionEvent.getAction() == android.view.MotionEvent.ACTION_UP) {
                    view.performClick();
                }
                return false;
            });
        }

        // 3. Анимация при любом другом закрытии
        dialog.setOnDismissListener(d -> {
            if (!isClosing[0]) {
                isClosing[0] = true;
                animateLanguageDialogClose(dialog, null);
            }
        });

        // 4. Обработка выбора пункта списка
        android.widget.ListView listView = dialog.getListView();
        if (listView != null) {
            listView.setOnItemClickListener((parent, view, position, id) -> {
                if (isClosing[0]) return;
                isClosing[0] = true;

                animateLanguageDialogClose(dialog, () -> {
                    if (onLanguageSelected != null) {
                        onLanguageSelected.accept(position);
                    }
                });
            });
        }
    }

    // =========================================================
    // 3. ЧАТ - ОТКРЫТИЕ / ЗАКРЫТИЕ
    // =========================================================

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

    // =========================================================
    // 4. ЧАТЫ - ДОБАВЛЕНИЕ / УДАЛЕНИЕ / ОБНОВЛЕНИЕ
    // =========================================================

    public static void animateNewChat(View itemView) {
        itemView.setAlpha(0f);
        itemView.setTranslationX(-40f);

        itemView.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(180)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

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

    public static void animateChatRemove(View itemView, Runnable onEnd) {
        itemView.animate()
                .alpha(0f)
                .translationX(-itemView.getWidth() * 0.3f)
                .setDuration(150)
                .withEndAction(onEnd)
                .start();
    }

    public static void animateRemoveChat(View itemView, Runnable endAction) {
        itemView.animate()
                .alpha(0f)
                .translationX(itemView.getWidth() * 0.35f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(endAction)
                .start();
    }

    public static void animateChatUpdated(View itemView) {
        itemView.setAlpha(0.5f);
        itemView.animate()
                .alpha(1f)
                .setDuration(200)
                .start();
    }

    // =========================================================
    // 5. BADGE НЕПРОЧИТАННЫХ СООБЩЕНИЙ
    // =========================================================

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

    // =========================================================
    // 6. НАЖАТИЕ НА ЭЛЕМЕНТ
    // =========================================================

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

    // =========================================================
    // 7. FAB
    // =========================================================

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

    // =========================================================
    // 8. СООБЩЕНИЯ В ЧАТЕ
    // =========================================================

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

    public static void animateMessageAppearance(View itemView) {
        itemView.setTranslationX(18f);
        itemView.setScaleX(0.92f);
        itemView.setScaleY(0.92f);
        itemView.setAlpha(0f);

        itemView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationX(0f)
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator(1.15f))
                .start();
    }

    public static void animateMessageSending(View itemView) {
        itemView.setScaleX(1f);
        itemView.setScaleY(1f);

        itemView.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(120)
                .withEndAction(() -> itemView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start())
                .start();
    }

    // =========================================================
    // 9. PIN BOUNCE
    // =========================================================

    public static void animatePinBounce(View pin) {
        pin.setScaleX(0.7f);
        pin.setScaleY(0.7f);

        pin.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180)
                .setInterpolator(new OvershootInterpolator(2f))
                .start();
    }

    // =========================================================
    // 10. ПОИСК
    // =========================================================

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

    public static void animateBlurReveal(View blurView, View content, Runnable onEnd) {
        if (blurView == null) return;

        blurView.animate().cancel();

        blurView.animate()
                .alpha(0f)
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(260)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    blurView.setVisibility(View.GONE);
                    if (onEnd != null) onEnd.run();
                })
                .start();

        content.setScaleX(0.95f);
        content.setScaleY(0.95f);
        content.setAlpha(0f);

        content.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(240)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    public static void animateQrReveal(View loadingView, Runnable onEnd) {
        if (loadingView == null) return;

        loadingView.animate().cancel();

        loadingView.animate()
                .alpha(0f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .translationY(20f)
                .setDuration(220)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    loadingView.setVisibility(View.GONE);
                    loadingView.setAlpha(1f);
                    loadingView.setScaleX(1f);
                    loadingView.setScaleY(1f);
                    loadingView.setTranslationY(0f);

                    if (onEnd != null) onEnd.run();
                })
                .start();
    }

    // =========================================================
    // 11. БЕСКОНЕЧНАЯ ПУЛЬСИРУЮЩАЯ АНИМАЦИЯ
    // =========================================================

    public static void startInfinitePulseAnimation(View iconView, View textView) {
        if (iconView == null) return;

        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(1f, 1.2f, 1f);
        scaleAnimator.setDuration(1600);
        scaleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        scaleAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            iconView.setScaleX(value);
            iconView.setScaleY(value);
        });
        scaleAnimator.start();

        if (textView != null) {
            ValueAnimator textAnimator = ValueAnimator.ofFloat(0f, -10f, 0f);
            textAnimator.setDuration(1600);
            textAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            textAnimator.setRepeatCount(ValueAnimator.INFINITE);
            textAnimator.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                textView.setTranslationY(value);
            });
            textAnimator.start();
        }
    }

    public static void stopInfinitePulseAnimation(View iconView, View textView) {
        if (iconView != null) {
            iconView.animate().cancel();
            iconView.clearAnimation();
            iconView.setScaleX(1f);
            iconView.setScaleY(1f);
        }
        if (textView != null) {
            textView.animate().cancel();
            textView.clearAnimation();
            textView.setTranslationY(0f);
        }
    }

    // =========================================================
    // 12. QR REVEAL WITH SCALE
    // =========================================================

    public static void animateQrRevealWithScale(View blurOverlay, View qrImage, View tapHintIcon, View tapHintText, Runnable onComplete) {
        if (blurOverlay == null) return;

        blurOverlay.animate().cancel();

        if (tapHintIcon != null) {
            tapHintIcon.animate()
                    .alpha(0f)
                    .scaleX(0.5f)
                    .scaleY(0.5f)
                    .setDuration(200)
                    .start();
        }

        if (tapHintText != null) {
            tapHintText.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(200)
                    .start();
        }

        blurOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    blurOverlay.setVisibility(View.GONE);
                    blurOverlay.setClickable(false);
                    if (tapHintIcon != null) tapHintIcon.setVisibility(View.GONE);
                    if (tapHintText != null) tapHintText.setVisibility(View.GONE);
                })
                .start();

        if (qrImage != null) {
            qrImage.setScaleX(0.85f);
            qrImage.setScaleY(0.85f);
            qrImage.setAlpha(0f);

            qrImage.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(250)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .withEndAction(() -> {
                        if (onComplete != null) onComplete.run();
                    })
                    .start();
        } else {
            if (onComplete != null) onComplete.run();
        }
    }

    // =========================================================
    // 13. ПЛАВНАЯ СМЕНА ТЕКСТА
    // =========================================================

    public static void animateTextChange(TextView textView, String newText, long duration) {
        if (textView == null) return;

        textView.animate()
                .alpha(0f)
                .setDuration(duration / 2)
                .withEndAction(() -> {
                    textView.setText(newText);
                    textView.animate()
                            .alpha(1f)
                            .setDuration(duration / 2)
                            .start();
                })
                .start();
    }

    public static void animateTextChangeWithSlide(TextView textView, String newText, long duration) {
        if (textView == null) return;

        textView.animate()
                .alpha(0f)
                .translationY(-10f)
                .setDuration(duration / 2)
                .withEndAction(() -> {
                    textView.setText(newText);
                    textView.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(duration / 2)
                            .start();
                })
                .start();
    }

    public static void animateTextChangeWithScale(TextView textView, String newText, long duration) {
        if (textView == null) return;

        textView.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(duration / 2)
                .withEndAction(() -> {
                    textView.setText(newText);
                    textView.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(duration / 2)
                            .start();
                })
                .start();
    }
}