package com.example.aichat.view.main.chatlist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.example.aichat.R;
import com.example.aichat.controller.main.chat.MessageController;
import com.example.aichat.controller.main.chatlist.ChatsListController;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.ChatType;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageChat;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.view.main.MainActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ChatsListFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private ChatsListController controller;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private TextView appNameView;
    private FloatingActionButton fab;

    private MenuItem createChatItem;
    private MenuItem cancelChatSearchItem;
    private MenuItem addUserItem;
    private MenuItem cancelUserAddItem;

    private UUID userId;
    private boolean isUserAdding;

    private static final int MENU_PIN_CHAT = 20001;
    private static final int MENU_UNPIN_CHAT = 20002;

    public ChatsListFragment() {}

    public static ChatsListFragment newInstance(boolean isUserAdding, UUID userId) {
        ChatsListFragment fragment = new ChatsListFragment();
        Bundle args = new Bundle();
        args.putBoolean("isUserAdding", isUserAdding);
        args.putString("userId", userId.toString());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            isUserAdding = getArguments().getBoolean("isUserAdding");
            userId = UUID.fromString(getArguments().getString("userId"));
        }

        ConnectionManager connectionManager =
                ConnectionSingleton.getInstance().getConnectionManager();

        controller = new ChatsListController(this, connectionManager, isUserAdding, userId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats_list, container, false);

        initRecyclerView(view);
        initToolbar(view);
        initSearch(view);
        initBottomPanel(view);
        initFab(view);

        loadChatsFromDatabase(userId);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnTest = view.findViewById(R.id.btn_test_create_chat);
        if (btnTest != null) {
            btnTest.setOnClickListener(v -> {
                Activity activity = getActivity();
                if (activity == null) return;
                Intent intent = new Intent(activity, CreateChatActivity.class);
                activity.startActivity(intent);
            });
        }
    }

    private void initRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.rv_chats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        chatAdapter = new ChatAdapter(
                new ChatAdapter.OnChatClickListener() {
                    @Override
                    public void onChatClick(Chat chat) {
                        Activity activity = getActivity();
                        if (activity instanceof MainActivity) {
                            ((MainActivity) activity).openChat(chat.getId());
                        }
                    }

                    @Override
                    public void onChatLongClick(Chat chat) {
                        showChatContextMenu(chat);
                    }
                },
                new MessageController(userId)
        );

        chatAdapter.setChatOpenedChecker(chatId ->
                getActivity() instanceof MainActivity &&
                        ((MainActivity) getActivity()).isChatOpened(chatId)
        );

        recyclerView.setAdapter(chatAdapter);
    }

    private void initToolbar(View view) {
        appNameView = view.findViewById(R.id.tv_app_name);
    }

    private void initSearch(View view) {
        TextView tvAppName = view.findViewById(R.id.tv_app_name);
        EditText etToolbarSearch = view.findViewById(R.id.et_toolbar_search);
        ImageButton btnToolbarSearch = view.findViewById(R.id.btn_search);
        ImageButton btnToolbarClose = view.findViewById(R.id.btn_close_search);

        ConstraintLayout overlayPanel = view.findViewById(R.id.overlay_search_bar);
        ImageButton btnOverlayBack = overlayPanel.findViewById(R.id.btn_back);
        EditText etOverlaySearch = overlayPanel.findViewById(R.id.et_search);

        ChatSearchManager searchManager = new ChatSearchManager(
                btnToolbarSearch,
                overlayPanel,
                btnOverlayBack,
                etOverlaySearch,
                recyclerView,
                (ConstraintLayout) view,
                query -> controller.searchChat(query)
        );

        ToolbarSearchManager toolbarSearchManager = new ToolbarSearchManager(
                tvAppName,
                etToolbarSearch,
                btnToolbarSearch,
                btnToolbarClose,
                new ToolbarSearchManager.ToolbarSearchListener() {
                    @Override
                    public void onSearch(String query) {
                        controller.searchChat(query);
                    }

                    @Override
                    public void onCloseSearch() {
                        controller.cancelSearch();
                    }
                }
        );

        btnToolbarSearch.setOnClickListener(v -> {
            if (searchManager.isSearchActive()) return;
            toolbarSearchManager.openSearch();
        });

        btnToolbarSearch.setOnLongClickListener(v -> {
            if (searchManager.isSearchActive()) return true;
            searchManager.openSearch();
            return true;
        });

        searchManager.setOnSearchStateChangedListener(active -> {
            btnToolbarSearch.setVisibility(active ? View.GONE : View.VISIBLE);
            btnToolbarSearch.setEnabled(!active);
            btnToolbarSearch.setClickable(!active);
        });

        overlayPanel.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) v.performClick();
            return overlayPanel.getVisibility() == View.VISIBLE;
        });
    }

    private void initBottomPanel(View view) {
        ConstraintLayout bottomPanel = view.findViewById(R.id.bottom_panel);
        ImageButton btnBottomRightSettings = bottomPanel.findViewById(R.id.btn_settings_bottom_4);
        btnBottomRightSettings.setOnClickListener(v -> controller.openSettings(requireActivity()));

        bottomPanel.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) v.performClick();
            return true;
        });
    }

    private void initFab(View view) {
        fab = view.findViewById(R.id.fab_add_chat);
        fab.setOnClickListener(this::showFabMenu);

        new FabScrollManager(fab, recyclerView);
    }

    private void showFabMenu(View anchor) {
        Activity activity = getActivity();
        if (activity == null) return;

        PopupMenu popupMenu = new PopupMenu(activity, anchor, Gravity.END);
        popupMenu.inflate(R.menu.fab_menu);

        createChatItem = popupMenu.getMenu().findItem(R.id.menu_create_chat);
        cancelChatSearchItem = popupMenu.getMenu().findItem(R.id.menu_cancel_chat_search);
        addUserItem = popupMenu.getMenu().findItem(R.id.menu_add_user_to_chat);
        cancelUserAddItem = popupMenu.getMenu().findItem(R.id.menu_cancel_user_add);

        updateMenuVisibility();

        try {
            Field mPopup = popupMenu.getClass().getDeclaredField("mPopup");
            mPopup.setAccessible(true);
            Object menuPopupHelper = mPopup.get(popupMenu);

            Method setForceIcons = menuPopupHelper.getClass()
                    .getDeclaredMethod("setForceShowIcon", boolean.class);
            setForceIcons.invoke(menuPopupHelper, true);

            Method showMethod = menuPopupHelper.getClass()
                    .getDeclaredMethod("show", int.class, int.class);

            int offsetY = -(fab.getHeight() + 16);
            showMethod.invoke(menuPopupHelper, 0, offsetY);

        } catch (Exception e) {
            popupMenu.show();
        }

        popupMenu.setOnMenuItemClickListener(this::handleFabMenuItem);
        popupMenu.setOnDismissListener(menu -> {
            createChatItem = null;
            cancelChatSearchItem = null;
            addUserItem = null;
            cancelUserAddItem = null;
        });
    }

    private boolean handleFabMenuItem(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_cancel_chat_search) { controller.stopSearchingChat(); return true; }
        if (id == R.id.menu_cancel_user_add) { controller.stopAddingUserToChat(); return true; }
        if (id == R.id.menu_human) { controller.addChat(ChatType.HUMAN); return true; }
        if (id == R.id.menu_ai) { controller.addChat(ChatType.AI); return true; }
        if (id == R.id.menu_random) { controller.addChat(ChatType.RANDOM); return true; }
        if (id == R.id.menu_group) { controller.addChat(ChatType.GROUP); return true; }
        if (id == R.id.join_a_group) { controller.addUserToChat(); return true; }
        return false;
    }

    private void updateMenuVisibility() {
        if (createChatItem == null) return;

        boolean isChatSearching = controller.getIsChatSearching();
        boolean isUserAdding = controller.getIsUserAddingToChat();

        createChatItem.setVisible(!isChatSearching);
        cancelChatSearchItem.setVisible(isChatSearching);
        addUserItem.setVisible(!isUserAdding);
        cancelUserAddItem.setVisible(isUserAdding);
    }

    private void showChatContextMenu(Chat chat) {
        Activity activity = getActivity();
        if (activity == null) return;

        View anchor = requireView().findViewById(R.id.main_toolbar);
        PopupMenu popupMenu = new PopupMenu(activity, anchor, Gravity.END);
        popupMenu.inflate(R.menu.chat_context_menu);

        if (!chat.isPinned()) {
            popupMenu.getMenu().add(0, MENU_PIN_CHAT, 100, getString(R.string.pin_chat));
        } else {
            popupMenu.getMenu().add(0, MENU_UNPIN_CHAT, 100, getString(R.string.unpin_chat));
        }

        popupMenu.setOnMenuItemClickListener(item -> handleChatMenuItem(item, chat));
        popupMenu.show();
    }

    private boolean handleChatMenuItem(MenuItem item, Chat chat) {
        int id = item.getItemId();

        if (id == R.id.menu_rename_chat) {
            showRenameDialog(chat);
            return true;
        }
        if (id == R.id.menu_delete_chat) {
            controller.deleteChat(chat.getId());
            return true;
        }
        if (id == R.id.menu_export_chat) {
            controller.exportChat(chat);
            return true;
        }

        if (id == MENU_PIN_CHAT) {
            databaseExecutor.execute(() -> {
                chat.setPinned(true);
                DatabaseManager.getDatabase().chatDao().updateChat(chat);

                List<MessageChat> chats = loadChatsSync();
                Collections.sort(chats);

                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() ->
                            chatAdapter.setChats(chats)
                    );
                }
            });
            return true;
        }

        if (id == MENU_UNPIN_CHAT) {
            databaseExecutor.execute(() -> {
                chat.setPinned(false);
                DatabaseManager.getDatabase().chatDao().updateChat(chat);

                List<MessageChat> chats = loadChatsSync();
                Collections.sort(chats);

                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() ->
                            chatAdapter.setChats(chats)
                    );
                }
            });
            return true;
        }

        return false;
    }

    private List<MessageChat> loadChatsSync() {
        AppDatabase database = DatabaseManager.getDatabase();
        List<Chat> chats = database.chatDao().getAllChats();
        List<UUID> chatIds = chats.stream().map(Chat::getId).collect(Collectors.toList());
        List<Message> lastMessages = database.messageDao().getLastMessages(chatIds);
        HashMap<UUID, List<Message>> unreadMessagesByChatId = database.messageDao().getUnreadMessages(chatIds, userId);
        return getMessageChats(lastMessages, chats, unreadMessagesByChatId);
    }

    private void showRenameDialog(Chat chat) {
        Activity activity = getActivity();
        if (activity == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.rename_chat);
        final EditText input = new EditText(activity);
        input.setHint(R.string.enter_new_chat_name);
        builder.setView(input);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) controller.renameChat(chat.getId(), newName);
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public void loadChatsFromDatabase(UUID userId) {
        databaseExecutor.execute(() -> {
            Activity activity = getActivity();
            if (activity == null || !isAdded()) return;

            List<MessageChat> messageChats = loadChatsSync();

            // ★ ВАЖНО: выставляем ended вручную, если чат завершён
            for (MessageChat mc : messageChats) {
                if (mc.getChat().getEndTime() != null) {
                    mc.setEnded(true);
                }
            }

            Collections.sort(messageChats);

            activity.runOnUiThread(() -> {
                if (!isAdded()) return;
                controller.setAllChats(messageChats);
                chatAdapter.setChats(messageChats);
            });
        });
    }


    private List<MessageChat> getMessageChats(List<Message> msgs, List<Chat> chats, HashMap<UUID, List<Message>> unreadMessages) {
        if (chats == null || chats.isEmpty()) return Collections.emptyList();
        Map<UUID, Message> messageByChatId = (msgs == null) ? Collections.emptyMap() :
                msgs.stream().collect(Collectors.toMap(Message::getChat, msg -> msg));
        List<MessageChat> result = new ArrayList<>(chats.size());
        for (Chat chat : chats) {
            Message msg = messageByChatId.get(chat.getId());
            List<Message> unreadMessagesInChat = unreadMessages.get(chat.getId());
            if (unreadMessagesInChat == null) unreadMessagesInChat = new ArrayList<>();
            result.add(new MessageChat(msg, chat, unreadMessagesInChat));
        }
        return result;
    }


    public void updateChatList(List<MessageChat> messageChats) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> chatAdapter.setVisibleChats(messageChats));
    }

    public void rollbackChats() {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(chatAdapter::setAllChatsVisible);
    }

    public void updateLastMessage(Message message) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;
        activity.runOnUiThread(() -> chatAdapter.updateLastMessage(message));
    }

    public void updateMessageStatus(MessageChat msg) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> chatAdapter.updateMessageStatus(msg));
    }

    public void updateMessagesStatus(UUID id, UUID chatId, MessageStatus status) {
        MessageChat msg = chatAdapter.getMessageChat(chatId);
        if (msg == null) return;

        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            if (status == MessageStatus.READ) msg.removeUnreadMessage(id);
        });
    }

    public @Nullable MessageChat getMessageChat(UUID chatId) {
        return chatAdapter.getMessageChat(chatId);
    }

    public void createChat(Chat chat) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            chatAdapter.addChat(new MessageChat(null, chat, new ArrayList<>()));
            recyclerView.scrollToPosition(0);
        });
    }

    public void removeChat(UUID chatId) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> chatAdapter.removeChat(chatId));
    }

    public void setCreateChatState() {
        updateMenuVisibility();
    }

    public void setAddUserState() {
        updateMenuVisibility();
    }

    public void endChat(Chat chat) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> chatAdapter.endChat(chat));
    }

    public void setConnectionSuccess(boolean isConnected) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() ->
                appNameView.setText(isConnected ? getString(R.string.app_name) : getString(R.string.connecting))
        );
    }

    @Override
    public void onDestroy() {
        if (controller != null) controller.Destroy();
        databaseExecutor.shutdown();
        super.onDestroy();
    }
}
