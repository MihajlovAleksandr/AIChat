package com.example.aichat.view.main;

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
import com.example.aichat.controller.main.ChatsListController;
import com.example.aichat.model.entities.Chat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ChatsListFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private ChatsListController controller;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats_list, container, false);

        recyclerView = view.findViewById(R.id.rv_chats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        controller = new ChatsListController();
        chatAdapter = new ChatAdapter(controller.getChats(), chat -> {
            ((MainActivity) requireActivity()).openChat(chat.getId());
        });
        recyclerView.setAdapter(chatAdapter);

        FloatingActionButton fabAddChat = view.findViewById(R.id.fab_add_chat);
        fabAddChat.setOnClickListener(v -> {
            Chat newChat = new Chat();
            controller.addChat(newChat);
            chatAdapter.notifyItemInserted(0);
            recyclerView.scrollToPosition(0);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(false) {
                    @Override
                    public void handleOnBackPressed() {
                        // Дополнительная обработка нажатия «Назад», если потребуется
                    }
                });
    }
}
