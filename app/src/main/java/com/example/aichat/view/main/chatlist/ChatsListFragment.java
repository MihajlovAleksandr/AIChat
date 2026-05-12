package com.example.aichat.view.main.chatlist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.ChatType;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageChat;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.model.utils.ChatExportService;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.view.main.chat.ui.UiAnimations;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
    private FabScrollManager fabScrollManager;

    private final Map<UUID, Long> recentlyUpdatedChatNames = new HashMap<>();
    private static final long RECENT_UPDATE_THRESHOLD_MS = 2000;
    private ChatExportService chatExportService;
    private MenuItem createChatItem;
    private MenuItem cancelChatSearchItem;
    private MenuItem addUserItem;
    private MenuItem cancelUserAddItem;

    private View emptyContainer;
    private UUID userId;
    private boolean isUserAdding;

    private static final String TAG = "ChatsListFragment";
    private volatile boolean isReloading = false;

    private int originalTopPadding = 0;

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
        controller = new ChatsListController(this, isUserAdding, userId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats_list, container, false);

        emptyContainer = view.findViewById(R.id.empty_chats_container);
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
        if (chatExportService == null && getActivity() != null) {
            chatExportService = new ChatExportService(requireActivity(), userId);
        }
    }

    private void initRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.rv_chats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        originalTopPadding = recyclerView.getPaddingTop();

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

        chatAdapter.setOnEmptyStateListener(isEmpty -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::updateEmptyContainerVisibility);
            }
        });

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
                query -> {
                    Log.d(TAG, "Search query: " + query);
                    controller.searchChat(query);
                }
        );

        ToolbarSearchManager toolbarSearchManager = new ToolbarSearchManager(
                tvAppName,
                etToolbarSearch,
                btnToolbarSearch,
                btnToolbarClose,
                new ToolbarSearchManager.ToolbarSearchListener() {
                    @Override
                    public void onSearch(String query) {
                        Log.d(TAG, "Toolbar search query: " + query);
                        removeTopPaddingForSearch();
                        if (fabScrollManager != null) {
                            fabScrollManager.setSearchActive(true);
                        }
                        controller.searchChat(query);
                    }

                    @Override
                    public void onCloseSearch() {
                        restoreTopPadding();
                        if (fabScrollManager != null) {
                            fabScrollManager.setSearchActive(false);
                        }
                        controller.cancelSearch();
                    }
                }
        );

        searchManager.setOnSearchStateChangedListener(active -> {
            if (!toolbarSearchManager.isSearchActive()) {
                btnToolbarSearch.setVisibility(active ? View.GONE : View.VISIBLE);
            }
            btnToolbarSearch.setEnabled(!active);
            btnToolbarSearch.setClickable(!active);

            if (active) {
                removeTopPaddingForSearch();
                if (fabScrollManager != null) {
                    fabScrollManager.setSearchActive(true);
                }
            } else {
                if (!toolbarSearchManager.isSearchActive()) {
                    restoreTopPadding();
                    if (fabScrollManager != null) {
                        fabScrollManager.setSearchActive(false);
                    }
                }
            }
        });

        overlayPanel.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) v.performClick();
            return overlayPanel.getVisibility() == View.VISIBLE;
        });
    }

    private void removeTopPaddingForSearch() {
        if (recyclerView != null) {
            recyclerView.setPadding(
                    recyclerView.getPaddingLeft(),
                    0,
                    recyclerView.getPaddingRight(),
                    recyclerView.getPaddingBottom()
            );
        }
    }

    private void restoreTopPadding() {
        if (recyclerView != null) {
            recyclerView.setPadding(
                    recyclerView.getPaddingLeft(),
                    originalTopPadding,
                    recyclerView.getPaddingRight(),
                    recyclerView.getPaddingBottom()
            );
        }
    }

    private void initBottomPanel(View view) {
        ConstraintLayout bottomPanel = view.findViewById(R.id.bottom_panel);

        ImageButton btnBottomRightSettings = bottomPanel.findViewById(R.id.btn_settings);
        ImageButton btnMarketplace = bottomPanel.findViewById(R.id.btn_marketplace);

        btnBottomRightSettings.setOnClickListener(v ->
                controller.openSettings(requireActivity())
        );

        btnMarketplace.setOnClickListener(v ->
                controller.openMarketplace(requireActivity())
        );

        bottomPanel.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) v.performClick();
            return true;
        });
    }

    private void initFab(View view) {
        fab = view.findViewById(R.id.fab_add_chat);
        fabScrollManager = new FabScrollManager(fab, recyclerView);
        fab.setOnClickListener(this::showFabMenu);
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
        if (id == R.id.menu_human) { controller.addChat(ChatType.Human); return true; }
        if (id == R.id.menu_ai) { controller.addChat(ChatType.AI); return true; }
        if (id == R.id.menu_random) { controller.addChat(ChatType.Random); return true; }
        if (id == R.id.menu_group) { controller.addChat(ChatType.Group); return true; }
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
            if (chatExportService == null && getActivity() != null) {
                chatExportService = new ChatExportService(requireActivity(), userId);
            }
            if (chatExportService != null) {
                chatExportService.exportChat(chat.getId(), chat.getName());
            }
            return true;
        }
        return false;
    }

    private List<MessageChat> loadChatsSync() {
        AppDatabase database = DatabaseManager.getDatabase();
        List<Chat> chats = database.chatDao().getAllChats();

        if (chats == null || chats.isEmpty()) {
            return new ArrayList<>();
        }

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

    private void updateEmptyContainerVisibility() {
        if (emptyContainer == null || chatAdapter == null) return;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;

            boolean isEmpty = chatAdapter.getItemCount() == 0;

            if (isEmpty) {
                if (emptyContainer.getVisibility() != View.VISIBLE) {
                    UiAnimations.fadeIn(emptyContainer);
                }
            } else {
                if (emptyContainer.getVisibility() == View.VISIBLE) {
                    UiAnimations.fadeOut(emptyContainer);
                }
            }
        }, 50);
    }

    public void reloadChatsFromDatabaseSafe() {
        if (isReloading) {
            return;
        }

        isReloading = true;

        databaseExecutor.execute(() -> {
            Activity activity = getActivity();

            if (activity == null || !isAdded()) {
                isReloading = false;
                return;
            }

            try {
                List<MessageChat> freshChats = loadChatsSync();

                for (MessageChat mc : freshChats) {
                    if (mc.getChat().getEndTime() != null) {
                        mc.setEnded(true);
                    }
                }

                List<MessageChat> filteredChats = filterRecentlyUpdatedChats(freshChats);
                if (controller != null) {
                    controller.setAllChats(filteredChats);
                    activity.runOnUiThread(() -> {
                        if (controller != null) {
                            controller.setAllChats(filteredChats);
                        }
                    });
                }

                activity.runOnUiThread(() -> {
                    if (!isAdded()) {
                        isReloading = false;
                        return;
                    }

                    chatAdapter.setChats(filteredChats);
                    updateEmptyContainerVisibility();
                    isReloading = false;
                });

            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> isReloading = false);
            }
        });
    }

    public void reloadChatsFromDatabaseSafe(Runnable onComplete) {
        if (isReloading) {
            if (onComplete != null) onComplete.run();
            return;
        }

        isReloading = true;

        databaseExecutor.execute(() -> {
            Activity activity = getActivity();

            if (activity == null || !isAdded()) {
                isReloading = false;
                if (onComplete != null) onComplete.run();
                return;
            }

            try {
                List<MessageChat> freshChats = loadChatsSync();

                for (MessageChat mc : freshChats) {
                    if (mc.getChat().getEndTime() != null) {
                        mc.setEnded(true);
                    }
                }
                List<MessageChat> filteredChats = filterRecentlyUpdatedChats(freshChats);

                if (controller != null) {
                    activity.runOnUiThread(() -> {
                        if (controller != null) {
                            controller.setAllChats(filteredChats);
                        }
                    });
                }

                activity.runOnUiThread(() -> {
                    if (!isAdded()) {
                        isReloading = false;
                        if (onComplete != null) onComplete.run();
                        return;
                    }

                    chatAdapter.setChats(filteredChats);
                    updateEmptyContainerVisibility();
                    isReloading = false;
                    if (onComplete != null) onComplete.run();
                });

            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> {
                    isReloading = false;
                    if (onComplete != null) onComplete.run();
                });
            }
        });
    }

    public void loadChatsFromDatabase(UUID userId) {
        databaseExecutor.execute(() -> {
            Activity activity = getActivity();
            if (activity == null || !isAdded()) return;

            List<MessageChat> messageChats = loadChatsSync();

            for (MessageChat mc : messageChats) {
                if (mc.getChat().getEndTime() != null) {
                    mc.setEnded(true);
                }
            }

            activity.runOnUiThread(() -> {
                if (!isAdded()) return;

                if (controller != null) {
                    controller.setAllChats(messageChats);
                }
                chatAdapter.setChats(messageChats);
                updateEmptyContainerVisibility();
            });
        });
    }

    private List<MessageChat> getMessageChats(List<Message> msgs, List<Chat> chats, HashMap<UUID, List<Message>> unreadMessages) {
        if (chats == null || chats.isEmpty()) return new ArrayList<>();

        Map<UUID, Message> messageByChatId = (msgs == null) ? new HashMap<>() :
                msgs.stream().collect(Collectors.toMap(
                        Message::getChat,
                        msg -> msg,
                        (existing, replacement) -> existing
                ));

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

        activity.runOnUiThread(() -> {
            chatAdapter.setVisibleChats(messageChats);
            updateEmptyContainerVisibility();
        });
    }

    public void rollbackChats() {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            chatAdapter.setAllChatsVisible();
            updateEmptyContainerVisibility();
            restoreTopPadding();
        });
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
            if (recyclerView != null) {
                recyclerView.scrollToPosition(0);
            }

            if (controller != null) {
                controller.setIsChatSearching(false);
                controller.setIsUserAdding(false);
            }

            updateMenuVisibility();
            updateEmptyContainerVisibility();
        });
    }

    public void setAddUserState() {
        updateMenuVisibility();
    }

    public void removeChat(UUID chatId) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            chatAdapter.removeChat(chatId);
            updateEmptyContainerVisibility();
        });
    }

    public void setChatSearchingStatus() {
        updateMenuVisibility();
    }

    public void setGroupSearchingStatus() {
        updateMenuVisibility();
    }

    public void endChat(UUID chat) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            chatAdapter.endChat(chat);
            updateEmptyContainerVisibility();
        });
    }

    public void setConnectionSuccess(boolean isConnected) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            if (appNameView != null) {
                appNameView.setText(isConnected ? getString(R.string.app_name) : getString(R.string.connecting));
            }
        });
    }

    public void updateChatName(UUID chatId, String newName) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        recentlyUpdatedChatNames.put(chatId, System.currentTimeMillis());

        activity.runOnUiThread(() -> chatAdapter.renameChat(chatId, newName));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && getActivity() != null) {
            reloadChatsFromDatabaseSafe();
        }
    }

    public boolean isReloading() {
        return isReloading;
    }

    public void removeUserFromChat(UUID chatId, UUID userId) {
        reloadChatsFromDatabaseSafe();
    }

    public void updateChatEndedStatus(UUID chatId) {
        if (chatAdapter != null) {
            chatAdapter.endChat(chatId);
            updateEmptyContainerVisibility();
            Log.d(TAG, "Chat ended status updated in adapter for chat: " + chatId);
        }
    }

    private List<MessageChat> filterRecentlyUpdatedChats(List<MessageChat> freshChats) {
        List<MessageChat> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (MessageChat mc : freshChats) {
            UUID chatId = mc.getChat().getId();
            Long lastUpdateTime = recentlyUpdatedChatNames.get(chatId);

            if (lastUpdateTime != null && (now - lastUpdateTime) < RECENT_UPDATE_THRESHOLD_MS) {
                MessageChat currentChat = chatAdapter.getMessageChat(chatId);
                if (currentChat != null) {
                    String currentName = currentChat.getChat().getName();
                    if (currentName != null && !currentName.equals(mc.getChat().getName())) {
                        Log.d(TAG, "filterRecentlyUpdatedChats: protecting name '" + currentName +
                                "' for chat " + chatId + " (was '" + mc.getChat().getName() + "' from DB)");
                        mc.getChat().setName(currentName);
                    }
                }
            }

            filtered.add(mc);

            if (lastUpdateTime != null && (now - lastUpdateTime) >= RECENT_UPDATE_THRESHOLD_MS) {
                recentlyUpdatedChatNames.remove(chatId);
            }
        }

        return filtered;
    }

    private UUID getUserId() {
        if (userId == null && getArguments() != null) {
            String userIdStr = getArguments().getString("userId");
            if (userIdStr != null && !userIdStr.isEmpty()) {
                userId = UUID.fromString(userIdStr);
            }
        }
        return userId;
    }

    @Override
    public void onDestroy() {
        if (controller != null) controller.Destroy();
        databaseExecutor.shutdown();
        super.onDestroy();
    }
}