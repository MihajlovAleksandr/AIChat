package com.example.aichat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class ChatsListFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<Chat> chatList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats_list, container, false);

        recyclerView = view.findViewById(R.id.rv_chats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        initSampleChats();

        chatAdapter = new ChatAdapter(chatList, chat -> {
            ((MainActivity) requireActivity()).openChat(chat.getId());
        });
        recyclerView.setAdapter(chatAdapter);

        FloatingActionButton fabAddChat = view.findViewById(R.id.fab_add_chat);
        fabAddChat.setOnClickListener(v -> addNewChat());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Отключаем обработку кнопки "Назад" в списке чатов
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(false) {
                    @Override
                    public void handleOnBackPressed() {
                        // Ничего не делаем - кнопка "Назад" будет работать по умолчанию
                    }
                });
    }
    private void initSampleChats() {
        // Создаем чаты с разными ID
        Chat chat1 = new Chat(1, new int[]{1, 2}, "Привет из первого чата", true);
        Chat chat2 = new Chat(2, new int[]{1, 3}, "Сообщение из второго чата", true);
        Chat chat3 = new Chat(3, new int[]{1, 4}, "Завершенный диалог", false);
        chat3.endChat();

        chatList.add(chat1);
        chatList.add(chat2);
        chatList.add(chat3);
    }

    public void addNewChat() {
        int newId = chatList.isEmpty() ? 1 : chatList.toArray().length + 1;
        Chat newChat = new Chat(newId, new int[]{1, 4}, "Чат начат", true);
        chatList.add(0, newChat);
        chatAdapter.notifyItemInserted(0);
        recyclerView.scrollToPosition(0);
    }
}