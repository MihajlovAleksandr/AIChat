package com.example.aichat.view.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.viewpager2.widget.ViewPager2;
import com.example.aichat.R;
import com.example.aichat.dto.response.EntryTokenResponse;
import com.example.aichat.dto.response.UserDataResponse;
import com.example.aichat.model.LocaleManager;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.InAppConnection;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.entities.Gender;
import com.example.aichat.model.notifications.MyFirebaseMessagingService;
import com.example.aichat.model.notifications.NotificationHelper;
import com.example.aichat.model.notifications.NotificationSettingsManager;
import com.example.aichat.model.notifications.NotificationSettingsManager.NotificationCallback;
import com.example.aichat.model.notifications.NotificationSingleton;
import com.example.aichat.model.utils.JsonHelper;
import com.example.aichat.view.BaseActivity;

import java.time.LocalDateTime;
import java.util.UUID;

public class MainActivity extends BaseActivity {
    private ViewPager2 viewPager;
    private MainActivityAdapter pagerAdapter;
    InAppConnection inAppConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DatabaseManager.init(this);
        super.onCreate(savedInstanceState);
        NotificationSingleton.init();
        setContentView(R.layout.activity_main);
        ConnectionManager connectionManager = setupConnection();
        setupViewPager();
        setupBackPressHandler();
        checkAndRequestNotificationPermission();
        inAppConnection =  new InAppConnection(connectionManager, this);
        Intent intent = getIntent();
        String chatId = intent.getStringExtra("chatId");
        if(chatId!=null) {
            openChat(UUID.fromString(chatId));
        }


    }

    private ConnectionManager setupConnection() {
        boolean isNewActivity= false;
        ConnectionManager connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        String token = SecurePreferencesManager.getAuthToken(this);
        if (connectionManager == null) {
            ConnectionSingleton.getInstance().setConnectionManager(
                    new ConnectionManager(token));
            connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
            isNewActivity = true;
        }

        String strUserId = getIntent().getStringExtra("userId");
        UUID userId = null;
        if(strUserId!=null) {
            userId =UUID.fromString(strUserId);
        }
        if (userId == null) {
            userId = SecurePreferencesManager.getUserId(this);
            pagerAdapter = new MainActivityAdapter(this, connectionManager, userId, isNewActivity);
        } else {
            isNewActivity = true;
            pagerAdapter = new MainActivityAdapter(this, connectionManager, userId, isNewActivity);
            connectionManager.SendCommand(new Command("SyncDB"));
        }

        if(token==null) {
            pagerAdapter.logout(this);
        }
        else{
            MyFirebaseMessagingService.sendRegistrationTokenToServer(SecurePreferencesManager.getNotificationToken(this));
        }
        return connectionManager;
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setUserInputEnabled(false);
    }

    private void setupBackPressHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (viewPager.getCurrentItem() == 1) {
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
                        if (granted && NotificationSettingsManager.canSendNotifications(MainActivity.this)) {

                        } else if (!granted) {
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.notifications_permission_denied),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openChat(UUID.fromString(intent.getStringExtra("chatId")));
    }

    public void openChat(UUID chatId) {
        if(chatId != null) {
            NotificationSingleton.getInstance().getNotificationHelper().setCurrentChatId(chatId);
            pagerAdapter.setChatId(chatId);
            viewPager.setCurrentItem(1, true);
        }
    }

    public void backToChats() {
        NotificationSingleton.getInstance().getNotificationHelper().setCurrentChatId(null);
        viewPager.setCurrentItem(0, true);
    }

    @Override
    protected void onDestroy() {
        boolean isLogout = pagerAdapter.destroy();
        if (!isLogout && ConnectionSingleton.getInstance().isAvailableToClose())
            inAppConnection.destroy();
        ConnectionSingleton.getInstance().setAvailableToClose(true);
        super.onDestroy();
    }
}