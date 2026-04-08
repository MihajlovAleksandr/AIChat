package com.example.aichat.controller.main.chatlist;

import android.content.Intent;

import androidx.fragment.app.FragmentActivity;

import com.example.aichat.SettingsActivity;
import com.example.aichat.dto.request.AddUserToChatRequest;
import com.example.aichat.dto.request.MessageRequest;
import com.example.aichat.dto.request.RemoveUserFromChatRequest;
import com.example.aichat.dto.request.SearchChatRequest;
import com.example.aichat.dto.request.UpdateChatNameRequest;
import com.example.aichat.dto.response.ChatResponse;
import com.example.aichat.dto.response.DeleteChatResponse;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.dto.response.SearchChatResponse;
import com.example.aichat.dto.response.SyncDBResponse;
import com.example.aichat.dto.response.UpdateChatNameResponse;
import com.example.aichat.dto.response.UpdateMessageStatusResponse;
import com.example.aichat.dto.response.UserAddingResponse;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.ChatType;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageChat;
import com.example.aichat.model.entities.MessageStatus;
import com.example.aichat.model.utils.ChatPdfExporter;
import com.example.aichat.model.utils.mappers.ChatMapper;
import com.example.aichat.model.utils.mappers.Mapper;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.model.utils.mappers.MessageMapper;
import com.example.aichat.view.main.chatlist.ChatsListFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChatsListController {

    private boolean isChatSearching = false;
    private boolean isUserAdding;
    private final ChatsListFragment fragment;
    private final ConnectionManager connectionManager;
    private final Mapper<MessageRequest, Message, MessageResponse> messageMapper;
    private final MapperResponse<Chat, ChatResponse> chatMapper = new ChatMapper();
    private final UUID userId;

    private List<MessageChat> allChats = new ArrayList<>();

    private CreateChatController createChatController;

    public ChatsListController(ChatsListFragment fragment,
                               ConnectionManager connectionManager,
                               boolean isUserAdding,
                               UUID userId) {
        this.fragment = fragment;
        this.isUserAdding = isUserAdding;
        this.connectionManager = connectionManager;
        this.userId = userId;
        this.messageMapper = new MessageMapper(userId);

        connectionManager.addConnectionEvent(connectionEvents);
    }

    private final OnConnectionEvents connectionEvents = new OnConnectionEvents() {
        @Override
        public void OnCommandGot(WSSCommand cmd) {

            switch (cmd.getOperation()) {

                case "SendMessage": {
                    MessageResponse messageResponse = cmd.getData(MessageResponse.class);
                    if (messageResponse != null) {
                        fragment.updateLastMessage(messageMapper.ToModel(messageResponse));
                    }
                    break;
                }

                case "CreateChat":
                case "AddChat": {
                    isUserAdding = false;
                    break;
                }

                case "EndChat": {
                    ChatResponse endedChat = cmd.getData(ChatResponse.class);
                    if (endedChat != null) {
                        fragment.endChat(chatMapper.ToModel(endedChat));
                    }
                    break;
                }

                case "SyncDB": {
                    SyncDBResponse response = cmd.getData(SyncDBResponse.class);
                    if (response != null) {
                        syncDB(response);
                    }
                    break;
                }

                case "SearchChat": {
                    SearchChatResponse response = cmd.getData(SearchChatResponse.class);
                    if (response != null) {
                        setIsChatSearching(response.isChatSearching);
                    }
                    break;
                }

                case "DeleteChat": {
                    DeleteChatResponse deleteChatResponse = cmd.getData(DeleteChatResponse.class);
                    if (deleteChatResponse != null) {
                        fragment.removeChat(deleteChatResponse.chatId);
                    }
                    break;
                }

                case "UserAdding": {
                    UserAddingResponse response = cmd.getData(UserAddingResponse.class);
                    setIsUserAdding(response != null && response.chatId != null);
                    break;
                }

                case "AddUserToChat":
                    setIsUserAdding(false);
                    break;

                case "UpdateChatName": {
                    cmd.getData(UpdateChatNameResponse.class);
                    break;
                }
                case "UpdateMessageStatus": {
                    UpdateMessageStatusResponse updateMessageStatusResponse =
                            cmd.getData(UpdateMessageStatusResponse.class);
                    if (updateMessageStatusResponse == null) break;

                    MessageChat msg = fragment.getMessageChat(updateMessageStatusResponse.chatId);
                    if (msg == null) break;

                    AppDatabase db = DatabaseManager.getDatabase();

                    for (UUID messageId : updateMessageStatusResponse.messageIds) {
                        db.messageDao().updateMessageStatusForUser(
                                messageId,
                                updateMessageStatusResponse.userId,
                                updateMessageStatusResponse.status
                        );
                        msg.removeUnreadMessage(messageId);
                        msg.updateLastMessageStatus(
                                updateMessageStatusResponse.userId,
                                updateMessageStatusResponse.status
                        );
                    }

                    fragment.updateMessageStatus(msg);
                    break;
                }
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

    private void syncDB(SyncDBResponse syncDBResponse) {
        if (syncDBResponse == null) return;

        ChatResponse[] newChatsResp = syncDBResponse.newChats;
        if (newChatsResp != null) {
            Chat[] newChats = ParseChat(newChatsResp);
            for (Chat newChat : newChats) {
                fragment.createChat(newChat);
            }
        }

        MessageResponse[] newMessagesResp = syncDBResponse.newMessages;
        if (newMessagesResp != null) {
            Message[] newMessages = ParseMessage(newMessagesResp);
            for (Message message : newMessages) {
                fragment.updateLastMessage(message);
            }
        }

        MessageResponse[] updatedMessagesResp = syncDBResponse.oldMessages;
        if (updatedMessagesResp != null) {
            Message[] updatedMessages = ParseMessage(updatedMessagesResp);
            for (Message updatedMessage : updatedMessages) {
                MessageChat msg = fragment.getMessageChat(updatedMessage.getChat());
                if (msg == null) continue;
                if (msg.getMessage() != null
                        && msg.getMessage().getId().equals(updatedMessage.getId())) {
                    fragment.updateLastMessage(updatedMessage);
                }
            }
        }

        MessageResponse[] deletedMessagesResp = syncDBResponse.deletedMessages;
        if (deletedMessagesResp != null) {
            Message[] deletedMessages = ParseMessage(deletedMessagesResp);
            for (Message deletedMessage : deletedMessages) {
                MessageChat msg = fragment.getMessageChat(deletedMessage.getChat());
                if (msg == null || msg.getMessage() == null) continue;
                if (msg.getMessage().getId().equals(deletedMessage.getId())) {
                    AppDatabase db = DatabaseManager.getDatabase();
                    Message last = db.messageDao()
                            .getLastMessageInChat(msg.getChat().getId());
                    fragment.updateLastMessage(last);
                }
            }
        }

        ChatResponse[] updatedChatsResp = syncDBResponse.oldChats;
        if (updatedChatsResp != null) {
            Chat[] updatedChats = ParseChat(updatedChatsResp);
            for (Chat updatedChat : updatedChats) {
                if (updatedChat.getEndTime() != null) {
                    fragment.endChat(updatedChat);
                }
            }
        }

        ChatResponse[] deletedChatsResp = syncDBResponse.deletedChats;
        if (deletedChatsResp != null) {
            Chat[] deletedChats = ParseChat(deletedChatsResp);
            for (Chat deletedChat : deletedChats) {
                fragment.removeChat(deletedChat.getId());
            }
        }
    }

    private Message[] ParseMessage(MessageResponse[] responses) {
        if (responses == null || responses.length == 0) return new Message[0];
        Message[] messages = new Message[responses.length];
        for (int i = 0; i < responses.length; i++) {
            messages[i] = messageMapper.ToModel(responses[i]);
        }
        return messages;
    }

    private Chat[] ParseChat(ChatResponse[] responses) {
        if (responses == null || responses.length == 0) return new Chat[0];
        Chat[] chats = new Chat[responses.length];
        for (int i = 0; i < responses.length; i++) {
            chats[i] = chatMapper.ToModel(responses[i]);
        }
        return chats;
    }

    public void addChat(ChatType param) {
        connectionManager.SendCommand(
                new WSSCommand("SearchChat", new SearchChatRequest(param))
        );
        setIsChatSearching(true);
    }

    public void addUserToChat() {
        connectionManager.SendCommand(
                new WSSCommand("AddUserToChat",
                        new AddUserToChatRequest(ChatType.GROUP, "AllMatch"))
        );
        setIsUserAdding(true);
    }

    public void stopSearchingChat() {
        connectionManager.SendCommand(new WSSCommand("StopSearchingChat"));
        setIsChatSearching(false);
    }

    public void stopAddingUserToChat() {
        connectionManager.SendCommand(new WSSCommand("StopAddingUserToChat"));
        setIsUserAdding(false);
    }

    public void deleteChat(UUID id) {
        connectionManager.SendCommand(
                new WSSCommand("RemoveUserFromChat",
                        new RemoveUserFromChatRequest(null, id))
        );
        fragment.removeChat(id);
    }

    public void renameChat(UUID id, String newName) {
        connectionManager.SendCommand(
                new WSSCommand("UpdateChatName", new UpdateChatNameRequest(id, newName))
        );
    }

    public void setAllChats(List<MessageChat> chats) {
        this.allChats = new ArrayList<>(chats);
    }

    public void searchChat(String query) {
        if (query == null || query.trim().isEmpty()) {
            fragment.rollbackChats();
            return;
        }

        String q = query.toLowerCase();

        List<MessageChat> filtered = allChats.stream()
                .filter(mc -> {
                    String name = mc.getChat().getName();
                    String last = mc.getMessage() != null ? mc.getMessage().getText() : "";
                    return (name != null && name.toLowerCase().contains(q)) ||
                            (last != null && last.toLowerCase().contains(q));
                })
                .collect(Collectors.toList());

        fragment.updateChatList(filtered);
    }

    public void cancelSearch() {
        fragment.rollbackChats();
    }

    public void openSettings(FragmentActivity activity) {
        Intent intent = new Intent(activity, SettingsActivity.class);
        activity.startActivity(intent);
    }

    public void exportChat(Chat chat) {
        new Thread(() -> {
            try {
                List<Message> messages =
                        DatabaseManager.getDatabase()
                                .messageDao()
                                .getMessagesByChatId(chat.getId());
                if (messages == null || messages.isEmpty()) return;
                ChatPdfExporter chatPdfExporter =
                        new ChatPdfExporter(fragment.getActivity(), userId);
                chatPdfExporter.exportChat(messages, chat.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public boolean getIsChatSearching() {
        return isChatSearching;
    }

    public boolean getIsUserAddingToChat() {
        return isUserAdding;
    }

    public void setIsChatSearching(boolean isChatSearching) {
        fragment.requireActivity().runOnUiThread(() -> {
            this.isChatSearching = isChatSearching;
            fragment.setCreateChatState();
        });
    }

    public void setIsUserAdding(boolean isUserAdding) {
        fragment.requireActivity().runOnUiThread(() -> {
            this.isUserAdding = isUserAdding;
            fragment.setAddUserState();
        });
    }

    public void Destroy() {
        connectionManager.removeConnectionEvent(connectionEvents);
    }

    public CreateChatController getCreateChatController() {
        if (createChatController == null) {
            createChatController = new CreateChatController(connectionManager, userId);
        }
        return createChatController;
    }
}
