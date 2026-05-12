package com.example.aichat.view.main;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;
import com.example.aichat.model.entities.Message;
import androidx.activity.OnBackPressedCallback;
import androidx.viewpager2.widget.ViewPager2;

import com.example.aichat.R;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionStateListener;
import com.example.aichat.model.connection.LogoutHelper;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.exceptions.UnauthorizedException;
import com.example.aichat.model.notifications.MyFirebaseMessagingService;
import com.example.aichat.model.notifications.NotificationSettingsManager;
import com.example.aichat.model.notifications.NotificationSettingsManager.NotificationCallback;
import com.example.aichat.model.notifications.NotificationSingleton;
import com.example.aichat.model.notifications.NotificationTokenManager;
import com.example.aichat.view.BaseActivity;
import com.example.aichat.view.LoginActivity;
import com.example.aichat.view.main.chatlist.ChatsListFragment;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.UUID;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private ViewPager2 viewPager;
    private MainActivityAdapter pagerAdapter;
    private UUID userId;
    private ChatsListFragment chatsListFragment;
    private View chatContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DatabaseManager.init(this);
        ConnectionSingleton.init(this);
        super.onCreate(savedInstanceState);
        NotificationSingleton.init();
        setContentView(R.layout.activity_main);
        chatContainer = findViewById(R.id.view_pager_fragment_container);
        setupViewPager();

        if (pagerAdapter != null && pagerAdapter.getController() != null) {
            ChatsListFragment fragment = pagerAdapter.getChatsListFragment();
            if (fragment != null) {
                pagerAdapter.getController().setChatsListFragment(fragment);
                Log.d(TAG, "✅ ChatsListFragment set to controller BEFORE connection");
            }
        }

        if (!setupConnection()) return;

        setupBackPressHandler();
        setupConnectionStateListener();
        checkAndRequestNotificationPermission();

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token ->
                        NotificationTokenManager.onNewToken(this, token)
                );

        String chatId = getIntent().getStringExtra("chatId");
        if (chatId != null) openChat(UUID.fromString(chatId));
    }

    private boolean setupConnection() {
        String token = SecurePreferencesManager.getAuthToken(this);

        userId = SecurePreferencesManager.getUserId(this);
        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return false;
        }

        pagerAdapter.connect();

        return true;
    }

    public void updateChatLastMessage(Message message) {
        if (chatsListFragment != null) {
            chatsListFragment.updateLastMessage(message);
        }
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

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Соединение установлено", Toast.LENGTH_SHORT).show();
                });

                NotificationTokenManager.onConnected(MainActivity.this);
            }

            @Override
            public void onDisconnected() {
                Log.d(TAG, "Disconnected from server");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Соединение потеряно", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onReconnecting() {
                Log.d(TAG, "Reconnecting to server...");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Переподключение...", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFatalError(Throwable t) {
                if (t instanceof UnauthorizedException) {
                    Log.e(TAG, "Unauthorized! Logging out...");
                    runOnUiThread(() -> logout());
                } else {
                    Log.e(TAG, "Fatal error: ", t);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Критическая ошибка: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.view_pager);

        // Создаём адаптер (внутри него создастся фрагмент)
        pagerAdapter = new MainActivityAdapter(this, null, true);

        viewPager.setSaveEnabled(false);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setUserInputEnabled(false);

        Log.d(TAG, "ViewPager and adapter initialized");
    }

    private void setupBackPressHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isChatOpen()) {
                    backToChats();
                } else {
                    finish();
                }
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

        NotificationSettingsManager.handlePermissionResult(
                this,
                requestCode,
                grantResults,
                new NotificationCallback() {
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
                }
        );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        String chatId = intent.getStringExtra("chatId");
        if (chatId != null) openChat(UUID.fromString(chatId));
    }

    private void animateOpenChat() {
        chatContainer.setVisibility(View.VISIBLE);
        chatContainer.setAlpha(0f);
        chatContainer.setTranslationX(80f);

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
                .withEndAction(() -> viewPager.setVisibility(View.GONE))
                .start();
    }

    private void animateCloseChat() {
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
                .withEndAction(() -> chatContainer.setVisibility(View.GONE))
                .start();
    }

    public void openChat(UUID chatId) {
        if (chatId != null) {
            NotificationSingleton.getInstance().getNotificationHelper().setCurrentChatId(chatId);

            pagerAdapter.setChatId(chatId);

            animateOpenChat();
        }
    }

    public void backToChats() {
        NotificationSingleton.getInstance().getNotificationHelper().setCurrentChatId(null);

        pagerAdapter.setChatId(null);

        animateCloseChat();
    }

    private boolean isChatOpen() {
        return chatContainer != null && chatContainer.getVisibility() == View.VISIBLE;
    }

    public boolean isChatOpened(UUID chatId) {
        if (!isChatOpen()) return false;

        UUID current = pagerAdapter.getCurrentChatId();
        return current != null && current.equals(chatId);
    }

    public UUID getOpenedChatId() {
        return pagerAdapter.getCurrentChatId();
    }

    public MainActivityAdapter getPagerAdapter() {
        return pagerAdapter;
    }

    @Override
    protected void onDestroy() {
        if (pagerAdapter != null) {
            boolean isLogout = pagerAdapter.destroy();
        }

        super.onDestroy();
    }

    private UUID currentChatId = null;

    public void setCurrentChatId(UUID chatId) {
        this.currentChatId = chatId;
    }

    public UUID getCurrentChatId() {
        return currentChatId;
    }
    private void logout() {
        Log.d(TAG, "Logging out due to unauthorized access");
        LogoutHelper.logout(this);
    }
}