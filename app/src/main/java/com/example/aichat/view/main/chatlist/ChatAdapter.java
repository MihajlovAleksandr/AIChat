package com.example.aichat.view.main.chatlist;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.List;
import java.util.UUID;

import io.noties.markwon.Markwon;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CHAT = 0;
    private static final int TYPE_PLACEHOLDER = 1;

    private final List<MessageChat> allChats = new ArrayList<>();
    private final List<MessageChat> visibleChats = new ArrayList<>();

    private final MessageController messageController;
    private final OnChatClickListener listener;

    private RecyclerView recyclerView;

    private ChatOpenedChecker openedChecker;

    public interface ChatOpenedChecker {
        boolean isChatOpened(UUID chatId);
    }

    public void setChatOpenedChecker(ChatOpenedChecker checker) {
        this.openedChecker = checker;
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

        visibleChats.clear();
        visibleChats.addAll(chats);

        notifyDataSetChanged();
    }

    public void setVisibleChats(List<MessageChat> chats) {
        visibleChats.clear();
        visibleChats.addAll(chats);
        notifyDataSetChanged();
    }

    public void setAllChatsVisible() {
        visibleChats.clear();
        visibleChats.addAll(allChats);
        notifyDataSetChanged();
    }

    public void addChat(MessageChat chat) {
        allChats.add(0, chat);
        visibleChats.add(0, chat);
        notifyItemInserted(0);

        if (recyclerView != null && recyclerView.getLayoutManager() != null) {
            View v = recyclerView.getLayoutManager().findViewByPosition(0);
            if (v != null) UiAnimations.animateNewMessage(v);
        }
    }

    public void removeChat(UUID chatId) {
        for (int i = 0; i < allChats.size(); i++) {
            if (allChats.get(i).getChat().getId().equals(chatId)) {
                allChats.remove(i);
                break;
            }
        }

        for (int i = 0; i < visibleChats.size(); i++) {
            if (visibleChats.get(i).getChat().getId().equals(chatId)) {

                final int index = i;

                if (recyclerView != null && recyclerView.getLayoutManager() != null) {
                    View v = recyclerView.getLayoutManager().findViewByPosition(index);
                    if (v != null) {
                        UiAnimations.animateChatRemove(v, () -> {
                            visibleChats.remove(index);
                            notifyItemRemoved(index);
                        });
                    } else {
                        visibleChats.remove(index);
                        notifyItemRemoved(index);
                    }
                } else {
                    visibleChats.remove(i);
                    notifyItemRemoved(i);
                }

                break;
            }
        }
    }

    public void renameChat(UUID chatId, String newName) {
        for (MessageChat mc : allChats) {
            if (mc.getChat().getId().equals(chatId)) {
                mc.getChat().setName(newName);
            }
        }
        for (int i = 0; i < visibleChats.size(); i++) {
            if (visibleChats.get(i).getChat().getId().equals(chatId)) {
                visibleChats.get(i).getChat().setName(newName);
                notifyItemChanged(i);
                break;
            }
        }
    }


    public void updateLastMessage(Message message) {
        UUID chatId = message.getChat();

        int oldPos = -1;

        for (int i = 0; i < visibleChats.size(); i++) {
            MessageChat mc = visibleChats.get(i);
            if (mc.getChat().getId().equals(chatId)) {

                mc.setMessage(message);

                if (openedChecker != null && !openedChecker.isChatOpened(chatId)) {
                    mc.addUnreadMessage(message);
                }

                oldPos = i;
                break;
            }
        }

        Collections.sort(allChats);
        Collections.sort(visibleChats);

        int newPos = -1;
        for (int i = 0; i < visibleChats.size(); i++) {
            if (visibleChats.get(i).getChat().getId().equals(chatId)) {
                newPos = i;
                break;
            }
        }

        if (oldPos != -1 && newPos != -1 && oldPos != newPos) {
            notifyItemMoved(oldPos, newPos);
            notifyItemChanged(newPos);
        } else {
            notifyDataSetChanged();
        }
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

    public void endChat(Chat chat) {
        for (MessageChat mc : allChats) {
            if (mc.getChat().getId().equals(chat.getId())) {
                mc.setEnded(true);
            }
        }
        for (int i = 0; i < visibleChats.size(); i++) {
            if (visibleChats.get(i).getChat().getId().equals(chat.getId())) {
                visibleChats.get(i).setEnded(true);
                notifyItemChanged(i);
                break;
            }
        }
    }

    @Nullable
    public MessageChat getMessageChat(UUID chatId) {
        for (MessageChat mc : allChats) {
            if (mc.getChat().getId().equals(chatId)) return mc;
        }
        return null;
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

        private final TextView tvChatName, tvLastMessage, tvTime, tvUnreadCount;
        private final ImageView ivChatStatus, ivMessageStatus, ivPin;
        private final MessageController messageController;
        private final Markwon markwon;

        public ChatViewHolder(@NonNull View itemView, MessageController messageController, Markwon markwon) {
            super(itemView);
            tvChatName = itemView.findViewById(R.id.tv_chat_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
            ivChatStatus = itemView.findViewById(R.id.iv_chat_status);
            ivMessageStatus = itemView.findViewById(R.id.iv_message_status);
            ivPin = itemView.findViewById(R.id.iv_pin);

            this.messageController = messageController;
            this.markwon = markwon;
        }

        public void bind(MessageChat messageChat) {
            tvChatName.setText(messageChat.getChat().getName());
            tvTime.setText(ChatController.getFormattedTime(messageChat.getTime()));

            ivChatStatus.setSelected(messageChat.getChat().isActive());

            String text = messageChat.getText(itemView.getResources());
            markwon.setMarkdown(tvLastMessage, text);

            int unread = messageChat.getUnreadMessagesCount();
            tvUnreadCount.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);

            if (unread > 0) {
                tvUnreadCount.setText(String.valueOf(unread));
            }

            tvLastMessage.setTypeface(
                    null,
                    unread > 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL
            );

            ivPin.setVisibility(messageChat.getChat().isPinned() ? View.VISIBLE : View.GONE);
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
