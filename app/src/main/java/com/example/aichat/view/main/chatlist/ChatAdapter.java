package com.example.aichat.view.main.chatlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.R;
import com.example.aichat.controller.main.chatlist.ChatController;
import com.example.aichat.model.entities.Chat;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Chat> chatList;
    private OnChatClickListener listener;
    private ChatController chatController;
    private RecyclerView recyclerView;

    public ChatAdapter(List<Chat> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
        this.chatController = new ChatController();
    }
    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public void addChat(Chat newChat) {
        chatList.add(0, newChat);
        notifyItemInserted(0);
    }
    public void endChat(Chat chat){
        for (Chat item: chatList) {
            if(item.equals(chat)) {
                item.end();
                break;
            }
        }
        ChatViewHolder chatViewHolder = getChatViewHolderById(chat.getId());
        if(chatViewHolder!=null)
            chatViewHolder.updateChatStatus(chat);
    }
    private ChatViewHolder getChatViewHolderById(int chatId) {
        if (recyclerView == null) return null;

        for (int i = 0; i < chatList.size(); i++) {
            if (chatId == chatList.get(i).getId()) {
                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
                if (holder instanceof ChatViewHolder) {
                    return (ChatViewHolder) holder;
                }
            }
        }
        return null;
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
        holder.bind(chat);
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

    class ChatViewHolder extends RecyclerView.ViewHolder {
        private TextView tvLastMessage;
        private TextView tvTime;
        private ImageView ivChatStatus;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivChatStatus = itemView.findViewById(R.id.iv_chat_status);
        }

        public void bind(Chat chat) {
            tvLastMessage.setText("Последнее сообщение");
            tvTime.setText(chatController.getFormattedTime(chat));
            updateChatStatus(chat);
        }
        public void updateChatStatus(Chat chat){
            ivChatStatus.setImageResource(chatController.getStatusIcon(chat));
        }

    }

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }
}