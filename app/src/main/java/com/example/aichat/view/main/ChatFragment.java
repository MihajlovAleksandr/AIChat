package com.example.aichat.view.main;

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

import com.example.aichat.R;
import com.example.aichat.controller.main.ChatFragmentController;
import com.example.aichat.model.entities.Message;
import com.example.aichat.view.LoginActivity;

public class ChatFragment extends Fragment {

    private RecyclerView rvMessages;
    private EditText tiMessage;
    private Button bSendMessage;
    private LoginActivity.MessageAdapter messageAdapter;
    private int currentUserId = 1;
    private int chatId = -1;
    private ChatFragmentController controller;

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

        // Инициализация контроллера для данного чата
        controller = new ChatFragmentController(chatId);
        messageAdapter = new LoginActivity.MessageAdapter(controller.loadMessages(), currentUserId);
        rvMessages.setAdapter(messageAdapter);

        tiMessage = view.findViewById(R.id.ti_message);
        bSendMessage = view.findViewById(R.id.b_send_message);
        bSendMessage.setOnClickListener(v -> {
            String messageText = tiMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                Message newMessage = controller.sendMessage(messageText, currentUserId);
                messageAdapter.notifyItemInserted(controller.getMessagesSize() - 1);
                rvMessages.scrollToPosition(controller.getMessagesSize() - 1);
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
}
