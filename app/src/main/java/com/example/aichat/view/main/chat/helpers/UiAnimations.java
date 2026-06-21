package com.example.aichat.view.main.chat.helpers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

public class UiAnimations {

    private static final long MEMBERS_PANEL_OPEN_DURATION_MS = 190L;
    private static final long MEMBERS_PANEL_CLOSE_DURATION_MS = 150L;
    private static final long MEMBERS_PANEL_OUTSIDE_FADE_MS = 120L;
    private static final long QUICK_PRESS_DURATION_MS = 80L;

    private static final float MEMBERS_PANEL_START_TRANSLATION_DP = -18f;
    private static final float MEMBERS_PANEL_CLOSE_TRANSLATION_DP = -14f;

    private UiAnimations() {
    }

    // =========================================================
    // 1. ОБЩИЕ FADE IN / FADE OUT
    // =========================================================

    public static void fadeIn(View view) {
        if (view == null) return;

        view.animate().cancel();
        view.clearAnimation();

        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationY(dp(view, 12f));

        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200L)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(null)
                .start();
    }

    public static void fadeOut(View view) {
        if (view == null) return;

        view.animate().cancel();

        view.animate()
                .alpha(0f)
                .translationY(dp(view, 12f))
                .setDuration(160L)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    view.setVisibility(View.GONE);
                    view.setAlpha(1f);
                    view.setTranslationY(0f);
                })
                .start();
    }

    // =========================================================
    // 2. ПАНЕЛЬ УЧАСТНИКОВ ЧАТА
    // =========================================================

    public static RecyclerView.ItemAnimator createMembersPanelItemAnimator() {
        DefaultItemAnimator animator = new DefaultItemAnimator();

        animator.setAddDuration(140L);
        animator.setRemoveDuration(140L);
        animator.setMoveDuration(150L);
        animator.setChangeDuration(100L);

        return animator;
    }

    public static void animateMembersPanelOpen(
            View membersPanel,
            View outsideClickArea,
            Runnable onEnd
    ) {
        if (membersPanel == null) {
            if (onEnd != null) onEnd.run();
            return;
        }

        cancelMembersPanelAnimations(membersPanel, outsideClickArea);

        membersPanel.setVisibility(View.VISIBLE);
        membersPanel.setClickable(true);
        membersPanel.setFocusable(true);
        membersPanel.setAlpha(0f);
        membersPanel.setTranslationY(dp(membersPanel, MEMBERS_PANEL_START_TRANSLATION_DP));
        membersPanel.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        if (outsideClickArea != null) {
            outsideClickArea.setVisibility(View.VISIBLE);
            outsideClickArea.setClickable(true);
            outsideClickArea.setFocusable(true);
            outsideClickArea.setAlpha(0f);
            outsideClickArea.animate()
                    .alpha(1f)
                    .setDuration(MEMBERS_PANEL_OUTSIDE_FADE_MS)
                    .setInterpolator(new DecelerateInterpolator())
                    .setListener(null)
                    .start();
        }

        membersPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(MEMBERS_PANEL_OPEN_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        resetPanelViewState(membersPanel);

                        if (outsideClickArea != null) {
                            outsideClickArea.setAlpha(1f);
                        }

                        if (onEnd != null) {
                            onEnd.run();
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        resetPanelViewState(membersPanel);

                        if (outsideClickArea != null) {
                            outsideClickArea.setAlpha(1f);
                        }

                        if (onEnd != null) {
                            onEnd.run();
                        }
                    }
                })
                .start();
    }

    public static void animateMembersPanelClose(
            View membersPanel,
            View outsideClickArea,
            Runnable onEnd
    ) {
        if (membersPanel == null || membersPanel.getVisibility() != View.VISIBLE) {
            hideOutsideClickAreaImmediately(outsideClickArea);

            if (onEnd != null) {
                onEnd.run();
            }

            return;
        }

        cancelMembersPanelAnimations(membersPanel, outsideClickArea);

        membersPanel.setClickable(false);
        membersPanel.setFocusable(false);
        membersPanel.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        if (outsideClickArea != null) {
            outsideClickArea.setClickable(false);
            outsideClickArea.setFocusable(false);
            outsideClickArea.animate()
                    .alpha(0f)
                    .setDuration(MEMBERS_PANEL_OUTSIDE_FADE_MS)
                    .setInterpolator(new AccelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            hideOutsideClickAreaImmediately(outsideClickArea);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            hideOutsideClickAreaImmediately(outsideClickArea);
                        }
                    })
                    .start();
        }

        membersPanel.animate()
                .alpha(0f)
                .translationY(dp(membersPanel, MEMBERS_PANEL_CLOSE_TRANSLATION_DP))
                .setDuration(MEMBERS_PANEL_CLOSE_DURATION_MS)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        membersPanel.setVisibility(View.GONE);
                        membersPanel.setClickable(true);
                        membersPanel.setFocusable(true);
                        resetPanelViewState(membersPanel);

                        if (onEnd != null) {
                            onEnd.run();
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        membersPanel.setVisibility(View.GONE);
                        membersPanel.setClickable(true);
                        membersPanel.setFocusable(true);
                        resetPanelViewState(membersPanel);

                        if (onEnd != null) {
                            onEnd.run();
                        }
                    }
                })
                .start();
    }

    public static void hideMembersPanelImmediately(View membersPanel, View outsideClickArea) {
        cancelMembersPanelAnimations(membersPanel, outsideClickArea);

        if (membersPanel != null) {
            membersPanel.setVisibility(View.GONE);
            membersPanel.setClickable(true);
            membersPanel.setFocusable(true);
            resetPanelViewState(membersPanel);
        }

        hideOutsideClickAreaImmediately(outsideClickArea);
    }

    public static void animateSearchPanelOpen(View panel, Runnable onEnd) {
        if (panel == null) {
            if (onEnd != null) onEnd.run();
            return;
        }

        panel.animate().setListener(null);
        panel.animate().cancel();
        panel.clearAnimation();

        if (panel.getVisibility() == View.VISIBLE && panel.getAlpha() == 1f) {
            if (onEnd != null) onEnd.run();
            return;
        }

        panel.setVisibility(View.VISIBLE);
        panel.setClickable(true);
        panel.setFocusable(true);
        panel.setAlpha(0f);
        panel.setTranslationY(dp(panel, MEMBERS_PANEL_START_TRANSLATION_DP));
        panel.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        panel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(MEMBERS_PANEL_OPEN_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        resetPanelViewState(panel);
                        if (onEnd != null) onEnd.run();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        resetPanelViewState(panel);
                        if (onEnd != null) onEnd.run();
                    }
                })
                .start();
    }

    public static void animateSearchPanelClose(View panel, Runnable onEnd) {
        if (panel == null || panel.getVisibility() != View.VISIBLE) {
            hideSearchPanelImmediately(panel);
            if (onEnd != null) onEnd.run();
            return;
        }

        panel.animate().setListener(null);
        panel.animate().cancel();
        panel.clearAnimation();
        panel.setClickable(false);
        panel.setFocusable(false);
        panel.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        panel.animate()
                .alpha(0f)
                .translationY(dp(panel, MEMBERS_PANEL_CLOSE_TRANSLATION_DP))
                .setDuration(MEMBERS_PANEL_CLOSE_DURATION_MS)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        panel.setVisibility(View.GONE);
                        panel.setClickable(true);
                        panel.setFocusable(true);
                        resetPanelViewState(panel);
                        if (onEnd != null) onEnd.run();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        panel.setVisibility(View.GONE);
                        panel.setClickable(true);
                        panel.setFocusable(true);
                        resetPanelViewState(panel);
                        if (onEnd != null) onEnd.run();
                    }
                })
                .start();
    }

    public static void hideSearchPanelImmediately(View panel) {
        if (panel == null) return;

        panel.animate().setListener(null);
        panel.animate().cancel();
        panel.clearAnimation();
        panel.setVisibility(View.GONE);
        panel.setClickable(true);
        panel.setFocusable(true);
        resetPanelViewState(panel);
    }

    public static void animateOptionsButtonHide(View button, Runnable onEnd) {
        if (button == null) {
            if (onEnd != null) onEnd.run();
            return;
        }

        button.animate().cancel();

        button.animate()
                .alpha(0f)
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(90L)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    button.setVisibility(View.GONE);
                    button.setAlpha(1f);
                    button.setScaleX(1f);
                    button.setScaleY(1f);

                    if (onEnd != null) {
                        onEnd.run();
                    }
                })
                .start();
    }

    public static void animateOptionsButtonShow(View button) {
        if (button == null) return;

        button.animate().cancel();

        button.setVisibility(View.VISIBLE);
        button.setAlpha(0f);
        button.setScaleX(0.92f);
        button.setScaleY(0.92f);

        button.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(120L)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(null)
                .start();
    }

    public static void animateSmallButtonClick(View view, Runnable endAction) {
        if (view == null) {
            if (endAction != null) {
                endAction.run();
            }

            return;
        }

        view.animate().cancel();

        view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(QUICK_PRESS_DURATION_MS)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(QUICK_PRESS_DURATION_MS)
                        .setInterpolator(new DecelerateInterpolator())
                        .withEndAction(() -> {
                            if (endAction != null) {
                                endAction.run();
                            }
                        })
                        .start())
                .start();
    }

    public static void animateListItemClick(View view, Runnable endAction) {
        if (view == null) {
            if (endAction != null) {
                endAction.run();
            }

            return;
        }

        view.animate().cancel();

        view.animate()
                .alpha(0.76f)
                .setDuration(70L)
                .withEndAction(() -> view.animate()
                        .alpha(1f)
                        .setDuration(90L)
                        .withEndAction(() -> {
                            if (endAction != null) {
                                endAction.run();
                            }
                        })
                        .start())
                .start();
    }

    public static void animateRemoveMemberClick(View view, Runnable endAction) {
        if (view == null) {
            if (endAction != null) {
                endAction.run();
            }

            return;
        }

        view.animate().cancel();

        view.animate()
                .scaleX(0.84f)
                .scaleY(0.84f)
                .alpha(0.45f)
                .setDuration(100L)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    view.setEnabled(false);

                    if (endAction != null) {
                        endAction.run();
                    }
                })
                .start();
    }

    public static void animateOnlineStatusAppear(View onlineStatus) {
        if (onlineStatus == null || onlineStatus.getVisibility() != View.VISIBLE) {
            return;
        }

        onlineStatus.animate().cancel();
        onlineStatus.setScaleX(0.8f);
        onlineStatus.setScaleY(0.8f);

        onlineStatus.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(130L)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(null)
                .start();
    }

    private static void cancelMembersPanelAnimations(View membersPanel, View outsideClickArea) {
        if (membersPanel != null) {
            membersPanel.animate().setListener(null);
            membersPanel.animate().cancel();
            membersPanel.clearAnimation();
        }

        if (outsideClickArea != null) {
            outsideClickArea.animate().setListener(null);
            outsideClickArea.animate().cancel();
            outsideClickArea.clearAnimation();
        }
    }

    private static void resetPanelViewState(View membersPanel) {
        if (membersPanel == null) return;

        membersPanel.setAlpha(1f);
        membersPanel.setTranslationY(0f);
        membersPanel.setScaleX(1f);
        membersPanel.setScaleY(1f);
        membersPanel.setLayerType(View.LAYER_TYPE_NONE, null);
    }

    private static void hideOutsideClickAreaImmediately(View outsideClickArea) {
        if (outsideClickArea == null) return;

        outsideClickArea.animate().setListener(null);
        outsideClickArea.animate().cancel();
        outsideClickArea.setVisibility(View.GONE);
        outsideClickArea.setClickable(false);
        outsideClickArea.setFocusable(false);
        outsideClickArea.setAlpha(1f);
        outsideClickArea.setTranslationY(0f);
        outsideClickArea.setScaleX(1f);
        outsideClickArea.setScaleY(1f);
    }

    // =========================================================
    // 3. ДИАЛОГ ВЫБОРА ЯЗЫКА - ОТКРЫТИЕ / ЗАКРЫТИЕ
    // =========================================================

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
                .setDuration(220L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

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
                .setDuration(220L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    dialog.dismiss();
                    if (onEnd != null) onEnd.run();
                })
                .start();
    }

    public static void setupLanguageDialogAnimations(
            AlertDialog dialog,
            java.util.function.Consumer<Integer> onLanguageSelected
    ) {
        if (dialog == null) return;

        if (dialog.getWindow() != null) {
            dialog.getWindow().setWindowAnimations(0);
        }

        dialog.setOnShowListener(d -> animateLanguageDialogOpen(dialog));

        final boolean[] isClosing = {false};

        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK
                    && event.getAction() == KeyEvent.ACTION_UP
                    && !isClosing[0]) {
                isClosing[0] = true;
                animateLanguageDialogClose(dialog, null);
                return true;
            }

            return false;
        });

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
    // 4. ЧАТ - ОТКРЫТИЕ / ЗАКРЫТИЕ
    // =========================================================

    public static void animateChatOpen(View chatContainer, View chatList) {
        if (chatContainer == null || chatList == null) return;

        chatContainer.setVisibility(View.VISIBLE);
        chatContainer.setAlpha(0f);
        chatContainer.setTranslationX(dp(chatContainer, 40f));

        chatContainer.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(180L)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        chatList.animate()
                .alpha(0f)
                .translationX(dp(chatList, -24f))
                .setDuration(150L)
                .withEndAction(() -> chatList.setVisibility(View.GONE))
                .start();
    }

    public static void animateChatClose(View chatContainer, View chatList) {
        if (chatContainer == null || chatList == null) return;

        chatList.setVisibility(View.VISIBLE);
        chatList.setAlpha(0f);
        chatList.setTranslationX(dp(chatList, -24f));

        chatList.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(180L)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        chatContainer.animate()
                .alpha(0f)
                .translationX(dp(chatContainer, 40f))
                .setDuration(150L)
                .withEndAction(() -> chatContainer.setVisibility(View.GONE))
                .start();
    }

    // =========================================================
    // 5. ЧАТЫ - ДОБАВЛЕНИЕ / УДАЛЕНИЕ / ОБНОВЛЕНИЕ
    // =========================================================

    public static void animateNewChat(View itemView) {
        if (itemView == null) return;

        itemView.setAlpha(0f);
        itemView.setTranslationX(dp(itemView, -32f));

        itemView.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(180L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public static void animateNewMessage(View itemView) {
        if (itemView == null) return;

        itemView.setScaleX(0.92f);
        itemView.setScaleY(0.92f);
        itemView.setAlpha(0f);

        itemView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(160L)
                .setInterpolator(new OvershootInterpolator(1.15f))
                .start();
    }

    public static void animateChatRemove(View itemView, Runnable onEnd) {
        if (itemView == null) {
            if (onEnd != null) onEnd.run();
            return;
        }

        itemView.animate()
                .alpha(0f)
                .translationX(-itemView.getWidth() * 0.3f)
                .setDuration(150L)
                .withEndAction(onEnd)
                .start();
    }

    public static void animateRemoveChat(View itemView, Runnable endAction) {
        if (itemView == null) {
            if (endAction != null) endAction.run();
            return;
        }

        itemView.animate()
                .alpha(0f)
                .translationX(itemView.getWidth() * 0.35f)
                .setDuration(200L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(endAction)
                .start();
    }

    public static void animateChatUpdated(View itemView) {
        if (itemView == null) return;

        itemView.setAlpha(0.5f);
        itemView.animate()
                .alpha(1f)
                .setDuration(200L)
                .start();
    }

    // =========================================================
    // 6. BADGE НЕПРОЧИТАННЫХ СООБЩЕНИЙ
    // =========================================================

    public static void animateUnreadBadge(View badge) {
        if (badge == null) return;

        badge.setScaleX(0.6f);
        badge.setScaleY(0.6f);

        badge.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(140L)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();
    }

    // =========================================================
    // 7. НАЖАТИЕ НА ЭЛЕМЕНТ
    // =========================================================

    public static void animatePress(View v, boolean pressed) {
        if (v == null) return;

        if (pressed) {
            v.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(80L)
                    .start();
        } else {
            v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120L)
                    .setInterpolator(new OvershootInterpolator(2f))
                    .start();
        }
    }

    // =========================================================
    // 8. FAB
    // =========================================================

    public static void animateFabAppear(View fab) {
        if (fab == null) return;

        fab.setScaleX(0f);
        fab.setScaleY(0f);
        fab.setAlpha(0f);

        fab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(200L)
                .setInterpolator(new OvershootInterpolator(1.3f))
                .start();
    }

    // =========================================================
    // 9. СООБЩЕНИЯ В ЧАТЕ
    // =========================================================

    public static void animateInitialMessages(RecyclerView recycler) {
        if (recycler == null) return;

        for (int i = 0; i < recycler.getChildCount(); i++) {
            View child = recycler.getChildAt(i);

            if (child == null) continue;

            child.setAlpha(0f);
            child.animate()
                    .alpha(1f)
                    .setStartDelay(i * 20L)
                    .setDuration(120L)
                    .start();
        }
    }

    public static void animateMessageAppearance(View itemView) {
        if (itemView == null) return;

        itemView.setTranslationX(dp(itemView, 18f));
        itemView.setScaleX(0.92f);
        itemView.setScaleY(0.92f);
        itemView.setAlpha(0f);

        itemView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationX(0f)
                .setDuration(200L)
                .setInterpolator(new OvershootInterpolator(1.15f))
                .start();
    }

    public static void animateMessageSending(View itemView) {
        if (itemView == null) return;

        itemView.setScaleX(1f);
        itemView.setScaleY(1f);

        itemView.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(120L)
                .withEndAction(() -> itemView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120L)
                        .start())
                .start();
    }

    // =========================================================
    // 10. PIN BOUNCE
    // =========================================================

    public static void animatePinBounce(View pin) {
        if (pin == null) return;

        pin.setScaleX(0.7f);
        pin.setScaleY(0.7f);

        pin.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180L)
                .setInterpolator(new OvershootInterpolator(2f))
                .start();
    }

    // =========================================================
    // 11. ПОИСК
    // =========================================================

    public static void animateSearchOpen(View panel) {
        if (panel == null) return;

        panel.setTranslationY(dp(panel, -32f));
        panel.setAlpha(0f);
        panel.setVisibility(View.VISIBLE);

        panel.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(160L)
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
                .setDuration(260L)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    blurView.setVisibility(View.GONE);

                    if (onEnd != null) {
                        onEnd.run();
                    }
                })
                .start();

        if (content != null) {
            content.setScaleX(0.95f);
            content.setScaleY(0.95f);
            content.setAlpha(0f);

            content.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(240L)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }
    }

    public static void animateQrReveal(View loadingView, Runnable onEnd) {
        if (loadingView == null) return;

        loadingView.animate().cancel();

        loadingView.animate()
                .alpha(0f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .translationY(dp(loadingView, 20f))
                .setDuration(220L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    loadingView.setVisibility(View.GONE);
                    loadingView.setAlpha(1f);
                    loadingView.setScaleX(1f);
                    loadingView.setScaleY(1f);
                    loadingView.setTranslationY(0f);

                    if (onEnd != null) {
                        onEnd.run();
                    }
                })
                .start();
    }

    // =========================================================
    // 12. БЕСКОНЕЧНАЯ ПУЛЬСИРУЮЩАЯ АНИМАЦИЯ
    // =========================================================

    public static void startInfinitePulseAnimation(View iconView, View textView) {
        if (iconView == null) return;

        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(1f, 1.2f, 1f);
        scaleAnimator.setDuration(1600L);
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
            textAnimator.setDuration(1600L);
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
    // 13. QR REVEAL WITH SCALE
    // =========================================================

    public static void animateQrRevealWithScale(
            View blurOverlay,
            View qrImage,
            View tapHintIcon,
            View tapHintText,
            Runnable onComplete
    ) {
        if (blurOverlay == null) return;

        blurOverlay.animate().cancel();

        if (tapHintIcon != null) {
            tapHintIcon.animate()
                    .alpha(0f)
                    .scaleX(0.5f)
                    .scaleY(0.5f)
                    .setDuration(200L)
                    .start();
        }

        if (tapHintText != null) {
            tapHintText.animate()
                    .alpha(0f)
                    .translationY(dp(tapHintText, -20f))
                    .setDuration(200L)
                    .start();
        }

        blurOverlay.animate()
                .alpha(0f)
                .setDuration(300L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    blurOverlay.setVisibility(View.GONE);
                    blurOverlay.setClickable(false);

                    if (tapHintIcon != null) {
                        tapHintIcon.setVisibility(View.GONE);
                    }

                    if (tapHintText != null) {
                        tapHintText.setVisibility(View.GONE);
                    }
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
                    .setDuration(250L)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .withEndAction(() -> {
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    })
                    .start();
        } else {
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    // =========================================================
    // 14. ПЛАВНАЯ СМЕНА ТЕКСТА
    // =========================================================

    public static void animateTextChange(TextView textView, String newText, long duration) {
        if (textView == null) return;

        textView.animate()
                .alpha(0f)
                .setDuration(duration / 2L)
                .withEndAction(() -> {
                    textView.setText(newText);
                    textView.animate()
                            .alpha(1f)
                            .setDuration(duration / 2L)
                            .start();
                })
                .start();
    }

    public static void animateTextChangeWithSlide(TextView textView, String newText, long duration) {
        if (textView == null) return;

        textView.animate()
                .alpha(0f)
                .translationY(dp(textView, -10f))
                .setDuration(duration / 2L)
                .withEndAction(() -> {
                    textView.setText(newText);
                    textView.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(duration / 2L)
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
                .setDuration(duration / 2L)
                .withEndAction(() -> {
                    textView.setText(newText);
                    textView.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(duration / 2L)
                            .start();
                })
                .start();
    }

    private static float dp(View view, float value) {
        if (view == null || view.getResources() == null) {
            return value;
        }

        return value * view.getResources().getDisplayMetrics().density;
    }
}
