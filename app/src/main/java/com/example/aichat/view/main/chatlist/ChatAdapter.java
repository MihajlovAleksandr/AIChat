package com.example.aichat.view.main.chatlist;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.R;
import com.example.aichat.controller.main.chat.MessageController;
import com.example.aichat.controller.main.chatlist.ChatController;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<Chat> chatList;
    private final List<Message> lastMessages;
    private final OnChatClickListener listener;

    public ChatAdapter(List<Chat> chatList, OnChatClickListener listener) {
        this.chatList = new ArrayList<>(chatList);
        this.lastMessages = new ArrayList<>();
        for (int i = 0; i < chatList.size(); i++) {
            lastMessages.add(null);
        }
        this.listener = listener;
    }

    public void addChat(Chat newChat, Message lastMessage) {
        chatList.add(0, newChat);
        lastMessages.add(0, lastMessage);
        notifyItemInserted(0);
    }

    public void updateLastMessage(Message message) {
        for (int i = 0; i < chatList.size(); i++) {
            if (message.getChat() == chatList.get(i).getId()) {
                if (i < lastMessages.size()) {
                    lastMessages.set(i, message);
                } else {
                    lastMessages.add(message);
                }
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void endChat(Chat chat) {
        for (int i = 0; i < chatList.size(); i++) {
            if (chat.equals(chatList.get(i))) {
                chatList.get(i).end();
                if (i < lastMessages.size()) {
                    lastMessages.set(i, null);
                }
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        Message lastMessage = position < lastMessages.size() ? lastMessages.get(position) : null;
        holder.bind(chat, lastMessage);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClick(chat);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvLastMessage;
        private final TextView tvTime;
        private final ImageView ivChatStatus;
        private final Resources resources;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivChatStatus = itemView.findViewById(R.id.iv_chat_status);
            resources = itemView.getContext().getResources();
        }

        public void bind(Chat chat, Message lastMessage) {
            if (lastMessage != null) {
                tvLastMessage.setText(lastMessage.getText());
                tvTime.setText(MessageController.getFormattedMessageTime(lastMessage));
            } else {
                if (chat.isActive()) {
                    tvLastMessage.setText(resources.getText(R.string.chat_created));
                } else {
                    tvLastMessage.setText(resources.getText(R.string.chat_ended));
                }
                tvTime.setText(ChatController.getFormattedTime(chat));
            }
            updateChatStatus(chat);
        }

        private void updateChatStatus(Chat chat) {
            ivChatStatus.setImageResource(ChatController.getStatusIcon(chat));
        }
    }

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }
}