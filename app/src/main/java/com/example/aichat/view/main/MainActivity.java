package com.example.aichat.view.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.aichat.R;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.entities.Command;
import com.example.aichat.view.BaseActivity;

public class MainActivity extends BaseActivity {

    private ViewPager2 viewPager;
    private MainActivityAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DatabaseManager.init(this);
        ConnectionManager connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        if (connectionManager == null) {
            ConnectionSingleton.getInstance().setConnectionManager(new ConnectionManager(SecurePreferencesManager.getAuthToken(this)));
            connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        }
        viewPager = findViewById(R.id.view_pager);
        Intent intent = getIntent();
        int userId = intent.getIntExtra("userId", -1);
        if(userId==-1){
            userId = SecurePreferencesManager.getUserId(this);
        }
        else{
            connectionManager.SendCommand(new Command("SyncDB"));
        }
        pagerAdapter = new MainActivityAdapter(this, connectionManager, userId);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setUserInputEnabled(false);

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



    public void openChat(int chatId) {
        pagerAdapter.setChatId(chatId);
        viewPager.setCurrentItem(1, true);
    }

    public void backToChats() {
        viewPager.setCurrentItem(0, true);
    }
}
