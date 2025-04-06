package com.example.aichat.view.main.chat;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.R;
import com.example.aichat.controller.main.chat.ChatFragmentController;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Message;
import com.example.aichat.view.main.MainActivity;

import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView rvMessages;
    private EditText tiMessage;
    private Button bSendMessage;
    private MessageAdapter messageAdapter;
    private ChatFragmentController controller;
    private int chatId;
    private int currentUserId;
    private ConnectionManager connectionManager;
    private View rootView;
    private FragmentActivity activity;

    public void setActivity(FragmentActivity activity) {
        this.activity = activity;
    }

    public ChatFragment(ConnectionManager connectionManager, int chatId, int currentUserId) {
        this.connectionManager = connectionManager;
        this.chatId = chatId;
        this.currentUserId = currentUserId;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_chat, container, false);

        // Инициализация UI компонентов
        rvMessages = rootView.findViewById(R.id.rv_messages);
        rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));

        ImageButton btnBack = rootView.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> navigateBack());

        tiMessage = rootView.findViewById(R.id.ti_message);
        bSendMessage = rootView.findViewById(R.id.b_send_message);

        // Настройка контроллера
        controller = new ChatFragmentController(this, connectionManager, chatId, currentUserId);

        // Запускаем загрузку сообщений
        new LoadMessagesTask().execute(chatId);

        bSendMessage.setOnClickListener(v -> {
            String messageText = tiMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                controller.sendMessage(messageText);
                tiMessage.setText("");
            }
        });

        return rootView;
    }
    public void sendMessage(Message  message){
        activity.runOnUiThread(()->{messageAdapter.addMessage(message);});
    }
    private void navigateBack() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).backToChats();
        }
    }

    private class LoadMessagesTask extends AsyncTask<Integer, Void, List<Message>> {
        @Override
        protected List<Message> doInBackground(Integer... params) {
            int chatId = params[0];
            return DatabaseManager.getDatabase().messageDao().getMessagesByChatId(chatId);
        }

        @Override
        protected void onPostExecute(List<Message> messages) {
            // Создаем адаптер с полученными сообщениями
            messageAdapter = new MessageAdapter(currentUserId, messages);
            rvMessages.setAdapter(messageAdapter);

            // Прокручиваем к последнему сообщению
            if (messages != null && !messages.isEmpty()) {
                rvMessages.scrollToPosition(messages.size() - 1);
            }
        }
    }
}