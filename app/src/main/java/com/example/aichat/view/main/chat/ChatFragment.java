package com.example.aichat.view.main.chat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.aichat.R;
import com.example.aichat.controller.main.chat.actions.ChatMessageActions;
import com.example.aichat.controller.main.chat.actions.ChatMembersActions;
import com.example.aichat.controller.main.chat.actions.ChatStateActions;
import com.example.aichat.controller.main.chat.connection.ChatConnectionHandler;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.model.entities.User;
import com.example.aichat.model.utils.ChatPdfExporter;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.view.main.chat.ui.ChatUi;
import com.example.aichat.view.main.chat.ui.ChatMessagesUi;
import com.example.aichat.view.main.chat.ui.ChatMembersUi;
import com.example.aichat.view.main.chat.ui.ChatSearchUi;

import java.util.List;
import java.util.UUID;

public class ChatFragment extends Fragment {

    private UUID chatId;
    private UUID currentUserId;

    private ChatMessageActions messageActions;
    private ChatMembersActions membersActions;
    private ChatStateActions stateActions;
    private ChatConnectionHandler connectionHandler;

    private ChatUi ui;
    private ChatMessagesUi messagesUi;
    private ChatMembersUi membersUi;
    private ChatSearchUi searchUi;

    private Message editingMessage = null;
    private Message replyingToMessage = null;

    private Chat chat;

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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
        View root = inf.inflate(R.layout.fragment_chat, c, false);

        ui = new ChatUi(root, this);
        messagesUi = new ChatMessagesUi(root, this);
        membersUi = new ChatMembersUi(root, this);
        searchUi = new ChatSearchUi(root, this);

        if (chatId != null) initActions(chatId);

        loadChatHistory();

        return root;
    }

    private void initActions(UUID id) {
        ConnectionManager cm = ConnectionSingleton.getInstance().getConnectionManager();

        messageActions = new ChatMessageActions(this, id, currentUserId);
        membersActions = new ChatMembersActions(this, id, currentUserId);
        stateActions = new ChatStateActions(this, id);

        connectionHandler = new ChatConnectionHandler(
                this,
                id,
                messageActions,
                membersActions,
                stateActions
        );

        if (cm != null) cm.addConnectionEvent(connectionHandler);

        membersActions.loadUsers();
        ui.enableSendButton();
    }

    private void loadChatHistory() {
        new Thread(() -> {
            chat = DatabaseManager.getDatabase().chatDao().getChatById(chatId);
            List<Message> messages = DatabaseManager.getDatabase().messageDao().getMessagesByChatId(chatId);

            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                if (chat != null) {
                    ui.setChatTitle(chat.getName());
                    if (chat.getEndTime() != null) ui.showChatEnded();
                }

                messagesUi.initAdapter(messages);
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
        if (text.isEmpty()) return;

        if (ui.isEditMode() && editingMessage != null) {
            messageActions.editMessage(editingMessage, text);
            ui.hideEditPanel();
            editingMessage = null;
            ui.clearMessageInput();
            return;
        }

        if (ui.isReplyMode() && replyingToMessage != null) {
            messageActions.replyToMessage(replyingToMessage, text);
            ui.hideReplyPanel();
            replyingToMessage = null;
            ui.clearMessageInput();
            return;
        }

        messageActions.updateUsers(membersUi.getUserIds());
        messageActions.sendMessage(text);

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

    public void onEndChatClick() {
        stateActions.endChat();
    }

    public void navigateBack() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).backToChats();
        }
    }

    public void onPinChatClick() {
        if (chat == null) return;

        chat.setPinned(true);
        DatabaseManager.getDatabase().chatDao().updateChat(chat);

        ui.setChatTitle(chat.getName());
    }

    public void onUnpinChatClick() {
        if (chat == null) return;

        chat.setPinned(false);
        DatabaseManager.getDatabase().chatDao().updateChat(chat);

        ui.setChatTitle(chat.getName());
    }

    public void setSearchingChatId(@Nullable UUID id) {
        if (membersUi != null) membersUi.setSearchingChatId(id);
    }

    public void onCancelSearchClick() {
        if (membersUi != null) membersUi.setSearchingChatId(null);
    }

    public void onEditMessage(Message message) {
        editingMessage = message;
        replyingToMessage = null;

        ui.showEditPanel(message.getText());
        ui.clearMessageInput();
    }

    public void onReplyToMessage(Message message) {
        replyingToMessage = message;
        editingMessage = null;

        ui.showReplyPanel(message.getText());
        ui.clearMessageInput();
    }

    public void onDeleteMessage(Message message) {
        messageActions.deleteMessage(message);
    }

    public void onCopyMessage(Message message) {
        ClipboardManager clipboard = (ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clip = ClipData.newPlainText("message", message.getText());
        clipboard.setPrimaryClip(clip);
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
        messagesUi.initAdapter(list);
        messagesUi.scrollToUnread(list, currentUserId);
    }

    public void addOrUpdateMessage(Message m) {
        messagesUi.addMessage(m);
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

    public void updateOnline(UUID id, boolean isOnline) {
        membersUi.updateOnline(id, isOnline);
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
        ConnectionManager cm = ConnectionSingleton.getInstance().getConnectionManager();
        if (cm != null && connectionHandler != null) {
            cm.removeConnectionEvent(connectionHandler);
        }
        super.onDestroy();
    }
}
