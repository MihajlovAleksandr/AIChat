package com.example.aichat.view.main.chatlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.R;
import com.example.aichat.controller.main.chatlist.ChatsListController;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.view.main.MainActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatsListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddChat;
    private ChatAdapter chatAdapter;
    private ChatsListController controller;
    private final Executor databaseExecutor = Executors.newSingleThreadExecutor();

    public ChatsListFragment(ConnectionManager connectionManager) {
        controller = new ChatsListController(this, connectionManager);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats_list, container, false);

        recyclerView = view.findViewById(R.id.rv_chats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Инициализируем адаптер с пустым списком вместо null
        chatAdapter = new ChatAdapter(new ArrayList<>(), chat -> {
            ((MainActivity) requireActivity()).openChat(chat.getId());
        });

        recyclerView.setAdapter(chatAdapter);
        chatAdapter.setRecyclerView(recyclerView);

        // Загружаем чаты в отдельном потоке
        loadChatsFromDatabase();

        fabAddChat = view.findViewById(R.id.fab_add_chat);
        fabAddChat.setOnClickListener(v -> {
            controller.addStopChat();
        });

        return view;
    }

    private void loadChatsFromDatabase() {
        databaseExecutor.execute(() -> {
            List<Chat> chats = DatabaseManager.getDatabase().chatDao().getAllChats();
            requireActivity().runOnUiThread(() -> {
                // Вместо setChats, добавляем все чаты через addChat
                for (Chat chat : chats) {
                    chatAdapter.addChat(chat);
                }
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(false) {
                    @Override
                    public void handleOnBackPressed() {
                    }
                });
    }

    public void createChat(Chat chat) {
        getActivity().runOnUiThread(() -> {
            chatAdapter.addChat(chat);
            recyclerView.scrollToPosition(0);
        });
    }

    public void setFabAddChatState(boolean isAdd) {
        fabAddChat.setImageResource(
                isAdd ? android.R.drawable.ic_input_add : R.drawable.ic_cancel
        );
    }

    public void endChat(Chat chat) {
        chatAdapter.endChat(chat);
    }

    @Override
    public void onDestroy() {
        controller.Destroy();
        super.onDestroy();
    }
}