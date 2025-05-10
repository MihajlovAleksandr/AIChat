package com.example.aichat.view.main;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.viewpager2.widget.ViewPager2;
import com.example.aichat.R;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.InAppConnection;
import com.example.aichat.model.connection.NetworkService;
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
    InAppConnection inAppConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DatabaseManager.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ConnectionManager connectionManager = setupConnection();
        setupViewPager();
        setupBackPressHandler();
        checkAndRequestNotificationPermission();
        if(NotificationSettingsManager.isBackgroundUsageAllowed(this)) {
            if (isServiceRunning(NetworkService.class)) {
                pagerAdapter.mainActivityState(true);
            } else {
                Intent serviceIntent = new Intent(this, NetworkService.class);
                serviceIntent.putExtra("currentUserId", pagerAdapter.getCurrentUserId());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            }
        }
        else {
            inAppConnection =  new InAppConnection(connectionManager, this,pagerAdapter.getCurrentUserId());
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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

        int userId = getIntent().getIntExtra("userId", -1);
        if (userId == -1) {
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

    public void openChat(int chatId) {
        pagerAdapter.setChatId(chatId);
        viewPager.setCurrentItem(1, true);
    }

    public void backToChats() {
        viewPager.setCurrentItem(0, true);
    }

    @Override
    protected void onDestroy() {
        if(inAppConnection!=null)
        {
            inAppConnection.destroy();
        }
        else {
            pagerAdapter.mainActivityState(false);
        }
        pagerAdapter.destroy();
        super.onDestroy();
    }
}