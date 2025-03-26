package com.example.aichat;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.aichat.model.ConnectionManager;
import com.example.aichat.model.ConnectionSingleton;
import com.example.aichat.model.DatabaseManager;
import com.example.aichat.model.TokenManager;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ChatPagerAdapter pagerAdapter;
    private int currentChatId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DatabaseManager.init(this);
        ConnectionManager connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        if(connectionManager == null) {
            ConnectionSingleton.getInstance().setConnectionManager(new ConnectionManager(TokenManager.getToken(this)));
            connectionManager =  ConnectionSingleton.getInstance().getConnectionManager();
        }
        viewPager = findViewById(R.id.view_pager);
        pagerAdapter = new ChatPagerAdapter(this, connectionManager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setUserInputEnabled(false);
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 1) {
            backToChats();
        } else {
            super.onBackPressed();
        }
    }

    public void openChat(int chatId) {
        currentChatId = chatId;
        pagerAdapter.setCurrentChatId(chatId);
        viewPager.setCurrentItem(1, true);
    }

    public void backToChats() {
        currentChatId = -1;
        viewPager.setCurrentItem(0, true);
    }
}
