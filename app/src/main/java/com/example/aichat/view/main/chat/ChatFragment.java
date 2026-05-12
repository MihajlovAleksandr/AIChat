package com.example.aichat.view.main.chat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.aichat.model.connection.files.UploadProgressListener;
import com.example.aichat.dto.response.ChatEndedResponse;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.R;
import com.example.aichat.controller.main.chat.actions.ChatMembersActions;
import com.example.aichat.controller.main.chat.actions.ChatMessageActions;
import com.example.aichat.controller.main.chat.actions.ChatStateActions;
import com.example.aichat.controller.main.chat.actions.SendMessageController;
import com.example.aichat.dto.response.ChatNameUpdatedResponse;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.dto.response.UpdateMessageStatusResponse;
import com.example.aichat.dto.response.UserOnlineChangesResponse;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.TokenStorage;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.FileType;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageInfo;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.model.entities.User;
import com.example.aichat.model.utils.AudioPlayerManager;
import com.example.aichat.model.utils.ChatPdfExporter;
import com.example.aichat.model.utils.FileDownloadProgressManager;
import com.example.aichat.model.utils.FileManager;
import com.example.aichat.model.utils.FileManagerHolder;
import com.example.aichat.model.utils.InternalFileAdapter;
import com.example.aichat.model.utils.ProgressManagerHolder;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.view.main.chat.ui.ChatMembersUi;
import com.example.aichat.view.main.chat.ui.ChatMessagesUi;
import com.example.aichat.view.main.chat.ui.ChatSearchUi;
import com.example.aichat.view.main.chat.ui.ChatUi;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@androidx.media3.common.util.UnstableApi
public class ChatFragment extends Fragment {

    private UUID chatId;
    private UUID currentUserId;

    private SendMessageController sendMessageController;
    private ChatMessageActions messageActions;
    private ChatMembersActions membersActions;
    private ChatStateActions stateActions;
    private ActivityResultLauncher<String[]> filePickerLauncher;
    private ActivityResultLauncher<String> imagePickerLauncher;

    private AudioPlayerManager audioPlayerManager = new AudioPlayerManager();
    private ChatUi ui;
    private ChatMessagesUi messagesUi;
    private ChatMembersUi membersUi;
    private ChatSearchUi searchUi;
    private Message editingMessage = null;
    private Message replyingToMessage = null;
    private Chat chat;
    private List<Uri> selectedUris = new ArrayList<>();
    private FileType selectedFileType = null;
    private FileManager fileManager;
    private static final String TAG = "ChatFragment";
    private TextView fileCounterTextView;
    private List<Uri> pendingNewUris = null;
    private FileType pendingNewType = null;

    public static ChatFragment newInstance(@Nullable UUID chatId, @NonNull UUID currentUserId) {
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        if (chatId != null) b.putString("chatId", chatId.toString());
        b.putString("currentUserId", currentUserId.toString());
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        Bundle a = getArguments();
        if (a != null) {
            String id = a.getString("chatId");
            chatId = id != null ? UUID.fromString(id) : null;
            currentUserId = UUID.fromString(a.getString("currentUserId"));
        }

        initFilePickers();
    }

    private void initFilePickers() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        addSelectedFiles(uris, FileType.MessageFile);
                    }
                }
        );

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        addSelectedFiles(uris, FileType.MessageImage);
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (messagesUi != null) {
            MessageAdapter adapter = messagesUi.getAdapter();

            if (adapter != null) {
                adapter.detachPlayer();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setCurrentChatId(null);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
        View root = inf.inflate(R.layout.fragment_chat, c, false);

        FileDownloadProgressManager progressManager = ProgressManagerHolder.INSTANCE;

        fileManager = FileManagerHolder.get(requireContext(), progressManager);

        ui = new ChatUi(root, this);
        messagesUi = new ChatMessagesUi(root, this);
        membersUi = new ChatMembersUi(root, this);
        searchUi = new ChatSearchUi(root, this);

        messagesUi.setProgressManager(progressManager);

        initFileCounter(root);

        if (chatId != null) initActions(chatId);

        loadChatHistory();

        return root;
    }
    private void initFileCounter(View root) {
        fileCounterTextView = root.findViewById(R.id.file_counter);
        if (fileCounterTextView != null) {
            updateFileCounter();

            // Добавляем обработчик клика на счетчик
            fileCounterTextView.setOnClickListener(v -> showSelectedFilesDialog());
        }
    }

    private void updateFileCounter() {
        if (fileCounterTextView == null) return;

        int count = selectedUris.size();
        if (count > 0) {
            fileCounterTextView.setVisibility(View.VISIBLE);
            fileCounterTextView.setText(String.valueOf(count));
        } else {
            fileCounterTextView.setVisibility(View.GONE);
            selectedFileType = null;
        }
    }

    private boolean canAddMoreFiles() {
        return selectedUris.size() < 10;
    }

    private void addSelectedFiles(List<Uri> newUris, FileType fileType) {
        if (selectedFileType != null && selectedFileType != fileType) {
            pendingNewUris = new ArrayList<>(newUris);
            pendingNewType = fileType;
            showTypeConflictDialog(fileType);
            return;
        }

        if (!canAddMoreFiles()) {
            showMaxFilesWarning();
            return;
        }
        selectedFileType = fileType;

        Set<Uri> uniqueUris = new HashSet<>(selectedUris);
        int added = 0;

        for (Uri uri : newUris) {
            if (selectedUris.size() >= 10) break;

            if (!uniqueUris.contains(uri)) {
                selectedUris.add(uri);
                uniqueUris.add(uri);
                added++;
            }
        }

        if (added > 0) {
            updateFileCounter();
            Log.d("ChatFragment", "Added " + added + " files. Total: " + selectedUris.size());
        }

        if (added < newUris.size()) {
            showDuplicatesWarning();
        }
    }


    private void showTypeConflictDialog(FileType newType) {
        String currentType = selectedFileType == FileType.MessageFile ? "файлы" : "изображения";
        String newTypeStr = newType == FileType.MessageFile ? "файлы" : "изображения";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Смена типа файлов")
                .setMessage("Сейчас выбраны " + currentType + ". Заменить их на " + newTypeStr + "?")
                .setPositiveButton("Заменить", (dialog, which) -> {
                    clearSelectedFiles();
                    selectedFileType = pendingNewType;
                    if (pendingNewUris != null && !pendingNewUris.isEmpty()) {
                        List<Uri> urisToAdd = new ArrayList<>(pendingNewUris);
                        FileType typeToAdd = pendingNewType;
                        pendingNewUris = null;
                        pendingNewType = null;
                        addSelectedFiles(urisToAdd, typeToAdd);
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> {
                    pendingNewUris = null;
                    pendingNewType = null;
                })
                .show();
    }

    private void showDuplicatesWarning() {
        android.widget.Toast.makeText(getContext(),
                "Некоторые файлы уже были выбраны",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private void showSelectedFilesDialog() {
        if (selectedUris.isEmpty()) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_selected_files, null);
        RecyclerView rv = dialogView.findViewById(R.id.rv_selected_files);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        InternalFileAdapter adapter = new InternalFileAdapter(
                selectedUris,
                selectedFileType == FileType.MessageImage,
                new InternalFileAdapter.OnFileInteractionListener() {
                    @Override
                    public void onFileClick(Uri uri) {
                        fileManager.openLocalFile(uri);
                    }

                    @Override
                    public void onDeleteClick(Uri uri) {
                        selectedUris.remove(uri);
                        updateFileCounter();

                        if (selectedUris.isEmpty()) {
                            selectedFileType = null;
                        }

                        if (rv.getAdapter() != null) {
                            rv.getAdapter().notifyDataSetChanged();
                        }

                        AlertDialog dialog = (AlertDialog) rv.getTag();
                        if (dialog != null) {
                            dialog.setTitle("Выбранные файлы (" + selectedUris.size() + "/10)");
                        }

                        if (selectedUris.isEmpty()) {
                            if (dialog != null) dialog.dismiss();
                        }
                    }
                },
                requireContext()
        );

        rv.setAdapter(adapter);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Выбранные файлы (" + selectedUris.size() + "/10)")
                .setView(dialogView)
                .setPositiveButton("Очистить все", (d, which) -> {
                    clearSelectedFiles();
                    d.dismiss();
                })
                .setNegativeButton("Закрыть", null)
                .create();

        rv.setTag(dialog);
        dialog.show();
    }

    public void downloadFile(String url,
                             String mimeType,
                             UUID fileId,
                             FileType fileType,
                             boolean openAfterDownload) {

        String token = getAuthToken();

        if (openAfterDownload) {

            fileManager.downloadAndOpenFile(
                    url,
                    mimeType,
                    fileId,
                    fileType,
                    token
            );

            return;
        }

        fileManager.downloadFile(
                url,
                mimeType,
                fileId,
                fileType,
                token
        );
    }

    private String getAuthToken() {
        try {
            TokenStorage tokenStorage = new TokenStorage(requireContext());
            return tokenStorage.getToken();
        } catch (Exception e) {
            Log.e(TAG, "Error getting auth token", e);
            return null;
        }
    }



    private void initActions(UUID id) {
        messageActions = new ChatMessageActions(this, id, currentUserId);
        membersActions = new ChatMembersActions(this, id, currentUserId);
        stateActions = new ChatStateActions(this, chatId, currentUserId);

        ConnectionDispatcher dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();

        if (dispatcher != null) {

            dispatcher.addEventListener("MessageSent",
                    MessageResponse.class,
                    command -> {
                        MessageResponse response = command.getPayload();
                        if (response != null && chatId != null && chatId.equals(response.chatId) && isAdded()) {
                            requireActivity().runOnUiThread(() -> messageActions.onMessageReceived(response));
                        }
                    });

            dispatcher.addEventListener("MessageStatusUpdated",
                    UpdateMessageStatusResponse.class,
                    command -> {
                        UpdateMessageStatusResponse response = command.getPayload();
                        if (response != null && chatId != null && chatId.equals(response.chatId) && isAdded()) {
                            requireActivity().runOnUiThread(() -> messageActions.onMessageStatusUpdated(response));
                        }
                    });

            dispatcher.addEventListener("ChatEnded",
                    ChatEndedResponse.class,
                    command -> {
                        ChatEndedResponse response = command.getPayload();
                        if (response != null && isAdded()) {
                            requireActivity().runOnUiThread(() -> onChatEnded(response.chatId));
                        }
                    });

            dispatcher.addEventListener("OnlineStatusChanged",
                    UserOnlineChangesResponse.class,
                    command -> {
                        UserOnlineChangesResponse response = command.getPayload();
                        if (response != null && isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    updateOnline(response.userId, response.lastOnline)
                            );
                        }
                    });

            dispatcher.addEventListener("ChatNameUpdated",
                    ChatNameUpdatedResponse.class,
                    command -> {
                        ChatNameUpdatedResponse response = command.getPayload();
                        if (response != null && isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    onChatNameUpdated(response.chatId, response.name)
                            );
                        }
                    });
        }

        FileDownloadProgressManager progressManager = ProgressManagerHolder.INSTANCE;

        UploadProgressListener uploadListener = progress -> {
            if (!isAdded()) return;

            UUID fileId = progress.getFileId();
            int percent = progress.getPercent();

            requireActivity().runOnUiThread(() ->
                    progressManager.updateProgress(fileId, percent)
            );
        };

        sendMessageController = new SendMessageController(
                requireContext(),
                dispatcher,
                uploadListener
        );

        ui.enableSendButton();
    }

    public void onChatEnded(UUID endedChatId) {
        if (!isAdded() || getActivity() == null) return;

        if (chatId != null && chatId.equals(endedChatId)) {
            Log.d(TAG, "onChatEnded: chat " + endedChatId + " has been ended");

            if (chat != null && chat.getEndTime() == null) {
                chat.setEndTime(com.example.aichat.model.utils.TimeConverter.getString(java.time.LocalDateTime.now()));
            }

            requireActivity().runOnUiThread(() -> {
                ui.showChatEnded();
                ui.disableMessageInput();
                ui.clearMessageInput();
                android.widget.Toast.makeText(getContext(), "Чат завершён", android.widget.Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Chat ended UI updated");
            });
        }
    }

    public void showAttachBottomSheet() {
        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext(), R.style.MyBottomSheetDialogTheme);

        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_attach, null);
        dialog.setContentView(view);

        view.findViewById(R.id.btn_close_attach)
                .setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btn_pick_file).setOnClickListener(v -> {
            if (canAddMoreFiles()) {
                dialog.dismiss();
                showFileTypeChoiceDialog();
            } else {
                showMaxFilesWarning();
            }
        });

        view.findViewById(R.id.btn_view_files).setOnClickListener(v -> {
            dialog.dismiss();
            if (!selectedUris.isEmpty()) {
                showSelectedFilesDialog();
            } else {
                android.widget.Toast.makeText(getContext(),
                        "Нет выбранных файлов",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showFileTypeChoiceDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Выберите тип файлов")
                .setItems(new String[]{"Документы и файлы", "Изображения"}, (dialog, which) -> {
                    if (which == 0) {
                        // Файлы
                        filePickerLauncher.launch(new String[]{"*/*"});
                    } else {
                        // Изображения
                        imagePickerLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void showMaxFilesWarning() {
        android.widget.Toast.makeText(getContext(),
                "Максимум 10 файлов можно прикрепить к одному сообщению",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private File getFileFromUri(Uri uri) {
        try {
            String fileName = getFileNameFromUri(uri);

            File file = new File(requireContext().getCacheDir(), fileName);

            InputStream inputStream =
                    requireContext().getContentResolver().openInputStream(uri);

            FileOutputStream outputStream =
                    new FileOutputStream(file);

            byte[] buffer = new byte[8192];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            return file;

        } catch (Exception e) {
            Log.e(TAG, "Error converting Uri to File", e);
            return null;
        }
    }

    private final Map<UUID, Long> fileSizeCache = new HashMap<>();

    public void setFileSize(UUID fileId, long size) {
        if (size > 0) {
            fileSizeCache.put(fileId, size);
        }
    }

    private void sendAllStoredFilesWithText(String textMessage) {

        if (selectedUris == null || selectedUris.isEmpty()) return;

        if (selectedFileType == null) {
            android.widget.Toast.makeText(getContext(),
                    "Не выбран тип файлов",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        List<File> allFiles = new ArrayList<>();
        List<String> originalNames = new ArrayList<>();
        boolean hasAllNames = true;

        for (Uri uri : selectedUris) {
            File file = getFileFromUri(uri);
            if (file != null) {
                allFiles.add(file);

                String name = getFileNameFromUri(uri);

                if (name == null || name.isEmpty()) {
                    hasAllNames = false;
                }

                originalNames.add(name);
            }
        }

        if (allFiles.isEmpty()) return;

        String textToSend = "";

        if (hasAllNames && !originalNames.isEmpty()) {
            StringBuilder builder = new StringBuilder();

            for (String name : originalNames) {
                builder.append("[file:")
                        .append(name)
                        .append("]");
            }

            textToSend = builder.toString();
        }

        final String finalTextToSend = textToSend;
        final List<String> finalOriginalNames = new ArrayList<>(originalNames);

        List<FileType> types = new ArrayList<>(allFiles.size());
        for (int i = 0; i < allFiles.size(); i++) {
            types.add(selectedFileType);
        }

        if (types.size() != allFiles.size()) {
            throw new IllegalStateException("files/types mismatch");
        }

        MessageInfo info = new MessageInfo(
                UUID.randomUUID(),
                chatId,
                finalTextToSend,
                new ArrayList<>(),
                types,
                allFiles
        );

        UUID messageId = info.id;

        Message localMessage = new Message(
                messageId,
                finalTextToSend.isEmpty() ? "Отправка файлов..." : finalTextToSend,
                currentUserId,
                chatId,
                com.example.aichat.model.utils.TimeConverter.getString(java.time.LocalDateTime.now()),
                com.example.aichat.model.utils.TimeConverter.getString(java.time.LocalDateTime.now()),
                new ArrayList<>(),
                new HashMap<>(),
                new ArrayList<>(),
                new HashMap<>(),
                new HashMap<>()
        );

        addOrUpdateMessage(localMessage);

        new Thread(() ->
                DatabaseManager.getDatabase().messageDao().upsertMessage(localMessage)
        ).start();

        sendMessageController.sendMessage(info)
                .thenAccept(cmd -> {
                    if (!isAdded()) return;

                    new Thread(() -> {
                        if (!cmd.isSuccess()) {
                            requireActivity().runOnUiThread(() -> {
                                android.widget.Toast.makeText(getContext(),
                                        "Ошибка отправки файлов",
                                        android.widget.Toast.LENGTH_SHORT).show();
                            });
                            return;
                        }

                        try {
                            MessageResponse response = cmd.getData(MessageResponse.class);

                            Message updated =
                                    new com.example.aichat.model.utils.mappers.MessageMapper(currentUserId)
                                            .ToModel(response);

                            if (!finalTextToSend.isEmpty()) {
                                updated.setText(finalTextToSend);
                            }

                            DatabaseManager.getDatabase().messageDao().upsertMessage(updated);

                            requireActivity().runOnUiThread(() -> {

                                MessageAdapter adapter = messagesUi.getAdapter();

                                if (adapter != null && response.files != null) {
                                    for (int i = 0; i < response.files.size(); i++) {
                                        if (i < finalOriginalNames.size()) {
                                            adapter.setLocalFileName(
                                                    response.files.get(i),
                                                    finalOriginalNames.get(i)
                                            );
                                        }
                                    }
                                }

                                addOrUpdateMessage(updated);
                                clearSelectedFiles();
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    public void clearSelectedFiles() {
        selectedUris.clear();
        selectedFileType = null;
        updateFileCounter();
        Log.d("ChatFragment", "Selected files cleared");
    }

    private void loadChatHistory() {
        new Thread(() -> {
            chat = DatabaseManager.getDatabase().chatDao().getChatById(chatId);
            List<Message> messages = DatabaseManager.getDatabase().messageDao().getMessagesByChatId(chatId);

            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                if (chat != null) {
                    ui.setChatTitle(chat.getName());
                    if (!chat.isActive()) ui.showChatEnded();
                }

                messagesUi.clearFileCache();

                messagesUi.initAdapter(messages);
                MessageAdapter adapter = messagesUi.getAdapter();
                if (adapter != null) {
                    adapter.setAudioPlayerManager(audioPlayerManager);
                }
                messagesUi.scrollToUnread(messages, currentUserId);
            });
        }).start();
    }

    public void onExportChatClick() {
        if (chat == null) return;

        new Thread(() -> {
            try {
                List<Message> messages =
                        DatabaseManager.getDatabase()
                                .messageDao()
                                .getMessagesByChatId(chat.getId());

                if (messages == null || messages.isEmpty()) return;
                ChatPdfExporter exporter =
                        new ChatPdfExporter(requireActivity(), currentUserId);

                exporter.exportChat(messages, chat.getName());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    public void onSendMessageClick() {
        String text = ui.getMessageText();

        if ((text == null || text.isEmpty()) && selectedUris.isEmpty()) return;

        if (ui.isEditMode() && editingMessage != null) {
            messageActions.editMessage(editingMessage, text);
            ui.hideEditPanel();
            editingMessage = null;
            ui.clearMessageInput();
            clearSelectedFiles();
            return;
        }

        if (ui.isReplyMode() && replyingToMessage != null) {
            messageActions.replyToMessage(replyingToMessage, text);
            ui.hideReplyPanel();
            replyingToMessage = null;
            ui.clearMessageInput();
            clearSelectedFiles();
            return;
        }

        if (!selectedUris.isEmpty()) {
            sendAllStoredFilesWithText(text);
        } else {
            messageActions.sendMessage(text);
        }

        ui.clearMessageInput();
    }

    public void onOptionsClick() {
        ui.showOptionsMenu();
    }

    public void onToggleMembersPanel() {
        ui.toggleMembersPanel();
    }

    public void onShowSearchPanel() {
        ui.showSearchPanel();
    }

    public void onSearchQuery(String q) {
        messageActions.findMessages(q);
    }

    public void onHideSearchPanel() {
        ui.hideSearchPanel();
    }

    public void onHideSearchResults() {
        searchUi.hideResults();
    }

    public void onMemberClick(User u) {
        ui.insertMention(u.getUserData().getName());
    }

    public void onAddMemberClick() {
        membersActions.addUserToChat();
    }

    public void onRemoveMemberClick(UUID id) {
        membersActions.removeUserFromChat(id);
    }

    public void onHighlightMessage(Message m) {
        messagesUi.highlightMessage(m);
    }

    public void onVisibleMessagesChanged() {
        messagesUi.checkVisibleMessages(messageActions);
    }

    public void reloadMessagesFromDatabase() {
        new Thread(() -> {
            List<Message> messages =
                    DatabaseManager.getDatabase()
                            .messageDao()
                            .getMessagesByChatId(chatId);

            if (getActivity() == null) return;

            getActivity().runOnUiThread(() ->
                    showMessages(messages)
            );
        }).start();
    }

    public void onEndChatClick() {
        Log.d(TAG, "onEndChatClick called");
        stateActions.endChat();
    }

    public void navigateBack() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).backToChats();
        }
    }

    public void setSearchingChatId(@Nullable UUID id) {
        if (membersUi != null) membersUi.setSearchState(id);
        else membersUi.setSearchState(ChatMembersUi.NEW_GROUP_SEARCH);
    }

    public void canselSearch(){
        membersUi.setSearchState(null);
    }

    public void onCancelSearchClick() {
        membersActions.stopSearchingChat();
    }

    public void onCancelEdit() {
        editingMessage = null;
        ui.hideEditPanel();
    }

    public void onCancelReply() {
        replyingToMessage = null;
        ui.hideReplyPanel();
    }

    public void showMessages(List<Message> list) {
        messagesUi.clearFileCache();

        messagesUi.initAdapter(list);
        messagesUi.scrollToUnread(list, currentUserId);

        for (Message message : list) {
            if (message.getFiles() != null && !message.getFiles().isEmpty()) {
                for (UUID fileId : message.getFiles()) {
                    setFileSize(fileId, 1024 * 1024);
                }
            }
        }
    }

    public void addOrUpdateMessage(Message m) {
        messagesUi.addMessage(m);

        if (m.getFiles() != null && !m.getFiles().isEmpty()) {
            for (UUID fileId : m.getFiles()) {
                setFileSize(fileId, 1024 * 1024);
            }
        }
    }

    public void updateMessage(Message message) {
        messagesUi.addMessage(message);
    }

    public void removeMessage(UUID id) {
        messagesUi.removeMessage(id);
    }

    public void updateMessageStatus(UUID msgId, UUID userId, MessageStatus st) {
        messagesUi.updateStatus(msgId, userId, st);
    }

    public void showUsers(List<User> users) {
        membersUi.updateMembers(users);
        messageActions.updateUsers(membersUi.getUserIds());
    }

    public void onChatNameUpdated(UUID updatedChatId, String newName) {
        if (!isAdded() || getActivity() == null) return;

        if (chatId != null && chatId.equals(updatedChatId)) {
            Log.d(TAG, "onChatNameUpdated: chat " + updatedChatId + " renamed to '" + newName + "'");

            if (chat != null) {
                chat.setName(newName);
            }

            requireActivity().runOnUiThread(() -> {
                ui.setChatTitle(newName);
                Log.d(TAG, "Chat title updated to: " + newName);
            });
        }
    }

    public void updateOnline(UUID id, @Nullable String lastOnline) {
        membersUi.updateOnline(id, lastOnline);
    }

    public void showChatEnded() {
        ui.showChatEnded();
    }

    public void showSearchResults(List<Message> m, String q) {
        searchUi.showResults(m, q);
    }

    public void setChatTitle(String name) {
        ui.setChatTitle(name);
    }

    public UUID getCurrentUserId() {
        return currentUserId;
    }

    public ChatUi getUi() {
        return ui;
    }

    public Chat getChat() {
        return chat;
    }

    public void close() {
        navigateBack();
    }

    @Override
    public void onDestroy() {

        if (messagesUi != null) {
            MessageAdapter adapter = messagesUi.getAdapter();

            if (adapter != null) {
                adapter.detachPlayer();
            }
        }

        if (audioPlayerManager != null) {
            audioPlayerManager.release();
            audioPlayerManager = null;
        }

        if (fileManager != null) {
            fileManager.cleanupTempFiles();
        }

        clearSelectedFiles();

        super.onDestroy();
    }

    public void onAudioDownloaded(
            UUID fileId,
            String localPath
    ) {

        if (messagesUi == null) {
            return;
        }

        MessageAdapter adapter =
                messagesUi.getAdapter();

        if (adapter == null) {
            return;
        }

        AudioPlayerManager player =
                adapter.getAudioPlayerManager();

        if (player == null) {
            return;
        }

        if (!player.shouldAutoPlay(fileId)) {
            return;
        }

        player.clearPendingPlay();

        requireActivity().runOnUiThread(() -> {

            player.playLocal(
                    requireContext(),
                    localPath,
                    fileId
            );
        });
    }

    public AudioPlayerManager getAudioPlayerManager() {
        return audioPlayerManager;
    }

    private String getFileNameFromUri(Uri uri) {
        Context ctx = getContext();
        if (ctx == null) return "file_" + System.currentTimeMillis();

        Cursor cursor = ctx.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                String name = cursor.getString(nameIndex);
                cursor.close();
                return name;
            }
            cursor.close();
        }
        return "file_" + System.currentTimeMillis();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (messagesUi != null) {
            messagesUi.clearFileCache();
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setCurrentChatId(chatId);
        }
    }

}