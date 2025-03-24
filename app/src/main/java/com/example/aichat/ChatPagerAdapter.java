package com.example.aichat;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.concurrent.ConcurrentNavigableMap;

public class ChatPagerAdapter extends FragmentStateAdapter {

    private int currentChatId = -1;
    private ConnectionManager connectionManager;

    public ChatPagerAdapter(@NonNull FragmentActivity fragmentActivity, ConnectionManager connectionManager) {
        super(fragmentActivity);
        this.connectionManager =  connectionManager;
        connectionManager.SetCommandGot(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {

            }

            @Override
            public void OnConnectionFailed() {

            }

            @Override
            public void OnOpen() {

            }
        });
    }

    public void setCurrentChatId(int chatId) {
        this.currentChatId = chatId;
        notifyItemChanged(1);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            ChatFragment chatFragment = new ChatFragment();
            if (currentChatId != -1) {
                Bundle args = new Bundle();
                args.putInt("chatId", currentChatId);
                chatFragment.setArguments(args);
            }
            return chatFragment;
        }
        return new ChatsListFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
