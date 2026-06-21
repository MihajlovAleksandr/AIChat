package com.example.aichat.controller.main.chat.actions;

import android.util.Log;
import android.widget.Toast;
import com.example.aichat.dto.request.SearchUserRequest;
import com.example.aichat.dto.response.RemoveUserFromChatResponse;
import com.example.aichat.dto.response.UserDataResponse;
import com.example.aichat.dto.response.UserInfoResponse;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.ChatStatusSingleton;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.database.DatabaseSaver;
import com.example.aichat.model.database.GroupSearchingStatusChangedListener;
import com.example.aichat.model.database.GroupSearchModel;
import com.example.aichat.model.database.SearchingHandler;
import com.example.aichat.model.entities.ChatType;
import com.example.aichat.model.entities.User;
import com.example.aichat.model.entities.UserData;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.model.utils.mappers.UserDataMapper;
import com.example.aichat.view.main.chat.ChatFragment;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

public class ChatMembersActions {

    private static final String TAG = "ChatMembersActions";
    private static final String SEARCH_MODE = "AllMatch";

    private final ChatFragment fragment;
    private final UUID chatId;
    private final UUID currentUserId;
    private final SearchingHandler handler;
    private final ConnectionDispatcher dispatcher;
    private final DatabaseSaver saver;
    private final MapperResponse<UserData, UserDataResponse> userDataMapper;

    private final List<User> users = new ArrayList<>();
    private final Set<UUID> pendingRemoveUserIds = new HashSet<>();
    private final Map<UUID, User> pendingRemovedUsers = new HashMap<>();
    private final Map<UUID, Integer> pendingRemovedIndexes = new HashMap<>();

    public ChatMembersActions(ChatFragment fragment, UUID chatId, UUID currentUserId) {
        this.fragment = fragment;
        this.chatId = chatId;
        this.currentUserId = currentUserId;
        this.userDataMapper = new UserDataMapper();

        AppDatabase db = DatabaseManager.getDatabase();

        this.saver = new DatabaseSaver(db, currentUserId);
        this.dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
        this.handler = ChatStatusSingleton.getInstance().getHandler();

        handler.addGroupSearchingStatusChangedListener(new GroupSearchingStatusChangedListener() {
            @Override
            public void handle(GroupSearchModel groupSearchModel) {
                runOnUiThreadSafe(() -> {
                    if (!isFragmentUsable()) return;

                    if (groupSearchModel != null && groupSearchModel.getIsSearching()) {
                        fragment.setSearchingChatId(groupSearchModel.getChatId());
                    } else {
                        fragment.canselSearch();
                    }
                });
            }
        });
    }

    public void addUserToChat() {
        if (dispatcher == null || chatId == null) return;

        if (!canAddUsersToCurrentChat()) {
            showAddUserDeniedToast();
            Log.w(TAG, "Add user denied for current chat type");
            return;
        }

        dispatcher.sendHttpRequestAsync(
                        "/api/matchmaking/group/chat",
                        HttpClient.HTTPMethod.POST,
                        new SearchUserRequest(chatId, SEARCH_MODE),
                        false
                )
                .thenAccept(cmd -> {
                    if (cmd != null && cmd.isSuccess()) {
                        handler.setGroupSearchModel(new GroupSearchModel(true, chatId));
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to start adding user to chat", throwable);
                    return null;
                });
    }

    public void stopSearchingChat() {
        if (dispatcher == null || chatId == null) return;

        dispatcher.sendHttpRequestAsync(
                        "/api/matchmaking/group",
                        HttpClient.HTTPMethod.DELETE,
                        new SearchUserRequest(chatId, SEARCH_MODE),
                        false
                )
                .thenAccept(cmd -> {
                    if (cmd != null && cmd.isSuccess()) {
                        handler.setGroupSearchModel(new GroupSearchModel(false, null));
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to stop adding user to chat", throwable);
                    return null;
                });
    }

    public CompletableFuture<List<User>> loadUsers(List<UUID> userIds) {
        List<CompletableFuture<User>> futures = new ArrayList<>();

        if (userIds == null || userIds.isEmpty()) {
            synchronized (users) {
                users.clear();
            }

            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        for (UUID userId : userIds) {
            CompletableFuture<User> future = dispatcher
                    .sendHttpRequestAsync(
                            "/api/user/" + userId + "/userdata/",
                            HttpClient.HTTPMethod.GET,
                            null,
                            true
                    )
                    .thenApply(cmd -> {
                        if (cmd != null && cmd.isSuccess()) {
                            UserInfoResponse response = cmd.getData(UserInfoResponse.class);

                            if (response == null || response.userData == null) {
                                return null;
                            }

                            return new User(
                                    userId,
                                    userDataMapper.ToModel(response.userData),
                                    response.lastOnline,
                                    response.region
                            );
                        }

                        Log.e(TAG, "Can't load user data: " + userId);
                        return null;
                    });

            futures.add(future);
        }

        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<User> loadedUsers = futures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    List<User> filteredUsers = new ArrayList<>();

                    synchronized (users) {
                        for (User user : loadedUsers) {
                            if (user == null || user.getId() == null) {
                                continue;
                            }

                            if (!pendingRemoveUserIds.contains(user.getId())) {
                                filteredUsers.add(user);
                            }
                        }

                        users.clear();
                        users.addAll(filteredUsers);

                        return new ArrayList<>(users);
                    }
                });
    }

    public void removeUserFromChat(UUID userId) {
        if (dispatcher == null || chatId == null || userId == null) return;

        if (!canRemoveUsersFromCurrentChat()) {
            showRemoveUserDeniedByChatTypeToast();
            Log.w(TAG, "Remove user denied for current chat type");
            return;
        }

        if (currentUserId != null && currentUserId.equals(userId)) {
            Log.w(TAG, "Current user cannot remove himself from chat members panel");
            return;
        }

        if (isProtectedCreatorTarget(userId)) {
            showRemoveDeniedToast();
            Log.w(TAG, "Regular user cannot remove chat creator. targetUserId=" + userId);
            return;
        }

        if (!applyOptimisticUserRemove(userId)) {
            return;
        }

        dispatcher.sendHttpRequestAsync(
                        "/api/chat/" + chatId + "/users/" + userId,
                        HttpClient.HTTPMethod.DELETE,
                        null,
                        false
                )
                .thenAccept(cmd -> {
                    if (cmd != null && cmd.isSuccess()) {
                        clearPendingRemove(userId);
                        runOnUiThreadSafe(() -> removeUserFromChatUI(userId));
                    } else {
                        restoreOptimisticUserRemove(userId);
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to remove user from chat", throwable);
                    restoreOptimisticUserRemove(userId);
                    return null;
                });
    }

    public void onUserRemoved(RemoveUserFromChatResponse response) {
        if (response == null || response.chatId == null || !chatId.equals(response.chatId)) {
            return;
        }

        new Thread(() -> {
            try {
                if (saver != null) {
                    saver.removeUserFromChat(response);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to save user remove from chat", e);
            }
        }).start();

        clearPendingRemove(response.userId);
        runOnUiThreadSafe(() -> removeUserFromChatUI(response.userId));
    }

    private boolean applyOptimisticUserRemove(UUID userId) {
        if (!canRemoveUsersFromCurrentChat()) {
            showRemoveUserDeniedByChatTypeToast();
            return false;
        }

        if (isProtectedCreatorTarget(userId)) {
            showRemoveDeniedToast();
            return false;
        }

        List<User> snapshot;

        synchronized (users) {
            if (pendingRemoveUserIds.contains(userId)) {
                return false;
            }

            pendingRemoveUserIds.add(userId);

            int index = findUserIndexLocked(userId);

            if (index >= 0) {
                User removedUser = users.remove(index);
                pendingRemovedUsers.put(userId, removedUser);
                pendingRemovedIndexes.put(userId, index);
            }

            snapshot = new ArrayList<>(users);
        }

        runOnUiThreadSafe(() -> fragment.showUsers(snapshot));
        return true;
    }

    private void restoreOptimisticUserRemove(UUID userId) {
        if (userId == null) return;

        List<User> snapshot;

        synchronized (users) {
            pendingRemoveUserIds.remove(userId);

            User removedUser = pendingRemovedUsers.remove(userId);
            Integer removedIndex = pendingRemovedIndexes.remove(userId);

            if (removedUser != null && findUserIndexLocked(userId) < 0) {
                int safeIndex = removedIndex != null ? removedIndex : users.size();

                if (safeIndex < 0) {
                    safeIndex = 0;
                }

                if (safeIndex > users.size()) {
                    safeIndex = users.size();
                }

                users.add(safeIndex, removedUser);
            }

            snapshot = new ArrayList<>(users);
        }

        runOnUiThreadSafe(() -> {
            fragment.showUsers(snapshot);

            if (fragment.getContext() != null) {
                Toast.makeText(
                        fragment.getContext(),
                        "Не удалось исключить пользователя",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void clearPendingRemove(UUID userId) {
        if (userId == null) return;

        synchronized (users) {
            pendingRemoveUserIds.remove(userId);
            pendingRemovedUsers.remove(userId);
            pendingRemovedIndexes.remove(userId);
        }
    }

    private void removeUserFromChatUI(UUID userId) {
        if (userId == null) return;

        if (currentUserId != null && userId.equals(currentUserId)) {
            if (isFragmentUsable()) {
                fragment.close();
            }

            return;
        }

        List<User> snapshot;

        synchronized (users) {
            pendingRemoveUserIds.remove(userId);
            pendingRemovedUsers.remove(userId);
            pendingRemovedIndexes.remove(userId);

            users.removeIf(user -> user != null && userId.equals(user.getId()));

            snapshot = new ArrayList<>(users);
        }

        if (isFragmentUsable()) {
            fragment.showUsers(snapshot);
        }
    }

    private boolean canAddUsersToCurrentChat() {
        if (fragment == null || fragment.getChat() == null) {
            return false;
        }

        return fragment.getCurrentChatType() == ChatType.Group;
    }

    private boolean canRemoveUsersFromCurrentChat() {
        if (fragment == null || fragment.getChat() == null) {
            return false;
        }

        return fragment.getCurrentChatType() == ChatType.Group;
    }

    private boolean isProtectedCreatorTarget(UUID targetUserId) {
        if (targetUserId == null || currentUserId == null) {
            return false;
        }

        UUID creatorId = getCreatorUserId();

        if (creatorId == null) {
            return false;
        }

        boolean targetIsCreator = creatorId.equals(targetUserId);
        boolean currentUserIsCreator = creatorId.equals(currentUserId);

        return targetIsCreator && !currentUserIsCreator;
    }

    private UUID getCreatorUserId() {
        if (fragment != null) {
            UUID creatorId = fragment.getChatCreatorId();

            if (creatorId != null) {
                return creatorId;
            }
        }

        synchronized (users) {
            if (!users.isEmpty() && users.get(0) != null) {
                return users.get(0).getId();
            }
        }

        return null;
    }

    private void showAddUserDeniedToast() {
        runOnUiThreadSafe(() -> {
            if (fragment.getContext() != null) {
                Toast.makeText(
                        fragment.getContext(),
                        "Добавление пользователей доступно только в групповом чате",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void showRemoveUserDeniedByChatTypeToast() {
        runOnUiThreadSafe(() -> {
            if (fragment.getContext() != null) {
                Toast.makeText(
                        fragment.getContext(),
                        "Исключение пользователей доступно только в групповом чате",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void showRemoveDeniedToast() {
        runOnUiThreadSafe(() -> {
            if (fragment.getContext() != null) {
                Toast.makeText(
                        fragment.getContext(),
                        "Нельзя исключить создателя чата",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private int findUserIndexLocked(UUID userId) {
        if (userId == null) return -1;

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);

            if (user != null && userId.equals(user.getId())) {
                return i;
            }
        }

        return -1;
    }

    public List<User> getUsers() {
        synchronized (users) {
            return new ArrayList<>(users);
        }
    }

    private void runOnUiThreadSafe(Runnable action) {
        if (action == null || fragment == null || !fragment.isAdded() || fragment.getActivity() == null) {
            return;
        }

        fragment.requireActivity().runOnUiThread(() -> {
            if (isFragmentUsable()) {
                action.run();
            }
        });
    }

    private boolean isFragmentUsable() {
        return fragment != null
                && fragment.isAdded()
                && fragment.getActivity() != null
                && fragment.getView() != null;
    }
}
