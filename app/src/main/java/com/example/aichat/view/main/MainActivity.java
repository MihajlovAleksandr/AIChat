package com.example.aichat.view.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.viewpager2.widget.ViewPager2;
import com.example.aichat.R;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.notifications.NotificationHelper;
import com.example.aichat.model.notifications.NotificationSettingsManager;
import com.example.aichat.model.notifications.NotificationSettingsManager.NotificationCallback;
import com.example.aichat.view.BaseActivity;

public class MainActivity extends BaseActivity {
    private ViewPager2 viewPager;
    private MainActivityAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DatabaseManager.init(this);
        setupConnection();
        setupViewPager();
        setupBackPressHandler();
        checkAndRequestNotificationPermission();
    }

    private void setupConnection() {
        ConnectionManager connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        if (connectionManager == null) {
            ConnectionSingleton.getInstance().setConnectionManager(
                    new ConnectionManager(SecurePreferencesManager.getAuthToken(this)));
            connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        }

        int userId = getIntent().getIntExtra("userId", -1);
        if (userId == -1) {
            userId = SecurePreferencesManager.getUserId(this);
        } else {
            connectionManager.SendCommand(new Command("SyncDB"));
        }

        pagerAdapter = new MainActivityAdapter(this, connectionManager, userId);
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
        if (NotificationSettingsManager.enableToSendNotifications(this)) {
            showDemoNotification();
        }
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
                        if (granted && NotificationSettingsManager.enableToSendNotifications(MainActivity.this)) {
                            showDemoNotification();
                        } else if (!granted) {
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.notifications_permission_denied),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void showDemoNotification() {
        if (NotificationSettingsManager.enableToSendNotifications(this)) {
            NotificationHelper.sendNotification(
                    this,
                    getString(R.string.demo_notification_title),
                    getString(R.string.demo_notification_message)
            );
        }
    }

    public void openChat(int chatId) {
        pagerAdapter.setChatId(chatId);
        viewPager.setCurrentItem(1, true);
    }

    public void backToChats() {
        viewPager.setCurrentItem(0, true);
    }
}