package com.example.aichat.controller.main.chat.actions;

import android.util.Log;

import com.example.aichat.dto.request.SearchUserRequest;
import com.example.aichat.dto.response.RemoveUserFromChatResponse;
import com.example.aichat.dto.response.UserDataResponse;
import com.example.aichat.dto.response.UserInfoResponse;

import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.ChatStatusSingleton;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.database.DatabaseSaver;
import com.example.aichat.model.database.GroupSearchModel;
import com.example.aichat.model.database.GroupSearchingStatusChangedListener;
import com.example.aichat.model.database.SearchingHandler;
import com.example.aichat.model.entities.User;
import com.example.aichat.model.entities.UserData;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.model.utils.mappers.UserDataMapper;
import com.example.aichat.view.main.chat.ChatFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ChatMembersActions {

    private static final String SEARCH_MODE = "AllMatch";

    private final ChatFragment fragment;
    private final UUID chatId;
    private final UUID currentUserId;
    private final SearchingHandler handler;
    private final ConnectionDispatcher dispatcher;
    private final DatabaseSaver saver;
    private final MapperResponse<UserData, UserDataResponse> userDataMapper;

    private final List<User> users = new ArrayList<>();

    public ChatMembersActions(ChatFragment fragment, UUID chatId, UUID currentUserId) {
        this.fragment = fragment;
        this.chatId = chatId;
        this.currentUserId = currentUserId;
        this.userDataMapper = new UserDataMapper();
        AppDatabase db = DatabaseManager.getDatabase();
        this.saver = new DatabaseSaver(db, currentUserId);
        dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
        handler = ChatStatusSingleton.getInstance().getHandler();
        handler.addGroupSearchingStatusChangedListener(new GroupSearchingStatusChangedListener() {
            @Override
            public void handle(GroupSearchModel groupSearchModel) {
                if (groupSearchModel.getIsSearching())
                    fragment.setSearchingChatId(groupSearchModel.getChatId());
                else fragment.canselSearch();
            }
        });
    }

    public void addUserToChat() {
        dispatcher.sendHttpRequestAsync("/api/matchmaking/group/chat", HttpClient.HTTPMethod.POST, new SearchUserRequest(chatId, ChatMembersActions.SEARCH_MODE), false)
                .thenAccept(cmd -> {
                    if (cmd.isSuccess()) {
                        handler.setGroupSearchModel(new GroupSearchModel(true, chatId));
                    }
                });
    }

    public void stopSearchingChat() {
        dispatcher.sendHttpRequestAsync("/api/matchmaking/group", HttpClient.HTTPMethod.DELETE, new SearchUserRequest(chatId, ChatMembersActions.SEARCH_MODE), false)
                .thenAccept(cmd -> {
                    if (cmd.isSuccess()) {
                        handler.setGroupSearchModel(new GroupSearchModel(false, null));
                    }
                });
    }

    public CompletableFuture<List<User>> loadUsers(List<UUID> userIds) {
        List<CompletableFuture<User>> futures = new ArrayList<>();

        for (UUID userId : userIds) {
            CompletableFuture<User> future = dispatcher
                    .sendHttpRequestAsync("/api/user/" + userId + "/userdata/", HttpClient.HTTPMethod.GET, null, true)
                    .thenApply(cmd -> {
                        if (cmd.isSuccess()) {
                            UserInfoResponse response = cmd.getData(UserInfoResponse.class);
                            return new User(userId, userDataMapper.ToModel(response.userData), response.lastOnline, response.region);
                        } else {
                            Log.e("loadUsers: ", "Can't load users");
                            return null;
                        }
                    });

            futures.add(future);
        }

        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }


    public void removeUserFromChat(UUID userId) {
        dispatcher.sendHttpRequestAsync("/api/chat/"+chatId+"/users/"+userId, HttpClient.HTTPMethod.DELETE, null, false)
                .thenAccept(cmd->{
                    if(cmd.isSuccess())
                        removeUserFromChatUI(userId);

                });
    }

    public void onUserRemoved(RemoveUserFromChatResponse response) {
        if (!chatId.equals(response.chatId)) return;

        // ДОБАВИТЬ СОХРАНЕНИЕ В БД
        if (saver != null) {
            saver.removeUserFromChat(response);
        }

        removeUserFromChat(response.userId);
    }

    private void removeUserFromChatUI(UUID userId){
        if (userId.equals(currentUserId)) {
            if (fragment.isAdded()) fragment.close();
            return;
        }

        users.removeIf(u -> u.getId().equals(userId));

        if (fragment.isAdded()) {
            fragment.showUsers(new ArrayList<>(users));
        }
    }

    public List<User> getUsers() {
        return new ArrayList<>(users);
    }
}
