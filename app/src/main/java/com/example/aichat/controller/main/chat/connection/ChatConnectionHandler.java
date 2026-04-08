package com.example.aichat.controller.main.chat.connection;

import com.example.aichat.controller.main.chat.actions.ChatMessageActions;
import com.example.aichat.controller.main.chat.actions.ChatMembersActions;
import com.example.aichat.controller.main.chat.actions.ChatStateActions;

import com.example.aichat.dto.response.AddUserToChatResponse;
import com.example.aichat.dto.response.DeleteChatResponse;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.dto.response.RemoveUserFromChatResponse;
import com.example.aichat.dto.response.UpdateChatNameResponse;
import com.example.aichat.dto.response.UpdateMessageStatusResponse;
import com.example.aichat.dto.response.UserOnlineChangesResponse;
import com.example.aichat.dto.response.UsersInChatResponse;

import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.view.main.chat.ChatFragment;

import java.util.UUID;

public class ChatConnectionHandler implements OnConnectionEvents {

    private final ChatFragment fragment;
    private final UUID chatId;

    private final ChatMessageActions messageActions;
    private final ChatMembersActions membersActions;
    private final ChatStateActions stateActions;

    public ChatConnectionHandler(
            ChatFragment fragment,
            UUID chatId,
            ChatMessageActions messageActions,
            ChatMembersActions membersActions,
            ChatStateActions stateActions
    ) {
        this.fragment = fragment;
        this.chatId = chatId;
        this.messageActions = messageActions;
        this.membersActions = membersActions;
        this.stateActions = stateActions;
    }

    @Override
    public void OnCommandGot(WSSCommand cmd) {

        switch (cmd.getOperation()) {

            case "SendMessage": {
                MessageResponse resp = cmd.getData(MessageResponse.class);
                if (resp != null && isThisChat(resp.chat)) {
                    messageActions.onMessageReceived(resp);
                }
                break;
            }

            case "UpdateMessageStatus": {
                UpdateMessageStatusResponse resp = cmd.getData(UpdateMessageStatusResponse.class);
                if (resp != null && isThisChat(resp.chatId)) {
                    messageActions.onMessageStatusUpdated(resp);
                }
                break;
            }

            case "LoadUsersInChat": {
                UsersInChatResponse resp = cmd.getData(UsersInChatResponse.class);
                if (resp != null) {
                    membersActions.onUsersLoaded(resp);
                }
                break;
            }

            case "UserOnlineChanges": {
                UserOnlineChangesResponse resp = cmd.getData(UserOnlineChangesResponse.class);
                if (resp != null) {
                    membersActions.onUserOnlineChanged(resp);
                }
                break;
            }

            case "AddUserToChat": {
                AddUserToChatResponse resp = cmd.getData(AddUserToChatResponse.class);
                if (resp != null && isThisChat(resp.chatId)) {
                    membersActions.onUserAdded(resp);
                }
                break;
            }

            case "RemoveUserFromChat": {
                RemoveUserFromChatResponse resp = cmd.getData(RemoveUserFromChatResponse.class);
                if (resp != null && isThisChat(resp.chatId)) {
                    membersActions.onUserRemoved(resp);
                }
                break;
            }

            case "EndChat": {
                stateActions.onChatEnded();
                break;
            }

            case "DeleteChat": {
                DeleteChatResponse resp = cmd.getData(DeleteChatResponse.class);
                if (resp != null && isThisChat(resp.chatId)) {
                    stateActions.onChatDeleted(resp);
                }
                break;
            }

            case "UpdateChatName": {
                UpdateChatNameResponse resp = cmd.getData(UpdateChatNameResponse.class);
                if (resp != null && isThisChat(resp.chatId)) {
                    stateActions.onChatRenamed(resp);
                }
                break;
            }
            case "SyncDB":
            case "Logout":
            default:
                break;
        }
    }

    @Override
    public void OnConnectionFailed() {
        if (fragment.getActivity() == null) return;

        fragment.getActivity().runOnUiThread(() -> {
            fragment.getUi().showDisconnectedState();
            fragment.getUi().showConnectionBanner(false);
        });
    }

    @Override
    public void OnOpen() {
        if (fragment.getActivity() == null) return;

        fragment.getActivity().runOnUiThread(() -> {
            fragment.getUi().showConnectedState();
            fragment.getUi().showConnectionBanner(true);
        });
        membersActions.loadUsers();
    }

    private boolean isThisChat(UUID respChatId) {
        return respChatId != null && respChatId.equals(chatId);
    }
}
