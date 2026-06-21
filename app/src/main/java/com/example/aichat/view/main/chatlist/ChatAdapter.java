package com.example.aichat.view.main.chatlist;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.aichat.controller.main.chat.MessageController;
import com.example.aichat.controller.main.chatlist.ChatController;
import com.example.aichat.dto.response.AISettingsResponse;
import com.example.aichat.dto.response.UserInfoResponse;
import com.example.aichat.model.ai.AIModel;
import com.example.aichat.model.ai.AISettingsStore;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.ChatType;
import com.example.aichat.model.entities.FileType;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageChat;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.model.utils.media.ChatMediaMarkers;
import com.example.aichat.R;
import com.example.aichat.view.main.chat.helpers.CircleVideoTextureView;
import com.example.aichat.view.main.chat.helpers.UiAnimations;
import com.example.aichat.view.main.chatlist.helpers.MessageChatComparator;
import com.example.aichat.view.main.chatlist.helpers.MessageChatDiffCallback;
import com.example.aichat.view.theme.binders.ChatItemThemeBinder;
import io.noties.markwon.Markwon;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ChatAdapter";
    private static final int TYPE_CHAT = 0;
    private static final int TYPE_PLACEHOLDER = 1;

    private final List<MessageChat> allChats = new ArrayList<>();
    private final List<MessageChat> visibleChats = new ArrayList<>();
    private final Map<UUID, String> typingStatuses = new HashMap<>();
    private final Map<UUID, String> userDisplayNames = new HashMap<>();
    private final Map<UUID, AIModel> aiModelsByChatId = new HashMap<>();
    private final List<UUID> loadingUserNames = new ArrayList<>();
    private final List<UUID> loadingAiSettings = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MessageController messageController;
    private final OnChatClickListener listener;
    private final Comparator<MessageChat> comparator = new MessageChatComparator();

    private RecyclerView recyclerView;
    private ChatOpenedChecker openedChecker;
    private OnEmptyStateListener emptyStateListener;

    public ChatAdapter(OnChatClickListener listener, MessageController messageController) {
        this.listener = listener;
        this.messageController = messageController;
        setHasStableIds(true);
    }

    public interface ChatOpenedChecker {
        boolean isChatOpened(UUID chatId);
    }

    public interface OnEmptyStateListener {
        void onEmptyState(boolean isEmpty);
    }

    public interface OnChatClickListener {
        void onChatClick(Chat chat);

        void onChatLongClick(Chat chat);
    }

    public void setChatOpenedChecker(ChatOpenedChecker checker) {
        this.openedChecker = checker;
    }

    public void setOnEmptyStateListener(OnEmptyStateListener listener) {
        this.emptyStateListener = listener;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView rv) {
        super.onAttachedToRecyclerView(rv);
        recyclerView = rv;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView rv) {
        super.onDetachedFromRecyclerView(rv);

        if (recyclerView == rv) {
            recyclerView = null;
        }
    }

    public void setChats(@Nullable List<MessageChat> chats) {
        List<MessageChat> safeChats = chats != null ? chats : new ArrayList<>();

        allChats.clear();
        allChats.addAll(safeChats);
        pruneTypingStatuses(safeChats);

        List<MessageChat> sorted = new ArrayList<>(safeChats);
        Collections.sort(sorted, comparator);

        applyDiff(sorted);
        notifyEmptyState();
    }

    public void setVisibleChats(@Nullable List<MessageChat> chats) {
        List<MessageChat> safeChats = chats != null ? chats : new ArrayList<>();
        List<MessageChat> sorted = new ArrayList<>(safeChats);

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

    public void setChatTypingStatus(@Nullable UUID chatId, @Nullable String statusText) {
        if (chatId == null) return;

        String normalized = statusText != null ? statusText.trim() : "";

        if (normalized.isEmpty()) {
            clearChatTypingStatus(chatId);
            return;
        }

        String previous = typingStatuses.get(chatId);

        if (normalized.equals(previous)) return;

        typingStatuses.put(chatId, normalized);
        Log.d(TAG, "Typing status shown for chat " + chatId + ": " + normalized);
        notifyChatChanged(chatId);
    }

    public void clearChatTypingStatus(@Nullable UUID chatId) {
        if (chatId == null) return;

        if (typingStatuses.remove(chatId) != null) {
            Log.d(TAG, "Typing status cleared for chat " + chatId);
            notifyChatChanged(chatId);
        }
    }

    public void clearAllTypingStatuses() {
        if (typingStatuses.isEmpty()) return;

        typingStatuses.clear();
        notifyDataSetChanged();
    }

    public void addChat(@NonNull MessageChat chat) {
        UUID chatId = getChatId(chat);

        if (chatId != null) {
            int existingIndex = findAllPositionByChatId(chatId);

            if (existingIndex != -1) {
                allChats.set(existingIndex, chat);
            } else {
                allChats.add(chat);
            }
        } else {
            allChats.add(chat);
        }

        List<MessageChat> sorted = new ArrayList<>(allChats);
        Collections.sort(sorted, comparator);

        applyDiff(sorted);
        notifyEmptyState();
    }

    public void removeChat(UUID chatId) {
        Log.d(TAG, "removeChat called for chatId: " + chatId);

        if (chatId == null) return;

        typingStatuses.remove(chatId);

        int position = findVisiblePositionByChatId(chatId);

        if (position != -1 && recyclerView != null) {
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);

            if (holder != null) {
                UiAnimations.animateRemoveChat(holder.itemView, () -> removeChatWithoutAnimation(chatId));
                return;
            }
        }

        removeChatWithoutAnimation(chatId);
    }

    private void removeChatWithoutAnimation(@NonNull UUID chatId) {
        allChats.removeIf(messageChat -> isSameChat(messageChat, chatId));

        List<MessageChat> sorted = new ArrayList<>(allChats);
        Collections.sort(sorted, comparator);

        applyDiff(sorted);
        notifyEmptyState();
        Log.d(TAG, "Chat removed: " + chatId);
    }

    public void renameChat(UUID chatId, String newName) {
        Log.d(TAG, "renameChat called for chatId: " + chatId + ", newName: " + newName);

        if (chatId == null) return;

        for (MessageChat messageChat : allChats) {
            if (isSameChat(messageChat, chatId)) {
                messageChat.getChat().setName(newName);
            }
        }

        int visiblePosition = findVisiblePositionByChatId(chatId);

        if (visiblePosition != -1) {
            MessageChat visibleChat = visibleChats.get(visiblePosition);

            if (visibleChat != null && visibleChat.getChat() != null) {
                visibleChat.getChat().setName(newName);
            }

            notifyItemChanged(visiblePosition);
            Log.d(TAG, "Chat renamed at position: " + visiblePosition);
        }
    }

    public void updateLastMessage(Message message) {
        if (message == null || message.getChat() == null) return;

        UUID chatId = message.getChat();
        boolean isMyMessage = messageController != null && messageController.isMyMessage(message);
        boolean isOpenedChat = openedChecker != null && openedChecker.isChatOpened(chatId);
        boolean shouldClearUnread = isMyMessage || isOpenedChat;
        int oldPosition = -1;

        for (int i = 0; i < visibleChats.size(); i++) {
            MessageChat messageChat = visibleChats.get(i);

            if (isSameChat(messageChat, chatId)) {
                messageChat.setMessage(message);

                if (shouldClearUnread) {
                    messageChat.clearUnreadMessages();
                } else if (!isMyMessage) {
                    messageChat.addUnreadMessage(message);
                }

                oldPosition = i;
                break;
            }
        }

        if (oldPosition == -1) return;

        for (MessageChat messageChat : allChats) {
            if (isSameChat(messageChat, chatId)) {
                messageChat.setMessage(message);

                if (shouldClearUnread) {
                    messageChat.clearUnreadMessages();
                } else if (!isMyMessage) {
                    messageChat.addUnreadMessage(message);
                }

                break;
            }
        }

        allChats.sort(comparator);
        visibleChats.sort(comparator);

        int newPosition = findVisiblePositionByChatId(chatId);
        if (newPosition == -1) return;

        if (oldPosition != newPosition) {
            notifyItemMoved(oldPosition, newPosition);
        }

        notifyItemChanged(newPosition);
    }

    public void updateMessageStatus(MessageChat messageChat) {
        if (messageChat == null || messageChat.getChat() == null || messageChat.getChat().getId() == null) {
            return;
        }

        UUID chatId = messageChat.getChat().getId();

        for (int i = 0; i < allChats.size(); i++) {
            if (isSameChat(allChats.get(i), chatId)) {
                allChats.set(i, messageChat);
                break;
            }
        }

        int visiblePosition = findVisiblePositionByChatId(chatId);

        if (visiblePosition != -1) {
            visibleChats.set(visiblePosition, messageChat);
            notifyItemChanged(visiblePosition);
        }

        notifyEmptyState();
    }

    public void updateMessageStatus(UUID messageId, UUID chatId, MessageStatus status) {
        if (messageId == null || chatId == null || status == null) return;

        for (MessageChat messageChat : allChats) {
            if (isSameChat(messageChat, chatId)) {
                applyMessageStatusToChat(messageChat, messageId, status);
                break;
            }
        }

        int visiblePosition = findVisiblePositionByChatId(chatId);

        if (visiblePosition != -1) {
            applyMessageStatusToChat(visibleChats.get(visiblePosition), messageId, status);
            notifyItemChanged(visiblePosition);
        }

        notifyEmptyState();
    }

    private boolean applyMessageStatusToChat(MessageChat messageChat, UUID messageId, MessageStatus status) {
        if (messageChat == null || messageId == null || status == null) return false;

        int beforeUnreadCount = messageChat.getUnreadMessagesCount();

        if (status == MessageStatus.READ) {
            messageChat.removeUnreadMessage(messageId);
        }

        return beforeUnreadCount != messageChat.getUnreadMessagesCount();
    }

    public void endChat(UUID chatId) {
        Log.d(TAG, "endChat called for chatId: " + chatId);

        if (chatId == null) return;

        typingStatuses.remove(chatId);

        for (MessageChat messageChat : allChats) {
            if (isSameChat(messageChat, chatId)) {
                messageChat.setEnded(true);
                break;
            }
        }

        int visiblePosition = findVisiblePositionByChatId(chatId);

        if (visiblePosition != -1) {
            visibleChats.get(visiblePosition).setEnded(true);
            notifyItemChanged(visiblePosition);
        }

        notifyEmptyState();
    }

    public boolean isChatEnded(UUID chatId) {
        MessageChat messageChat = getMessageChat(chatId);
        return messageChat != null && messageChat.isEnded();
    }

    @Nullable
    public MessageChat getMessageChat(UUID chatId) {
        if (chatId == null) return null;

        for (MessageChat messageChat : allChats) {
            if (isSameChat(messageChat, chatId)) return messageChat;
        }

        return null;
    }
    public void setAiModel(@Nullable UUID chatId, @Nullable AIModel model) {
        if (chatId == null) {
            return;
        }

        AIModel safeModel = model != null ? model : AIModel.Default;
        AIModel previous = aiModelsByChatId.get(chatId);

        aiModelsByChatId.put(chatId, safeModel);
        loadingAiSettings.remove(chatId);

        if (itemContext() != null) {
            AISettingsStore.saveSelectedChatModel(itemContext(), chatId, safeModel);
        }

        if (previous != safeModel) {
            notifyChatChanged(chatId);
        } else {
            notifyChatChanged(chatId);
        }
    }

    public void refreshAiModelBadges() {
        android.content.Context context = itemContext();

        if (context == null) {
            return;
        }

        boolean changed = false;

        for (MessageChat messageChat : allChats) {
            if (messageChat == null
                    || messageChat.getChat() == null
                    || messageChat.getChat().getType() != ChatType.AI) {
                continue;
            }

            UUID chatId = getChatId(messageChat);

            if (chatId == null) {
                continue;
            }

            AIModel storedModel = AISettingsStore.getSelectedChatModel(context, chatId);
            AIModel currentModel = aiModelsByChatId.get(chatId);

            if (currentModel != storedModel) {
                aiModelsByChatId.put(chatId, storedModel);
                changed = true;
            }
        }

        if (changed) {
            notifyDataSetChanged();
        }
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
        notifyEmptyState();
    }

    private void applyDiff(@NonNull List<MessageChat> newList) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new MessageChatDiffCallback(visibleChats, newList));

        visibleChats.clear();
        visibleChats.addAll(newList);

        diff.dispatchUpdatesTo(this);
    }

    private void notifyEmptyState() {
        if (emptyStateListener != null) {
            emptyStateListener.onEmptyState(visibleChats.isEmpty());
        }
    }

    private void notifyChatChanged(@NonNull UUID chatId) {
        int position = findVisiblePositionByChatId(chatId);

        if (position != -1) {
            notifyItemChanged(position);
        } else {
            Log.d(TAG, "Typing status was updated, but chat is not visible now: " + chatId);
        }
    }

    public boolean hasChat(@Nullable UUID chatId) {
        return findAllPositionByChatId(chatId) != -1;
    }

    private int findAllPositionByChatId(@Nullable UUID chatId) {
        if (chatId == null) return -1;

        for (int i = 0; i < allChats.size(); i++) {
            if (isSameChat(allChats.get(i), chatId)) return i;
        }

        return -1;
    }

    private int findVisiblePositionByChatId(@NonNull UUID chatId) {
        for (int i = 0; i < visibleChats.size(); i++) {
            if (isSameChat(visibleChats.get(i), chatId)) return i;
        }

        return -1;
    }

    private boolean isSameChat(MessageChat messageChat, UUID chatId) {
        return messageChat != null
                && messageChat.getChat() != null
                && messageChat.getChat().getId() != null
                && messageChat.getChat().getId().equals(chatId);
    }

    @Nullable
    private UUID getChatId(@Nullable MessageChat messageChat) {
        if (messageChat == null || messageChat.getChat() == null) return null;
        return messageChat.getChat().getId();
    }

    @Nullable
    private String getTypingStatusFor(@Nullable MessageChat messageChat) {
        UUID chatId = getChatId(messageChat);
        return chatId != null ? typingStatuses.get(chatId) : null;
    }

    private void pruneTypingStatuses(@NonNull List<MessageChat> chats) {
        if (typingStatuses.isEmpty()) return;

        List<UUID> existingChatIds = new ArrayList<>();

        for (MessageChat messageChat : chats) {
            UUID chatId = getChatId(messageChat);

            if (chatId != null) {
                existingChatIds.add(chatId);
            }
        }

        typingStatuses.keySet().removeIf(chatId -> !existingChatIds.contains(chatId));
    }

    @Override
    public int getItemViewType(int position) {
        return visibleChats.get(position) == null ? TYPE_PLACEHOLDER : TYPE_CHAT;
    }

    @Override
    public int getItemCount() {
        return visibleChats.size();
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= visibleChats.size()) {
            return RecyclerView.NO_ID;
        }

        MessageChat messageChat = visibleChats.get(position);
        UUID chatId = getChatId(messageChat);

        if (chatId == null) {
            return RecyclerView.NO_ID;
        }

        return chatId.getMostSignificantBits() ^ chatId.getLeastSignificantBits();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_PLACEHOLDER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_placeholder, parent, false);
            return new PlaceholderViewHolder(view);
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        Markwon markwon = Markwon.builder(parent.getContext()).build();

        return new ChatViewHolder(this, view, messageController, markwon);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageChat messageChat = visibleChats.get(position);

        if (messageChat == null || !(holder instanceof ChatViewHolder)) return;

        ChatViewHolder chatHolder = (ChatViewHolder) holder;

        ChatItemThemeBinder.apply(holder.itemView);
        chatHolder.bind(messageChat, getTypingStatusFor(messageChat));
        chatHolder.setClickHandlers(messageChat, listener);
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        View view = holder.itemView;

        view.animate().cancel();
        view.setAlpha(1f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setRotation(0f);

        if (holder instanceof ChatViewHolder) {
            ((ChatViewHolder) holder).resetTransientState();
        }

        ChatItemThemeBinder.apply(view);
    }


    @Nullable
    private String getCachedUserDisplayName(@Nullable UUID userId) {
        if (userId == null) {
            return null;
        }

        String cached = userDisplayNames.get(userId);
        return cached != null && !cached.trim().isEmpty() ? cached : null;
    }

    private void requestUserDisplayNameIfNeeded(@Nullable UUID userId, @Nullable UUID chatId) {
        if (userId == null || chatId == null) {
            return;
        }

        if (userDisplayNames.containsKey(userId) || loadingUserNames.contains(userId)) {
            return;
        }

        ConnectionDispatcher dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();

        if (dispatcher == null) {
            return;
        }

        loadingUserNames.add(userId);

        dispatcher.sendHttpRequestAsync(
                        "/api/user/" + userId + "/userdata/",
                        HttpClient.HTTPMethod.GET,
                        null,
                        true
                )
                .thenAccept(command -> {
                    try {
                        if (command != null && command.isSuccess()) {
                            UserInfoResponse response = command.getData(UserInfoResponse.class);

                            if (response != null
                                    && response.userData != null
                                    && response.userData.name != null
                                    && !response.userData.name.trim().isEmpty()) {
                                userDisplayNames.put(userId, response.userData.name.trim());
                                mainHandler.post(() -> notifyChatChanged(chatId));
                            }
                        }
                    } catch (Exception exception) {
                        Log.w(TAG, "Cannot load chat circle sender name", exception);
                    } finally {
                        loadingUserNames.remove(userId);
                    }
                })
                .exceptionally(throwable -> {
                    loadingUserNames.remove(userId);
                    Log.w(TAG, "Cannot request chat circle sender name", throwable);
                    return null;
                });
    }

    @Nullable
    private AIModel getCachedAiModel(@Nullable UUID chatId) {
        if (chatId == null) {
            return null;
        }

        AIModel cached = aiModelsByChatId.get(chatId);

        if (cached != null) {
            return cached;
        }

        return AISettingsStore.getSelectedChatModel(itemContext(), chatId);
    }

    private android.content.Context itemContext() {
        return recyclerView != null ? recyclerView.getContext() : null;
    }

    private void requestAiSettingsIfNeeded(@Nullable UUID chatId) {
        if (chatId == null) {
            return;
        }

        if (aiModelsByChatId.containsKey(chatId) || loadingAiSettings.contains(chatId)) {
            return;
        }

        ConnectionDispatcher dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();

        if (dispatcher == null) {
            return;
        }

        loadingAiSettings.add(chatId);

        dispatcher.sendHttpRequestAsync(
                        "/api/chat/" + chatId + "/ai",
                        HttpClient.HTTPMethod.GET,
                        null,
                        false
                )
                .thenAccept(command -> {
                    try {
                        if (command != null && command.isSuccess()) {
                            AISettingsResponse response = command.getData(AISettingsResponse.class);
                            AIModel model = response != null && response.aiModel != null
                                    ? response.aiModel
                                    : AIModel.Default;

                            aiModelsByChatId.put(chatId, model);
                            AISettingsStore.saveSelectedChatModel(itemContext(), chatId, model);
                            mainHandler.post(() -> notifyChatChanged(chatId));
                        }
                    } catch (Exception exception) {
                        Log.w(TAG, "Cannot load chat AI model", exception);
                    } finally {
                        loadingAiSettings.remove(chatId);
                    }
                })
                .exceptionally(throwable -> {
                    loadingAiSettings.remove(chatId);
                    Log.w(TAG, "Cannot request chat AI model", throwable);
                    return null;
                });
    }

    @NonNull
    private String getAiModelInitials(@Nullable AIModel model) {
        AIModel safeModel = model != null ? model : AIModel.Default;

        switch (safeModel) {
            case DeepSeekChat:
                return "DS";
            case OllamaQwen3_4B:
                return "Q3";
            case OllamaLlama3:
                return "L3";
            case OllamaMistral:
                return "MI";
            case OllamaGemma4e:
                return "G4";
            case Default:
            default:
                return "DF";
        }
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {

        private static CircleVideoTextureView activeCirclePreview;

        private final TextView tvChatName;
        private final TextView tvLastMessage;
        private final TextView tvTime;
        private final TextView tvUnreadCount;
        private final TextView tvChatTypeLetter;
        private final TextView tvChatCircleInitials;
        private final ImageView ivChatStatus;
        private final View circlePreviewRoot;
        private final CircleVideoTextureView circlePreviewVideo;
        private final ImageView circlePreviewThumbnail;
        private final ImageView circlePreviewPlay;
        private final ChatAdapter adapter;
        private final MessageController messageController;
        private final Markwon markwon;

        private CirclePreviewInfo lastCirclePreviewInfo;

        ChatViewHolder(@NonNull ChatAdapter adapter, @NonNull View itemView, MessageController messageController, Markwon markwon) {
            super(itemView);

            this.adapter = adapter;

            tvChatName = itemView.findViewById(R.id.tv_chat_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
            ivChatStatus = itemView.findViewById(R.id.iv_chat_status);
            tvChatTypeLetter = itemView.findViewById(R.id.tv_chat_type_letter);
            tvChatCircleInitials = itemView.findViewById(R.id.tv_chat_circle_initials);
            circlePreviewRoot = itemView.findViewById(R.id.chat_circle_preview_root);
            circlePreviewVideo = itemView.findViewById(R.id.chat_circle_video_preview);
            circlePreviewThumbnail = itemView.findViewById(R.id.chat_circle_video_thumbnail);
            circlePreviewPlay = itemView.findViewById(R.id.chat_circle_preview_play);

            this.messageController = messageController;
            this.markwon = markwon;
        }

        void bind(@NonNull MessageChat messageChat, @Nullable String typingStatus) {
            itemView.setTag(R.id.item_chat_root, messageChat);
            tvChatName.setText(messageChat.getChat().getName());
            tvTime.setText(ChatController.getFormattedTime(messageChat.getTime()));

            lastCirclePreviewInfo = findCirclePreviewInfo(messageChat.getMessage());

            bindChatActiveState(messageChat);
            bindCirclePreview(lastCirclePreviewInfo);
            bindChatCircleInitials(messageChat, lastCirclePreviewInfo != null);
            bindChatTypeHint(messageChat);
            bindLastLine(messageChat, typingStatus, lastCirclePreviewInfo != null);
            bindUnreadCount(messageChat, typingStatus);
        }

        private void bindChatActiveState(@NonNull MessageChat messageChat) {
            boolean isActive = messageChat.getChat().isActive();
            boolean isEnded = messageChat.getChat().getEndTime() != null;

            float alpha = !isActive || isEnded ? 0.5f : 1f;
            ivChatStatus.setAlpha(alpha);

            if (tvChatCircleInitials != null) {
                tvChatCircleInitials.setAlpha(alpha);
            }
        }

        private void bindCirclePreview(@Nullable CirclePreviewInfo info) {
            stopLocalPreview(false);

            if (circlePreviewRoot != null) {
                circlePreviewRoot.setOnClickListener(null);
            }

            if (info == null || info.localFile == null || !info.localFile.exists()) {
                if (circlePreviewVideo != null) circlePreviewVideo.setVisibility(View.GONE);
                if (circlePreviewThumbnail != null) circlePreviewThumbnail.setVisibility(View.GONE);
                if (circlePreviewPlay != null) circlePreviewPlay.setVisibility(View.GONE);
                if (ivChatStatus != null) ivChatStatus.setVisibility(View.VISIBLE);
                return;
            }

            if (ivChatStatus != null) ivChatStatus.setVisibility(View.GONE);
            if (circlePreviewVideo != null) circlePreviewVideo.setVisibility(View.GONE);
            if (circlePreviewThumbnail != null) {
                circlePreviewThumbnail.setVisibility(View.VISIBLE);
                Glide.with(circlePreviewThumbnail)
                        .load(info.localFile)
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.chat_status_selector)
                        .error(R.drawable.chat_status_selector)
                        .into(circlePreviewThumbnail);
            }
            if (circlePreviewPlay != null) circlePreviewPlay.setVisibility(View.VISIBLE);

            if (circlePreviewRoot != null) {
                circlePreviewRoot.setOnClickListener(v -> toggleCirclePreviewPlayback(info));
            }
        }

        private void toggleCirclePreviewPlayback(@NonNull CirclePreviewInfo info) {
            if (info.localFile == null || !info.localFile.exists() || circlePreviewVideo == null) {
                return;
            }

            if (activeCirclePreview == circlePreviewVideo && circlePreviewVideo.isPlaying()) {
                stopLocalPreview(false);
                if (circlePreviewThumbnail != null) circlePreviewThumbnail.setVisibility(View.VISIBLE);
                if (circlePreviewPlay != null) circlePreviewPlay.setVisibility(View.VISIBLE);
                return;
            }

            if (activeCirclePreview != null && activeCirclePreview != circlePreviewVideo) {
                try {
                    activeCirclePreview.stopPlayback();
                    activeCirclePreview.setVisibility(View.GONE);

                    View parent = activeCirclePreview.getParent() instanceof View
                            ? (View) activeCirclePreview.getParent()
                            : null;

                    if (parent != null) {
                        View thumbnail = parent.findViewById(R.id.chat_circle_video_thumbnail);
                        View root = parent;

                        while (root.getParent() instanceof View
                                && root.getId() != R.id.chat_circle_preview_root) {
                            root = (View) root.getParent();
                        }

                        View play = root.findViewById(R.id.chat_circle_preview_play);

                        if (thumbnail != null) thumbnail.setVisibility(View.VISIBLE);
                        if (play != null) play.setVisibility(View.VISIBLE);
                    }
                } catch (Exception ignored) {
                }
            }

            try {
                activeCirclePreview = circlePreviewVideo;
                if (circlePreviewThumbnail != null) circlePreviewThumbnail.setVisibility(View.GONE);
                if (circlePreviewPlay != null) circlePreviewPlay.setVisibility(View.GONE);
                circlePreviewVideo.setVisibility(View.VISIBLE);
                circlePreviewVideo.setOnPreparedListener(mp -> circlePreviewVideo.start());
                circlePreviewVideo.setOnErrorListener((mp, what, extra) -> {
                    stopLocalPreview(false);
                    if (circlePreviewThumbnail != null) circlePreviewThumbnail.setVisibility(View.VISIBLE);
                    if (circlePreviewPlay != null) circlePreviewPlay.setVisibility(View.VISIBLE);
                    return true;
                });
                circlePreviewVideo.setLooping(true);
                circlePreviewVideo.setMuted(true);
                circlePreviewVideo.setVideoURI(Uri.fromFile(info.localFile));
                circlePreviewVideo.start();
            } catch (Exception exception) {
                stopLocalPreview(false);
                if (circlePreviewThumbnail != null) circlePreviewThumbnail.setVisibility(View.VISIBLE);
                if (circlePreviewPlay != null) circlePreviewPlay.setVisibility(View.VISIBLE);
            }
        }

        private void stopLocalPreview(boolean resetVisibility) {
            if (circlePreviewVideo != null) {
                try {
                    circlePreviewVideo.stopPlayback();
                } catch (Exception ignored) {
                }

                circlePreviewVideo.setOnPreparedListener(null);
                circlePreviewVideo.setOnErrorListener(null);

                if (activeCirclePreview == circlePreviewVideo) {
                    activeCirclePreview = null;
                }
            }

            if (resetVisibility) {
                if (circlePreviewVideo != null) circlePreviewVideo.setVisibility(View.GONE);
                if (circlePreviewThumbnail != null) circlePreviewThumbnail.setVisibility(View.GONE);
                if (circlePreviewPlay != null) circlePreviewPlay.setVisibility(View.GONE);
                if (ivChatStatus != null) ivChatStatus.setVisibility(View.VISIBLE);
            }
        }

        private void bindChatCircleInitials(
                @NonNull MessageChat messageChat,
                boolean hasCirclePreview
        ) {
            if (tvChatCircleInitials == null) {
                return;
            }

            Chat chat = messageChat.getChat();
            ChatType chatType = chat != null ? chat.getType() : ChatType.Human;

            if (chatType == null) {
                chatType = ChatType.Human;
            }

            if (hasCirclePreview || chatType == ChatType.Random) {
                tvChatCircleInitials.setVisibility(View.GONE);
                tvChatCircleInitials.setText("");
                return;
            }

            String initials = getChatCircleInitials(chat, chatType);

            if (initials.trim().isEmpty()) {
                tvChatCircleInitials.setVisibility(View.GONE);
                tvChatCircleInitials.setText("");
                return;
            }

            tvChatCircleInitials.setVisibility(View.VISIBLE);
            tvChatCircleInitials.setText(initials);
        }

        @NonNull
        private String getChatCircleInitials(@Nullable Chat chat, @NonNull ChatType chatType) {
            if (chatType == ChatType.Random) {
                return "";
            }

            Message lastMessage = getBoundLastMessage();

            if (lastMessage != null && messageController != null && messageController.isMyMessage(lastMessage)) {
                return normalizeInitials(itemView.getContext().getString(R.string.chat_circle_me));
            }

            if (chatType == ChatType.AI) {
                UUID chatId = chat != null ? chat.getId() : null;
                AIModel cachedModel = adapter.getCachedAiModel(chatId);
                adapter.requestAiSettingsIfNeeded(chatId);
                return adapter.getAiModelInitials(cachedModel);
            }

            String source = getLastMessageAuthorSource(chat, chatType, lastMessage);

            if (source == null || source.trim().isEmpty()) {
                source = chat != null ? chat.getName() : null;
            }

            return normalizeInitials(source);
        }

        @Nullable
        private Message getBoundLastMessage() {
            Object tag = itemView.getTag(R.id.item_chat_root);

            if (tag instanceof MessageChat) {
                return ((MessageChat) tag).getMessage();
            }

            return null;
        }

        @Nullable
        private String getLastMessageAuthorSource(
                @Nullable Chat chat,
                @NonNull ChatType chatType,
                @Nullable Message message
        ) {
            if (chatType == ChatType.Random) {
                return null;
            }

            if (message == null) {
                return chat != null ? chat.getName() : null;
            }

            if (messageController != null && messageController.isMyMessage(message)) {
                return itemView.getContext().getString(R.string.chat_circle_me);
            }

            UUID senderId = message.getSender();
            UUID chatId = chat != null ? chat.getId() : null;
            String displayName = adapter.getCachedUserDisplayName(senderId);

            if (displayName == null || displayName.trim().isEmpty()) {
                adapter.requestUserDisplayNameIfNeeded(senderId, chatId);
                displayName = chat != null ? chat.getName() : null;
            }

            return displayName;
        }

        @NonNull
        private String normalizeInitials(@Nullable String source) {
            if (source == null) {
                return "";
            }

            String cleaned = source
                    .trim()
                    .replaceAll("\\s+", " ");

            if (cleaned.isEmpty()) {
                return "";
            }

            StringBuilder result = new StringBuilder();
            int count = 0;

            for (int offset = 0; offset < cleaned.length() && count < 2; ) {
                int codePoint = cleaned.codePointAt(offset);
                offset += Character.charCount(codePoint);

                if (Character.isWhitespace(codePoint)) {
                    continue;
                }

                result.appendCodePoint(codePoint);
                count++;
            }

            return result.toString().toUpperCase(Locale.getDefault());
        }

        private void bindChatTypeHint(@NonNull MessageChat messageChat) {
            Chat chat = messageChat.getChat();
            ChatType chatType = chat != null ? chat.getType() : ChatType.Human;

            if (chatType == null) {
                chatType = ChatType.Human;
            }

            tvChatTypeLetter.setVisibility(View.VISIBLE);
            tvChatTypeLetter.setText(getChatTypeLetter(chatType));
            tvChatTypeLetter.setTextColor(0xFFFFFFFF);
            tvChatTypeLetter.setBackgroundTintList(ColorStateList.valueOf(getChatTypeColor(chatType)));

            String tooltip = getChatTypeTooltip(chatType);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tvChatTypeLetter.setTooltipText(tooltip);
                tvChatTypeLetter.setOnLongClickListener(null);
            } else {
                tvChatTypeLetter.setOnLongClickListener(view -> {
                    Toast.makeText(view.getContext(), tooltip, Toast.LENGTH_SHORT).show();
                    return true;
                });
            }
        }

        @NonNull
        private String getChatTypeLetter(@NonNull ChatType chatType) {
            switch (chatType) {
                case Group:
                    return itemView.getContext().getString(R.string.chat_type_letter_group);
                case AI:
                    return itemView.getContext().getString(R.string.chat_type_letter_ai);
                case Random:
                    return itemView.getContext().getString(R.string.chat_type_letter_random);
                case Human:
                default:
                    return itemView.getContext().getString(R.string.chat_type_letter_human);
            }
        }

        @NonNull
        private String getChatTypeTooltip(@NonNull ChatType chatType) {
            switch (chatType) {
                case Group:
                    return itemView.getContext().getString(R.string.chat_type_group);
                case AI:
                    return itemView.getContext().getString(R.string.chat_type_ai);
                case Random:
                    return itemView.getContext().getString(R.string.chat_type_random);
                case Human:
                default:
                    return itemView.getContext().getString(R.string.chat_type_human);
            }
        }

        private int getChatTypeColor(@NonNull ChatType chatType) {
            switch (chatType) {
                case Group:
                    return 0xFF4A90E2;
                case AI:
                    return 0xFF7E57C2;
                case Random:
                    return 0xFFFF9800;
                case Human:
                default:
                    return 0xFF9E9E9E;
            }
        }

        private void bindLastLine(
                @NonNull MessageChat messageChat,
                @Nullable String typingStatus,
                boolean hasCirclePreview
        ) {
            String safeTypingStatus = typingStatus != null ? typingStatus.trim() : "";

            if (!safeTypingStatus.isEmpty()) {
                tvLastMessage.setText(safeTypingStatus);
                tvLastMessage.setTextColor(resolvePrimaryColor());
                tvLastMessage.setTypeface(null, Typeface.BOLD);
                return;
            }

            if (hasCirclePreview) {
                tvLastMessage.setText("◉ Видео-кружок");
                tvLastMessage.setTextColor(resolveOnSurfaceVariantColor());
                tvLastMessage.setTypeface(null, Typeface.BOLD);
                return;
            }

            String text = buildLastMessagePreview(messageChat);

            if (text.contains("[file:")) {
                tvLastMessage.setText(text);
            } else {
                markwon.setMarkdown(tvLastMessage, text);
            }

            tvLastMessage.setTextColor(resolveOnSurfaceVariantColor());
            tvLastMessage.setTypeface(null, Typeface.NORMAL);
        }

        @NonNull
        private String buildLastMessagePreview(@NonNull MessageChat messageChat) {
            String text = messageChat.getText(itemView.getResources());

            if (text == null || text.trim().isEmpty()) {
                return "";
            }

            if (!text.contains("[file:")) {
                return text;
            }

            java.util.List<String> fileNames = extractMarkerDisplayNames(text);
            String userText = text
                    .replaceAll("\\s*\\[file:[^\\]]*\\]\\s*", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (!userText.isEmpty()) {
                return userText;
            }

            if (fileNames.isEmpty()) {
                return "📎 Файл";
            }

            String firstName = fileNames.get(0);
            String prefix = getPreviewIconForFileName(firstName);
            String preview = prefix + firstName;

            if (fileNames.size() > 1) {
                preview += " +" + (fileNames.size() - 1);
            }

            return preview;
        }

        @NonNull
        private java.util.List<String> extractMarkerDisplayNames(@Nullable String text) {
            java.util.List<String> names = new ArrayList<>();

            if (text == null || text.isEmpty()) {
                return names;
            }

            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[file:([^\\]]*)\\]");
            java.util.regex.Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                String payload = matcher.group(1);
                String displayName = ChatMediaMarkers.extractDisplayNameFromMarkerPayload(payload);

                if (displayName != null && !displayName.trim().isEmpty()) {
                    names.add(displayName.trim());
                }
            }

            return names;
        }

        @NonNull
        private String getPreviewIconForFileName(@Nullable String fileName) {
            String lower = fileName == null ? "" : fileName.trim().toLowerCase(Locale.US);

            if (lower.endsWith(".mp3")
                    || lower.endsWith(".m4a")
                    || lower.endsWith(".wav")
                    || lower.endsWith(".flac")
                    || lower.endsWith(".ogg")
                    || lower.endsWith(".aac")) {
                return "🎵 ";
            }

            if (lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg")
                    || lower.endsWith(".png")
                    || lower.endsWith(".webp")) {
                return "🖼 ";
            }

            if (lower.endsWith(".mp4")
                    || lower.endsWith(".mkv")
                    || lower.endsWith(".webm")
                    || lower.endsWith(".mov")) {
                return "🎬 ";
            }

            return "📎 ";
        }

        private void bindUnreadCount(@NonNull MessageChat messageChat, @Nullable String typingStatus) {
            if (messageController != null && messageController.isMyMessage(messageChat.getMessage())) {
                messageChat.clearUnreadMessages();
            }

            int unread = messageChat.getUnreadMessagesCount();

            tvUnreadCount.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);

            if (unread > 0) {
                tvUnreadCount.setText(String.valueOf(unread));
                UiAnimations.animateUnreadBadge(tvUnreadCount);
            } else {
                tvUnreadCount.setText("");
                tvUnreadCount.animate().cancel();
                tvUnreadCount.setAlpha(1f);
                tvUnreadCount.setScaleX(1f);
                tvUnreadCount.setScaleY(1f);
            }

            boolean hasTypingStatus = typingStatus != null && !typingStatus.trim().isEmpty();
            boolean hasCirclePreview = lastCirclePreviewInfo != null;

            tvLastMessage.setTypeface(
                    null,
                    hasTypingStatus || hasCirclePreview || unread > 0 ? Typeface.BOLD : Typeface.NORMAL
            );
        }

        private int resolvePrimaryColor() {
            android.util.TypedValue value = new android.util.TypedValue();

            if (itemView.getContext().getTheme().resolveAttribute(R.attr.colorPrimary, value, true)) {
                return value.data;
            }

            return 0xFF4A90E2;
        }

        private int resolveOnSurfaceVariantColor() {
            android.util.TypedValue value = new android.util.TypedValue();

            if (itemView.getContext().getTheme().resolveAttribute(R.attr.colorOnSurfaceVariant, value, true)) {
                return value.data;
            }

            return 0xFF777777;
        }

        void setClickHandlers(MessageChat messageChat, OnChatClickListener listener) {
            itemView.setOnTouchListener((view, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        UiAnimations.animatePress(view, true);
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        UiAnimations.animatePress(view, false);
                        break;

                    default:
                        break;
                }

                return false;
            });

            itemView.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onChatClick(messageChat.getChat());
                }
            });

            itemView.setOnLongClickListener(view -> {
                if (listener != null) {
                    listener.onChatLongClick(messageChat.getChat());
                }

                return true;
            });
        }

        void resetTransientState() {
            itemView.setOnTouchListener(null);
            itemView.setOnClickListener(null);
            itemView.setOnLongClickListener(null);
            if (circlePreviewRoot != null) circlePreviewRoot.setOnClickListener(null);

            stopLocalPreview(true);

            tvUnreadCount.animate().cancel();
            tvUnreadCount.setAlpha(1f);
            tvUnreadCount.setScaleX(1f);
            tvUnreadCount.setScaleY(1f);
            tvLastMessage.setTypeface(null, Typeface.NORMAL);

            if (tvChatCircleInitials != null) {
                tvChatCircleInitials.setVisibility(View.GONE);
                tvChatCircleInitials.setText("");
                tvChatCircleInitials.setAlpha(1f);
            }

            lastCirclePreviewInfo = null;
        }

        @Nullable
        private static CirclePreviewInfo findCirclePreviewInfo(@Nullable Message message) {
            if (message == null || message.getFiles() == null || message.getFiles().isEmpty()) {
                return null;
            }

            Map<UUID, String> names = extractFileNames(message.getText(), message.getFiles());
            Map<UUID, FileType> types = message.getFileTypes();
            Map<UUID, String> mimes = message.getFileMimeTypes();

            for (int i = message.getFiles().size() - 1; i >= 0; i--) {
                UUID fileId = message.getFiles().get(i);
                if (fileId == null) continue;

                com.example.aichat.model.entities.File entity = null;

                try {
                    entity = DatabaseManager.getDatabase().fileDao().getById(fileId);
                } catch (Exception ignored) {
                }

                String name = names.get(fileId);

                if ((name == null || name.trim().isEmpty()) && entity != null) {
                    name = entity.fileName;
                }

                if ((name == null || name.trim().isEmpty()) && entity != null && entity.localPath != null) {
                    int slash = entity.localPath.lastIndexOf('/');
                    name = slash >= 0 ? entity.localPath.substring(slash + 1) : entity.localPath;
                }

                FileType type = types != null ? types.get(fileId) : null;
                String mime = mimes != null ? mimes.get(fileId) : null;

                boolean isVideo = type == FileType.VideoMessage
                        || (mime != null && mime.startsWith("video"))
                        || isVideoFile(name);

                if (!isVideo || !ChatMediaMarkers.isCircleVideoFileName(name)) {
                    continue;
                }

                File localFile = null;

                if (entity != null && entity.localPath != null && !entity.localPath.trim().isEmpty()) {
                    localFile = new File(entity.localPath);
                }

                return new CirclePreviewInfo(fileId, name, localFile);
            }

            return null;
        }

        @NonNull
        private static Map<UUID, String> extractFileNames(String text, List<UUID> fileIds) {
            Map<UUID, String> result = new HashMap<>();

            if (text == null || text.isEmpty() || fileIds == null) return result;

            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[file:([^\\]]*)\\]");
            java.util.regex.Matcher matcher = pattern.matcher(text);

            int index = 0;

            while (matcher.find()) {
                String payload = matcher.group(1);

                if (payload == null) {
                    continue;
                }

                payload = payload.trim();

                String displayName = ChatMediaMarkers.extractDisplayNameFromMarkerPayload(payload);

                if (payload.length() > 36 && payload.charAt(36) == ':') {
                    try {
                        UUID markerFileId = UUID.fromString(payload.substring(0, 36));

                        if (fileIds.contains(markerFileId) && !displayName.isEmpty()) {
                            result.put(markerFileId, displayName);
                            continue;
                        }
                    } catch (Exception ignored) {
                    }
                }

                while (index < fileIds.size() && result.containsKey(fileIds.get(index))) {
                    index++;
                }

                if (index < fileIds.size() && !displayName.isEmpty()) {
                    result.put(fileIds.get(index), displayName);
                    index++;
                }
            }

            return result;
        }

        private static boolean isVideoFile(@Nullable String fileName) {
            if (fileName == null) return false;

            String lower = fileName.trim().toLowerCase();

            return lower.endsWith(".mp4")
                    || lower.endsWith(".m4v")
                    || lower.endsWith(".mov")
                    || lower.endsWith(".mkv")
                    || lower.endsWith(".webm")
                    || lower.endsWith(".avi")
                    || lower.endsWith(".3gp")
                    || lower.endsWith(".3gpp");
        }
    }

    static class PlaceholderViewHolder extends RecyclerView.ViewHolder {

        PlaceholderViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private static final class CirclePreviewInfo {
        final UUID fileId;
        final String fileName;
        final File localFile;

        CirclePreviewInfo(@NonNull UUID fileId, @Nullable String fileName, @Nullable File localFile) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.localFile = localFile;
        }
    }
}
