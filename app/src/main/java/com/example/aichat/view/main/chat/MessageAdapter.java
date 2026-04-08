package com.example.aichat.view.main.chat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.R;
import com.example.aichat.controller.main.chat.actions.ChatMessageActions;
import com.example.aichat.controller.main.chat.MessageController;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.noties.markwon.Markwon;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>
        implements ReplyRenderer.MessageFinder, ReplyRenderer.ReplyClickListener {

    private final List<Message> messages;
    private final UUID userId;
    private final Markwon markwon;
    private final ReplyRenderer replyRenderer;

    private RecyclerView recyclerView;

    private OnMessageActionListener actionListener;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable readMessagesRunnable;
    private static final long READ_DELAY_MS = 1000;

    private final MessageController messageController;

    private UUID highlightedMessageId = null;

    public interface OnMessageActionListener {
        void onEditMessage(Message message);
        void onDeleteMessage(Message message);
        void onReplyToMessage(Message message);
        void onCopyMessageText(Message message);
    }

    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.actionListener = listener;
    }

    public MessageAdapter(UUID currentUserId, List<Message> messages, Context context) {
        this.userId = currentUserId;
        this.messages = messages != null ? messages : new ArrayList<>();
        this.markwon = Markwon.create(context);
        this.replyRenderer = new ReplyRenderer(context, this, this);
        this.messageController = new MessageController(currentUserId);
    }

    @Override
    public Message findMessageById(UUID id) {
        for (Message m : messages) {
            if (m.getId().equals(id)) return m;
        }
        return null;
    }

    @Override
    public void onReplyClicked(UUID replyMessageId) {
        Message original = findMessageById(replyMessageId);
        if (original != null) {
            highlightMessage(original.getId());
            scrollToMessage(original.getId());
        }
    }
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView rv) {
        super.onAttachedToRecyclerView(rv);
        recyclerView = rv;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView rv) {
        super.onDetachedFromRecyclerView(rv);
        if (readMessagesRunnable != null) {
            handler.removeCallbacks(readMessagesRunnable);
            readMessagesRunnable = null;
        }
        recyclerView = null;
    }
    public void checkVisibleMessages(ChatMessageActions actions) {
        if (recyclerView == null || messages.isEmpty() || actions == null) return;

        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
        if (!(lm instanceof LinearLayoutManager)) return;

        LinearLayoutManager manager = (LinearLayoutManager) lm;
        int firstVisible = manager.findFirstVisibleItemPosition();
        int lastVisible = manager.findLastVisibleItemPosition();

        boolean atBottom = lastVisible == messages.size() - 1;

        List<Message> toRead = new ArrayList<>();

        for (int i = firstVisible; i <= lastVisible && i < messages.size(); i++) {
            View itemView = manager.findViewByPosition(i);
            if (itemView == null) continue;

            if (!isBottomEdgeVisible(itemView)) continue;

            Message msg = messages.get(i);

            if (!messageController.isMyMessage(msg)) {
                Map<UUID, MessageStatus> statuses = msg.getStatuses();

                if (statuses != null &&
                        statuses.containsKey(userId) &&
                        statuses.get(userId) != MessageStatus.READ) {
                    toRead.add(msg);
                }
            }
        }

        if (!toRead.isEmpty()) {
            if (readMessagesRunnable != null) {
                handler.removeCallbacks(readMessagesRunnable);
                readMessagesRunnable = null;
            }

            if (atBottom) {
                readMessages(toRead, actions);
            } else {
                readMessagesRunnable = () -> {
                    readMessages(toRead, actions);
                    readMessagesRunnable = null;
                };
                handler.postDelayed(readMessagesRunnable, READ_DELAY_MS);
            }
        } else {
            if (readMessagesRunnable != null) {
                handler.removeCallbacks(readMessagesRunnable);
                readMessagesRunnable = null;
            }
        }
    }

    private boolean isBottomEdgeVisible(View itemView) {
        int bottom = itemView.getBottom();
        int height = recyclerView.getHeight();
        return bottom <= height;
    }

    private void readMessages(List<Message> msgs, ChatMessageActions actions) {
        for (Message msg : msgs) {
            if (msg.getStatuses() != null) {
                msg.getStatuses().replace(userId, MessageStatus.READ);
            }
            int pos = getMessagePosition(msg.getId());
            if (pos != -1) notifyItemChanged(pos);
        }
        actions.readMessages(msgs);
    }
    public void addOrUpdateMessage(Message newMsg) {
        int pos = getMessagePosition(newMsg.getId());
        if (pos != -1) {
            messages.set(pos, newMsg);
            notifyItemChanged(pos);
        } else {
            messages.add(newMsg);
            notifyItemInserted(messages.size() - 1);
        }

        if (messageController.isMyMessage(newMsg) && recyclerView != null) {
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(messages.size() - 1));
        }
    }

    public void updateStatus(UUID messageId, UUID userId, MessageStatus status) {
        int pos = getMessagePosition(messageId);
        if (pos == -1) return;

        Message msg = messages.get(pos);

        if (msg.getStatuses() != null && msg.getStatuses().containsKey(userId)) {
            msg.getStatuses().replace(userId, status);
            notifyItemChanged(pos);
        }
    }

    public void removeMessage(UUID id) {
        int pos = getMessagePosition(id);
        if (pos != -1) {
            messages.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    public void highlightMessage(UUID id) {
        highlightedMessageId = id;
        notifyDataSetChanged();
    }

    public void scrollToMessage(UUID id) {
        int pos = getMessagePosition(id);
        if (pos != -1 && recyclerView != null) {
            recyclerView.smoothScrollToPosition(pos);
        }
    }

    public int getMessagePosition(UUID id) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getId().equals(id)) return i;
        }
        return -1;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == 0 ? R.layout.my_message : R.layout.other_message;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(view, viewType == 0);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder h, int pos) {
        Message m = messages.get(pos);
        h.bind(m);

        if (highlightedMessageId != null && highlightedMessageId.equals(m.getId())) {
            h.itemView.setBackgroundColor(0xFFFFF2AA);
        } else {
            h.itemView.setBackgroundColor(0x00000000);
        }
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

        private final TextView messageText;
        private final TextView timeText;
        private final ImageView statusIcon;
        private final LinearLayout replyContainer;

        private Message message;
        private final boolean isMyMessage;

        MessageViewHolder(@NonNull View itemView, boolean isMyMessage) {
            super(itemView);

            this.isMyMessage = isMyMessage;

            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
            replyContainer = itemView.findViewById(R.id.reply_container);
            statusIcon = isMyMessage ? itemView.findViewById(R.id.status_icon) : null;

            if (messageText != null) {
                messageText.setMovementMethod(LinkMovementMethod.getInstance());
            }

            itemView.setOnLongClickListener(v -> {
                if (message != null && actionListener != null) {
                    showMessageMenu(v);
                    return true;
                }
                return false;
            });
        }

        private void showMessageMenu(View anchor) {
            PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
            menu.inflate(R.menu.message_context_menu);

            menu.setOnMenuItemClickListener(item -> {
                if (message == null) return false;

                int id = item.getItemId();

                if (id == R.id.menu_edit_message) {
                    actionListener.onEditMessage(message);
                    return true;
                }
                if (id == R.id.menu_delete_message) {
                    actionListener.onDeleteMessage(message);
                    return true;
                }
                if (id == R.id.menu_reply_message) {
                    actionListener.onReplyToMessage(message);
                    return true;
                }
                if (id == R.id.menu_copy_text) {
                    actionListener.onCopyMessageText(message);
                    return true;
                }

                return false;
            });

            menu.show();
        }

        void bind(Message m) {
            this.message = m;

            markwon.setMarkdown(messageText, m.getText());
            timeText.setText(MessageController.getFormattedMessageTime(m));

            replyRenderer.renderReplies(replyContainer, m.getReplyMessages());

            updateStatus();
        }

        void updateStatus() {
            if (isMyMessage && statusIcon != null) {
                MessageStatus max = MessageController.getMaxStatus(message.getStatuses());
                statusIcon.setImageResource(MessageController.getStatusIconRes(max));
            }
        }
    }
}
