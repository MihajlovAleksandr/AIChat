package com.example.aichat.view.main.chat.helpers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.view.animation.LinearInterpolator;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;
import com.example.aichat.model.entities.ChatGameState;
import com.example.aichat.model.entities.ChatType;
import com.example.aichat.R;
import com.example.aichat.view.main.chat.ChatFragment;
import com.google.android.material.button.MaterialButton;

@UnstableApi
public class ChatUi {

    private static final long TYPING_FADE_IN_MS = 160L;
    private static final long TYPING_FADE_OUT_MS = 130L;
    private static final long TYPING_DOT_DURATION_MS = 900L;

    private final ChatFragment fragment;
    private final View root;

    private ImageButton btnAttachFile;
    private ImageButton btnRecordVoice;
    private ImageButton btnRecordVideoCircle;
    private ImageButton btnBack;
    private ImageButton btnOptions;

    private MaterialButton bSendMessage;
    private Button bExportChat;
    private Button btnUnreadJump;

    private TextView tvChatEnded;
    private View tvChatEndedWithOptions;
    private Button btnChatWithPerson;
    private Button btnChatWithAi;
    private TextView tvChatTitle;
    private View typingStatusContainer;
    private TextView tvTypingStatus;
    private TextView typingDot1;
    private TextView typingDot2;
    private TextView typingDot3;

    private View membersPanel;
    private View invisibleClickArea;

    private View searchPanel;
    private View resultPanel;
    private View searchStatus;
    private View cancelSearchButton;

    private View inputPanel;
    private View fileCounterLayout;
    private View dividerChatEnded;

    private EditText messageInput;

    private View editPanel;
    private TextView editOriginalText;
    private ImageButton editCancel;

    private View replyPanel;
    private TextView replyOriginalText;
    private ImageButton replyCancel;

    private View connectionBanner;
    private TextView connectionBannerText;

    private boolean isEditMode = false;
    private boolean isReplyMode = false;
    private boolean isChatEnded = false;

    private String currentChatTitle = "";
    private boolean typingStatusVisible = false;
    private AnimatorSet typingDotsAnimator;

    public ChatUi(View root, ChatFragment fragment) {
        this.root = root;
        this.fragment = fragment;
        init();
    }

    private void init() {
        btnBack = root.findViewById(R.id.btn_back);
        btnOptions = root.findViewById(R.id.btn_options);
        btnAttachFile = root.findViewById(R.id.btn_attach_file);
        btnRecordVoice = root.findViewById(R.id.btn_record_voice);
        btnRecordVideoCircle = root.findViewById(R.id.btn_record_video_circle);

        bSendMessage = root.findViewById(R.id.b_send_message);
        bExportChat = root.findViewById(R.id.b_export_chat);
        btnUnreadJump = root.findViewById(R.id.btn_unread_jump);

        tvChatEnded = root.findViewById(R.id.tv_chat_ended);
        tvChatEndedWithOptions = root.findViewById(R.id.tv_chat_ended_with_options);
        btnChatWithPerson = root.findViewById(R.id.btn_chat_with_person);
        btnChatWithAi = root.findViewById(R.id.btn_chat_with_ai);
        tvChatTitle = root.findViewById(R.id.tv_chat_title);
        typingStatusContainer = root.findViewById(R.id.typing_status_container);
        tvTypingStatus = root.findViewById(R.id.tv_typing_status);
        typingDot1 = root.findViewById(R.id.typing_dot_1);
        typingDot2 = root.findViewById(R.id.typing_dot_2);
        typingDot3 = root.findViewById(R.id.typing_dot_3);

        membersPanel = root.findViewById(R.id.membersPanel);
        invisibleClickArea = root.findViewById(R.id.invisibleClickArea);

        searchPanel = root.findViewById(R.id.search_layout);
        resultPanel = root.findViewById(R.id.searchResult_layout);
        searchStatus = root.findViewById(R.id.tv_search_status);
        cancelSearchButton = root.findViewById(R.id.btn_cancel_search);

        inputPanel = root.findViewById(R.id.input_panel);
        fileCounterLayout = root.findViewById(R.id.file_counter_layout);
        dividerChatEnded = root.findViewById(R.id.divider_chat_ended);

        messageInput = root.findViewById(R.id.ti_message);

        editPanel = root.findViewById(R.id.edit_panel);
        editOriginalText = root.findViewById(R.id.edit_original_text);
        editCancel = root.findViewById(R.id.edit_cancel);

        replyPanel = root.findViewById(R.id.reply_panel);
        replyOriginalText = root.findViewById(R.id.reply_original_text);
        replyCancel = root.findViewById(R.id.reply_cancel);

        connectionBanner = root.findViewById(R.id.connectionBanner);
        connectionBannerText = root.findViewById(R.id.connectionBannerText);

        if (typingStatusContainer != null) {
            typingStatusContainer.setVisibility(View.GONE);
            typingStatusContainer.setAlpha(0f);
        }

        if (btnBack != null) btnBack.setOnClickListener(v -> fragment.navigateBack());
        if (btnOptions != null) btnOptions.setOnClickListener(v -> fragment.onOptionsClick());
        if (bSendMessage != null) {
            configureSendButton(false);
            bSendMessage.setOnClickListener(v -> fragment.onSendMessageClick());
        }
        if (bExportChat != null) bExportChat.setOnClickListener(v -> fragment.onExportChatClick());
        if (btnAttachFile != null) btnAttachFile.setOnClickListener(v -> fragment.showAttachBottomSheet());
        if (editCancel != null) editCancel.setOnClickListener(v -> fragment.onCancelEdit());
        if (replyCancel != null) replyCancel.setOnClickListener(v -> fragment.onCancelReply());
        if (btnUnreadJump != null) btnUnreadJump.setOnClickListener(v -> fragment.onUnreadJumpClick());
        if (btnChatWithPerson != null) btnChatWithPerson.setOnClickListener(v -> fragment.onChatWithPersonClick());
        if (btnChatWithAi != null) btnChatWithAi.setOnClickListener(v -> fragment.onChatWithAiClick());
    }

    public EditText getMessageInput() {
        return messageInput;
    }

    public void enableSendButton() {
        if (isChatEnded) return;
        if (bSendMessage != null) bSendMessage.setEnabled(true);
    }

    public void disableSendButton() {
        if (bSendMessage != null) bSendMessage.setEnabled(false);
    }

    public void disableMessageInput() {
        if (bSendMessage != null) {
            bSendMessage.setEnabled(false);
            bSendMessage.setVisibility(View.GONE);
        }

        if (messageInput != null) {
            messageInput.setEnabled(false);
            messageInput.setVisibility(View.GONE);
        }

        if (btnAttachFile != null) {
            btnAttachFile.setEnabled(false);
            btnAttachFile.setVisibility(View.GONE);
        }

        if (inputPanel != null) inputPanel.setVisibility(View.GONE);
        if (fileCounterLayout != null) fileCounterLayout.setVisibility(View.GONE);
    }

    public void showChatEnded(boolean isRandomChatQ) {
        isChatEnded = true;
        isEditMode = false;
        isReplyMode = false;

        hideSearchUi();
        hideEditPanel();
        hideReplyPanel();
        hideUnreadJumpButton();
        hideTypingStatus();
        disableMessageInput();

        if (membersPanel != null) membersPanel.setVisibility(View.GONE);
        if (invisibleClickArea != null) invisibleClickArea.setVisibility(View.GONE);
        if (btnOptions != null) btnOptions.setEnabled(false);
        if (dividerChatEnded != null) dividerChatEnded.setVisibility(View.GONE);

        if (isRandomChatQ) {
            if (tvChatEndedWithOptions != null) {
                tvChatEndedWithOptions.setVisibility(View.VISIBLE);
                showRandomChatLoadingState();
            }
            if (tvChatEnded != null) {
                tvChatEnded.setVisibility(View.GONE);
            }
        } else {
            if (tvChatEnded != null) {
                tvChatEnded.setVisibility(View.VISIBLE);
            }
            if (tvChatEndedWithOptions != null) {
                tvChatEndedWithOptions.setVisibility(View.GONE);
            }
        }

        if (bExportChat != null) {
            bExportChat.setVisibility(View.VISIBLE);
            bExportChat.setEnabled(true);
        }
    }

    public void setRandomChatResult(ChatGameState state) {
        if (tvChatEndedWithOptions == null) {
            return;
        }

        ProgressBar loadingProgress = tvChatEndedWithOptions.findViewById(R.id.loading_progress);
        TextView loadingText = tvChatEndedWithOptions.findViewById(R.id.loading_text);
        TextView resultStatus = tvChatEndedWithOptions.findViewById(R.id.result_status);
        TextView chatEndedMessage = tvChatEndedWithOptions.findViewById(R.id.chat_ended_message);
        LinearLayout buttonsContainer = tvChatEndedWithOptions.findViewById(R.id.buttons_container);

        assert fragment.getActivity() != null;
        fragment.getActivity().runOnUiThread(() -> {
            if (state == ChatGameState.Win) {
                if (loadingProgress != null) loadingProgress.setVisibility(View.GONE);
                if (loadingText != null) loadingText.setVisibility(View.GONE);
                if (resultStatus != null) {
                    resultStatus.setVisibility(View.VISIBLE);
                    resultStatus.setText("Победа");
                }
                if (chatEndedMessage != null) chatEndedMessage.setVisibility(View.GONE);
                if (buttonsContainer != null) buttonsContainer.setVisibility(View.GONE);

                if (tvChatEnded != null) {
                    tvChatEnded.setVisibility(View.VISIBLE);
                    tvChatEnded.setText("Чат был окончен");
                }
                if (bExportChat != null) {
                    bExportChat.setVisibility(View.VISIBLE);
                    bExportChat.setEnabled(true);
                }
            } else if (state == ChatGameState.Lose) {
                if (loadingProgress != null) loadingProgress.setVisibility(View.GONE);
                if (loadingText != null) loadingText.setVisibility(View.GONE);
                if (resultStatus != null) {
                    resultStatus.setVisibility(View.VISIBLE);
                    resultStatus.setText("Поражение");
                }
                if (chatEndedMessage != null) chatEndedMessage.setVisibility(View.GONE);
                if (buttonsContainer != null) buttonsContainer.setVisibility(View.GONE);

                if (tvChatEnded != null) {
                    tvChatEnded.setVisibility(View.VISIBLE);
                    tvChatEnded.setText("Чат был окончен");
                }
                if (bExportChat != null) {
                    bExportChat.setVisibility(View.VISIBLE);
                    bExportChat.setEnabled(true);
                }
            } else if (state == ChatGameState.Pending) {
                if (tvChatEndedWithOptions != null && tvChatEndedWithOptions.getVisibility() == View.VISIBLE) {
                    if (loadingProgress != null) loadingProgress.setVisibility(View.GONE);
                    if (loadingText != null) {
                        loadingText.setVisibility(View.VISIBLE);
                        loadingText.setText("Чат был окончен. С кем вы общались:");
                    }
                    if (resultStatus != null) resultStatus.setVisibility(View.GONE);
                    if (chatEndedMessage != null) chatEndedMessage.setVisibility(View.GONE);
                    if (buttonsContainer != null) buttonsContainer.setVisibility(View.VISIBLE);
                }
            } else {
                if (tvChatEndedWithOptions != null) {
                    tvChatEndedWithOptions.setVisibility(View.GONE);
                }
                if (tvChatEnded != null) {
                    tvChatEnded.setVisibility(View.VISIBLE);
                    tvChatEnded.setText("Чат был окончен");
                }
                if (bExportChat != null) {
                    bExportChat.setVisibility(View.VISIBLE);
                    bExportChat.setEnabled(true);
                }
            }
        });
    }

    private void showRandomChatLoadingState() {
        if (tvChatEndedWithOptions == null) return;

        ProgressBar loadingProgress = tvChatEndedWithOptions.findViewById(R.id.loading_progress);
        TextView loadingText = tvChatEndedWithOptions.findViewById(R.id.loading_text);
        TextView resultStatus = tvChatEndedWithOptions.findViewById(R.id.result_status);
        TextView chatEndedMessage = tvChatEndedWithOptions.findViewById(R.id.chat_ended_message);
        LinearLayout buttonsContainer = tvChatEndedWithOptions.findViewById(R.id.buttons_container);

        if (loadingProgress != null) loadingProgress.setVisibility(View.VISIBLE);
        if (loadingText != null) {
            loadingText.setVisibility(View.VISIBLE);
            loadingText.setText("Загрузка...");
        }
        if (resultStatus != null) resultStatus.setVisibility(View.GONE);
        if (chatEndedMessage != null) chatEndedMessage.setVisibility(View.GONE);
        if (buttonsContainer != null) buttonsContainer.setVisibility(View.GONE);
    }

    private void hideSearchUi() {
        UiAnimations.hideSearchPanelImmediately(searchPanel);
        UiAnimations.hideSearchPanelImmediately(resultPanel);
        if (searchStatus != null) searchStatus.setVisibility(View.GONE);
        if (cancelSearchButton != null) cancelSearchButton.setVisibility(View.GONE);
    }

    public void toggleMembersPanel() {
        if (isChatEnded || membersPanel == null || invisibleClickArea == null) return;

        boolean visible = membersPanel.getVisibility() == View.VISIBLE;
        membersPanel.setVisibility(visible ? View.GONE : View.VISIBLE);
        invisibleClickArea.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    public void showSearchPanel() {
        if (isChatEnded) return;

        hideTypingStatus();
        UiAnimations.hideSearchPanelImmediately(resultPanel);
        UiAnimations.animateSearchPanelOpen(searchPanel, null);
        if (btnOptions != null) btnOptions.setEnabled(false);
        if (btnBack != null) btnBack.setEnabled(false);
    }

    public void hideSearchPanel() {
        UiAnimations.animateSearchPanelClose(searchPanel, () -> {
            if (btnOptions != null) btnOptions.setEnabled(!isChatEnded);
            if (btnBack != null) btnBack.setEnabled(true);
        });
    }

    public void showSearchResults() {
        if (isChatEnded) return;

        hideTypingStatus();
        UiAnimations.hideSearchPanelImmediately(searchPanel);
        UiAnimations.animateSearchPanelOpen(resultPanel, null);
    }

    public void hideSearchResults() {
        if (isChatEnded) return;

        UiAnimations.animateSearchPanelClose(resultPanel, () ->
                UiAnimations.animateSearchPanelOpen(searchPanel, null)
        );
    }

    public String getMessageText() {
        return messageInput != null ? messageInput.getText().toString().trim() : "";
    }

    public void clearMessageInput() {
        if (messageInput != null) messageInput.setText("");
    }

    public void setChatTitle(String name) {
        currentChatTitle = name != null ? name : "";

        if (tvChatTitle != null) {
            tvChatTitle.setText(currentChatTitle);
        }
    }

    public void showTypingStatus(String text) {
        if (isChatEnded || typingStatusContainer == null) return;

        String normalizedText = normalizeTypingText(text);
        boolean wasVisible = typingStatusVisible && typingStatusContainer.getVisibility() == View.VISIBLE;
        typingStatusVisible = true;

        if (tvTypingStatus != null && !normalizedText.contentEquals(tvTypingStatus.getText())) {
            tvTypingStatus.setText(normalizedText);
        }

        if (wasVisible) {
            ensureTypingDotsAnimation();
            return;
        }

        typingStatusContainer.animate().cancel();
        typingStatusContainer.setAlpha(0f);
        typingStatusContainer.setTranslationY(6f);
        typingStatusContainer.setVisibility(View.VISIBLE);
        typingStatusContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(TYPING_FADE_IN_MS)
                .setListener(null)
                .start();

        ensureTypingDotsAnimation();
    }

    public void hideTypingStatus() {
        typingStatusVisible = false;
        stopTypingDotsAnimation();

        if (typingStatusContainer == null || typingStatusContainer.getVisibility() != View.VISIBLE) return;

        typingStatusContainer.animate().cancel();
        typingStatusContainer.animate()
                .alpha(0f)
                .translationY(10f)
                .setDuration(TYPING_FADE_OUT_MS)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!typingStatusVisible && typingStatusContainer != null) {
                            typingStatusContainer.setVisibility(View.GONE);
                            typingStatusContainer.setAlpha(1f);
                            typingStatusContainer.setTranslationY(0f);
                        }
                    }
                })
                .start();
    }

    private String normalizeTypingText(String text) {
        String fallback = root.getContext().getString(R.string.chat_typing_status);

        if (text == null || text.trim().isEmpty()) {
            return fallback;
        }

        String value = text.trim()
                .replaceAll("[.。…]+$", "")
                .trim();

        return value.isEmpty() ? fallback : value;
    }

    private void ensureTypingDotsAnimation() {
        if (typingDotsAnimator != null && typingDotsAnimator.isStarted()) return;
        startTypingDotsAnimation();
    }

    private void startTypingDotsAnimation() {
        stopTypingDotsAnimation();

        if (typingDot1 == null || typingDot2 == null || typingDot3 == null) return;

        typingDotsAnimator = new AnimatorSet();
        typingDotsAnimator.playTogether(
                createDotPulse(typingDot1, 0L),
                createDotPulse(typingDot2, 150L),
                createDotPulse(typingDot3, 300L)
        );
        typingDotsAnimator.start();
    }

    private AnimatorSet createDotPulse(View dot, long delay) {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(dot, View.ALPHA, 0.28f, 1f, 0.28f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(dot, View.SCALE_X, 0.82f, 1.18f, 0.82f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(dot, View.SCALE_Y, 0.82f, 1.18f, 0.82f);

        alpha.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);

        alpha.setRepeatMode(ObjectAnimator.RESTART);
        scaleX.setRepeatMode(ObjectAnimator.RESTART);
        scaleY.setRepeatMode(ObjectAnimator.RESTART);

        alpha.setDuration(TYPING_DOT_DURATION_MS);
        scaleX.setDuration(TYPING_DOT_DURATION_MS);
        scaleY.setDuration(TYPING_DOT_DURATION_MS);

        alpha.setStartDelay(delay);
        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);

        alpha.setInterpolator(new LinearInterpolator());
        scaleX.setInterpolator(new LinearInterpolator());
        scaleY.setInterpolator(new LinearInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, scaleX, scaleY);
        return set;
    }

    private void stopTypingDotsAnimation() {
        if (typingDotsAnimator != null) {
            typingDotsAnimator.cancel();
            typingDotsAnimator = null;
        }

        resetDot(typingDot1);
        resetDot(typingDot2);
        resetDot(typingDot3);
    }

    private void resetDot(View dot) {
        if (dot == null) return;

        dot.animate().cancel();
        dot.setAlpha(1f);
        dot.setScaleX(1f);
        dot.setScaleY(1f);
    }

    public void showEditPanel(String originalText) {
        if (isChatEnded) return;

        isEditMode = true;
        isReplyMode = false;

        if (replyPanel != null) replyPanel.setVisibility(View.GONE);
        if (editOriginalText != null) editOriginalText.setText(originalText);
        if (editPanel != null) editPanel.setVisibility(View.VISIBLE);
        configureSendButton(true);
    }

    public void hideEditPanel() {
        isEditMode = false;
        if (editPanel != null) editPanel.setVisibility(View.GONE);
        configureSendButton(false);
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public void showReplyPanel(String originalText) {
        if (isChatEnded) return;

        isReplyMode = true;
        isEditMode = false;

        if (editPanel != null) editPanel.setVisibility(View.GONE);
        if (replyOriginalText != null) replyOriginalText.setText(originalText);
        if (replyPanel != null) replyPanel.setVisibility(View.VISIBLE);
        configureSendButton(false);
    }

    public void hideReplyPanel() {
        isReplyMode = false;
        if (replyPanel != null) replyPanel.setVisibility(View.GONE);
    }

    public boolean isReplyMode() {
        return isReplyMode;
    }

    public void applyChatInputMode(@Nullable ChatType chatType) {
        boolean mediaSupported = chatType != ChatType.AI && chatType != ChatType.Random;

        setMediaButtonVisible(btnAttachFile, mediaSupported);
        setMediaButtonVisible(btnRecordVoice, mediaSupported);
        setMediaButtonVisible(btnRecordVideoCircle, mediaSupported);

        if (!mediaSupported && fragment != null) {
            fragment.clearSelectedFiles();
        }
    }

    private void setMediaButtonVisible(@Nullable View view, boolean visible) {
        if (view == null) return;

        view.animate().cancel();
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
        view.setEnabled(visible);
        view.setAlpha(1f);
        view.setScaleX(1f);
        view.setScaleY(1f);
    }

    private void configureSendButton(boolean editMode) {
        if (bSendMessage == null) return;

        bSendMessage.setText(null);
        bSendMessage.setIconResource(editMode ? R.drawable.ic_check : R.drawable.ic_send_telegram);
        bSendMessage.setContentDescription(
                root.getContext().getString(editMode ? R.string.save_button : R.string.send_message)
        );
    }

    public void insertMention(String name) {
        if (isChatEnded || messageInput == null) return;

        String current = messageInput.getText().toString();
        messageInput.setText(current + "@" + name + " ");
        messageInput.setSelection(messageInput.getText().length());
    }

    public void showOptionsMenu() {
        if (btnOptions == null || isChatEnded) return;

        PopupMenu popupMenu = new PopupMenu(createPopupMenuContext(), btnOptions);
        popupMenu.getMenuInflater().inflate(R.menu.chat_options_menu, popupMenu.getMenu());

        Menu menu = popupMenu.getMenu();

        menu.findItem(R.id.menu_end_chat).setVisible(fragment.shouldShowEndChat());
        menu.findItem(R.id.menu_view_members).setVisible(fragment.shouldShowViewMembers());
        menu.findItem(R.id.menu_select_model).setVisible(fragment.shouldShowSelectModel());

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_end_chat) {
                fragment.onEndChatClick();
                return true;
            }

            if (id == R.id.menu_view_members) {
                fragment.onToggleMembersPanel();
                return true;
            }

            if (id == R.id.menu_search_words) {
                fragment.onShowSearchPanel();
                return true;
            }

            if (id == R.id.menu_export_chat) {
                fragment.onExportChatClick();
                return true;
            }

            if (id == R.id.menu_select_model) {
                fragment.onSelectModelClick();
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private Context createPopupMenuContext() {
        Context context = root.getContext();
        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        boolean dark = nightMode == Configuration.UI_MODE_NIGHT_YES;

        return new ContextThemeWrapper(
                context,
                dark
                        ? R.style.ThemeOverlay_AIChat_PopupMenu_DarkFixed
                        : R.style.ThemeOverlay_AIChat_PopupMenu_LightFixed
        );
    }

    public void showUnreadJumpButton(int unreadCount) {
        if (isChatEnded || btnUnreadJump == null) return;

        if (unreadCount <= 0) {
            hideUnreadJumpButton();
            return;
        }

        String text = unreadCount > 99
                ? "99+ непроч."
                : unreadCount + " непроч.";

        btnUnreadJump.setText(text);

        if (btnUnreadJump.getVisibility() != View.VISIBLE) {
            btnUnreadJump.animate().cancel();
            btnUnreadJump.setAlpha(0f);
            btnUnreadJump.setTranslationY(16f);
            btnUnreadJump.setVisibility(View.VISIBLE);

            btnUnreadJump.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180)
                    .start();
        }
    }

    public void hideUnreadJumpButton() {
        if (btnUnreadJump == null || btnUnreadJump.getVisibility() != View.VISIBLE) return;

        btnUnreadJump.animate().cancel();
        btnUnreadJump.animate()
                .alpha(0f)
                .translationY(16f)
                .setDuration(140)
                .withEndAction(() -> {
                    if (btnUnreadJump != null) {
                        btnUnreadJump.setVisibility(View.GONE);
                        btnUnreadJump.setAlpha(1f);
                        btnUnreadJump.setTranslationY(0f);
                    }
                })
                .start();
    }

    public void showConnectedState() {
        if (bSendMessage != null) bSendMessage.setEnabled(!isChatEnded);
        if (btnOptions != null) btnOptions.setEnabled(!isChatEnded);
        if (tvChatTitle != null) tvChatTitle.setAlpha(1f);
        if (typingStatusContainer != null) typingStatusContainer.setAlpha(typingStatusVisible ? 1f : typingStatusContainer.getAlpha());
    }

    public void showDisconnectedState() {
        if (bSendMessage != null) bSendMessage.setEnabled(false);
        if (btnOptions != null) btnOptions.setEnabled(false);
        if (tvChatTitle != null) tvChatTitle.setAlpha(0.5f);
        if (typingStatusContainer != null) typingStatusContainer.setAlpha(0.5f);
    }

    public void showConnectionBanner(boolean connected) {
        if (connectionBanner == null || connectionBannerText == null) return;

        int color = ContextCompat.getColor(
                root.getContext(),
                connected ? R.color.connection_ok : R.color.connection_error
        );

        connectionBanner.setBackgroundColor(color);
        connectionBannerText.setText(root.getContext().getString(
                connected ? R.string.connection_restored : R.string.connection_lost
        ));

        connectionBanner.setVisibility(View.VISIBLE);
        connectionBannerText.setVisibility(View.VISIBLE);

        connectionBanner.setAlpha(1f);
        connectionBannerText.setAlpha(1f);

        connectionBanner.animate()
                .alpha(0f)
                .setDuration(800)
                .setStartDelay(1200)
                .withEndAction(() -> connectionBanner.setVisibility(View.GONE));

        connectionBannerText.animate()
                .alpha(0f)
                .setDuration(800)
                .setStartDelay(1200)
                .withEndAction(() -> connectionBannerText.setVisibility(View.GONE));
    }
}
