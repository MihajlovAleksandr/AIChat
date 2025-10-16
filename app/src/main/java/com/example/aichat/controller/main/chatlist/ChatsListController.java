package com.example.aichat.controller.main.chatlist;

import android.content.Intent;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.example.aichat.SettingsActivity;
import com.example.aichat.dto.request.MessageRequest;
import com.example.aichat.dto.request.SearchChatRequest;
import com.example.aichat.dto.response.ChatResponse;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.dto.response.SearchChatResponse;
import com.example.aichat.dto.response.SyncDBResponse;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.ChatType;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageChat;
import com.example.aichat.model.utils.mappers.ChatMapper;
import com.example.aichat.model.utils.mappers.Mapper;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.model.utils.mappers.MessageMapper;
import com.example.aichat.view.main.chatlist.ChatsListFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatsListController {
    private boolean isChatSearching = false;
    private final ChatsListFragment fragment;
    private final ConnectionManager connectionManager;
    private final Mapper<MessageRequest, Message, MessageResponse> messageMapper = new MessageMapper();
    private final MapperResponse<Chat, ChatResponse> chatMapper = new ChatMapper();

    public ChatsListController(ChatsListFragment fragment, ConnectionManager connectionManager) {
        this.fragment = fragment;
        Log.d("ChatsListController", "Constructor started");
        connectionManager.addConnectionEvent(connectionEvents);
        this.connectionManager = connectionManager;
    }

    private OnConnectionEvents connectionEvents = new OnConnectionEvents() {
        @Override
        public void OnCommandGot(Command command) {
            switch (command.getOperation()) {
                case "SendMessage":
                    MessageResponse messageResponse = command.getData(MessageResponse.class);
                    fragment.updateLastMessage(messageMapper.ToModel(messageResponse));
                    break;
                case "CreateChat":
                    ChatResponse createdChat = command.getData(ChatResponse.class);
                    fragment.requireActivity().runOnUiThread(() -> {
                        fragment.createChat(chatMapper.ToModel(createdChat));
                        fragment.setFabAddChatState(true);
                    });
                    isChatSearching = false;
                    break;
                case "EndChat":
                    ChatResponse endedChat = command.getData(ChatResponse.class);
                    fragment.endChat(chatMapper.ToModel(endedChat));
                    break;
                case "SyncDB":
                    SyncDBResponse syncDBResponse = command.getData(SyncDBResponse.class);
                    setIsChatSearching(syncDBResponse.isChatSearching);
                    Chat[] newChats = ParseChat(syncDBResponse.newChats);
                    Message[] messageList = ChatController.getLastMessages(ParseMessage(syncDBResponse.newMessages));
                    int currentMessage = 0;
                    List<MessageChat> messageChats = new ArrayList<>();

                    for (int i = 0; i < newChats.length; i++) {
                        if (!newChats[i].isActive()) {
                            messageChats.add(new MessageChat(null, newChats[i]));
                        }
                        boolean isAdded = false;
                        UUID currentChatId = newChats[i].getId();

                        for (int j = currentMessage; j < messageList.length; j++) {
                            UUID messageChatId = messageList[j].getChat();
                            int comparison = currentChatId.compareTo(messageChatId);

                            if (comparison == 0) { // IDs равны
                                fragment.createChat(new MessageChat(messageList[j], newChats[i]));
                                currentMessage = j + 1;
                                isAdded = true;
                                break;
                            } else if (comparison < 0) { // currentChatId < messageChatId
                                currentMessage = j;
                                break;
                            } else { // currentChatId > messageChatId
                                fragment.updateLastMessage(messageList[j]);
                                currentMessage = j + 1;
                            }
                        }
                        if (!isAdded) {
                            fragment.createChat(newChats[i]);
                        }
                    }

                    Chat[] oldChats = ParseChat(syncDBResponse.oldChats);
                    for (Chat chat : oldChats) {
                        fragment.endChat(chat);
                    }
                    break;
                case "SearchChat":
                    setIsChatSearching(command.getData(SearchChatResponse.class).isChatSearching);
                    break;
            }
        }

        @Override
        public void OnConnectionFailed() {
            fragment.setConnectionSuccess(false);
        }

        @Override
        public void OnOpen() {
            fragment.setConnectionSuccess(true);
        }
    };

    private Message[] ParseMessage(MessageResponse[] responses){
        Message[] messages = new Message[responses.length];
        for (int i = 0; i < responses.length; i++) {
            messages[i] = messageMapper.ToModel(responses[i]);
        }
        return  messages;
    }
    private Chat[] ParseChat(ChatResponse[] responses){
        Chat[] chats = new Chat[responses.length];
        for (int i = 0; i < responses.length; i++) {
            chats[i] = chatMapper.ToModel(responses[i]);
        }
        return chats;
    }

    public void addChat(ChatType param) {
        Command addChat = new Command("SearchChat", new SearchChatRequest(param));
        connectionManager.SendCommand(addChat);
        setIsChatSearching(true);
    }

    public void stopSearchingChat() {
        connectionManager.SendCommand(new Command("StopSearchingChat"));
        setIsChatSearching(false);
    }

    public boolean getIsChatSearching() {
        return isChatSearching;
    }

    public void setIsChatSearching(boolean isChatSearching) {
        fragment.requireActivity().runOnUiThread(() -> {
            this.isChatSearching = isChatSearching;
            fragment.setFabAddChatState(!isChatSearching);
        });
    }

    public void openSettings(FragmentActivity activity) {
        Intent intent = new Intent(activity, SettingsActivity.class);
        activity.startActivity(intent);
    }

    public void searchChat(String query) {
        new Thread(() -> {
            AppDatabase appDatabase = DatabaseManager.getDatabase();
            List<Message> messages = appDatabase.messageDao().getMessagesByText("%" + query + "%");
            List<MessageChat> messageChats = new ArrayList<MessageChat>();
            for (int i = 0; i < messages.size(); i++) {
                Message msg = messages.get(i);
                messageChats.add(new MessageChat(msg, appDatabase.chatDao().getChatById(msg.getChat())));
            }
            fragment.updateChatList(messageChats);
        }).start();
    }

    public void cancelSearch() {
        fragment.rollbackChats();
    }

    public void Destroy() {
        connectionManager.removeConnectionEvent(connectionEvents);
    }
}