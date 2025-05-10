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
import com.example.aichat.model.entities.MessageChat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<MessageChat> chatList;
    private List<MessageChat> visibleChats;
    private final OnChatClickListener listener;

    public ChatAdapter(OnChatClickListener listener) {
        this.chatList = new ArrayList<>();
        visibleChats = chatList;
        this.listener = listener;
    }

    public void addChat(MessageChat messageChat) {
        chatList.add(0, messageChat);
        notifyItemInserted(0);
    }
    public void setChats(List<MessageChat> newChats) {
        Collections.reverse(newChats);
        chatList.clear();
        chatList.addAll(newChats);
        notifyDataSetChanged();
    }
    public void setVisibleChats(List<MessageChat> chats){
        visibleChats =  chats;
        notifyDataSetChanged();
    }
    public void setAllChatsVisible(){
        visibleChats = chatList;
        notifyDataSetChanged();
    }
    public void updateLastMessage(Message message) {
        for (int i = 0; i < chatList.size(); i++) {
            if (message.getChat() == chatList.get(i).getChat().getId()) {
                MessageChat messageChat = chatList.get(i);
                messageChat.setMessage(message);
                chatList.remove(i);
                chatList.add(0, messageChat);
                notifyItemMoved(i, 0);
                notifyItemChanged(0);
                break;
            }
        }
    }
    public void endChat(Chat chat) {
        for (int i = 0; i < chatList.size(); i++) {
            if (chat.equals(chatList.get(i).getChat())) {
                MessageChat messageChat = chatList.get(i);
                messageChat.getChat().end();
                messageChat.setMessage(null);
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
        MessageChat messageChat = visibleChats.get(position);
        holder.bind(messageChat);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClick(messageChat.getChat());
            }
        });
    }

    @Override
    public int getItemCount() {
        return visibleChats.size();
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

        public void bind(MessageChat messageChat) {
            tvLastMessage.setText(messageChat.getText(resources));
            tvTime.setText(ChatController.getFormattedTime(messageChat.getTime()));
            updateChatStatus(messageChat.getChat());
        }

        private void updateChatStatus(Chat chat) {
            ivChatStatus.setImageResource(ChatController.getStatusIcon(chat));
        }
    }

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }
}