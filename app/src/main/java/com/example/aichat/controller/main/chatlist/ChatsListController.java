package com.example.aichat.controller.main.chatlist;

import android.content.Intent;
import android.util.Log;
import androidx.fragment.app.FragmentActivity;
import com.example.aichat.dto.request.CreateChatRequest;
import com.example.aichat.dto.request.MatchmakingRequest;
import com.example.aichat.dto.request.RemoveUserFromChatRequest;
import com.example.aichat.dto.request.SearchGroupRequest;
import com.example.aichat.dto.request.UpdateChatNameRequest;
import com.example.aichat.dto.response.ChatResponse;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.LeaderboardActivity;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.ChatStatusSingleton;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.database.DatabaseSaver;
import com.example.aichat.model.database.GroupSearchModel;
import com.example.aichat.model.database.SearchingHandler;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.ChatType;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageChat;
import com.example.aichat.model.utils.mappers.ChatMapper;
import com.example.aichat.model.utils.mappers.Mapper;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.model.utils.mappers.MessageMapper;
import com.example.aichat.R;
import com.example.aichat.SettingsActivity;
import com.example.aichat.view.main.chatlist.ChatsListFragment;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@androidx.media3.common.util.UnstableApi
public class ChatsListController {
    private static final String TAG = "ChatsListController";

    private final SearchingHandler handler;
    private boolean isUserAdding;
    private final ChatsListFragment fragment;
    private final Mapper<com.example.aichat.dto.request.MessageRequest, Message, MessageResponse> messageMapper;
    private final MapperResponse<Chat, ChatResponse> chatMapper = new ChatMapper();
    private final UUID userId;
    private final DatabaseSaver saver;
    private final ConnectionDispatcher dispatcher;
    private List<MessageChat> allChats = new ArrayList<>();
    private CreateChatController createChatController;

    public ChatsListController(
            ChatsListFragment fragment,
            boolean isUserAdding,
            UUID userId
    ) {
        this.fragment = fragment;
        this.userId = userId;
        this.isUserAdding = isUserAdding;
        this.messageMapper = new MessageMapper(userId);
        this.handler = ChatStatusSingleton.getInstance().getHandler();
        this.dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
        AppDatabase db = DatabaseManager.getDatabase();
        this.saver = new DatabaseSaver(db, userId);

        registerUIHandlers();
    }

    private void registerUIHandlers() {
        Log.d(TAG, "Registering UI handlers (no DB operations)");

        dispatcher.addEventListener("ChatSearchingStatusUpdated",
                com.example.aichat.dto.response.ChatSearchingStatusResponse.class,
                command -> {
                    com.example.aichat.dto.response.ChatSearchingStatusResponse response = command.getPayload();
                    if (response != null) {
                        setIsChatSearching(response.isSearching);
                        fragment.setChatSearchingStatus();
                    }
                });

        dispatcher.addEventListener("GroupSearchingStatusUpdated",
                com.example.aichat.dto.response.GroupSearchingStatusResponse.class,
                command -> {
                    com.example.aichat.dto.response.GroupSearchingStatusResponse response = command.getPayload();
                    if (response != null) {
                        setIsGroupSearching(response.isSearching);
                        fragment.setGroupSearchingStatus();
                    }
                });
    }

    public void addChat(ChatType type) {
        if (type == ChatType.AI || type == ChatType.Group) {
            dispatcher.sendHttpRequestAsync("/api/chat", HttpClient.HTTPMethod.POST,
                    new CreateChatRequest(type, ""), false).thenAccept(cmd -> {
                if (cmd.isSuccess()) {
                    ChatResponse response = cmd.getData(ChatResponse.class);
                    createChat(response, type);
                    setIsGroupSearching(false);
                    saver.saveChatFromResponse(response);
                }
            });
        } else {
            dispatcher.sendHttpRequestAsync("/api/matchmaking/direct", HttpClient.HTTPMethod.POST,
                    new MatchmakingRequest(type, "", "AllMatch"), false).thenAccept(cmd -> {
                if (cmd.isSuccess()) {
                    setIsChatSearching(true);
                }
            });
        }
    }

    public void addUserToChat() {
        dispatcher.sendHttpRequestAsync("/api/matchmaking/group/user", HttpClient.HTTPMethod.POST,
                new SearchGroupRequest("Group"), false).thenAccept(cmd -> {
            if (cmd.isSuccess()) {
                setIsGroupSearching(true);
            }
        });
    }

    public void stopSearchingChat() {
        dispatcher.sendHttpRequestAsync("/api/matchmaking/direct", HttpClient.HTTPMethod.DELETE, null, false)
                .thenAccept(cmd -> {
                    if (cmd.isSuccess()) {
                        setIsChatSearching(false);
                    }
                });
    }

    public void stopAddingUserToChat() {
        dispatcher.sendHttpRequestAsync("/api/matchmaking/group", HttpClient.HTTPMethod.DELETE, null, false)
                .thenAccept(cmd -> {
                    if (cmd.isSuccess()) {
                        setIsGroupSearching(false);
                    }
                });
    }

    public void deleteChat(UUID id) {
        Log.d(TAG, "deleteChat called for chatId: " + id);

        dispatcher.sendHttpRequestAsync("/api/chat/" + id + "/users/", HttpClient.HTTPMethod.DELETE, null, false)
                .thenAccept(cmd -> {
                    if (cmd.isSuccess()) {
                        Log.d(TAG, "HTTP delete successful for chatId: " + id);
                        fragment.removeChat(id);
                        saver.removeChat(id);
                        sendDeleteChatNotification(id);
                    } else {
                        Log.e(TAG, "HTTP delete failed with code: " + cmd.getCode());
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "HTTP delete error", throwable);
                    return null;
                });
    }

    private void sendDeleteChatNotification(UUID chatId) {
        try {
            RemoveUserFromChatRequest request = new RemoveUserFromChatRequest(null, chatId);
            dispatcher.sendSignalRRequestAsync("RemoveUserFromChat", request);
            Log.d(TAG, "SignalR RemoveUserFromChat notification sent for chatId: " + chatId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SignalR notification", e);
        }
    }

    public void renameChat(UUID id, String newName) {
        dispatcher.sendHttpRequestAsync("/api/chat/" + id + "/name", HttpClient.HTTPMethod.PUT,
                new UpdateChatNameRequest(newName), false).thenAccept(cmd -> {
            if (cmd.isSuccess()) {
                fragment.updateChatName(id, newName);
                saver.updateChatName(id, newName);
            }
        });
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
        activity.startActivity(new Intent(activity, SettingsActivity.class));
    }

    public boolean getIsChatSearching() {
        return handler.getIsChatSearching();
    }

    public boolean getIsUserAddingToChat() {
        return handler.getGroupSearchModel().getIsSearching();
    }

    public void setIsChatSearching(boolean value) {
        fragment.requireActivity().runOnUiThread(() -> handler.setChatSearching(value));
    }

    public void setIsGroupSearching(boolean value) {
        fragment.requireActivity().runOnUiThread(() ->
                handler.setGroupSearchModel(new GroupSearchModel(value, null)));
    }

    public void Destroy() {

    }

    public CreateChatController getCreateChatController() {
        if (createChatController == null) {
            createChatController = new CreateChatController(userId);
        }
        return createChatController;
    }

    private void createChat(ChatResponse response, ChatType type) {
        if (response != null) {
            Chat chat = chatMapper.ToModel(response);
            fragment.getActivity().runOnUiThread(() -> fragment.createChat(chat));
            saver.saveChat(chat);
        }
    }

    public void setIsUserAdding(boolean value) {
        fragment.requireActivity().runOnUiThread(() -> {
            isUserAdding = value;
            fragment.setAddUserState();
        });
    }
}
