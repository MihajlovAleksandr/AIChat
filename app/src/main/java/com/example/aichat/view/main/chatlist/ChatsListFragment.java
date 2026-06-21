package com.example.aichat.view.main.chatlist;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.example.aichat.controller.main.chat.MessageController;
import com.example.aichat.controller.main.chatlist.ChatsListController;
import com.example.aichat.dto.response.UserInfoResponse;
import com.example.aichat.dto.response.UserTypingResponse;
import com.example.aichat.model.ai.AIModel;
import com.example.aichat.model.ai.AISettingsStore;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.ChatType;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageChat;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.model.utils.export.ChatExportService;
import com.example.aichat.R;
import com.example.aichat.LeaderboardActivity;
import com.example.aichat.view.main.chat.helpers.UiAnimations;
import com.example.aichat.view.main.chatlist.helpers.ChatSearchManager;
import com.example.aichat.view.main.chatlist.helpers.FabScrollManager;
import com.example.aichat.view.main.chatlist.helpers.ToolbarSearchManager;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.view.payment.ProductsActivity;
import com.example.aichat.view.theme.binders.ChatsListThemeBinder;
import com.example.aichat.view.theme.ThemesActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

@androidx.media3.common.util.UnstableApi
public class ChatsListFragment extends Fragment {

    private static final String TAG = "ChatsListFragment";
    private static final long RECENT_UPDATE_THRESHOLD_MS = 2000;
    private static final long TYPING_HIDE_DELAY_MS = 3500;
    private static final long TYPING_LISTENER_RETRY_DELAY_MS = 700;
    private static final int TYPING_NAME_MAX_CHARS = 20;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private ChatsListController controller;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private ImageButton btnSubscribe;
    private TextView appNameView;
    private FloatingActionButton fab;
    private FabScrollManager fabScrollManager;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<UUID, Long> recentlyUpdatedChatNames = new HashMap<>();

    private final Map<UUID, Runnable> typingHideRunnables = new HashMap<>();
    private final Map<UUID, UUID> typingUserByChatId = new HashMap<>();
    private final Map<UUID, String> typingNamesByUserId = new HashMap<>();
    private final HashSet<UUID> loadingTypingNameUserIds = new HashSet<>();

    private ChatExportService chatExportService;
    private MenuItem createChatItem;
    private MenuItem cancelChatSearchItem;
    private MenuItem addUserItem;
    private MenuItem cancelUserAddItem;

    private View emptyContainer;
    private UUID userId;
    private boolean isUserAdding;
    private volatile boolean isReloading = false;
    private boolean typingListenerRegistered = false;
    private int originalTopPadding = 0;
    private boolean aiSettingsReceiverRegistered = false;

    private final BroadcastReceiver aiSettingsChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null
                    || !AISettingsStore.ACTION_CHAT_AI_MODEL_CHANGED.equals(intent.getAction())) {
                return;
            }

            String chatIdValue = intent.getStringExtra(AISettingsStore.EXTRA_CHAT_ID);
            String modelValue = intent.getStringExtra(AISettingsStore.EXTRA_AI_MODEL);

            if (chatIdValue == null || chatIdValue.trim().isEmpty()) {
                return;
            }

            try {
                UUID chatId = UUID.fromString(chatIdValue);
                AIModel model = AIModel.fromServerValue(modelValue);

                if (chatAdapter != null) {
                    chatAdapter.setAiModel(chatId, model);
                }
            } catch (Exception exception) {
                Log.w(TAG, "Cannot apply AI model update to chat list", exception);
            }
        }
    };

    public ChatsListFragment() {
    }

    public static ChatsListFragment newInstance(boolean isUserAdding, UUID userId) {
        ChatsListFragment fragment = new ChatsListFragment();
        Bundle args = new Bundle();
        args.putBoolean("isUserAdding", isUserAdding);

        if (userId != null) {
            args.putString("userId", userId.toString());
        }

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            isUserAdding = getArguments().getBoolean("isUserAdding");

            String userIdString = getArguments().getString("userId");
            if (userIdString != null && !userIdString.trim().isEmpty()) {
                userId = UUID.fromString(userIdString);
            }
        }

        controller = new ChatsListController(this, isUserAdding, userId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats_list, container, false);

        btnSubscribe = view.findViewById(R.id.btn_subscribe);
        setupBottomPanel();

        emptyContainer = view.findViewById(R.id.empty_chats_container);

        initRecyclerView(view);
        initToolbar(view);
        initSearch(view);
        initBottomPanel(view);
        initFab(view);

        applyRuntimeTheme(view);
        applyRuntimeThemeLater(view);
        loadChatsFromDatabase(userId);
        registerTypingListenerWhenReady();

        return view;
    }

    private void applyRuntimeTheme() {
        View view = getView();

        if (view != null) {
            applyRuntimeTheme(view);
        }
    }

    private void applyRuntimeTheme(View view) {
        if (view == null) {
            return;
        }

        ChatsListThemeBinder.bind(view);
    }

    private void applyRuntimeThemeLater() {
        View view = getView();

        if (view != null) {
            applyRuntimeThemeLater(view);
        }
    }

    private void applyRuntimeThemeLater(View view) {
        if (view == null) {
            return;
        }

        view.post(() -> {
            if (isAdded()) {
                ChatsListThemeBinder.bind(view);
            }
        });
    }

    private void initRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.rv_chats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
        if (itemAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }

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

    private void setupBottomPanel() {
        if (btnSubscribe != null) {
            btnSubscribe.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ProductsActivity.class);
                startActivity(intent);
            });
        }
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
                        applyRuntimeThemeLater();
                    }

                    @Override
                    public void onCloseSearch() {
                        restoreTopPadding();

                        if (fabScrollManager != null) {
                            fabScrollManager.setSearchActive(false);
                        }

                        controller.cancelSearch();
                        applyRuntimeThemeLater();
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

            applyRuntimeThemeLater();
        });

        overlayPanel.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }

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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void initBottomPanel(View view) {
        ConstraintLayout bottomPanel = view.findViewById(R.id.bottom_panel);

        ImageButton btnBottomRightSettings = bottomPanel.findViewById(R.id.btn_settings);
        ImageButton btnLeaderboard = bottomPanel.findViewById(R.id.btn_leaderboard);

        ImageButton btnThemes = bottomPanel.findViewById(R.id.btn_themes);

        btnBottomRightSettings.setOnClickListener(v ->
                controller.openSettings(requireActivity())
        );

        btnThemes.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), ThemesActivity.class);
            startActivity(intent);
        });

        if (btnLeaderboard != null) {
            btnLeaderboard.setImageDrawable(new LeaderboardIconDrawable());
            btnLeaderboard.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            btnLeaderboard.setPadding(dp(6), dp(6), dp(6), dp(6));

            btnLeaderboard.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), LeaderboardActivity.class);
                startActivity(intent);
            });
        }

        bottomPanel.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }

            return true;
        });
    }

    private void initFab(View view) {
        fab = view.findViewById(R.id.fab_add_chat);
        fabScrollManager = new FabScrollManager(fab, recyclerView);
        fab.setOnClickListener(this::showFabMenu);

        applyRuntimeTheme(view);
        fab.post(this::applyRuntimeTheme);
    }

    private void showFabMenu(View anchor) {
        Activity activity = getActivity();
        if (activity == null) return;

        PopupMenu popupMenu = new PopupMenu(createPopupMenuContext(activity), anchor, Gravity.END);
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

    private Context createPopupMenuContext(@NonNull Context baseContext) {
        int nightMode = baseContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        boolean dark = nightMode == Configuration.UI_MODE_NIGHT_YES;

        return new ContextThemeWrapper(
                baseContext,
                dark
                        ? R.style.ThemeOverlay_AIChat_PopupMenu_DarkFixed
                        : R.style.ThemeOverlay_AIChat_PopupMenu_LightFixed
        );
    }

    private boolean handleFabMenuItem(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_cancel_chat_search) {
            controller.stopSearchingChat();
            applyRuntimeThemeLater();
            return true;
        }

        if (id == R.id.menu_cancel_user_add) {
            controller.stopAddingUserToChat();
            applyRuntimeThemeLater();
            return true;
        }

        if (id == R.id.menu_human) {
            controller.addChat(ChatType.Human);
            return true;
        }

        if (id == R.id.menu_ai) {
            controller.addChat(ChatType.AI);
            return true;
        }

        if (id == R.id.menu_random) {
            controller.addChat(ChatType.Random);
            return true;
        }

        if (id == R.id.menu_group) {
            controller.addChat(ChatType.Group);
            return true;
        }

        if (id == R.id.join_a_group) {
            controller.addUserToChat();
            return true;
        }

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
        PopupMenu popupMenu = new PopupMenu(createPopupMenuContext(activity), anchor, Gravity.END);
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
        HashMap<UUID, List<Message>> unreadMessagesByChatId =
                database.messageDao().getUnreadMessages(chatIds, userId);

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

        mainHandler.postDelayed(() -> {
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

            applyRuntimeThemeLater();
        }, 50);
    }

    public void reloadChatsFromDatabaseSafe() {
        reloadChatsFromDatabaseSafe(null);
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

                activity.runOnUiThread(() -> {
                    if (!isAdded()) {
                        isReloading = false;
                        if (onComplete != null) onComplete.run();
                        return;
                    }

                    if (controller != null) {
                        controller.setAllChats(filteredChats);
                    }

                    chatAdapter.setChats(filteredChats);
                    restoreActiveTypingStatuses();
                    updateEmptyContainerVisibility();

                    isReloading = false;
                    applyRuntimeThemeLater();

                    if (onComplete != null) onComplete.run();
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to reload chats", e);

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
                restoreActiveTypingStatuses();
                updateEmptyContainerVisibility();
                applyRuntimeThemeLater();
            });
        });
    }

    private List<MessageChat> getMessageChats(
            List<Message> msgs,
            List<Chat> chats,
            HashMap<UUID, List<Message>> unreadMessages
    ) {
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

            List<Message> unreadMessagesInChat = unreadMessages != null
                    ? unreadMessages.get(chat.getId())
                    : null;

            if (unreadMessagesInChat == null) unreadMessagesInChat = new ArrayList<>();

            result.add(new MessageChat(msg, chat, unreadMessagesInChat));
        }

        return result;
    }

    @Nullable
    private MessageChat loadSingleMessageChatSync(@NonNull UUID chatId) {
        AppDatabase database = DatabaseManager.getDatabase();

        if (database == null) return null;

        Chat chat = database.chatDao().getChatById(chatId);

        if (chat == null) return null;

        List<Message> messages = database.messageDao().getMessagesByChatId(chatId);
        Message lastMessage = null;

        if (messages != null && !messages.isEmpty()) {
            lastMessage = messages.get(messages.size() - 1);
        }

        MessageChat messageChat = new MessageChat(lastMessage, chat, new ArrayList<>());

        if (chat.getEndTime() != null) {
            messageChat.setEnded(true);
        }

        return messageChat;
    }

    public void updateChatList(List<MessageChat> messageChats) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            chatAdapter.setVisibleChats(messageChats);
            restoreActiveTypingStatuses();
            updateEmptyContainerVisibility();
            applyRuntimeThemeLater();
        });
    }

    public void rollbackChats() {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            chatAdapter.setAllChatsVisible();
            restoreActiveTypingStatuses();
            updateEmptyContainerVisibility();
            restoreTopPadding();
            applyRuntimeThemeLater();
        });
    }

    public void updateLastMessage(Message message) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            chatAdapter.updateLastMessage(message);
            restoreActiveTypingStatuses();
            applyRuntimeThemeLater();
        });
    }

    public void updateMessageStatus(MessageChat msg) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            chatAdapter.updateMessageStatus(msg);
            restoreActiveTypingStatuses();
            applyRuntimeThemeLater();
        });
    }

    public void updateMessagesStatus(UUID id, UUID chatId, MessageStatus status) {
        if (id == null || chatId == null || status == null) {
            return;
        }

        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            if (chatAdapter == null) {
                return;
            }

            chatAdapter.updateMessageStatus(id, chatId, status);
            restoreActiveTypingStatuses();
            updateEmptyContainerVisibility();
            applyRuntimeThemeLater();
        });
    }

    public @Nullable MessageChat getMessageChat(UUID chatId) {
        return chatAdapter != null ? chatAdapter.getMessageChat(chatId) : null;
    }

    public boolean hasChat(@Nullable UUID chatId) {
        return chatAdapter != null && chatAdapter.hasChat(chatId);
    }

    public void createChat(Chat chat) {
        Activity activity = getActivity();
        if (activity == null || !isAdded() || chat == null) return;

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
            applyRuntimeThemeLater();
        });
    }

    public void refreshChatOnly(@Nullable UUID chatId) {
        if (chatId == null) return;

        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        databaseExecutor.execute(() -> {
            try {
                MessageChat updated = loadSingleMessageChatSync(chatId);

                if (updated == null) return;

                activity.runOnUiThread(() -> {
                    if (!isAdded() || chatAdapter == null) return;

                    chatAdapter.addChat(updated);
                    restoreActiveTypingStatuses();
                    updateEmptyContainerVisibility();
                    applyRuntimeThemeLater();
                });
            } catch (Exception exception) {
                Log.e(TAG, "Failed to refresh single chat", exception);
            }
        });
    }

    public void setAddUserState() {
        updateMenuVisibility();
        applyRuntimeThemeLater();
    }

    public void removeChat(UUID chatId) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            chatAdapter.removeChat(chatId);
            clearTypingStatus(chatId);
            updateEmptyContainerVisibility();
            applyRuntimeThemeLater();
        });
    }

    public void setChatSearchingStatus() {
        updateMenuVisibility();
        applyRuntimeThemeLater();
    }

    public void setGroupSearchingStatus() {
        updateMenuVisibility();
        applyRuntimeThemeLater();
    }

    public void endChat(UUID chat) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            chatAdapter.endChat(chat);
            clearTypingStatus(chat);
            updateEmptyContainerVisibility();
            applyRuntimeThemeLater();
        });
    }

    public void setConnectionSuccess(boolean isConnected) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            if (appNameView != null) {
                appNameView.setText(isConnected
                        ? getString(R.string.app_name)
                        : getString(R.string.connecting));
            }

            if (isConnected) {
                registerTypingListenerWhenReady();
            }

            applyRuntimeThemeLater();
        });
    }

    public void updateChatName(UUID chatId, String newName) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;

        recentlyUpdatedChatNames.put(chatId, System.currentTimeMillis());

        activity.runOnUiThread(() -> {
            chatAdapter.renameChat(chatId, newName);
            restoreActiveTypingStatuses();
            applyRuntimeThemeLater();
        });
    }

    private void registerTypingListenerWhenReady() {
        if (typingListenerRegistered || !isAdded()) {
            return;
        }

        ConnectionDispatcher dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();

        if (dispatcher == null) {
            mainHandler.postDelayed(this::registerTypingListenerWhenReady, TYPING_LISTENER_RETRY_DELAY_MS);
            return;
        }

        try {
            dispatcher.addEventListener("Typing", UserTypingResponse.class, command -> {
                UserTypingResponse response = command.getPayload();
                handleTypingResponse(response);
            });

            typingListenerRegistered = true;
            Log.d(TAG, "Typing listener registered in chats list");

        } catch (Exception exception) {
            Log.w(TAG, "Typing listener registration failed. Retry later", exception);
            mainHandler.postDelayed(this::registerTypingListenerWhenReady, TYPING_LISTENER_RETRY_DELAY_MS);
        }
    }

    private void handleTypingResponse(@Nullable UserTypingResponse response) {
        if (response == null || response.chatId == null || response.userId == null) {
            return;
        }

        if (userId != null && userId.equals(response.userId)) {
            return;
        }

        Activity activity = getActivity();
        if (activity == null || !isAdded()) {
            return;
        }

        activity.runOnUiThread(() -> {
            if (!isAdded() || chatAdapter == null) {
                return;
            }

            if (response.isTyping) {
                showTypingStatus(response.chatId, response.userId);
            } else {
                clearTypingStatus(response.chatId);
            }
        });
    }

    private void showTypingStatus(@NonNull UUID chatId, @NonNull UUID typingUserId) {
        typingUserByChatId.put(chatId, typingUserId);

        String displayName = getDisplayNameForTypingUser(typingUserId);
        String status = displayName + " " + getString(R.string.chat_typing_status) + "...";

        chatAdapter.setChatTypingStatus(chatId, status);

        Log.d(TAG, "Update list typing row: chatId=" + chatId + ", userId=" + typingUserId + ", status=" + status);

        Runnable oldRunnable = typingHideRunnables.remove(chatId);
        if (oldRunnable != null) {
            mainHandler.removeCallbacks(oldRunnable);
        }

        Runnable hideRunnable = () -> clearTypingStatus(chatId);
        typingHideRunnables.put(chatId, hideRunnable);
        mainHandler.postDelayed(hideRunnable, TYPING_HIDE_DELAY_MS);

        requestTypingNameIfNeeded(typingUserId);
    }

    private void clearTypingStatus(@Nullable UUID chatId) {
        if (chatId == null) {
            return;
        }

        Runnable oldRunnable = typingHideRunnables.remove(chatId);
        if (oldRunnable != null) {
            mainHandler.removeCallbacks(oldRunnable);
        }

        typingUserByChatId.remove(chatId);

        if (chatAdapter != null) {
            chatAdapter.clearChatTypingStatus(chatId);
        }

        Log.d(TAG, "Clear list typing row: chatId=" + chatId);
    }

    private void restoreActiveTypingStatuses() {
        if (chatAdapter == null || typingUserByChatId.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, UUID> entry : new HashMap<>(typingUserByChatId).entrySet()) {
            UUID chatId = entry.getKey();
            UUID typingUserId = entry.getValue();

            if (chatId == null || typingUserId == null) {
                continue;
            }

            String displayName = getDisplayNameForTypingUser(typingUserId);
            String status = displayName + " " + getString(R.string.chat_typing_status) + "...";

            chatAdapter.setChatTypingStatus(chatId, status);
        }
    }

    private String getDisplayNameForTypingUser(@NonNull UUID typingUserId) {
        String cached = typingNamesByUserId.get(typingUserId);

        if (cached != null && !cached.trim().isEmpty()) {
            return cached;
        }

        String fallback = buildTypingNameFromRaw(typingUserId.toString());
        typingNamesByUserId.put(typingUserId, fallback);

        return fallback;
    }

    private void requestTypingNameIfNeeded(@NonNull UUID typingUserId) {
        String current = typingNamesByUserId.get(typingUserId);

        if (current != null && !current.equals(buildTypingNameFromRaw(typingUserId.toString()))) {
            return;
        }

        if (loadingTypingNameUserIds.contains(typingUserId)) {
            return;
        }

        ConnectionDispatcher dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();

        if (dispatcher == null) {
            return;
        }

        loadingTypingNameUserIds.add(typingUserId);

        dispatcher.sendHttpRequestAsync(
                        "/api/user/" + typingUserId + "/userdata/",
                        HttpClient.HTTPMethod.GET,
                        null,
                        true
                )
                .thenAccept(command -> {
                    try {
                        if (command != null && command.isSuccess()) {
                            UserInfoResponse response = command.getData(UserInfoResponse.class);

                            if (response != null && response.userData != null && response.userData.name != null) {
                                String displayName = buildTypingNameFromName(response.userData.name);

                                if (!displayName.trim().isEmpty()) {
                                    typingNamesByUserId.put(typingUserId, displayName);
                                    refreshTypingStatusesForUser(typingUserId);
                                }
                            }
                        }
                    } catch (Exception exception) {
                        Log.w(TAG, "Failed to parse typing user name", exception);
                    } finally {
                        loadingTypingNameUserIds.remove(typingUserId);
                    }
                })
                .exceptionally(throwable -> {
                    loadingTypingNameUserIds.remove(typingUserId);
                    Log.w(TAG, "Failed to load typing user name", throwable);
                    return null;
                });
    }

    private void refreshTypingStatusesForUser(@NonNull UUID typingUserId) {
        Activity activity = getActivity();

        if (activity == null || !isAdded()) {
            return;
        }

        activity.runOnUiThread(() -> {
            if (!isAdded() || chatAdapter == null) {
                return;
            }

            for (Map.Entry<UUID, UUID> entry : new HashMap<>(typingUserByChatId).entrySet()) {
                UUID chatId = entry.getKey();
                UUID userIdInChat = entry.getValue();

                if (typingUserId.equals(userIdInChat)) {
                    String displayName = getDisplayNameForTypingUser(typingUserId);
                    String status = displayName + " " + getString(R.string.chat_typing_status) + "...";

                    chatAdapter.setChatTypingStatus(chatId, status);
                }
            }
        });
    }

    private String buildTypingNameFromName(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }

        String clean = name.trim().replaceAll("\\s+", " ");
        return limitTypingName(clean);
    }

    private String buildTypingNameFromRaw(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "??";
        }

        String clean = raw.replace("-", "").replace("_", "").trim();

        if (clean.isEmpty()) {
            return "??";
        }

        int count = Math.min(2, clean.codePointCount(0, clean.length()));
        int endIndex = clean.offsetByCodePoints(0, count);

        return clean.substring(0, endIndex).toUpperCase();
    }

    private String limitTypingName(@NonNull String value) {
        String clean = value.trim().replaceAll("\\s+", " ");

        if (clean.codePointCount(0, clean.length()) <= TYPING_NAME_MAX_CHARS) {
            return clean;
        }

        int endIndex = clean.offsetByCodePoints(0, TYPING_NAME_MAX_CHARS - 1);

        return clean.substring(0, endIndex).trim() + "…";
    }

    @Override
    public void onResume() {
        super.onResume();

        applyRuntimeTheme();
        applyRuntimeThemeLater();
        registerTypingListenerWhenReady();
        restoreActiveTypingStatuses();
        registerAiSettingsChangedReceiver();

        if (chatAdapter != null) {
            chatAdapter.refreshAiModelBadges();
        }
    }

    @Override
    public void onPause() {
        unregisterAiSettingsChangedReceiver();
        super.onPause();
    }

    private void registerAiSettingsChangedReceiver() {
        Context context = getContext();

        if (context == null || aiSettingsReceiverRegistered) {
            return;
        }

        IntentFilter filter = new IntentFilter(AISettingsStore.ACTION_CHAT_AI_MODEL_CHANGED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(aiSettingsChangedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(aiSettingsChangedReceiver, filter);
        }

        aiSettingsReceiverRegistered = true;
    }

    private void unregisterAiSettingsChangedReceiver() {
        Context context = getContext();

        if (context == null || !aiSettingsReceiverRegistered) {
            return;
        }

        try {
            context.unregisterReceiver(aiSettingsChangedReceiver);
        } catch (Exception exception) {
            Log.w(TAG, "AI settings receiver already unregistered", exception);
        }

        aiSettingsReceiverRegistered = false;
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
            clearTypingStatus(chatId);
            updateEmptyContainerVisibility();
            applyRuntimeThemeLater();

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
    public void onDestroyView() {
        for (Runnable runnable : typingHideRunnables.values()) {
            mainHandler.removeCallbacks(runnable);
        }

        typingHideRunnables.clear();

        if (chatAdapter != null) {
            chatAdapter.clearAllTypingStatuses();
        }

        unregisterAiSettingsChangedReceiver();

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (controller != null) controller.Destroy();

        databaseExecutor.shutdown();
        mainHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }
}
