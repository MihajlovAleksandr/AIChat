package com.example.aichat.view.main.chat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.R;
import com.example.aichat.controller.main.chat.MessageController;
import com.example.aichat.model.entities.Message;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Message> messages;
    public MessageController messageController;
    private RecyclerView recyclerView;

    public MessageAdapter(int currentUserId, List<Message> messages) {
        this.messageController = new MessageController(currentUserId);
        this.messages = messages;

    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    public void addMessage(Message newMessage) {
        messages.add(newMessage);
        notifyItemInserted(messages.size() - 1);

        if (messageController.isMyMessage(newMessage)) {
            if (recyclerView != null) {
                recyclerView.post(() -> {
                    recyclerView.smoothScrollToPosition(messages.size() - 1);
                });
            }
        }
    }
    public void findMessages(Message messageToFind) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getId() == messageToFind.getId()) {
                if (recyclerView != null) {
                    recyclerView.scrollToPosition(i);
                }
                break;
            }
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = viewType == 0 ? R.layout.my_message : R.layout.other_message;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemViewType(int position) {
        return messageController.isMyMessage(messages.get(position)) ? 0 : 1;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        Message message;
        TextView messageText;
        TextView timeText;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            messageText.setMaxWidth((int) (recyclerView.getWidth()*0.8));
            timeText = itemView.findViewById(R.id.time_text);
        }

        public void bind(Message message) {
            this.message = message;
            messageText.setText(message.getText());
            timeText.setText(messageController.getFormattedMessageTime(message));
        }
        public boolean isCurrentMessage(Message message){
            return message.getId() == this.message.getId();
        }
    }
}