package com.example.aichat.view.main.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.R;
import com.example.aichat.controller.main.chat.MessageController;
import com.example.aichat.model.entities.Message;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages;
    public MessageController messageController;

    public MessageAdapter(int currentUserId, List<Message> messages) {
        this.messageController = new MessageController(currentUserId);
        this.messages = messages;
    }
    public void addMessage(Message newMessage) {
        messages.add(newMessage); // Просто add() без индекса добавит элемент в конец
        notifyItemInserted(messages.size() - 1); // Уведомляем о вставке по последнему индексу
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
        TextView messageText;
        TextView timeText;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
        }

        public void bind(Message message) {
            messageText.setText(message.getText());
            timeText.setText(messageController.getFormattedMessageTime(message));
        }
    }
}