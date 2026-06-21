package com.example.aichat.view.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.WindowCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.example.aichat.BuildConfig;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.ConnectionStateListener;
import com.example.aichat.model.connection.LogoutHelper;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.model.exceptions.UnauthorizedException;
import com.example.aichat.model.utils.media.video.VideoPlayerReturnGuard;
import com.example.aichat.model.notifications.MyFirebaseMessagingService;
import com.example.aichat.model.notifications.NotificationSettingsManager.NotificationCallback;
import com.example.aichat.model.notifications.NotificationSettingsManager;
import com.example.aichat.model.notifications.NotificationSingleton;
import com.example.aichat.model.notifications.NotificationTokenManager;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.R;
import com.example.aichat.view.LoginActivity;
import com.example.aichat.view.main.chat.VideoPlayerActivity;
import com.example.aichat.view.main.chatlist.ChatsListFragment;
import com.example.aichat.view.theme.binders.MainActivityThemeBinder;
import com.example.aichat.view.theme.binders.ThemeBackgroundBinder;
import com.google.firebase.messaging.FirebaseMessaging;
import com.stripe.android.PaymentConfiguration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@androidx.media3.common.util.UnstableApi
public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private static final long VIDEO_RETURN_THEME_REPAIR_WINDOW_MS = 3500L;
    private static final long VIDEO_RETURN_BACK_GUARD_MS = 1400L;

    private ViewPager2 viewPager;
    private MainActivityAdapter pagerAdapter;
    private UUID userId;
    private ChatsListFragment chatsListFragment;
    private View chatContainer;
    private UUID currentChatId;
    private int navigationAnimationToken = 0;
    private boolean chatDataReceiverRegistered = false;
    private long ignoreBackPressUntilMs = 0L;

    private static final String DEEP_LINK_PREFIX = "DEEP_LINK: ";

    private final BroadcastReceiver chatDataChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !MyFirebaseMessagingService.ACTION_CHAT_DATA_CHANGED.equals(intent.getAction())) return;

            String chatIdString = intent.getStringExtra(MyFirebaseMessagingService.EXTRA_CHAT_ID);
            String body = intent.getStringExtra(MyFirebaseMessagingService.EXTRA_BODY);

            Log.d(TAG, "Chat data changed push received. chatId=" + chatIdString + ", body=" + body);
            handleChatDataChangedPush(chatIdString, body);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DatabaseManager.init(this);
        ConnectionSingleton.init(this);
        super.onCreate(savedInstanceState);
        PaymentConfiguration.init(this, BuildConfig.PAYMENT_TOKEN);
        NotificationSingleton.init();

        setContentView(R.layout.activity_main);

        restoreWindowAfterVideoPlayer();
        disableForceDarkForViewTree(findViewById(android.R.id.content));

        initializeChatContainer();
        setupViewPager();
        applyThemeBackground();
        bindChatsListFragmentToController();

        if (!setupConnection()) return;

        setupBackPressHandler();
        setupConnectionStateListener();
        registerChatDataChangedReceiver();
        checkAndRequestNotificationPermission();

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> NotificationTokenManager.onNewToken(this, token));

        handleDeepLink(getIntent());

        openChatFromIntent(getIntent());
        drainPendingChatDataUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean returnedFromVideo = VideoPlayerReturnGuard.consumeRecentFinish(
                this,
                VIDEO_RETURN_THEME_REPAIR_WINDOW_MS
        );

        if (returnedFromVideo) {
            ignoreBackPressUntilMs = SystemClock.uptimeMillis() + VIDEO_RETURN_BACK_GUARD_MS;
            Log.d(TAG, "Returned from VideoPlayerActivity: repairing theme and guarding back press");
        }

        restoreMainActivityVisualState(returnedFromVideo);

        if (viewPager != null) {
            viewPager.postDelayed(() -> restoreMainActivityVisualState(returnedFromVideo), 80);
            viewPager.postDelayed(() -> restoreMainActivityVisualState(returnedFromVideo), 240);
        }

        refreshChatsListUnreadState();
        drainPendingChatDataUpdates();
    }

    private void restoreWindowAfterVideoPlayer() {
        try {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getWindow().getDecorView().setForceDarkAllowed(false);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to restore window after video player", e);
        }
    }

    private void restoreMainActivityVisualState(boolean returnedFromVideo) {
        restoreWindowAfterVideoPlayer();
        disableForceDarkForViewTree(findViewById(android.R.id.content));
        applyThemeBackground();

        if (!returnedFromVideo) {
            return;
        }

        cancelNavigationAnimations();

        UUID openedChatId = getOpenedChatId();

        if (currentChatId == null && openedChatId != null) {
            currentChatId = openedChatId;
        }

        boolean shouldKeepChatOpen =
                currentChatId != null
                        || openedChatId != null
                        || (chatContainer != null && chatContainer.getVisibility() == View.VISIBLE);

        if (viewPager != null) {
            viewPager.animate().cancel();
            viewPager.setAlpha(1f);
            viewPager.setTranslationX(0f);
            ThemeBackgroundBinder.makeViewPagerTransparent(viewPager);
        }

        if (chatContainer != null) {
            chatContainer.animate().cancel();
            chatContainer.setAlpha(1f);
            chatContainer.setTranslationX(0f);
            chatContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        if (shouldKeepChatOpen) {
            if (chatContainer != null) {
                chatContainer.setVisibility(View.VISIBLE);
            }

            if (viewPager != null) {
                viewPager.setVisibility(View.GONE);
            }
        } else {
            if (viewPager != null) {
                viewPager.setVisibility(View.VISIBLE);
            }

            if (chatContainer != null) {
                chatContainer.setVisibility(View.GONE);
            }
        }

        View content = findViewById(android.R.id.content);
        if (content != null) {
            content.invalidate();
            content.requestLayout();
        }
    }

    private void disableForceDarkForViewTree(View view) {
        if (view == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }

        try {
            view.setForceDarkAllowed(false);
        } catch (Exception ignored) {
        }

        if (!(view instanceof ViewGroup)) {
            return;
        }

        ViewGroup group = (ViewGroup) view;

        for (int i = 0; i < group.getChildCount(); i++) {
            disableForceDarkForViewTree(group.getChildAt(i));
        }
    }

    private void initializeChatContainer() {
        chatContainer = findViewById(R.id.view_pager_fragment_container);

        if (chatContainer == null) return;

        chatContainer.setVisibility(View.GONE);
        chatContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        chatContainer.setClickable(true);
        chatContainer.setFocusable(true);
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.view_pager);
        pagerAdapter = new MainActivityAdapter(this, null, true);

        if (viewPager == null) return;

        viewPager.setSaveEnabled(false);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setUserInputEnabled(false);
        viewPager.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        ThemeBackgroundBinder.makeViewPagerTransparent(viewPager);

        chatsListFragment = pagerAdapter.getChatsListFragment();
        Log.d(TAG, "ViewPager and adapter initialized");
    }

    private void applyThemeBackground() {
        restoreWindowAfterVideoPlayer();

        MainActivityThemeBinder.apply(this);

        if (viewPager != null) ThemeBackgroundBinder.makeViewPagerTransparent(viewPager);
        if (chatContainer != null) chatContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        disableForceDarkForViewTree(findViewById(android.R.id.content));
    }

    private void bindChatsListFragmentToController() {
        if (pagerAdapter == null || pagerAdapter.getController() == null) return;

        ChatsListFragment fragment = pagerAdapter.getChatsListFragment();

        if (fragment == null) return;

        chatsListFragment = fragment;
        pagerAdapter.getController().setChatsListFragment(fragment);

        Log.d(TAG, "ChatsListFragment set to controller BEFORE connection");
    }

    private boolean setupConnection() {
        userId = SecurePreferencesManager.getUserId(this);

        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return false;
        }

        if (pagerAdapter != null) pagerAdapter.connect();

        return true;
    }

    private void registerChatDataChangedReceiver() {
        if (chatDataReceiverRegistered) return;

        IntentFilter filter = new IntentFilter(MyFirebaseMessagingService.ACTION_CHAT_DATA_CHANGED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(chatDataChangedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(chatDataChangedReceiver, filter);
        }

        chatDataReceiverRegistered = true;
    }

    private void unregisterChatDataChangedReceiver() {
        if (!chatDataReceiverRegistered) return;

        try {
            unregisterReceiver(chatDataChangedReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Chat data receiver already unregistered", e);
        }

        chatDataReceiverRegistered = false;
    }

    private void handleChatDataChangedPush(String chatIdString, String body) {
        if (chatIdString == null || chatIdString.trim().isEmpty()) return;

        try {
            UUID chatId = UUID.fromString(chatIdString);
            scheduleChatDataRefresh(chatId, body);
        } catch (Exception e) {
            Log.e(TAG, "Invalid chatId from chat data push: " + chatIdString, e);
        }
    }

    private void scheduleChatDataRefresh(UUID chatId, String reason) {
        if (chatId == null) return;

        View anchor = chatContainer != null ? chatContainer : viewPager;

        Runnable refreshRunnable = () -> {
            if (pagerAdapter != null && pagerAdapter.getController() != null) {
                pagerAdapter.getController().handleChatDataPush(chatId, reason);
            }

            refreshChatsListUnreadState();
        };

        if (anchor != null) anchor.postDelayed(refreshRunnable, 250);
        else refreshRunnable.run();
    }

    private void drainPendingChatDataUpdates() {
        SharedPreferences preferences = getSharedPreferences(MyFirebaseMessagingService.PREFS_CHAT_PUSH, Context.MODE_PRIVATE);
        Set<String> pending = preferences.getStringSet(MyFirebaseMessagingService.PREF_PENDING_CHAT_IDS, new HashSet<>());

        if (pending == null || pending.isEmpty()) return;

        Set<String> copy = new HashSet<>(pending);

        preferences.edit().remove(MyFirebaseMessagingService.PREF_PENDING_CHAT_IDS).apply();

        for (String chatIdString : copy) {
            handleChatDataChangedPush(chatIdString, "pending_push");
        }
    }

    public void updateChatLastMessage(Message message) {
        if (chatsListFragment != null) chatsListFragment.updateLastMessage(message);
    }

    public void updateChatMessageStatusInList(UUID messageId, UUID chatId, MessageStatus status) {
        if (messageId == null || chatId == null || status == null) return;

        runOnUiThread(() -> {
            try {
                if (chatsListFragment != null) chatsListFragment.updateMessagesStatus(messageId, chatId, status);
            } catch (Exception e) {
                Log.e(TAG, "Failed to update chat message status in list", e);
            }
        });
    }

    public void refreshChatsListUnreadState() {
        runOnUiThread(() -> {
            try {
                if (chatsListFragment != null) chatsListFragment.reloadChatsFromDatabaseSafe(null);
            } catch (Exception e) {
                Log.e(TAG, "Failed to refresh chats unread state", e);
            }
        });
    }

    private void setupConnectionStateListener() {
        ConnectionDispatcher dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();

        if (dispatcher == null) {
            Log.e(TAG, "ConnectionDispatcher is null, cannot setup listener");
            return;
        }

        dispatcher.addStateListener(new ConnectionStateListener() {
            @Override
            public void onConnected() {
                Log.d(TAG, "Connected to server");

                NotificationTokenManager.onConnected(MainActivity.this);
                refreshChatsListUnreadState();
                drainPendingChatDataUpdates();
            }

            @Override
            public void onDisconnected() {
                Log.d(TAG, "Disconnected from server");
            }

            @Override
            public void onReconnecting() {
                Log.d(TAG, "Reconnecting to server...");
            }

            @Override
            public void onFatalError(Throwable throwable) {
                if (throwable instanceof UnauthorizedException) {
                    Log.e(TAG, "Unauthorized! Logging out...");
                    runOnUiThread(MainActivity.this::logout);
                    return;
                }

                Log.e(TAG, "Fatal error: ", throwable);

                runOnUiThread(() -> Toast.makeText(
                        MainActivity.this,
                        getString(R.string.critical_error, throwable.getMessage()),
                        Toast.LENGTH_LONG
                ).show());
            }
        });
    }

    private void setupBackPressHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (SystemClock.uptimeMillis() < ignoreBackPressUntilMs) {
                    Log.d(TAG, "Ignoring back press immediately after VideoPlayerActivity return");
                    return;
                }

                if (isChatOpen()) backToChats();
                else finish();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void checkAndRequestNotificationPermission() {
        NotificationSettingsManager.requestNotificationPermissionIfNeeded(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        NotificationSettingsManager.handlePermissionResult(this, requestCode, grantResults, new NotificationCallback() {
            @Override
            public void onPermissionResult(boolean granted) {
                if (!granted) {
                    Toast.makeText(
                            MainActivity.this,
                            getString(R.string.notifications_permission_denied),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyThemeBackground();

        handleDeepLink(intent);

        openChatFromIntent(intent);
    }

    private void handleDeepLink(Intent intent) {
        if (intent == null) return;

        Uri data = intent.getData();
        if (data == null) {
            Log.d(TAG, "No deep link data found in intent");
            return;
        }

        String path = data.getPath();
        if (path != null && path.startsWith("/chat/join/")) {
            String token = path.substring("/chat/join/".length());
            Log.d(TAG, "Join chat with token: " + token);

        }
    }

    private void openChatFromIntent(Intent intent) {
        if (intent == null) return;

        String chatId = intent.getStringExtra("chatId");

        if (chatId == null) return;

        try {
            openChat(UUID.fromString(chatId));
        } catch (Exception ex) {
            Log.e(TAG, "Invalid chatId: " + chatId, ex);
        }
    }

    private void cancelNavigationAnimations() {
        if (chatContainer != null) chatContainer.animate().cancel();
        if (viewPager != null) viewPager.animate().cancel();
    }

    private void animateOpenChat(int token) {
        if (chatContainer == null || viewPager == null) return;

        cancelNavigationAnimations();

        chatContainer.setVisibility(View.VISIBLE);
        chatContainer.setAlpha(0f);
        chatContainer.setTranslationX(80f);

        viewPager.setVisibility(View.VISIBLE);
        viewPager.setAlpha(1f);
        viewPager.setTranslationX(0f);

        chatContainer.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(180)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        viewPager.animate()
                .alpha(0f)
                .translationX(-40f)
                .setDuration(150)
                .withEndAction(() -> {
                    if (token != navigationAnimationToken) return;

                    viewPager.setVisibility(View.GONE);
                    viewPager.setAlpha(1f);
                    viewPager.setTranslationX(0f);
                })
                .start();
    }

    private void animateCloseChat(int token, Runnable endAction) {
        if (chatContainer == null || viewPager == null) {
            if (endAction != null) endAction.run();
            return;
        }

        cancelNavigationAnimations();

        viewPager.setVisibility(View.VISIBLE);
        viewPager.setAlpha(0f);
        viewPager.setTranslationX(-40f);

        viewPager.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(180)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        chatContainer.animate()
                .alpha(0f)
                .translationX(80f)
                .setDuration(150)
                .withEndAction(() -> {
                    if (token != navigationAnimationToken) return;

                    chatContainer.setVisibility(View.GONE);
                    chatContainer.setAlpha(1f);
                    chatContainer.setTranslationX(0f);

                    if (endAction != null) endAction.run();
                })
                .start();
    }

    public void openChat(UUID chatId) {
        if (chatId == null || pagerAdapter == null) return;

        if (isChatOpened(chatId)) {
            refreshChatsListUnreadState();
            return;
        }

        applyThemeBackground();

        currentChatId = chatId;

        NotificationSingleton.getInstance()
                .getNotificationHelper()
                .setCurrentChatId(chatId);

        int token = ++navigationAnimationToken;

        pagerAdapter.setChatId(chatId);
        animateOpenChat(token);
    }

    public void backToChats() {
        NotificationSingleton.getInstance()
                .getNotificationHelper()
                .setCurrentChatId(null);

        currentChatId = null;

        int token = ++navigationAnimationToken;

        animateCloseChat(token, () -> {
            if (token != navigationAnimationToken) return;

            if (pagerAdapter != null) pagerAdapter.setChatId(null);

            applyThemeBackground();
            refreshChatsListUnreadState();
        });
    }

    private boolean isChatOpen() {
        return chatContainer != null && chatContainer.getVisibility() == View.VISIBLE;
    }

    public boolean isChatOpened(UUID chatId) {
        if (!isChatOpen() || chatId == null || pagerAdapter == null) return false;

        UUID current = pagerAdapter.getCurrentChatId();

        return current != null && current.equals(chatId);
    }

    public UUID getOpenedChatId() {
        return pagerAdapter == null ? null : pagerAdapter.getCurrentChatId();
    }

    public MainActivityAdapter getPagerAdapter() {
        return pagerAdapter;
    }

    public void setCurrentChatId(UUID chatId) {
        currentChatId = chatId;
    }

    public UUID getCurrentChatId() {
        return currentChatId;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            restoreWindowAfterVideoPlayer();
            disableForceDarkForViewTree(findViewById(android.R.id.content));
            applyThemeBackground();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterChatDataChangedReceiver();
        cancelNavigationAnimations();

        if (pagerAdapter != null) {
            pagerAdapter.destroy();
            pagerAdapter = null;
        }

        super.onDestroy();
    }

    private void logout() {
        Log.d(TAG, "Logging out due to unauthorized access");
        LogoutHelper.logout(this);
    }
}
