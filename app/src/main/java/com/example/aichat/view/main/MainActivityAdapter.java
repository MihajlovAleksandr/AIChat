package com.example.aichat.view.main;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.aichat.controller.main.MainActivityController;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.R;
import com.example.aichat.view.main.chat.ChatFragment;
import com.example.aichat.view.main.chatlist.ChatsListFragment;
import java.util.UUID;

public class MainActivityAdapter extends FragmentStateAdapter {

    private static final String TAG = "MainActivityAdapter";
    private static final String CHAT_FRAGMENT_TAG = "CHAT_FRAGMENT";

    private final MainActivityController controller;
    private final UUID currentUserId;
    private final FragmentActivity activity;

    private ChatsListFragment chatsListFragment;

    public MainActivityAdapter(@NonNull FragmentActivity activity, UUID ignoredUserId, boolean isNewActivity) {
        super(activity);
        this.activity = activity;
        this.currentUserId = SecurePreferencesManager.getUserId(activity);
        controller = new MainActivityController(activity, this, currentUserId);
        createChatsListFragment();
    }

    private void createChatsListFragment() {
        if (currentUserId == null) return;

        chatsListFragment = ChatsListFragment.newInstance(false, currentUserId);
        chatsListFragment.loadChatsFromDatabase(currentUserId);
        controller.setChatsListFragment(chatsListFragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (chatsListFragment == null) createChatsListFragment();
        return chatsListFragment != null ? chatsListFragment : new Fragment();
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public void setChatId(@Nullable UUID chatId) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment existing = fragmentManager.findFragmentByTag(CHAT_FRAGMENT_TAG);
        UUID previousChatId = controller.getCurrentChatId();

        if (chatId != null && existing instanceof ChatFragment && previousChatId != null && previousChatId.equals(chatId)) {
            Log.d(TAG, "Same chat already opened, skip replace: " + chatId);
            return;
        }

        controller.setCurrentChatId(chatId);

        if (existing != null) {
            FragmentTransaction removeTransaction = fragmentManager.beginTransaction();
            removeTransaction.remove(existing);
            removeTransaction.commitNowAllowingStateLoss();
        }

        if (chatId == null) {
            Log.d(TAG, "Chat fragment removed");
            return;
        }

        if (currentUserId == null) {
            Log.e(TAG, "Cannot open chat: currentUserId is null");
            return;
        }

        ChatFragment newFragment = ChatFragment.newInstance(chatId, currentUserId);
        FragmentTransaction addTransaction = fragmentManager.beginTransaction();
        addTransaction.replace(R.id.view_pager_fragment_container, newFragment, CHAT_FRAGMENT_TAG);
        addTransaction.commitNowAllowingStateLoss();

        Log.d(TAG, "Chat fragment replaced: " + chatId);
    }

    public UUID getCurrentChatId() {
        return controller.getCurrentChatId();
    }

    public void onChatCreated(Chat chat) {
        if (chatsListFragment != null && chat != null) chatsListFragment.createChat(chat);
    }

    public void reloadChats() {
        if (chatsListFragment != null) chatsListFragment.loadChatsFromDatabase(currentUserId);
    }

    public void connect() {
        controller.connect();
    }

    public MainActivityController getController() {
        return controller;
    }

    public ChatsListFragment getChatsListFragment() {
        return chatsListFragment;
    }

    public boolean destroy() {
        boolean isLogout = controller.destroy();
        removeOpenedChatFragment();
        chatsListFragment = null;
        return isLogout;
    }

    private void removeOpenedChatFragment() {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment existing = fragmentManager.findFragmentByTag(CHAT_FRAGMENT_TAG);

        if (existing == null) return;

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.remove(existing);
        transaction.commitAllowingStateLoss();
    }
}
