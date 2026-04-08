package com.example.aichat.view.main.chat.ui;

import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.R;
import com.example.aichat.controller.main.chat.actions.ChatMessageActions;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.view.main.chat.ChatFragment;
import com.example.aichat.view.main.chat.MessageAdapter;

import java.util.List;
import java.util.UUID;

public class ChatMessagesUi {

    private final ChatFragment fragment;
    private final View root;

    private RecyclerView rvMessages;
    private MessageAdapter adapter;

    public ChatMessagesUi(View root, ChatFragment fragment) {
        this.root = root;
        this.fragment = fragment;
        init();
    }

    private void init() {
        rvMessages = root.findViewById(R.id.rv_messages);
        rvMessages.setLayoutManager(new LinearLayoutManager(root.getContext()));

        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (adapter != null && fragment.isAdded()) {
                    fragment.onVisibleMessagesChanged();
                }
            }
        });
    }

    public void initAdapter(List<Message> initialMessages) {
        if (!fragment.isAdded()) return;

        adapter = new MessageAdapter(
                fragment.getCurrentUserId(),
                initialMessages,
                root.getContext()
        );

        adapter.setOnMessageActionListener(new MessageAdapter.OnMessageActionListener() {
            @Override
            public void onEditMessage(Message message) {
                fragment.onEditMessage(message);
            }

            @Override
            public void onDeleteMessage(Message message) {
                fragment.onDeleteMessage(message);
            }

            @Override
            public void onReplyToMessage(Message message) {
                fragment.onReplyToMessage(message);
            }

            @Override
            public void onCopyMessageText(Message message) {
                fragment.onCopyMessage(message);
            }
        });

        rvMessages.setAdapter(adapter);
        scrollToBottom();
    }

    public void addMessage(Message m) {
        if (!fragment.isAdded() || adapter == null) return;

        boolean wasLast = isLastVisible();
        adapter.addOrUpdateMessage(m);

        if (wasLast) {
            smoothScrollToBottom();
        }
    }

    public void removeMessage(UUID id) {
        if (!fragment.isAdded() || adapter == null) return;
        adapter.removeMessage(id);
    }

    public void updateStatus(UUID messageId, UUID userId, MessageStatus status) {
        if (!fragment.isAdded() || adapter == null) return;
        adapter.updateStatus(messageId, userId, status);
    }
    public void highlightMessage(Message message) {
        if (!fragment.isAdded() || adapter == null) return;

        adapter.highlightMessage(message.getId());
        smoothScrollToMessage(message.getId());
    }

    private void smoothScrollToMessage(UUID messageId) {
        int pos = adapter.getMessagePosition(messageId);
        if (pos >= 0) rvMessages.smoothScrollToPosition(pos);
    }

    public void scrollToBottom() {
        if (adapter != null && adapter.getItemCount() > 0) {
            rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void smoothScrollToBottom() {
        if (adapter != null && adapter.getItemCount() > 0) {
            rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    public void scrollToUnread(List<Message> messages, UUID currentUserId) {
        if (!fragment.isAdded() || messages == null || messages.isEmpty()) {
            scrollToBottom();
            return;
        }

        int firstUnread = -1;

        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);

            boolean isNotMine = !m.getSender().equals(currentUserId);
            boolean hasStatus = m.getStatuses() != null && m.getStatuses().containsKey(currentUserId);
            boolean unread = hasStatus && m.getStatuses().get(currentUserId) != MessageStatus.READ;

            if (isNotMine && unread) {
                firstUnread = i;
                break;
            }
        }

        if (firstUnread != -1) {
            rvMessages.scrollToPosition(firstUnread);
        } else {
            scrollToBottom();
        }
    }
    public void checkVisibleMessages(ChatMessageActions actions) {
        if (adapter != null && fragment.isAdded()) {
            adapter.checkVisibleMessages(actions);
        }
    }
    private boolean isLastVisible() {
        LinearLayoutManager lm = (LinearLayoutManager) rvMessages.getLayoutManager();
        int lastVisible = lm.findLastVisibleItemPosition();
        return lastVisible == adapter.getItemCount() - 1;
    }
}
