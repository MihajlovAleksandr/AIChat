package com.example.aichat.view.main.chat.ui;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.aichat.R;
import com.example.aichat.view.main.chat.ChatFragment;

public class ChatUi {

    private final ChatFragment fragment;
    private final View root;

    private ImageButton btnBack;
    private ImageButton btnOptions;
    private Button bSendMessage;
    private Button bExportChat;
    private TextView tvChatEnded;
    private TextView tvChatTitle;

    private View membersPanel;
    private View invisibleClickArea;

    private View searchPanel;
    private View resultPanel;

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

    private static final int MENU_PIN_CHAT = 10001;
    private static final int MENU_UNPIN_CHAT = 10002;

    public ChatUi(View root, ChatFragment fragment) {
        this.root = root;
        this.fragment = fragment;
        init();
    }

    private void init() {
        btnBack = root.findViewById(R.id.btn_back);
        btnOptions = root.findViewById(R.id.btn_options);
        bSendMessage = root.findViewById(R.id.b_send_message);
        bExportChat = root.findViewById(R.id.b_export_chat);
        tvChatEnded = root.findViewById(R.id.tv_chat_ended);
        tvChatTitle = root.findViewById(R.id.tv_chat_title);

        membersPanel = root.findViewById(R.id.membersPanel);
        invisibleClickArea = root.findViewById(R.id.invisibleClickArea);

        searchPanel = root.findViewById(R.id.search_layout);
        resultPanel = root.findViewById(R.id.searchResult_layout);

        messageInput = root.findViewById(R.id.ti_message);

        editPanel = root.findViewById(R.id.edit_panel);
        editOriginalText = root.findViewById(R.id.edit_original_text);
        editCancel = root.findViewById(R.id.edit_cancel);

        replyPanel = root.findViewById(R.id.reply_panel);
        replyOriginalText = root.findViewById(R.id.reply_original_text);
        replyCancel = root.findViewById(R.id.reply_cancel);

        connectionBanner = root.findViewById(R.id.connectionBanner);
        connectionBannerText = root.findViewById(R.id.connectionBannerText);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> fragment.navigateBack());
        }
        if (btnOptions != null) {
            btnOptions.setOnClickListener(v -> fragment.onOptionsClick());
        }
        if (bSendMessage != null) {
            bSendMessage.setOnClickListener(v -> fragment.onSendMessageClick());
        }
        if (bExportChat != null) {
            bExportChat.setOnClickListener(v -> fragment.onExportChatClick());
        }

        if (invisibleClickArea != null) {
            invisibleClickArea.setOnClickListener(v -> fragment.onToggleMembersPanel());
        }

        if (editCancel != null) {
            editCancel.setOnClickListener(v -> fragment.onCancelEdit());
        }

        if (replyCancel != null) {
            replyCancel.setOnClickListener(v -> fragment.onCancelReply());
        }
    }

    public void enableSendButton() {
        if (bSendMessage != null) bSendMessage.setEnabled(true);
    }

    public void disableSendButton() {
        if (bSendMessage != null) bSendMessage.setEnabled(false);
    }

    public void showChatEnded() {
        if (tvChatEnded != null) tvChatEnded.setVisibility(View.VISIBLE);
        if (bExportChat != null) bExportChat.setVisibility(View.VISIBLE);
        if (bSendMessage != null) bSendMessage.setVisibility(View.GONE);
        if (messageInput != null) messageInput.setVisibility(View.GONE);
        if (btnOptions != null) btnOptions.setEnabled(false);
    }

    public void toggleMembersPanel() {
        if (membersPanel == null || invisibleClickArea == null) return;

        if (membersPanel.getVisibility() == View.VISIBLE) {
            membersPanel.setVisibility(View.GONE);
            invisibleClickArea.setVisibility(View.GONE);
        } else {
            membersPanel.setVisibility(View.VISIBLE);
            invisibleClickArea.setVisibility(View.VISIBLE);
        }
    }

    public void showSearchPanel() {
        if (searchPanel != null) searchPanel.setVisibility(View.VISIBLE);
        if (resultPanel != null) resultPanel.setVisibility(View.GONE);
        if (btnOptions != null) btnOptions.setEnabled(false);
        if (btnBack != null) btnBack.setEnabled(false);
    }

    public void hideSearchPanel() {
        if (searchPanel != null) searchPanel.setVisibility(View.GONE);
        if (btnOptions != null) btnOptions.setEnabled(true);
        if (btnBack != null) btnBack.setEnabled(true);
    }

    public void showSearchResults() {
        if (searchPanel != null) searchPanel.setVisibility(View.GONE);
        if (resultPanel != null) resultPanel.setVisibility(View.VISIBLE);
    }

    public void hideSearchResults() {
        if (resultPanel != null) resultPanel.setVisibility(View.GONE);
        if (searchPanel != null) searchPanel.setVisibility(View.VISIBLE);
    }

    public String getMessageText() {
        return messageInput != null
                ? messageInput.getText().toString().trim()
                : "";
    }

    public void clearMessageInput() {
        if (messageInput != null) {
            messageInput.setText("");
        }
    }

    public void setChatTitle(String name) {
        if (tvChatTitle != null) {
            tvChatTitle.setText(name);
        }
    }

    public void showEditPanel(String originalText) {
        isEditMode = true;
        isReplyMode = false;

        if (replyPanel != null) replyPanel.setVisibility(View.GONE);

        if (editOriginalText != null) editOriginalText.setText(originalText);
        if (editPanel != null) editPanel.setVisibility(View.VISIBLE);

        if (bSendMessage != null) bSendMessage.setText(R.string.save_button);
    }

    public void hideEditPanel() {
        isEditMode = false;
        if (editPanel != null) editPanel.setVisibility(View.GONE);
        if (bSendMessage != null) bSendMessage.setText(R.string.send_button);
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public void showReplyPanel(String originalText) {
        isReplyMode = true;
        isEditMode = false;

        if (editPanel != null) editPanel.setVisibility(View.GONE);

        if (replyOriginalText != null) replyOriginalText.setText(originalText);
        if (replyPanel != null) replyPanel.setVisibility(View.VISIBLE);

        if (bSendMessage != null) bSendMessage.setText(R.string.send_button);
    }

    public void hideReplyPanel() {
        isReplyMode = false;
        if (replyPanel != null) replyPanel.setVisibility(View.GONE);
    }

    public boolean isReplyMode() {
        return isReplyMode;
    }

    public void insertMention(String name) {
        if (messageInput == null) return;

        String current = messageInput.getText().toString();
        messageInput.setText(current + "@" + name + " ");
        messageInput.setSelection(messageInput.getText().length());
    }
    public void showOptionsMenu() {
        if (btnOptions == null) return;

        PopupMenu popupMenu = new PopupMenu(root.getContext(), btnOptions);
        popupMenu.getMenuInflater().inflate(R.menu.chat_options_menu, popupMenu.getMenu());
        boolean pinned = fragment.getChat().isPinned();

        if (!pinned) {
            popupMenu.getMenu().add(0, MENU_PIN_CHAT, 100, "Закрепить чат");
        } else {
            popupMenu.getMenu().add(0, MENU_UNPIN_CHAT, 100, "Открепить чат");
        }

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
            if (id == MENU_PIN_CHAT) {
                fragment.onPinChatClick();
                return true;
            }
            if (id == MENU_UNPIN_CHAT) {
                fragment.onUnpinChatClick();
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    public void showConnectedState() {
        if (bSendMessage != null) bSendMessage.setEnabled(true);
        if (btnOptions != null) btnOptions.setEnabled(true);
        if (tvChatTitle != null) tvChatTitle.setAlpha(1f);
    }

    public void showDisconnectedState() {
        if (bSendMessage != null) bSendMessage.setEnabled(false);
        if (btnOptions != null) btnOptions.setEnabled(false);
        if (tvChatTitle != null) tvChatTitle.setAlpha(0.5f);
    }

    public void showConnectionBanner(boolean connected) {
        if (connectionBanner == null || connectionBannerText == null) return;

        int color = ContextCompat.getColor(
                root.getContext(),
                connected ? R.color.connection_ok : R.color.connection_error
        );
        connectionBanner.setBackgroundColor(color);

        connectionBannerText.setText(
                root.getContext().getString(
                        connected
                                ? R.string.connection_restored
                                : R.string.connection_lost
                )
        );

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
