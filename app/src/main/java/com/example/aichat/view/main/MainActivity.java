package com.example.aichat.view.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.viewpager2.widget.ViewPager2;

import com.example.aichat.R;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.InAppConnection;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.CommandOperation;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.model.notifications.MyFirebaseMessagingService;
import com.example.aichat.model.notifications.NotificationSettingsManager;
import com.example.aichat.model.notifications.NotificationSettingsManager.NotificationCallback;
import com.example.aichat.model.notifications.NotificationSingleton;
import com.example.aichat.view.BaseActivity;
import com.example.aichat.view.LoginActivity;

import java.util.UUID;

public class MainActivity extends BaseActivity {

    private ViewPager2 viewPager;
    private MainActivityAdapter pagerAdapter;
    private InAppConnection inAppConnection;
    private UUID userId;
    private boolean isNewActivity;

    private View chatContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        DatabaseManager.init(this);
        super.onCreate(savedInstanceState);
        NotificationSingleton.init();
        setContentView(R.layout.activity_main);

        HttpClient http = new HttpClient();

        http.fetchAsync("api/chat/08dd271f-c435-485f-a59d-078eb25f00bb", HttpClient.HTTPMethod.GET, null)
                .thenAccept(cmd -> {
                    if (cmd.isSuccess()) {
                        Log.e("OK:", cmd.getOperation()+"");
                    } else {
                        Log.e("ERROR: ", cmd.getCode()+"");
                    }
                });


        chatContainer = findViewById(R.id.view_pager_fragment_container);

        if (!setupConnection()) return;

        setupViewPager();
        setupBackPressHandler();
        checkAndRequestNotificationPermission();

        String chatId = getIntent().getStringExtra("chatId");
        if (chatId != null) openChat(UUID.fromString(chatId));
    }

    private boolean setupConnection() {

        String token = SecurePreferencesManager.getAuthToken(this);

        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return false;
        }

        ConnectionSingleton singleton = ConnectionSingleton.getInstance();
        ConnectionManager connectionManager = singleton.getConnectionManager();

        isNewActivity = false;

        if (connectionManager == null) {
            connectionManager = new ConnectionManager(token);
            singleton.setConnectionManager(connectionManager);
            singleton.setToken(token);
            isNewActivity = true;
        } else {
            String storedToken = singleton.getToken();
            if (storedToken == null || !storedToken.equals(token)) {
                singleton.setToken(token);
                connectionManager.setToken(token);
            }
        }

        userId = SecurePreferencesManager.getUserId(this);
        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return false;
        }

        pagerAdapter = new MainActivityAdapter(this, userId, isNewActivity);
        inAppConnection = new InAppConnection(connectionManager, this, userId);

        if (!isNewActivity) {
            connectionManager.SendCommand(new WSSCommand("SyncDB"));
        }

        MyFirebaseMessagingService.sendRegistrationTokenToServer(
                SecurePreferencesManager.getNotificationToken(this)
        );

        return true;
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.view_pager);

        viewPager.setSaveEnabled(false);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setUserInputEnabled(false);
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


    @Override
    protected void onPause() {
        super.onPause();
        if (inAppConnection != null) {
            inAppConnection.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (inAppConnection != null) {
            inAppConnection.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (pagerAdapter != null) {
            boolean isLogout = pagerAdapter.destroy();

            if (!isLogout && ConnectionSingleton.getInstance().isAvailableToClose()) {
                if (inAppConnection != null) inAppConnection.destroy();
            }

            ConnectionSingleton.getInstance().setAvailableToClose(true);
        }

        super.onDestroy();
    }
}
