package com.example.aichat.controller.main.chat.actions;

import com.example.aichat.dto.request.AddOtherUserToChatRequest;
import com.example.aichat.dto.request.RemoveUserFromChatRequest;
import com.example.aichat.dto.request.UsersInChatRequest;
import com.example.aichat.dto.response.AddUserToChatResponse;
import com.example.aichat.dto.response.RemoveUserFromChatResponse;
import com.example.aichat.dto.response.UserDataResponse;
import com.example.aichat.dto.response.UserOnlineChangesResponse;
import com.example.aichat.dto.response.UsersInChatResponse;

import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.entities.User;
import com.example.aichat.model.entities.UserData;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.model.utils.mappers.UserDataMapper;
import com.example.aichat.view.main.chat.ChatFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatMembersActions {

    private static final String SEARCH_MODE = "AllMatch";

    private final ChatFragment fragment;
    private final UUID chatId;
    private final UUID currentUserId;

    private final ConnectionManager connectionManager;
    private final MapperResponse<UserData, UserDataResponse> userDataMapper;

    private final List<User> users = new ArrayList<>();

    public ChatMembersActions(ChatFragment fragment, UUID chatId, UUID currentUserId) {
        this.fragment = fragment;
        this.chatId = chatId;
        this.currentUserId = currentUserId;

        this.connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        this.userDataMapper = new UserDataMapper();
    }

    public void loadUsers() {
        connectionManager.SendCommand(
                new WSSCommand("LoadUsersInChat", new UsersInChatRequest(chatId))
        );
    }

    public void onUsersLoaded(UsersInChatResponse response) {
        users.clear();

        for (int i = 0; i < response.ids.length; i++) {
            users.add(new User(
                    response.ids[i],
                    userDataMapper.ToModel(response.userData[i]),
                    response.isOnline[i]
            ));
        }

        if (fragment.isAdded()) {
            fragment.showUsers(new ArrayList<>(users));
        }
    }

    public void onUserOnlineChanged(UserOnlineChangesResponse response) {

        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            if (u.getId().equals(response.userId)) {
                users.set(i, new User(u.getId(), u.getUserData(), response.isOnline));
                break;
            }
        }

        if (fragment.isAdded()) {
            fragment.updateOnline(response.userId, response.isOnline);
        }
    }

    public void addUserToChat() {
        connectionManager.SendCommand(
                new WSSCommand("AddOtherUserToChat", new AddOtherUserToChatRequest(chatId, SEARCH_MODE))
        );
    }

    public void onUserAdded(AddUserToChatResponse response) {
        User user = new User(
                response.userId,
                userDataMapper.ToModel(response.userData),
                response.isOnline
        );

        users.add(user);

        if (fragment.isAdded()) {
            fragment.showUsers(new ArrayList<>(users));
            fragment.setSearchingChatId(null);
        }
    }

    public void removeUserFromChat(UUID userId) {
        connectionManager.SendCommand(
                new WSSCommand("RemoveUserFromChat", new RemoveUserFromChatRequest(userId, chatId))
        );
    }

    public void onUserRemoved(RemoveUserFromChatResponse response) {
        if (!chatId.equals(response.chatId)) return;
        if (response.userId.equals(currentUserId)) {
            if (fragment.isAdded()) fragment.close();
            return;
        }

        users.removeIf(u -> u.getId().equals(response.userId));

        if (fragment.isAdded()) {
            fragment.showUsers(new ArrayList<>(users));
        }
    }

    public void onUserAdding(UUID chatId) {
        if (fragment.isAdded()) {
            fragment.setSearchingChatId(chatId);
        }
    }

    public void onAddChat() {
        if (fragment.isAdded()) {
            fragment.setSearchingChatId(null);
        }
    }

    public List<User> getUsers() {
        return new ArrayList<>(users);
    }
}
