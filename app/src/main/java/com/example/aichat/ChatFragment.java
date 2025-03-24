package com.example.aichat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView rvMessages;
    private EditText tiMessage;
    private Button bSendMessage;
    private MessageAdapter messageAdapter;
    private List<Message> messages;
    private int currentUserId = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        rvMessages = view.findViewById(R.id.rvMessages);
        rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));

        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(messages, currentUserId);
        rvMessages.setAdapter(messageAdapter);

        addSampleMessages();

        tiMessage = view.findViewById(R.id.tiMessage);
        bSendMessage = view.findViewById(R.id.bSendMessage);

        bSendMessage.setOnClickListener(v -> {
            String messageText = tiMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                Message message = new Message(messageText, currentUserId, 1);
                messages.add(message);
                messageAdapter.notifyItemInserted(messages.size() - 1);
                tiMessage.setText("");
            }
        });
        rvMessages.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    // Сохраняем текущую позицию прокрутки
                    final int scrollY = rvMessages.computeVerticalScrollOffset();

                    rvMessages.post(new Runnable() {
                        @Override
                        public void run() {
                            if (messageAdapter != null) {
                                // Восстанавливаем позицию прокрутки
                                rvMessages.scrollBy(0, scrollY);
                            }
                        }
                    });
                }
            }
        });
        return view;
    }

    private void addSampleMessages() {
        messages.add(new Message("Привет! Все отлично, спасибо!", currentUserId, 1));
        messages.add(new Message("Как дела?", 2, 1));
        messages.add(new Message("Все хорошо, спасибо!", currentUserId, 1));
        messageAdapter.notifyDataSetChanged();
    }
}