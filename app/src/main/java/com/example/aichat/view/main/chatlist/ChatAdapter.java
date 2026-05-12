package com.example.aichat.view.main.chatlist;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.R;
import com.example.aichat.controller.main.chat.MessageController;
import com.example.aichat.controller.main.chatlist.ChatController;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageChat;
import com.example.aichat.view.main.chat.ui.UiAnimations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import io.noties.markwon.Markwon;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ChatAdapter";
    private static final int TYPE_CHAT = 0;
    private static final int TYPE_PLACEHOLDER = 1;

    private final List<MessageChat> allChats = new ArrayList<>();
    private final List<MessageChat> visibleChats = new ArrayList<>();

    private final MessageController messageController;
    private final OnChatClickListener listener;

    private final Comparator<MessageChat> comparator = new MessageChatComparator();
    private RecyclerView recyclerView;

    private ChatOpenedChecker openedChecker;
    private OnEmptyStateListener emptyStateListener;

    public interface ChatOpenedChecker {
        boolean isChatOpened(UUID chatId);
    }

    public interface OnEmptyStateListener {
        void onEmptyState(boolean isEmpty);
    }

    public void setChatOpenedChecker(ChatOpenedChecker checker) {
        this.openedChecker = checker;
    }

    public void setOnEmptyStateListener(OnEmptyStateListener listener) {
        this.emptyStateListener = listener;
    }

    public ChatAdapter(OnChatClickListener listener, MessageController messageController) {
        this.listener = listener;
        this.messageController = messageController;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView rv) {
        super.onAttachedToRecyclerView(rv);
        this.recyclerView = rv;
    }

    public void setChats(List<MessageChat> chats) {
        allChats.clear();
        allChats.addAll(chats);

        List<MessageChat> sorted = new ArrayList<>(chats);
        Collections.sort(sorted, comparator);

        applyDiff(sorted);
        notifyEmptyState();
    }

    private void applyDiff(List<MessageChat> newList) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(
                new MessageChatDiffCallback(visibleChats, newList)
        );
        visibleChats.clear();
        visibleChats.addAll(newList);
        diff.dispatchUpdatesTo(this);
    }

    private void notifyEmptyState() {
        if (emptyStateListener != null) {
            emptyStateListener.onEmptyState(visibleChats.isEmpty());
        }
    }

    public void setVisibleChats(List<MessageChat> chats) {
        List<MessageChat> sorted = new ArrayList<>(chats);
        Collections.sort(sorted, comparator);

        applyDiff(sorted);
        notifyEmptyState();
    }

    public void setAllChatsVisible() {
        List<MessageChat> sorted = new ArrayList<>(allChats);
        Collections.sort(sorted, comparator);

        applyDiff(sorted);
        notifyEmptyState();
    }

    public void addChat(MessageChat chat) {
        allChats.add(chat);

        List<MessageChat> sorted = new ArrayList<>(allChats);
        Collections.sort(sorted, comparator);

        applyDiff(sorted);
        notifyEmptyState();

        recyclerView.post(() -> {
            int pos = visibleChats.indexOf(chat);
            if (pos != -1) {
                RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(pos);
                if (vh != null) {
                    UiAnimations.animateNewChat(vh.itemView);
                }
            }
        });
    }

    public void removeChat(UUID chatId) {
        Log.d(TAG, "removeChat called for chatId: " + chatId);
        int pos = -1;
        for (int i = 0; i < visibleChats.size(); i++) {
            if (visibleChats.get(i).getChat().getId().equals(chatId)) {
                pos = i;
                break;
            }
        }

        if (pos != -1 && recyclerView != null) {
            RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(pos);
            if (vh != null) {
                UiAnimations.animateRemoveChat(vh.itemView, () -> {
                    allChats.removeIf(mc -> mc.getChat().getId().equals(chatId));
                    List<MessageChat> sorted = new ArrayList<>(allChats);
                    Collections.sort(sorted, comparator);
                    applyDiff(sorted);
                    notifyEmptyState();
                    Log.d(TAG, "Chat removed with animation");
                });
                return;
            }
        }
        allChats.removeIf(mc -> mc.getChat().getId().equals(chatId));
        List<MessageChat> sorted = new ArrayList<>(allChats);
        Collections.sort(sorted, comparator);
        applyDiff(sorted);
        notifyEmptyState();
        Log.d(TAG, "Chat removed without animation");
    }

    public void renameChat(UUID chatId, String newName) {
        Log.d(TAG, "renameChat called for chatId: " + chatId + ", newName: " + newName);
        for (MessageChat mc : allChats) {
            if (mc.getChat().getId().equals(chatId)) {
                mc.getChat().setName(newName);
            }
        }
        for (int i = 0; i < visibleChats.size(); i++) {
            if (visibleChats.get(i).getChat().getId().equals(chatId)) {
                visibleChats.get(i).getChat().setName(newName);
                notifyItemChanged(i);
                Log.d(TAG, "Chat renamed at position: " + i);
                break;
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        View v = holder.itemView;

        v.animate().cancel();

        v.setAlpha(1f);
        v.setTranslationX(0f);
        v.setTranslationY(0f);
        v.setScaleX(1f);
        v.setScaleY(1f);
        v.setRotation(0f);
    }

    public void updateLastMessage(Message message) {
        UUID chatId = message.getChat();

        int oldPos = -1;

        for (int i = 0; i < visibleChats.size(); i++) {
            MessageChat mc = visibleChats.get(i);

            if (mc.getChat().getId().equals(chatId)) {

                mc.setMessage(message);

                if (openedChecker != null
                        && !openedChecker.isChatOpened(chatId)
                        && !messageController.isMyMessage(message)) {
                    mc.addUnreadMessage(message);
                }

                oldPos = i;
                break;
            }
        }

        if (oldPos == -1) return;

        allChats.sort(comparator);
        visibleChats.sort(comparator);

        int newPos = -1;

        for (int i = 0; i < visibleChats.size(); i++) {
            if (visibleChats.get(i).getChat().getId().equals(chatId)) {
                newPos = i;
                break;
            }
        }

        if (newPos == -1) return;

        if (oldPos != newPos) {
            notifyItemMoved(oldPos, newPos);
        }

        notifyItemChanged(newPos);
    }

    public void updateMessageStatus(MessageChat msg) {
        for (int i = 0; i < allChats.size(); i++) {
            if (allChats.get(i).getChat().getId().equals(msg.getChat().getId())) {
                allChats.set(i, msg);
                break;
            }
        }
        for (int i = 0; i < visibleChats.size(); i++) {
            if (visibleChats.get(i).getChat().getId().equals(msg.getChat().getId())) {
                visibleChats.set(i, msg);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void endChat(UUID chatId) {
        Log.d(TAG, "endChat called for chatId: " + chatId);

        for (MessageChat mc : allChats) {
            if (mc.getChat().getId().equals(chatId)) {
                mc.setEnded(true);
                Log.d(TAG, "Chat marked as ended in allChats: " + chatId);
                break;
            }
        }

        for (int i = 0; i < visibleChats.size(); i++) {
            if (visibleChats.get(i).getChat().getId().equals(chatId)) {
                visibleChats.get(i).setEnded(true);
                notifyItemChanged(i);
                Log.d(TAG, "Chat marked as ended at position: " + i);
                break;
            }
        }

        notifyEmptyState();
    }

    public boolean isChatEnded(UUID chatId) {
        MessageChat mc = getMessageChat(chatId);
        return mc != null && mc.isEnded();
    }

    @Nullable
    public MessageChat getMessageChat(UUID chatId) {
        for (MessageChat mc : allChats) {
            if (mc.getChat().getId().equals(chatId)) return mc;
        }
        return null;
    }

    public int getChatsCount() {
        return visibleChats.size();
    }

    public void setPlaceholders(int count) {
        visibleChats.clear();
        for (int i = 0; i < count; i++) {
            visibleChats.add(null);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return visibleChats.get(position) == null ? TYPE_PLACEHOLDER : TYPE_CHAT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_PLACEHOLDER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_placeholder, parent, false);
            return new PlaceholderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat, parent, false);
            Markwon markwon = Markwon.builder(parent.getContext()).build();
            return new ChatViewHolder(view, messageController, markwon);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageChat mc = visibleChats.get(position);
        if (mc != null) {
            ChatViewHolder chatHolder = (ChatViewHolder) holder;
            chatHolder.bind(mc);
            chatHolder.setClickHandlers(mc, listener);
        }
    }

    @Override
    public int getItemCount() {
        return visibleChats.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvChatName, tvLastMessage, tvTime, tvUnreadCount, tvChatTypeLetter;
        private final ImageView ivChatStatus;
        private final MessageController messageController;
        private final Markwon markwon;

        public ChatViewHolder(@NonNull View itemView, MessageController messageController, Markwon markwon) {
            super(itemView);
            tvChatName = itemView.findViewById(R.id.tv_chat_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
            ivChatStatus = itemView.findViewById(R.id.iv_chat_status);
            tvChatTypeLetter = itemView.findViewById(R.id.tv_chat_type_letter);

            this.messageController = messageController;
            this.markwon = markwon;
        }

        public void bind(MessageChat messageChat) {
            tvChatName.setText(messageChat.getChat().getName());
            tvTime.setText(ChatController.getFormattedTime(messageChat.getTime()));

            boolean isActive = messageChat.getChat().isActive();
            boolean isEnded = messageChat.getChat().getEndTime() != null;

            if (!isActive || isEnded) {
                ivChatStatus.setAlpha(0.5f);
            } else {
                ivChatStatus.setAlpha(1f);
            }

            String chatTypeHint = messageChat.getChat().getChatTypeHint();

            if (chatTypeHint != null && !chatTypeHint.isEmpty()) {
                tvChatTypeLetter.setVisibility(View.VISIBLE);
                tvChatTypeLetter.setText(chatTypeHint);

                int color;
                String tooltip;

                if (chatTypeHint.equals(itemView.getContext().getString(R.string.chat_type_letter_group))) {
                    color = 0xFF4A90E2;
                    tooltip = itemView.getContext().getString(R.string.chat_type_group);
                } else if (chatTypeHint.equals(itemView.getContext().getString(R.string.chat_type_letter_ai))) {
                    color = 0xFF9B59B6;
                    tooltip = itemView.getContext().getString(R.string.chat_type_ai);
                } else if (chatTypeHint.equals(itemView.getContext().getString(R.string.chat_type_letter_human))) {
                    color = 0xFF2ECC71;
                    tooltip = itemView.getContext().getString(R.string.chat_type_human);
                } else if (chatTypeHint.equals(itemView.getContext().getString(R.string.chat_type_letter_random))) {
                    color = 0xFF7F8C8D;
                    tooltip = itemView.getContext().getString(R.string.chat_type_random);
                } else {
                    color = 0xFFAAAAAA;
                    tooltip = "";
                }

                tvChatTypeLetter.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(color)
                );

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tvChatTypeLetter.setTooltipText(tooltip);
                } else {
                    tvChatTypeLetter.setOnLongClickListener(v -> {
                        android.widget.Toast.makeText(
                                v.getContext(),
                                tooltip,
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                        return true;
                    });
                }

            } else {
                tvChatTypeLetter.setVisibility(View.GONE);
            }

            String text = messageChat.getText(itemView.getResources());
            markwon.setMarkdown(tvLastMessage, text);

            int unread = messageChat.getUnreadMessagesCount();
            tvUnreadCount.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);

            if (unread > 0) {
                tvUnreadCount.setText(String.valueOf(unread));
                UiAnimations.animateUnreadBadge(tvUnreadCount);
            }

            tvLastMessage.setTypeface(
                    null,
                    unread > 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL
            );
        }
        public void setClickHandlers(MessageChat messageChat, OnChatClickListener listener) {
            itemView.setOnTouchListener((v, e) -> {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        UiAnimations.animatePress(v, true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        UiAnimations.animatePress(v, false);
                        break;
                }
                return false;
            });

            itemView.setOnClickListener(v -> listener.onChatClick(messageChat.getChat()));
            itemView.setOnLongClickListener(v -> {
                listener.onChatLongClick(messageChat.getChat());
                return true;
            });
        }
    }

    static class PlaceholderViewHolder extends RecyclerView.ViewHolder {
        public PlaceholderViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
        void onChatLongClick(Chat chat);
    }
}