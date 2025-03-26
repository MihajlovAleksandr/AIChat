package com.example.aichat.view.main;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.aichat.R;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.TokenManager;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ChatPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DatabaseManager.init(this);

        ConnectionManager connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        if (connectionManager == null) {
            ConnectionSingleton.getInstance().setConnectionManager(new ConnectionManager(TokenManager.getToken(this)));
            connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        }

        viewPager = findViewById(R.id.view_pager);
        pagerAdapter = new ChatPagerAdapter(this, connectionManager);
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
        viewPager.setCurrentItem(1, true);
    }

    public void backToChats() {
        viewPager.setCurrentItem(0, true);
    }
}
