package com.example.aichat.view.main;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.aichat.controller.main.ChatPageController;
import com.example.aichat.model.connection.ConnectionManager;

public class ChatPagerAdapter extends FragmentStateAdapter {
    private ChatPageController chatPageController;

    public ChatPagerAdapter(@NonNull FragmentActivity fragmentActivity, ConnectionManager connectionManager) {
        super(fragmentActivity);
        this.chatPageController = new ChatPageController(connectionManager);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            ChatFragment chatFragment = new ChatFragment();
            int currentChatId = chatPageController.getCurrentChatId();
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
