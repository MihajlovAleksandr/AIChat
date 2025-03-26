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

import com.example.aichat.model.Chat;
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
        fabAddChat.setOnClickListener(v -> addNewChat(new Chat()));

        return view;
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
    private void initSampleChats() {
        Chat chat1 = new Chat();
        Chat chat2 = new Chat();
        Chat chat3 = new Chat();
        chatList.add(chat1);
        chatList.add(chat2);
        chatList.add(chat3);
    }

    public void addNewChat(Chat newChat) {
        chatList.add(0, newChat);
        chatAdapter.notifyItemInserted(0);
        recyclerView.scrollToPosition(0);
    }
}