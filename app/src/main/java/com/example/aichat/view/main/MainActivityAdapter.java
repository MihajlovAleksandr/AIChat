package com.example.aichat.view.main;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.ActionMenuItem;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.aichat.controller.main.MainActivityController;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.entities.Command;
import com.example.aichat.view.main.chat.ChatFragment;
import com.example.aichat.view.main.chatlist.ChatsListFragment;

public class MainActivityAdapter extends FragmentStateAdapter {
    private MainActivityController chatPageController;
    private ConnectionManager connectionManager;
    private final int currentUserId;
    private long lastChatFragmentId = System.currentTimeMillis();
    private ChatsListFragment chatsListFragment;
    private FragmentActivity fragmentActivity;

    public MainActivityAdapter(@NonNull FragmentActivity fragmentActivity,
                               ConnectionManager connectionManager,
                               int currentUserId,boolean isNewActivity) {
        super(fragmentActivity);
        this.fragmentActivity = fragmentActivity;
        this.currentUserId = currentUserId;
        this.chatPageController = new MainActivityController(connectionManager, fragmentActivity,this,  isNewActivity);
        this.connectionManager = connectionManager;
    }

    public void setChatId(int chatId) {
        chatPageController.setCurrentChatId(chatId);
        lastChatFragmentId = System.currentTimeMillis();
        fragmentActivity.runOnUiThread(() -> notifyItemChanged(1));
    }
    public void loadChatList()
    {
        chatsListFragment.loadChatsFromDatabase();
    }

    public int getCurrentUserId(){
        return currentUserId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.d("Fragment", "Create Fragment");
        if (position == 1) {
            // Создаем фрагмент с привязкой к текущей Activity
            ChatFragment fragment = new ChatFragment(connectionManager,
                    chatPageController.getCurrentChatId(),
                    currentUserId);
            // Убедимся, что фрагмент прикреплен к Activity
            fragment.setActivity(fragmentActivity);
            return fragment;
        }
        chatsListFragment = new ChatsListFragment(connectionManager);
        return chatsListFragment;
    }

    public void logout(Activity activity){
        chatPageController.logout(activity);
    }

    public void mainActivityState(boolean isOnline){
        Command command =  new Command("MainActivityState");
        command.addData("isOnline",isOnline);
        connectionManager.SendCommand(command);
    }

    public void destroy(){
        chatPageController.destroy();
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    @Override
    public long getItemId(int position) {
        return position == 1 ? lastChatFragmentId : super.getItemId(position);
    }

    @Override
    public boolean containsItem(long itemId) {
        return itemId == lastChatFragmentId || super.containsItem(itemId);
    }
}