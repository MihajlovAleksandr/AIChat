package com.example.aichat;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.model.AppDatabase;
import com.example.aichat.model.DatabaseClient;
import com.example.aichat.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView rvMessages;
    private EditText tiMessage;
    private Button bSendMessage;
    private MessageAdapter messageAdapter;
    private List<Message> messages;
    private int currentUserId = 1;
    private int chatId = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        if (getArguments() != null) {
            chatId = getArguments().getInt("chatId", -1);
        }
        Log.d("ChatFragment", "Chat ID: " + chatId);

        rvMessages = view.findViewById(R.id.rv_messages);
        rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> navigateBack());


        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(messages, currentUserId);
        rvMessages.setAdapter(messageAdapter);

        loadMessages(chatId);

        tiMessage = view.findViewById(R.id.ti_message);
        bSendMessage = view.findViewById(R.id.b_send_message);
        bSendMessage.setOnClickListener(v -> {
            String messageText = tiMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                Message message = new Message(messageText, currentUserId, chatId);
                messages.add(message);
                messageAdapter.notifyItemInserted(messages.size() - 1);
                rvMessages.scrollToPosition(messages.size() - 1);
                tiMessage.setText("");
            }
        });

        return view;
    }
    private void navigateBack() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).backToChats();
        }
    }
    private void loadMessages(int chatId) {
        messageAdapter.notifyDataSetChanged();
    }
}
