package com.example.aichat.view.main.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.aichat.BuildConfig;
import com.example.aichat.R;
import com.example.aichat.controller.main.chat.actions.ChatMessageActions;
import com.example.aichat.controller.main.chat.MessageController;
import com.example.aichat.model.connection.TokenStorage;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.FileType;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.model.utils.AudioPlayerManager;
import com.example.aichat.model.utils.FileDownloadProgressManager;
import com.example.aichat.model.utils.GlideAuthHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.noties.markwon.Markwon;

@androidx.media3.common.util.UnstableApi
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>
        implements ReplyRenderer.MessageFinder, ReplyRenderer.ReplyClickListener {

    private final List<Message> messages;
    private final UUID userId;

    private UUID activeAudioFileId;
    private final Markwon markwon;
    private final ReplyRenderer replyRenderer;

    public AudioPlayerManager getAudioPlayerManager() {
        return audioPlayerManager;
    }

    private FileDownloadProgressManager progressManager;
    private RecyclerView recyclerView;
    private OnMessageActionListener actionListener;

    private final Map<UUID, com.example.aichat.model.entities.File> fileCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    private final Map<UUID, Boolean> loadingFiles =
            new java.util.concurrent.ConcurrentHashMap<>();
    private OnFileDownloadListener fileDownloadListener;

    private final Map<UUID, String> localFileNames = new HashMap<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable readMessagesRunnable;
    private static final long READ_DELAY_MS = 1000;

    private AudioPlayerManager audioPlayerManager;
    private final MessageController messageController;
    private UUID highlightedMessageId = null;

    public interface OnFileDownloadListener {
        void onFileDownloadRequired(
                String url,
                String mimeType,
                UUID fileId,
                FileType fileType,
                boolean openAfterDownload
        );
    }
    public interface OnMessageActionListener {
        void onEditMessage(Message message);
        void onDeleteMessage(Message message);
        void onReplyToMessage(Message message);
        void onCopyMessageText(Message message);
    }

    public void setOnFileDownloadListener(OnFileDownloadListener listener) {
        this.fileDownloadListener = listener;
    }

    public MessageAdapter(UUID currentUserId,
                          List<Message> messages,
                          Context context,
                          FileDownloadProgressManager progressManager) {

        setHasStableIds(true);

        this.userId = currentUserId;
        this.messages = messages != null ? messages : new ArrayList<>();
        this.markwon = Markwon.create(context);
        this.replyRenderer = new ReplyRenderer(context, this, this);
        this.messageController = new MessageController(currentUserId);
        this.progressManager = progressManager;

        preloadDownloadedFiles();

        if (progressManager != null) {

            progressManager.addListener(
                    new FileDownloadProgressManager.Listener() {

                        @Override
                        public void onProgress(
                                UUID fileId,
                                int progress
                        ) {

                            updateItemByFileId(fileId);
                        }

                        @Override
                        public void onCompleted(UUID fileId) {

                            new Thread(() -> {

                                com.example.aichat.model.entities.File entity = null;

                                for (int i = 0; i < 20; i++) {

                                    entity = DatabaseManager.getDatabase()
                                            .fileDao()
                                            .getById(fileId);

                                    if (entity != null &&
                                            entity.localPath != null &&
                                            !entity.localPath.isEmpty()) {

                                        File localFile =
                                                new File(entity.localPath);

                                        if (localFile.exists()) {
                                            break;
                                        }
                                    }

                                    entity = null;

                                    try {
                                        Thread.sleep(150);
                                    } catch (InterruptedException ignored) {
                                    }
                                }

                                if (entity == null) {
                                    return;
                                }

                                File localFile =
                                        new File(entity.localPath);

                                if (!localFile.exists()) {
                                    return;
                                }

                                fileCache.put(fileId, entity);

                                int pos =
                                        findMessagePositionByFileId(fileId);

                                if (pos == -1) {
                                    return;
                                }

                                if (recyclerView == null) {
                                    return;
                                }

                                com.example.aichat.model.entities.File finalEntity =
                                        entity;

                                recyclerView.post(() -> {

                                    notifyItemChanged(pos);

                                    if (audioPlayerManager != null &&
                                            audioPlayerManager.shouldAutoPlay(fileId)) {

                                        audioPlayerManager.clearPendingPlay();

                                        audioPlayerManager.playLocal(
                                                recyclerView.getContext(),
                                                finalEntity.localPath,
                                                fileId
                                        );
                                    }
                                });

                            }).start();
                        }

                        @Override
                        public void onError(UUID fileId) {

                            int pos =
                                    findMessagePositionByFileId(fileId);

                            if (pos == -1) {
                                return;
                            }

                            if (recyclerView != null) {

                                recyclerView.post(() ->
                                        notifyItemChanged(pos)
                                );

                                return;
                            }

                            notifyItemChanged(pos);
                        }

                        @Override
                        public void onCleared() {

                            fileCache.clear();

                            if (recyclerView != null) {

                                recyclerView.post(
                                        MessageAdapter.this::notifyDataSetChanged
                                );

                                return;
                            }

                            notifyDataSetChanged();
                        }
                    }
            );
        }
    }

    @Override
    public long getItemId(int position) {

        return messages.get(position)
                .getId()
                .getMostSignificantBits();
    }

    private void updateVisibleAudioProgress(UUID fileId) {

        if (recyclerView == null) {
            return;
        }

        for (int i = 0; i < recyclerView.getChildCount(); i++) {

            View child =
                    recyclerView.getChildAt(i);

            RecyclerView.ViewHolder holder =
                    recyclerView.getChildViewHolder(child);

            if (!(holder instanceof MessageViewHolder)) {
                continue;
            }

            ((MessageViewHolder) holder)
                    .updateAudioProgress(fileId);
        }
    }
    public void detachPlayer() {

        if (audioPlayerManager != null) {
            audioPlayerManager.detach();
        }
    }
    private int findMessagePositionByFileId(UUID fileId) {
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            if (m.getFiles() != null && m.getFiles().contains(fileId)) {
                return i;
            }
        }
        return -1;
    }

    private String resolveFileName(UUID fileId,
                                   com.example.aichat.model.entities.File entity,
                                   String url) {

        String local = localFileNames.get(fileId);
        if (local != null && !local.isEmpty()) {
            return local;
        }

        if (entity != null && entity.fileName != null && !entity.fileName.isEmpty()) {
            return entity.fileName;
        }

        if (entity != null && entity.localPath != null) {
            int index = entity.localPath.lastIndexOf('/');
            if (index != -1 && index < entity.localPath.length() - 1) {
                return entity.localPath.substring(index + 1);
            }
        }

        if (url != null) {
            int index = url.lastIndexOf('/');
            if (index != -1 && index < url.length() - 1) {
                return url.substring(index + 1);
            }
        }

        return "Файл";
    }

    private void updateItemByFileId(UUID fileId) {
        int position = findMessagePositionByFileId(fileId);

        if (position == -1) return;

        if (recyclerView != null) {
            recyclerView.post(() -> notifyItemChanged(position, fileId));
        } else {
            notifyItemChanged(position, fileId);
        }
    }
    @Override
    public Message findMessageById(UUID id) {
        for (Message m : messages) {
            if (m.getId().equals(id)) return m;
        }
        return null;
    }

    public void setAudioPlayerManager(AudioPlayerManager manager) {

        this.audioPlayerManager = manager;

        if (audioPlayerManager == null) {
            return;
        }

        audioPlayerManager.setListener(new AudioPlayerManager.Listener() {

            @Override
            public void onStart(UUID fileId) {

                activeAudioFileId = fileId;

                int position =
                        findMessagePositionByFileId(fileId);

                if (position == -1 || recyclerView == null) {
                    return;
                }

                recyclerView.post(() ->
                        notifyItemChanged(position, fileId)
                );
            }

            @Override
            public void onStop(UUID fileId) {

                if (fileId.equals(activeAudioFileId)) {
                    activeAudioFileId = null;
                }

                int position =
                        findMessagePositionByFileId(fileId);

                if (position == -1 || recyclerView == null) {
                    return;
                }

                recyclerView.post(() ->
                        notifyItemChanged(position, fileId)
                );
            }

            @Override
            public void onProgress(UUID fileId) {

                updateVisibleAudioProgress(fileId);
            }
        });
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

    private Map<UUID, String> extractFileNames(String text, List<UUID> fileIds) {
        Map<UUID, String> result = new HashMap<>();

        if (text == null || text.isEmpty() || fileIds == null) return result;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[file:(.*?)]");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        int index = 0;

        while (matcher.find() && index < fileIds.size()) {
            result.put(fileIds.get(index), matcher.group(1));
            index++;
        }

        return result;
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

    public void setMessages(List<Message> messages) {
        this.messages.clear();
        this.messages.addAll(messages);
        notifyDataSetChanged();
    }

    public void updateMessages(List<Message> newMessages) {

        this.messages.clear();

        if (newMessages != null) {
            this.messages.addAll(newMessages);
        }

        notifyDataSetChanged();
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
        return itemView.getBottom() <= recyclerView.getHeight();
    }

    private void readMessages(List<Message> msgs, ChatMessageActions actions) {
        for (Message msg : msgs) {
            if (msg.getStatuses() != null) {
                msg.getStatuses().replace(userId, MessageStatus.READ);
            }
            int pos = getMessagePosition(msg.getId());
            if (pos != -1) {
                if (recyclerView != null) {
                    recyclerView.post(() -> {
                        if (recyclerView != null) {
                            notifyItemChanged(pos);
                        }
                    });
                } else {
                    notifyItemChanged(pos);
                }
            }
        }
        actions.readMessages(msgs);
    }

    private void applyFileState(
            ProgressBar progressBar,
            TextView progressText,
            ImageView status,
            Integer progress,
            boolean isDownloaded
    ) {

        if (!isDownloaded &&
                progress != null &&
                progress < 100) {

            if (progressText != null) {

                progressText.setVisibility(View.VISIBLE);

                progressText.setText(progress + "%");
            }

            if (status != null) {
                status.setVisibility(View.GONE);
            }

            return;
        }

        if (isDownloaded) {

            if (progressText != null) {
                progressText.setVisibility(View.GONE);
            }

            if (status != null) {
                status.setVisibility(View.VISIBLE);
            }

            return;
        }

        if (progressText != null) {
            progressText.setVisibility(View.GONE);
        }

        if (status != null) {
            status.setVisibility(View.GONE);
        }
    }

    public void clearFileCache() {
        fileCache.clear();
        notifyDataSetChanged();
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

    public void setLocalFileName(UUID fileId, String name) {
        localFileNames.put(fileId, name);
        updateItemByFileId(fileId);
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

        int pos = getMessagePosition(id);

        if (pos != -1) {
            if (recyclerView != null) {
                recyclerView.post(() -> notifyItemChanged(pos));
            } else {
                notifyItemChanged(pos);
            }
        }
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
    public void onBindViewHolder(
            @NonNull MessageViewHolder holder,
            int position,
            @NonNull List<Object> payloads
    ) {

        if (!payloads.isEmpty()) {

            Object payload = payloads.get(0);

            if (payload instanceof UUID) {

                UUID fileId = (UUID) payload;

                holder.updateFileProgress(fileId);

                holder.updateAudioProgress(fileId);

                return;
            }
        }

        onBindViewHolder(holder, position);
    }

    private String removeFileTags(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.replaceAll("\\[file:.*?]", "").replaceAll("\\s+", " ").trim();
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message m = messages.get(position);
        holder.bind(m);

        if (highlightedMessageId != null && highlightedMessageId.equals(m.getId())) {
            holder.itemView.setBackgroundColor(0xFFFFF2AA);
        } else {
            holder.itemView.setBackgroundColor(0x00000000);
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

    public void setProgressManager(FileDownloadProgressManager manager) {
        this.progressManager = manager;
    }

    private void preloadDownloadedFiles() {

        new Thread(() -> {

            List<com.example.aichat.model.entities.File> files =
                    DatabaseManager.getDatabase()
                            .fileDao()
                            .getAll();

            if (files == null) {
                return;
            }

            for (com.example.aichat.model.entities.File file : files) {

                if (file.localPath == null) {
                    continue;
                }

                File localFile =
                        new File(file.localPath);

                if (!localFile.exists()) {

                    DatabaseManager.getDatabase()
                            .fileDao()
                            .delete(file.fileId);

                    continue;
                }

                fileCache.put(file.fileId, file);
            }

            handler.post(this::notifyDataSetChanged);

        }).start();
    }
    private void loadFileFromDb(UUID fileId) {

        if (fileCache.containsKey(fileId)) {
            return;
        }

        if (Boolean.TRUE.equals(loadingFiles.get(fileId))) {
            return;
        }

        loadingFiles.put(fileId, true);

        new Thread(() -> {

            com.example.aichat.model.entities.File entity =
                    DatabaseManager.getDatabase()
                            .fileDao()
                            .getById(fileId);

            loadingFiles.remove(fileId);

            if (entity == null) {
                return;
            }

            if (entity.localPath == null ||
                    entity.localPath.isEmpty()) {

                return;
            }

            File localFile =
                    new File(entity.localPath);

            if (!localFile.exists()) {

                DatabaseManager.getDatabase()
                        .fileDao()
                        .delete(fileId);

                return;
            }

            fileCache.put(fileId, entity);

            int pos =
                    findMessagePositionByFileId(fileId);

            if (pos != -1) {

                handler.post(() ->
                        notifyItemChanged(pos)
                );
            }

        }).start();
    }


    class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView messageText;
        private final TextView timeText;
        private final ImageView statusIcon;
        private final LinearLayout replyContainer;
        private final LinearLayout filesContainer;
        private View filesScroll;

        private Message message;
        private final boolean isMyMessage;
        private final RequestOptions glideOptions;

        MessageViewHolder(@NonNull View itemView, boolean isMyMessage) {
            super(itemView);

            this.isMyMessage = isMyMessage;

            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
            replyContainer = itemView.findViewById(R.id.reply_container);
            filesContainer = itemView.findViewById(R.id.files_container);
            filesScroll = itemView.findViewById(R.id.files_scroll);
            statusIcon = isMyMessage ? itemView.findViewById(R.id.status_icon) : null;

            messageText.setMovementMethod(LinkMovementMethod.getInstance());

            glideOptions = new RequestOptions()
                    .placeholder(R.drawable.ic_file)
                    .error(R.drawable.ic_file)
                    .centerCrop();

            itemView.setOnLongClickListener(v -> {
                if (message != null && actionListener != null) {
                    showMessageMenu(v);
                    return true;
                }
                return false;
            });
        }



        public int getImageIndex(UUID targetFileId) {
            int index = 0;

            for (Message msg : messages) {
                if (msg.getFiles() == null) continue;

                Map<UUID, FileType> types = msg.getFileTypes();
                Map<UUID, String> mimes = msg.getFileMimeTypes();

                for (UUID id : msg.getFiles()) {
                    FileType t = types != null ? types.get(id) : null;
                    String mime = mimes != null ? mimes.get(id) : null;

                    String name = resolveFileName(id, fileCache.get(id), "/api/files/" + id);

                    boolean isImage =
                            (mime != null && mime.startsWith("image"))
                                    || t == FileType.MessageImage
                                    || (mime == null && t == null && isImageFile(name));

                    if (!isImage) continue;

                    if (id.equals(targetFileId)) return index;

                    index++;
                }
            }

            return 0;
        }


        public List<String> getAllImageUrls() {
            List<String> result = new ArrayList<>();

            for (Message msg : messages) {
                if (msg.getFiles() == null) continue;

                Map<UUID, FileType> types = msg.getFileTypes();
                Map<UUID, String> mimes = msg.getFileMimeTypes();

                for (UUID id : msg.getFiles()) {
                    FileType t = types != null ? types.get(id) : null;
                    String mime = mimes != null ? mimes.get(id) : null;

                    boolean isImage =
                            (mime != null && mime.startsWith("image"))
                                    || t == FileType.MessageImage;

                    if (!isImage) continue;

                    result.add(BuildConfig.SERVER_URL + "/api/files/" + id);
                }
            }

            return result;
        }

        private boolean isAudioFile(String name) {
            if (name == null) return false;
            String lower = name.toLowerCase();
            return lower.endsWith(".mp3")
                    || lower.endsWith(".wav")
                    || lower.endsWith(".ogg")
                    || lower.endsWith(".m4a");
        }



        private void showMessageMenu(View anchor) {
            PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
            menu.inflate(R.menu.message_context_menu);

            menu.setOnMenuItemClickListener(item -> {
                if (message == null) return false;

                if (item.getItemId() == R.id.menu_edit_message) {
                    actionListener.onEditMessage(message);
                    return true;
                }
                if (item.getItemId() == R.id.menu_delete_message) {
                    actionListener.onDeleteMessage(message);
                    return true;
                }
                if (item.getItemId() == R.id.menu_reply_message) {
                    actionListener.onReplyToMessage(message);
                    return true;
                }
                if (item.getItemId() == R.id.menu_copy_text) {
                    actionListener.onCopyMessageText(message);
                    return true;
                }

                return false;
            });

            menu.show();
        }

        void bind(Message m) {

            this.message = m;

            replyContainer.removeAllViews();
            filesContainer.removeAllViews();

            Map<UUID, String> parsedNames =
                    extractFileNames(m.getText(), m.getFiles());

            String cleanedText = removeFileTags(m.getText());

            boolean hasText =
                    cleanedText != null &&
                            !cleanedText.trim().isEmpty();

            boolean hasFiles =
                    m.getFiles() != null &&
                            !m.getFiles().isEmpty();

            messageText.setVisibility(
                    hasText ? View.VISIBLE : View.GONE
            );

            if (hasText) {
                markwon.setMarkdown(messageText, cleanedText);
            }

            if (filesScroll != null) {

                filesScroll.setVisibility(
                        hasFiles ? View.VISIBLE : View.GONE
                );
            }

            if (hasFiles) {

                filesContainer.setVisibility(View.VISIBLE);

                filesContainer.setOrientation(
                        LinearLayout.HORIZONTAL
                );

                Map<UUID, FileType> fileTypes =
                        m.getFileTypes();

                Map<UUID, String> mimeMap =
                        m.getFileMimeTypes();

                LayoutInflater inflater =
                        LayoutInflater.from(itemView.getContext());

                for (int i = 0; i < m.getFiles().size(); i++) {

                    UUID fileId = m.getFiles().get(i);

                    String url = "/api/files/" + fileId;

                    String fullUrl =
                            BuildConfig.SERVER_URL + url;

                    FileType fileType =
                            fileTypes != null
                                    ? fileTypes.get(fileId)
                                    : null;

                    String mimeType =
                            mimeMap != null
                                    ? mimeMap.get(fileId)
                                    : null;

                    com.example.aichat.model.entities.File entity =
                            fileCache.get(fileId);

                    if (entity == null) {

                        loadFileFromDb(fileId);
                    }

                    if (entity != null &&
                            (entity.localPath == null ||
                                    !new File(entity.localPath).exists())) {

                        fileCache.remove(fileId);

                        entity = null;
                    }

                    final com.example.aichat.model.entities.File finalEntity =
                            entity;

                    String nameFromText =
                            parsedNames.get(fileId);

                    String name =
                            nameFromText != null
                                    ? nameFromText
                                    : resolveFileName(
                                    fileId,
                                    entity,
                                    url
                            );

                    boolean isImage =
                            fileType == FileType.MessageImage ||
                                    (mimeType != null &&
                                            mimeType.startsWith("image")) ||
                                    isImageFile(name);

                    boolean isAudio =
                            fileType == FileType.VoiceMessage ||
                                    (mimeType != null &&
                                            mimeType.startsWith("audio")) ||
                                    isAudioFile(name);

                    Integer progress =
                            progressManager != null
                                    ? progressManager.getProgress(fileId)
                                    : null;

                    boolean isDownloaded =
                            entity != null &&
                                    entity.localPath != null &&
                                    new File(entity.localPath).exists();

                    boolean isLoading =
                            progress != null &&
                                    progress >= 0 &&
                                    progress < 100;

                    if (isImage) {

                        View imageView =
                                inflater.inflate(
                                        R.layout.item_chat_image,
                                        filesContainer,
                                        false
                                );

                        ImageView image =
                                imageView.findViewById(R.id.chat_image);


                        if (finalEntity != null &&
                                finalEntity.localPath != null &&
                                new File(finalEntity.localPath).exists()) {

                            Glide.with(image)
                                    .load(new File(finalEntity.localPath))
                                    .apply(glideOptions)
                                    .thumbnail(0.25f)
                                    .into(image);

                        } else {


                            Glide.with(image)
                                    .load(
                                            GlideAuthHelper.build(
                                                    fullUrl,
                                                    image.getContext()
                                            )
                                    )
                                    .apply(glideOptions)
                                    .thumbnail(0.25f)
                                    .into(image);
                        }

                        image.setOnClickListener(v -> {

                            Context ctx = v.getContext();

                            ArrayList<String> imageUrls =
                                    new ArrayList<>();

                            int clickedIndex = 0;

                            for (Message msg : messages) {

                                if (msg.getFiles() == null) {
                                    continue;
                                }

                                Map<UUID, String> msgParsedNames =
                                        extractFileNames(
                                                msg.getText(),
                                                msg.getFiles()
                                        );

                                Map<UUID, FileType> msgTypes =
                                        msg.getFileTypes();

                                Map<UUID, String> msgMimes =
                                        msg.getFileMimeTypes();

                                for (UUID id : msg.getFiles()) {

                                    com.example.aichat.model.entities.File entityLocal =
                                            fileCache.get(id);

                                    String nameInner =
                                            msgParsedNames.get(id);

                                    if (nameInner == null) {

                                        nameInner = resolveFileName(
                                                id,
                                                entityLocal,
                                                "/api/files/" + id
                                        );
                                    }

                                    FileType type =
                                            msgTypes != null
                                                    ? msgTypes.get(id)
                                                    : null;

                                    String mime =
                                            msgMimes != null
                                                    ? msgMimes.get(id)
                                                    : null;

                                    boolean isImageLocal =
                                            type == FileType.MessageImage ||
                                                    (mime != null &&
                                                            mime.startsWith("image")) ||
                                                    isImageFile(nameInner);

                                    if (!isImageLocal) {
                                        continue;
                                    }

                                    String urlItem =
                                            BuildConfig.SERVER_URL +
                                                    "/api/files/" + id;

                                    if (id.equals(fileId)) {
                                        clickedIndex = imageUrls.size();
                                    }

                                    imageUrls.add(urlItem);
                                }
                            }

                            if (imageUrls.isEmpty()) {

                                imageUrls.add(fullUrl);

                                clickedIndex = 0;
                            }

                            Intent intent =
                                    new Intent(
                                            ctx,
                                            com.example.aichat.ImagePreviewActivity.class
                                    );

                            intent.putStringArrayListExtra(
                                    "urls",
                                    imageUrls
                            );

                            intent.putExtra(
                                    "index",
                                    clickedIndex
                            );

                            ctx.startActivity(intent);
                        });

                        filesContainer.addView(imageView);

                    } else {

                        View fileView =
                                inflater.inflate(
                                        R.layout.item_chat_file,
                                        filesContainer,
                                        false
                                );

                        fileView.setTag(fileId);

                        ImageView playIcon =
                                fileView.findViewById(R.id.play_icon);

                        ImageView menuBtn =
                                fileView.findViewById(R.id.file_menu);

                        TextView fileName =
                                fileView.findViewById(R.id.file_name);

                        TextView fileExt =
                                fileView.findViewById(R.id.file_ext);

                        TextView progressText =
                                fileView.findViewById(R.id.file_progress_text);

                        ImageView status =
                                fileView.findViewById(R.id.file_status);

                        View audioContainer =
                                fileView.findViewById(R.id.audio_container);

                        SeekBar seekBar =
                                fileView.findViewById(R.id.audio_seekbar);

                        TextView currentTime =
                                fileView.findViewById(R.id.audio_current_time);

                        TextView totalTime =
                                fileView.findViewById(R.id.audio_total_time);

                        View metaContainer =
                                fileView.findViewById(R.id.file_meta_container);

                        fileName.setText(name);

                        fileExt.setText(
                                getFileExtension(
                                        mimeType,
                                        fileType,
                                        name
                                )
                        );

                        progressText.setVisibility(
                                isLoading
                                        ? View.VISIBLE
                                        : View.GONE
                        );

                        status.setVisibility(
                                isDownloaded
                                        ? View.VISIBLE
                                        : View.GONE
                        );

                        if (isLoading && progress != null) {
                            progressText.setText(progress + "%");
                        }

                        menuBtn.setOnClickListener(v -> {

                            PopupMenu menu =
                                    new PopupMenu(v.getContext(), v);

                            menu.getMenu().add("Открыть с помощью");

                            menu.setOnMenuItemClickListener(item -> {

                                if (finalEntity != null &&
                                        finalEntity.localPath != null &&
                                        new File(finalEntity.localPath).exists()) {

                                    openFileExternal(
                                            v.getContext(),
                                            finalEntity
                                    );

                                } else if (fileDownloadListener != null) {

                                    if (progressManager != null) {

                                        progressManager.updateProgress(
                                                fileId,
                                                0
                                        );
                                    }

                                    fileDownloadListener.onFileDownloadRequired(
                                            url,
                                            mimeType,
                                            fileId,
                                            fileType,
                                            true
                                    );
                                }

                                return true;
                            });

                            menu.show();
                        });

                        if (isAudio) {

                            audioContainer.setVisibility(View.VISIBLE);

                            metaContainer.setVisibility(View.GONE);

                            boolean isPlaying =
                                    audioPlayerManager != null &&
                                            audioPlayerManager.isPlaying(fileId);

                            boolean isPreparing =
                                    audioPlayerManager != null &&
                                            audioPlayerManager.isPreparing(fileId);

                            boolean isPaused =
                                    audioPlayerManager != null &&
                                            audioPlayerManager.isPaused(fileId);

                            playIcon.setVisibility(View.VISIBLE);

                            playIcon.setImageResource(
                                    (isPlaying || isPreparing)
                                            ? R.drawable.ic_pause
                                            : R.drawable.ic_play_circle
                            );

                            int duration =
                                    audioPlayerManager != null
                                            ? audioPlayerManager.getDuration(fileId)
                                            : 0;

                            int position =
                                    audioPlayerManager != null
                                            ? audioPlayerManager.getCurrentPosition(fileId)
                                            : 0;

                            if (duration > 0) {

                                seekBar.setProgress(
                                        (int) (
                                                position * 100f / duration
                                        )
                                );

                                currentTime.setText(
                                        formatTime(position)
                                );

                                totalTime.setText(
                                        formatTime(duration)
                                );

                            } else {

                                seekBar.setProgress(0);

                                currentTime.setText("0:00");

                                totalTime.setText("0:00");
                            }

                            seekBar.setOnSeekBarChangeListener(
                                    new SeekBar.OnSeekBarChangeListener() {

                                        @Override
                                        public void onProgressChanged(
                                                SeekBar seekBar,
                                                int progress,
                                                boolean fromUser
                                        ) {

                                            if (!fromUser ||
                                                    audioPlayerManager == null) {

                                                return;
                                            }

                                            if (
                                                    !audioPlayerManager.isPlaying(fileId) &&
                                                            !audioPlayerManager.isPaused(fileId)
                                            ) {

                                                return;
                                            }

                                            int duration =
                                                    audioPlayerManager.getDuration(fileId);

                                            int newPosition =
                                                    (int) (
                                                            duration *
                                                                    (progress / 100f)
                                                    );

                                            audioPlayerManager.seekTo(newPosition);
                                        }

                                        @Override
                                        public void onStartTrackingTouch(
                                                SeekBar seekBar
                                        ) {
                                        }

                                        @Override
                                        public void onStopTrackingTouch(
                                                SeekBar seekBar
                                        ) {
                                        }
                                    }
                            );

                            playIcon.setOnClickListener(v -> {

                                if (audioPlayerManager == null) {
                                    return;
                                }

                                if (audioPlayerManager.isPlaying(fileId)) {

                                    audioPlayerManager.pause();

                                    updateItemByFileId(fileId);

                                    return;
                                }

                                if (audioPlayerManager.isPaused(fileId)) {

                                    audioPlayerManager.resume();

                                    updateItemByFileId(fileId);

                                    return;
                                }

                                if (finalEntity != null &&
                                        finalEntity.localPath != null) {

                                    File localFile =
                                            new File(finalEntity.localPath);

                                    if (localFile.exists()) {

                                        audioPlayerManager.clearPendingPlay();

                                        audioPlayerManager.playLocal(
                                                v.getContext(),
                                                finalEntity.localPath,
                                                fileId
                                        );

                                        updateItemByFileId(fileId);

                                        return;
                                    }
                                }

                                if (audioPlayerManager.shouldAutoPlay(fileId)) {
                                    return;
                                }

                                audioPlayerManager.requestPlayAfterDownload(fileId);

                                if (progressManager != null) {

                                    progressManager.updateProgress(
                                            fileId,
                                            0
                                    );
                                }

                                if (fileDownloadListener != null) {

                                    fileDownloadListener.onFileDownloadRequired(
                                            url,
                                            mimeType,
                                            fileId,
                                            fileType,
                                            false
                                    );
                                }

                                progressText.setVisibility(View.VISIBLE);

                                progressText.setText("0%");

                                updateItemByFileId(fileId);
                            });

                        } else {

                            audioContainer.setVisibility(View.GONE);

                            metaContainer.setVisibility(View.VISIBLE);

                            playIcon.setVisibility(View.VISIBLE);

                            playIcon.setOnClickListener(null);

                            playIcon.setImageResource(
                                    getFileIconResource(
                                            mimeType,
                                            fileType,
                                            name
                                    )
                            );
                        }

                        filesContainer.addView(fileView);
                    }
                }

            } else {

                filesContainer.setVisibility(View.GONE);
            }

            replyRenderer.renderReplies(
                    replyContainer,
                    m.getReplyMessages()
            );

            timeText.setText(
                    MessageController.getFormattedMessageTime(m)
            );

            updateStatus();
        }
        private boolean isImageFile(String name) {
            if (name == null) return false;

            String lower = name.toLowerCase();

            return lower.endsWith(".jpg") ||
                    lower.endsWith(".jpeg") ||
                    lower.endsWith(".png") ||
                    lower.endsWith(".webp");
        }

        private void openFileExternal(Context context, com.example.aichat.model.entities.File file) {
            if (file == null || file.localPath == null) return;

            File f = new File(file.localPath);
            if (!f.exists()) return;

            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    f
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(intent, "Открыть через"));
        }
        private String formatTime(long millis) {
            if (millis <= 0) return "0:00";

            int totalSec = (int) (millis / 1000);
            int min = totalSec / 60;
            int sec = totalSec % 60;

            return String.format("%d:%02d", min, sec);
        }

        void updateFileProgress(UUID fileId) {

            for (int i = 0; i < filesContainer.getChildCount(); i++) {

                View fileView = filesContainer.getChildAt(i);

                if (!fileId.equals(fileView.getTag())) continue;

                ProgressBar progressBar = null;
                TextView progressText = fileView.findViewById(R.id.file_progress_text);
                ImageView status = fileView.findViewById(R.id.file_status);

                Integer progress = progressManager != null
                        ? progressManager.getProgress(fileId)
                        : null;

                com.example.aichat.model.entities.File entity = fileCache.get(fileId);

                if (entity != null && !new File(entity.localPath).exists()) {
                    fileCache.remove(fileId);
                    entity = null;
                }

                boolean isDownloaded =
                        entity != null &&
                                entity.localPath != null &&
                                new File(entity.localPath).exists();

                applyFileState(progressBar, progressText, status, progress, isDownloaded);

                break;
            }
        }

        void updateAudioProgress(UUID fileId) {

            for (int i = 0; i < filesContainer.getChildCount(); i++) {

                View fileView = filesContainer.getChildAt(i);

                if (!fileId.equals(fileView.getTag())) {
                    continue;
                }

                SeekBar seekBar =
                        fileView.findViewById(R.id.audio_seekbar);

                TextView currentTime =
                        fileView.findViewById(R.id.audio_current_time);

                TextView totalTime =
                        fileView.findViewById(R.id.audio_total_time);

                ImageView playIcon =
                        fileView.findViewById(R.id.play_icon);

                if (audioPlayerManager == null) {
                    return;
                }

                boolean isPlaying =
                        audioPlayerManager.isPlaying(fileId);

                boolean isPreparing =
                        audioPlayerManager.isPreparing(fileId);

                playIcon.setImageResource(
                        (isPlaying || isPreparing)
                                ? R.drawable.ic_pause
                                : R.drawable.ic_play_circle
                );

                int duration =
                        audioPlayerManager.getDuration(fileId);

                int position =
                        audioPlayerManager.getCurrentPosition(fileId);

                if (duration > 0) {

                    int progress =
                            (int) (position * 100f / duration);

                    if (!seekBar.isPressed()) {

                        seekBar.setProgress(progress);
                    }

                    currentTime.setText(
                            formatTime(position)
                    );

                    totalTime.setText(
                            formatTime(duration)
                    );

                } else {

                    seekBar.setProgress(0);

                    currentTime.setText("0:00");

                    totalTime.setText("0:00");
                }

                break;
            }
        }




        private String getFileExtension(String mimeType, FileType fileType, String fileName) {

            if (mimeType != null) {
                if (mimeType.contains("image")) return "IMG";
                if (mimeType.contains("video")) return "VID";
                if (mimeType.contains("pdf")) return "PDF";
                if (mimeType.contains("word")) return "DOC";
                if (mimeType.contains("excel")) return "XLS";
                if (mimeType.contains("zip")) return "ZIP";
                if (mimeType.contains("audio")) return "AUDIO";
                if (mimeType.contains("text")) return "TXT";
            }

            if (fileName != null && fileName.contains(".")) {
                String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

                switch (ext) {
                    case "jpg":
                    case "jpeg":
                    case "png":
                    case "webp":
                        return "IMG";
                    case "mp4":
                    case "mkv":
                    case "webm":
                        return "VID";
                    case "pdf":
                        return "PDF";
                    case "doc":
                    case "docx":
                        return "DOC";
                    case "xls":
                    case "xlsx":
                        return "XLS";
                    case "zip":
                    case "rar":
                        return "ZIP";
                    case "mp3":
                    case "wav":
                        return "AUDIO";
                    case "txt":
                        return "TXT";
                }
            }

            if (fileType != null) {
                switch (fileType) {
                    case MessageImage:
                        return "IMG";
                    case VideoMessage:
                        return "VID";
                    case VoiceMessage:
                        return "AUDIO";
                    default:
                        return "FILE";
                }
            }

            return "FILE";
        }



        private int getFileIconResource(String mimeType, FileType fileType, String fileName) {

            if (mimeType != null) {
                if (mimeType.contains("pdf")) return R.drawable.ic_pdf;
                if (mimeType.contains("word") || mimeType.contains("document")) return R.drawable.ic_doc;
                if (mimeType.contains("excel") || mimeType.contains("sheet")) return R.drawable.ic_excel;
                if (mimeType.contains("zip") || mimeType.contains("rar")) return R.drawable.ic_zip;
                if (mimeType.contains("audio")) return R.drawable.ic_audio;
                if (mimeType.contains("video")) return R.drawable.ic_video;
                if (mimeType.contains("text")) return R.drawable.ic_txt;
            }

            if (fileName != null && fileName.contains(".")) {
                String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

                switch (ext) {
                    case "jpg":
                    case "jpeg":
                    case "png":
                    case "webp":
                        return R.drawable.ic_image;
                    case "mp4":
                    case "mkv":
                    case "webm":
                        return R.drawable.ic_video;
                    case "pdf":
                        return R.drawable.ic_pdf;
                    case "doc":
                    case "docx":
                        return R.drawable.ic_doc;
                    case "xls":
                    case "xlsx":
                        return R.drawable.ic_excel;
                    case "zip":
                    case "rar":
                        return R.drawable.ic_zip;
                    case "mp3":
                    case "wav":
                        return R.drawable.ic_audio;
                    case "txt":
                        return R.drawable.ic_txt;
                }
            }

            if (fileType != null) {
                switch (fileType) {
                    case MessageImage:
                        return R.drawable.ic_image;
                    case VideoMessage:
                        return R.drawable.ic_video;
                    case VoiceMessage:
                        return R.drawable.ic_audio;
                    default:
                        return R.drawable.ic_file;
                }
            }

            return R.drawable.ic_file;
        }

        private void bindPreview(ImageView thumbnail,
                                 ImageView playIcon,
                                 String fileName,
                                 String url,
                                 FileType fileType,
                                 String mimeType) {

            boolean isImage =
                    fileType == FileType.MessageImage ||
                            (mimeType != null && mimeType.startsWith("image")) ||
                            isImageFile(fileName);

            boolean isVideo =
                    fileType == FileType.VideoMessage ||
                            (mimeType != null && mimeType.startsWith("video"));

            boolean isPdf =
                    (fileType == FileType.MessageFile && mimeType != null && mimeType.contains("pdf"));

            String fullUrl = BuildConfig.SERVER_URL + url;

            if (isImage) {

                String token = new TokenStorage(thumbnail.getContext()).getToken();
                String device = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;

                Glide.with(thumbnail)
                        .load(GlideAuthHelper.build(fullUrl, thumbnail.getContext()))
                        .apply(glideOptions)
                        .override(200, 200)
                        .thumbnail(0.25f)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(thumbnail);

                playIcon.setVisibility(View.GONE);

                thumbnail.setOnClickListener(v -> {
                    Context ctx = v.getContext();
                    Intent intent = new Intent(ctx, com.example.aichat.ImagePreviewActivity.class);
                    intent.putExtra("url", fullUrl);
                    ctx.startActivity(intent);
                });

            } else if (isVideo) {
                Glide.with(thumbnail)
                        .load(fullUrl)
                        .frame(1_000_000)
                        .override(200, 200)
                        .apply(glideOptions)
                        .into(thumbnail);

                playIcon.setVisibility(View.VISIBLE);
                thumbnail.setOnClickListener(null);

            } else if (isPdf) {
                thumbnail.setImageResource(R.drawable.ic_pdf);
                playIcon.setVisibility(View.GONE);
                thumbnail.setOnClickListener(null);

            } else {
                thumbnail.setImageResource(getFileIconResource(mimeType, fileType, fileName));
                playIcon.setVisibility(View.GONE);
                thumbnail.setOnClickListener(null);
            }
        }
        void updateStatus() {
            if (isMyMessage && statusIcon != null) {
                MessageStatus max = MessageController.getMaxStatus(message.getStatuses());
                statusIcon.setImageResource(MessageController.getStatusIconRes(max));
            }
        }
    }
}




