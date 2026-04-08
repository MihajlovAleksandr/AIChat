package com.example.aichat.view.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.aichat.R;
import com.example.aichat.controller.main.MainActivityController;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.view.main.chat.ChatFragment;
import com.example.aichat.view.main.chatlist.ChatsListFragment;

import java.util.UUID;


public class MainActivityAdapter extends FragmentStateAdapter {

    private final MainActivityController controller;
    private final UUID currentUserId;
    private final FragmentActivity activity;

    private ChatsListFragment chatsListFragment;

    public MainActivityAdapter(
            @NonNull FragmentActivity activity,
            UUID ignoredUserId,
            boolean isNewActivity
    ) {
        super(activity);

        this.activity = activity;
        this.currentUserId = SecurePreferencesManager.getUserId(activity);

        controller = new MainActivityController(
                activity,
                this,
                currentUserId,
                isNewActivity
        );
    }
    @NonNull
    @Override
    public androidx.fragment.app.Fragment createFragment(int position) {
        if (position == 1) {
            UUID chatId = controller.getCurrentChatId();
            return ChatFragment.newInstance(chatId, currentUserId);
        }

        if (chatsListFragment == null) {
            chatsListFragment = ChatsListFragment.newInstance(false, currentUserId);
            chatsListFragment.loadChatsFromDatabase(currentUserId);
        }

        return chatsListFragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public void setChatId(@Nullable UUID chatId) {
        controller.setCurrentChatId(chatId);

        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        FragmentManager fm = activity.getSupportFragmentManager();
        String tag = "CHAT_FRAGMENT";
        ChatFragment existing = (ChatFragment) fm.findFragmentByTag(tag);
        if (existing != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(existing);
            ft.commitNowAllowingStateLoss();
        }

        if (chatId != null) {
            ChatFragment newFragment = ChatFragment.newInstance(chatId, currentUserId);
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.view_pager_fragment_container, newFragment, tag);
            ft.commitNowAllowingStateLoss();
        }
    }

    public UUID getCurrentChatId() {
        return controller.getCurrentChatId();
    }

    public void onChatCreated(Chat chat) {
        if (chatsListFragment != null) {
            chatsListFragment.createChat(chat);
        }
    }

    public void reloadChats() {
        if (chatsListFragment != null) {
            chatsListFragment.loadChatsFromDatabase(currentUserId);
        }
    }


    public boolean destroy() {
        boolean isLogout = controller.destroy();
        if (!isLogout) {
            com.example.aichat.model.connection.ConnectionSingleton.getInstance().setAvailableToClose(false);
        }
        chatsListFragment = null;
        return isLogout;
    }
}