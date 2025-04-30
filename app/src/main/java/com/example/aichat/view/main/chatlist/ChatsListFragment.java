package com.example.aichat.view.main.chatlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

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
import com.example.aichat.model.entities.MessageChat;
import com.example.aichat.view.main.MainActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ChatsListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddChat;
    private ImageButton btnLanguage;
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
        chatAdapter = new ChatAdapter(chat -> {
            ((MainActivity) requireActivity()).openChat(chat.getId());
        });
        recyclerView.setAdapter(chatAdapter);

        // Инициализация кнопок
        btnLanguage = view.findViewById(R.id.btn_settings);
        fabAddChat = view.findViewById(R.id.fab_add_chat);

        // Установка обработчиков кликов
        btnLanguage.setOnClickListener(v -> openLanguageSettings());
        fabAddChat.setOnClickListener(v -> controller.addStopChat());

        loadChatsFromDatabase();

        return view;
    }

    public void loadChatsFromDatabase() {databaseExecutor.execute(() -> {
        List<Chat> chats = DatabaseManager.getDatabase().chatDao().getAllChats();
        List<Integer> chatIds = chats.stream()
                .map(Chat::getId)
                .collect(Collectors.toList());
        List<Message> lastMessages = DatabaseManager.getDatabase().messageDao().getLastMessages(chatIds);
        int currentMessage = 0;
        List<MessageChat> messageChats = new ArrayList<>();
        if(lastMessages.size()==0){
            for (Chat chat:chats) {
                messageChats.add(new MessageChat(null, chat));
            }
        }else {
            for (int i = 0; i < chats.size(); i++) {
                int finalI = i;
                if(currentMessage >= lastMessages.size()){
                    messageChats.add(new MessageChat(null, chats.get(finalI)));
                }
                else {
                    if (chats.get(finalI).getId() == lastMessages.get(currentMessage).getChat()) {
                        int finalCurrentMessage = currentMessage;
                        currentMessage++;
                        messageChats.add(new MessageChat(lastMessages.get(finalCurrentMessage), chats.get(finalI)));
                    } else {
                        messageChats.add(new MessageChat(null, chats.get(finalI)));
                    }
                }
            }}
        Collections.sort(messageChats);
        requireActivity().runOnUiThread(() -> {
            chatAdapter.setChats(messageChats);
        });
    });
    }
    public void updateLastMessage(Message message){
        requireActivity().runOnUiThread(() -> {
            chatAdapter.updateLastMessage(message);
        });
    }
    public void openLanguageSettings() {
        controller.openSettings(requireActivity());
    }


    public void createChat(Chat chat) {
        requireActivity().runOnUiThread(() -> {
            chatAdapter.addChat(new MessageChat(null, chat));
            recyclerView.scrollToPosition(0);
        });
    }
    public void createChat(MessageChat chat) {
        requireActivity().runOnUiThread(() -> {
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
        requireActivity().runOnUiThread(() -> chatAdapter.endChat(chat));
    }

    @Override
    public void onDestroy() {
        controller.Destroy();
        super.onDestroy();
    }
}