package com.example.aichat.view.main.chatlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.R;
import com.example.aichat.controller.main.chatlist.ChatsListController;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Message;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.LanguageFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatsListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddChat;
    private Button btnLanguage;
    private ChatAdapter chatAdapter;
    private ChatsListController controller;
    private final Executor databaseExecutor = Executors.newSingleThreadExecutor();
    public ChatsListFragment() {
        this.controller = new ChatsListController(this, ConnectionSingleton.getInstance().getConnectionManager());
    }
    public ChatsListFragment(ConnectionManager connectionManager) {
        this.controller = new ChatsListController(this, connectionManager);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats_list, container, false);

        // Инициализация RecyclerView
        recyclerView = view.findViewById(R.id.rv_chats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Инициализация адаптера
        chatAdapter = new ChatAdapter(new ArrayList<>(), chat -> {
            ((MainActivity) requireActivity()).openChat(chat.getId());
        });
        recyclerView.setAdapter(chatAdapter);

        // Инициализация кнопок
        btnLanguage = view.findViewById(R.id.btn_language);
        fabAddChat = view.findViewById(R.id.fab_add_chat);

        // Установка обработчиков кликов
        btnLanguage.setOnClickListener(v -> openLanguageSettings());
        fabAddChat.setOnClickListener(v -> controller.addStopChat());

        // Загрузка чатов из базы данных
        loadChatsFromDatabase();

        return view;
    }

    private void loadChatsFromDatabase() {
        databaseExecutor.execute(() -> {
            List<Chat> chats = DatabaseManager.getDatabase().chatDao().getAllChats();
            for (Chat chat : chats) {
                Message message = DatabaseManager.getDatabase().messageDao().getLastMessageInChat(chat.getId());
                requireActivity().runOnUiThread(() -> {
                    chatAdapter.addChat(chat, message);
                });
            }
        });
    }
    public void updateLastMessage(Message message){
        requireActivity().runOnUiThread(() -> {
            chatAdapter.updateLastMessage(message);
        });
    }
    private void openLanguageSettings() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, new LanguageFragment())
                .addToBackStack(null)
                .commit();
    }


    public void createChat(Chat chat) {
        requireActivity().runOnUiThread(() -> {
            chatAdapter.addChat(chat, null);
            recyclerView.scrollToPosition(0);
        });
    }

    public void setFabAddChatState(boolean isAdd) {
        fabAddChat.setImageResource(
                isAdd ? android.R.drawable.ic_input_add : R.drawable.ic_cancel
        );
    }

    public void endChat(Chat chat) {
        requireActivity().runOnUiThread(() -> chatAdapter.endChat(chat));
    }

    @Override
    public void onDestroy() {
        controller.Destroy();
        super.onDestroy();
    }
}