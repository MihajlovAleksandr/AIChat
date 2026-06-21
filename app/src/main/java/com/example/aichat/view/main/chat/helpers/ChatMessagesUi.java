package com.example.aichat.view.main.chat.helpers;

import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.controller.main.chat.actions.ChatMessageActions;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.model.utils.files.FileDownloadProgressManager;
import com.example.aichat.R;
import com.example.aichat.view.main.chat.ChatFragment;
import com.example.aichat.view.main.chat.MessageAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@androidx.media3.common.util.UnstableApi
public class ChatMessagesUi {

    private static final int UNREAD_FOCUS_TOP_OFFSET_DP = 12;
    private static final int MAX_SCROLL_FINISH_DELAY_MS = 1200;
    private static final int MIN_SCROLL_FINISH_DELAY_MS = 450;
    private static final int UNREAD_BATCH_SETTLE_DELAY_MS = 180;
    private static final int MAX_UNREAD_BATCH_AUTO_STEPS = 80;

    private final View root;
    private final ChatFragment fragment;
    private final RecyclerView recyclerView;
    private final LinearLayoutManager layoutManager;

    private MessageAdapter adapter;
    private FileDownloadProgressManager progressManager;

    private boolean programmaticUnreadScrollInProgress = false;
    private boolean firstUnreadJumpCompleted = false;
    private boolean silentPositionRestoreInProgress = false;
    private boolean unreadAutoReadInProgress = false;

    private RecyclerView.OnScrollListener activeFinishListener;

    private static class ScrollAnchor {
        final int position;
        final int top;
        final boolean valid;

        ScrollAnchor(int position, int top, boolean valid) {
            this.position = position;
            this.top = top;
            this.valid = valid;
        }
    }

    public ChatMessagesUi(View root, ChatFragment fragment) {
        this.root = root;
        this.fragment = fragment;
        this.recyclerView = root.findViewById(R.id.rv_messages);
        this.layoutManager = new LinearLayoutManager(root.getContext());
        this.layoutManager.setStackFromEnd(true);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    firstUnreadJumpCompleted = false;
                    unreadAutoReadInProgress = false;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (programmaticUnreadScrollInProgress || silentPositionRestoreInProgress) return;

                fragment.onVisibleMessagesChanged();
            }
        });
    }

    public void setProgressManager(FileDownloadProgressManager progressManager) {
        this.progressManager = progressManager;

        if (adapter != null) adapter.setProgressManager(progressManager);
    }

    public void initAdapter(List<Message> messages) {
        List<Message> safeMessages = messages != null ? messages : new ArrayList<>();

        adapter = new MessageAdapter(
                fragment.getCurrentUserId(),
                safeMessages,
                root.getContext(),
                progressManager
        );

        adapter.setOnFileDownloadListener((url, mimeType, fileId, fileType, openAfterDownload) ->
                fragment.downloadFile(url, mimeType, fileId, fileType, openAfterDownload)
        );

        adapter.setOnMessageActionListener(new MessageAdapter.OnMessageActionListener() {
            @Override
            public void onEditMessage(Message message) {
                fragment.onMessageEditSelected(message);
            }

            @Override
            public void onDeleteMessage(Message message) {
                fragment.onMessageDeleteSelected(message);
            }

            @Override
            public void onReplyToMessage(Message message) {
                fragment.onMessageReplySelected(message);
            }

            @Override
            public void onCopyMessageText(Message message) {
                fragment.onMessageCopyTextSelected(message);
            }
        });

        recyclerView.setAdapter(adapter);
    }

    public MessageAdapter getAdapter() {
        return adapter;
    }

    public void clearFileCache() {
        if (adapter != null) adapter.clearFileCache();
    }

    public void applyMessagesWithoutReload(List<Message> newMessages) {
        if (newMessages == null) return;

        if (adapter == null) {
            initAdapter(newMessages);
            scrollToBottomImmediately();
            return;
        }

        ScrollAnchor anchor = captureScrollAnchor();
        recyclerView.stopScroll();

        boolean hasChanges = false;

        for (Message newMessage : newMessages) {
            if (newMessage == null) continue;

            Message oldMessage = adapter.findMessageById(newMessage.getId());

            if (oldMessage == null) {
                adapter.addOrUpdateMessage(newMessage);
                hasChanges = true;
                continue;
            }

            if (!messagesLookSame(oldMessage, newMessage)) {
                adapter.addOrUpdateMessage(newMessage);
                hasChanges = true;
            }
        }

        if (hasChanges) restoreScrollAnchorAfterAdapterMutation(anchor);
    }

    private boolean messagesLookSame(Message oldMessage, Message newMessage) {
        if (oldMessage == null || newMessage == null) return false;

        return Objects.equals(oldMessage.getId(), newMessage.getId())
                && Objects.equals(oldMessage.getText(), newMessage.getText())
                && Objects.equals(oldMessage.getStatuses(), newMessage.getStatuses())
                && Objects.equals(oldMessage.getFiles(), newMessage.getFiles())
                && Objects.equals(oldMessage.getFileTypes(), newMessage.getFileTypes())
                && Objects.equals(oldMessage.getFileMimeTypes(), newMessage.getFileMimeTypes())
                && Objects.equals(oldMessage.getReplyMessages(), newMessage.getReplyMessages());
    }

    private void restoreScrollAnchorAfterAdapterMutation(ScrollAnchor anchor) {
        if (anchor == null || !anchor.valid) return;
        if (adapter == null || adapter.getItemCount() <= 0) return;

        int safePosition = Math.min(anchor.position, adapter.getItemCount() - 1);

        layoutManager.scrollToPositionWithOffset(safePosition, anchor.top);

        recyclerView.post(() -> {
            if (adapter == null || adapter.getItemCount() <= 0) return;

            int secondSafePosition = Math.min(anchor.position, adapter.getItemCount() - 1);

            layoutManager.scrollToPositionWithOffset(secondSafePosition, anchor.top);
        });
    }

    private ScrollAnchor captureScrollAnchor() {
        int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();

        if (firstVisiblePosition == RecyclerView.NO_POSITION) {
            return new ScrollAnchor(RecyclerView.NO_POSITION, 0, false);
        }

        View firstVisibleView = layoutManager.findViewByPosition(firstVisiblePosition);
        int top = firstVisibleView != null ? firstVisibleView.getTop() : 0;

        return new ScrollAnchor(firstVisiblePosition, top, true);
    }

    public void addMessage(Message message) {
        if (adapter == null || message == null) return;

        boolean isMyMessage = adapter.isMyMessage(message);
        int anchorPosition = RecyclerView.NO_POSITION;
        int anchorTop = 0;

        if (!isMyMessage) {
            anchorPosition = layoutManager.findFirstVisibleItemPosition();

            View anchorView = anchorPosition != RecyclerView.NO_POSITION
                    ? layoutManager.findViewByPosition(anchorPosition)
                    : null;

            if (anchorView != null) anchorTop = anchorView.getTop();
        }

        adapter.addOrUpdateMessage(message);

        if (isMyMessage) {
            resetUnreadJumpState();
            scrollToBottom();
            return;
        }

        restoreScrollPositionAfterIncomingMessage(anchorPosition, anchorTop);
    }

    public void addMessageWithoutAutoScroll(Message message) {
        if (adapter == null || message == null) return;

        int anchorPosition = layoutManager.findFirstVisibleItemPosition();
        int anchorTop = 0;

        View anchorView = anchorPosition != RecyclerView.NO_POSITION
                ? layoutManager.findViewByPosition(anchorPosition)
                : null;

        if (anchorView != null) anchorTop = anchorView.getTop();

        adapter.addOrUpdateMessage(message);
        restoreScrollPositionAfterIncomingMessage(anchorPosition, anchorTop);
    }

    private void restoreScrollPositionAfterIncomingMessage(int anchorPosition, int anchorTop) {
        if (adapter == null) return;
        if (anchorPosition == RecyclerView.NO_POSITION) return;

        recyclerView.post(() -> {
            if (adapter == null) return;
            if (anchorPosition >= adapter.getItemCount()) return;

            layoutManager.scrollToPositionWithOffset(anchorPosition, anchorTop);
        });
    }

    public void removeMessage(UUID id) {
        if (adapter == null || id == null) return;

        adapter.removeMessage(id);
    }

    public void updateStatus(UUID messageId, UUID userId, MessageStatus status) {
        if (adapter == null || messageId == null || userId == null || status == null) return;

        adapter.updateStatus(messageId, userId, status);
    }

    public void highlightMessage(Message message) {
        if (adapter == null || message == null) return;

        adapter.highlightMessage(message.getId());
        adapter.scrollToMessage(message.getId());
    }

    public void checkVisibleMessages(ChatMessageActions actions) {
        if (adapter != null) adapter.checkVisibleMessages(actions);
    }

    public void scrollToBottomIfNoUnread(UUID currentUserId) {
        if (adapter == null) return;
        if (adapter.getUnreadMessagesCount(currentUserId) > 0) return;

        scrollToBottomImmediately();
    }

    public void scrollToBottomImmediately() {
        if (adapter == null || adapter.getItemCount() == 0) return;

        int lastPosition = adapter.getItemCount() - 1;

        recyclerView.stopScroll();
        layoutManager.scrollToPositionWithOffset(lastPosition, dp(8));
        recyclerView.scrollToPosition(lastPosition);
    }

    public void scrollByUnreadButton(UUID currentUserId, ChatMessageActions actions, Runnable onFinished) {
        if (adapter == null || currentUserId == null || actions == null) {
            if (onFinished != null) onFinished.run();
            return;
        }

        if (programmaticUnreadScrollInProgress || unreadAutoReadInProgress) return;

        unreadAutoReadInProgress = true;
        scrollToNextUnreadBatch(currentUserId, actions, onFinished, 0);
    }

    private void scrollToNextUnreadBatch(UUID currentUserId, ChatMessageActions actions, Runnable onFinished, int step) {
        if (!unreadAutoReadInProgress) {
            finishUnreadAutoRead(onFinished);
            return;
        }

        if (adapter == null || currentUserId == null || actions == null) {
            finishUnreadAutoRead(onFinished);
            return;
        }

        if (step >= MAX_UNREAD_BATCH_AUTO_STEPS) {
            finishUnreadAutoRead(onFinished);
            return;
        }

        int targetPosition = adapter.getFirstUnreadMessagePosition(currentUserId);

        if (targetPosition < 0) {
            resetUnreadJumpState();
            scrollToBottom();
            finishUnreadAutoRead(onFinished);
            return;
        }

        smoothScrollToPositionFocused(targetPosition, () -> {
            if (adapter == null) {
                finishUnreadAutoRead(onFinished);
                return;
            }

            recyclerView.postDelayed(() -> {
                if (!unreadAutoReadInProgress || adapter == null) {
                    finishUnreadAutoRead(onFinished);
                    return;
                }

                int readCount = adapter.readVisibleMessagesImmediately(actions, true);

                if (readCount <= 0) readCount = adapter.readUnreadMessageAt(targetPosition, actions);

                final int finalReadCount = readCount;

                recyclerView.postDelayed(() -> {
                    if (!unreadAutoReadInProgress || adapter == null) {
                        finishUnreadAutoRead(onFinished);
                        return;
                    }

                    int unreadCount = adapter.getUnreadMessagesCount(currentUserId);

                    if (unreadCount <= 0) {
                        resetUnreadJumpState();
                        scrollToBottom();
                        finishUnreadAutoRead(onFinished);
                        return;
                    }

                    int nextUnreadPosition = adapter.getFirstUnreadMessagePosition(currentUserId);

                    if (nextUnreadPosition < 0) {
                        resetUnreadJumpState();
                        scrollToBottom();
                        finishUnreadAutoRead(onFinished);
                        return;
                    }

                    if (nextUnreadPosition == targetPosition && finalReadCount <= 0) {
                        finishUnreadAutoRead(onFinished);
                        return;
                    }

                    scrollToNextUnreadBatch(currentUserId, actions, onFinished, step + 1);
                }, UNREAD_BATCH_SETTLE_DELAY_MS);
            }, UNREAD_BATCH_SETTLE_DELAY_MS);
        });
    }

    private void finishUnreadAutoRead(Runnable onFinished) {
        boolean wasRunning = unreadAutoReadInProgress;

        unreadAutoReadInProgress = false;
        programmaticUnreadScrollInProgress = false;

        if (wasRunning && onFinished != null) onFinished.run();
    }

    private void smoothScrollToPositionFocused(int targetPosition, Runnable onFinished) {
        if (adapter == null) return;
        if (targetPosition < 0 || targetPosition >= adapter.getItemCount()) return;

        recyclerView.stopScroll();
        programmaticUnreadScrollInProgress = true;

        if (activeFinishListener != null) {
            recyclerView.removeOnScrollListener(activeFinishListener);
            activeFinishListener = null;
        }

        int currentPosition = layoutManager.findFirstVisibleItemPosition();
        int distance = currentPosition == RecyclerView.NO_POSITION ? 0 : Math.abs(currentPosition - targetPosition);
        int finishDelay = Math.min(MAX_SCROLL_FINISH_DELAY_MS, Math.max(MIN_SCROLL_FINISH_DELAY_MS, distance * 28));
        final boolean[] finished = new boolean[]{false};
        Runnable finishAction = createFinishAction(targetPosition, onFinished, finished);

        activeFinishListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) recyclerView.post(finishAction);
            }
        };

        recyclerView.addOnScrollListener(activeFinishListener);

        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(root.getContext()) {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return layoutManager.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_START;
            }

            @Override
            public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                return boxStart + dp(UNREAD_FOCUS_TOP_OFFSET_DP) - viewStart;
            }

            @Override
            protected float calculateSpeedPerPixel(@NonNull DisplayMetrics displayMetrics) {
                return 18f / displayMetrics.densityDpi;
            }
        };

        smoothScroller.setTargetPosition(targetPosition);
        layoutManager.startSmoothScroll(smoothScroller);
        recyclerView.postDelayed(finishAction, finishDelay);
    }

    private Runnable createFinishAction(int targetPosition, Runnable onFinished, boolean[] finished) {
        return () -> {
            if (finished[0]) return;

            finished[0] = true;

            if (activeFinishListener != null) {
                recyclerView.removeOnScrollListener(activeFinishListener);
                activeFinishListener = null;
            }

            layoutManager.scrollToPositionWithOffset(targetPosition, dp(UNREAD_FOCUS_TOP_OFFSET_DP));
            programmaticUnreadScrollInProgress = false;

            if (onFinished != null) onFinished.run();
        };
    }

    public int getUnreadMessagesCount(UUID currentUserId) {
        if (adapter == null) return 0;

        return adapter.getUnreadMessagesCount(currentUserId);
    }

    public boolean shouldShowUnreadJump(UUID currentUserId) {
        if (unreadAutoReadInProgress) return false;
        if (adapter == null || currentUserId == null) return false;

        int unreadCount = adapter.getUnreadMessagesCount(currentUserId);

        if (unreadCount <= 0) {
            resetUnreadJumpState();
            return false;
        }

        int firstUnreadPosition = adapter.getFirstUnreadMessagePosition(currentUserId);
        int lastUnreadPosition = adapter.getLastUnreadMessagePosition(currentUserId);

        if (firstUnreadPosition < 0 || lastUnreadPosition < 0) {
            resetUnreadJumpState();
            return false;
        }

        int lastVisible = layoutManager.findLastVisibleItemPosition();

        if (lastVisible == RecyclerView.NO_POSITION) return true;

        return firstUnreadPosition > lastVisible || lastUnreadPosition > lastVisible;
    }

    public void resetUnreadJumpState() {
        firstUnreadJumpCompleted = false;
    }

    public void scrollToBottom() {
        if (adapter == null || adapter.getItemCount() == 0) return;

        recyclerView.post(() -> recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1));
    }

    private int dp(int value) {
        return Math.round(value * root.getResources().getDisplayMetrics().density);
    }
}
