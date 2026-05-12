package com.example.aichat.model.database;

import android.util.Log;
import android.widget.Toast;

import com.example.aichat.dto.request.MessageRequest;
import com.example.aichat.dto.response.AddUserToChatResponse;
import com.example.aichat.dto.response.ChatEndedResponse;
import com.example.aichat.dto.response.ChatNameUpdatedResponse;
import com.example.aichat.dto.response.ChatResponse;
import com.example.aichat.dto.response.ChatSearchingStatusResponse;
import com.example.aichat.dto.response.ChatUserActionResponse;
import com.example.aichat.dto.response.DeleteChatResponse;
import com.example.aichat.dto.response.GroupSearchingStatusResponse;
import com.example.aichat.dto.response.MessageResponse;
import com.example.aichat.dto.response.RemoveUserFromChatResponse;
import com.example.aichat.dto.response.UpdateMessageStatusResponse;
import com.example.aichat.model.connection.EventHandler;
import com.example.aichat.model.connection.SignalRCommand;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageChat;
import com.example.aichat.model.utils.mappers.ChatMapper;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.model.utils.mappers.MessageMapper;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.view.main.chatlist.ChatsListFragment;

import java.util.UUID;

public class DatabaseEventHandler {

    private static final String TAG = "DatabaseEventHandler";

    private final DatabaseSaver saver;
    private final MessageMapper messageMapper;
    private final MapperResponse<Chat, ChatResponse> chatMapper = new ChatMapper();
    private final UUID userId;

    // Ссылка на UI фрагмент для перерисовки
    private ChatsListFragment chatsListFragment;

    // Ссылка на контроллер для обновления состояния поиска
    private ChatsListControllerCallback controllerCallback;

    public interface ChatsListControllerCallback {
        void setIsChatSearching(boolean value);
        void setIsGroupSearching(boolean value);
        void setAddUserState();
        void setChatSearchingStatus();
        void setGroupSearchingStatus();
    }

    public DatabaseEventHandler(DatabaseSaver saver, UUID userId) {
        this.saver = saver;
        this.userId = userId;
        this.messageMapper = new MessageMapper(userId);
    }

    public void setChatsListFragment(ChatsListFragment fragment) {
        this.chatsListFragment = fragment;
        Log.d(TAG, "ChatsListFragment set: " + (fragment != null));
    }

    public void setControllerCallback(ChatsListControllerCallback callback) {
        this.controllerCallback = callback;
    }

    // ==================== 1. CHAT CREATED ====================
    public EventHandler<ChatResponse> onChatCreated() {
        return command -> {
            ChatResponse response = command.getPayload();
            if (response != null && saver != null) {
                Log.d(TAG, "DatabaseOperation: AddChat - saving to DB");
                saver.saveChatFromResponse(response);

                // ✅ UI обновление
                if (chatsListFragment != null) {
                    Chat chat = chatMapper.ToModel(response);
                    chatsListFragment.createChat(chat);
                    chatsListFragment.reloadChatsFromDatabaseSafe();
                    Log.d(TAG, "UI updated: createChat + reload");
                }
            }
        };
    }

    // ==================== 2. CHAT ENDED ====================
    public EventHandler<ChatEndedResponse> onChatEnded() {
        return command -> {
            ChatEndedResponse response = command.getPayload();
            if (response != null && saver != null) {
                Log.d(TAG, "DatabaseOperation: EndChat - saving to DB");
                Chat endedChat = new Chat();
                endedChat.setId(response.chatId);
                endedChat.setEndTime(response.endedTime);
                saver.endChat(endedChat);

                // ✅ UI обновление
                if (chatsListFragment != null) {
                    chatsListFragment.endChat(response.chatId);
                    chatsListFragment.reloadChatsFromDatabaseSafe();
                    Log.d(TAG, "UI updated: endChat + reload");
                }
            }
        };
    }

    // ==================== 3. DELETE CHAT ====================
    public EventHandler<DeleteChatResponse> onDeleteChat() {
        return command -> {
            DeleteChatResponse response = command.getPayload();
            if (response != null && saver != null) {
                Log.d(TAG, "DatabaseOperation: DeleteChat - deleting from DB");
                saver.deleteChatFromResponse(response);

                // ✅ UI обновление
                if (chatsListFragment != null) {
                    chatsListFragment.removeChat(response.chatId);
                    chatsListFragment.reloadChatsFromDatabaseSafe();
                    Log.d(TAG, "UI updated: deleteChat + reload");
                }
            }
        };
    }

    // ==================== 4. CHAT USER REMOVED ====================
    public EventHandler<ChatUserActionResponse> onChatUserRemoved() {
        return command -> {
            ChatUserActionResponse response = command.getPayload();
            if (response != null && saver != null) {
                Log.d(TAG, "DatabaseOperation: UserRemoved - userId=" + response.userId + ", chatId=" + response.chatId);

                saver.removeUserFromChat(response);

                // ✅ UI обновление
                if (chatsListFragment != null) {
                    if (response.userId.equals(userId)) {
                        // Текущего пользователя удалили из чата
                        Log.d(TAG, "Current user was removed from chat: " + response.chatId);
                        DeleteChatResponse deleteResponse = new DeleteChatResponse(response.chatId);
                        saver.deleteChatFromResponse(deleteResponse);

                        chatsListFragment.removeChat(response.chatId);
                        chatsListFragment.reloadChatsFromDatabaseSafe();

                        // Показать Toast и закрыть чат если открыт
                        if (chatsListFragment.getActivity() instanceof MainActivity) {
                            MainActivity activity = (MainActivity) chatsListFragment.getActivity();
                            activity.runOnUiThread(() -> {
                                Toast.makeText(activity, "Вас удалили из чата", Toast.LENGTH_LONG).show();
                                if (activity.getCurrentChatId() != null &&
                                        activity.getCurrentChatId().equals(response.chatId)) {
                                    activity.backToChats();
                                }
                            });
                        }
                    } else {
                        chatsListFragment.reloadChatsFromDatabaseSafe();
                    }
                }
            }
        };
    }

    // ==================== 5. SEND MESSAGE ====================
    public EventHandler<MessageResponse> onSendMessage() {
        return command -> {
            MessageResponse response = command.getPayload();
            Log.e(TAG, "🔥 SendMessage event received! ChatId=" + (response != null ? response.chatId : "null"));

            if (response == null || saver == null) return;

            new Thread(() -> {
                saver.saveMessageFromResponse(response);

                if (chatsListFragment != null && chatsListFragment.getActivity() != null) {
                    chatsListFragment.getActivity().runOnUiThread(() -> {
                        chatsListFragment.reloadChatsFromDatabaseSafe();
                        Log.d(TAG, "UI reloaded from DB after receiving message");
                    });
                }
            }).start();
        };
    }

    // ==================== 6. UPDATE MESSAGE STATUS ====================
    public EventHandler<UpdateMessageStatusResponse> onUpdateMessageStatus() {
        return command -> {
            UpdateMessageStatusResponse response = command.getPayload();
            if (response != null && saver != null) {
                Log.d(TAG, "DatabaseOperation: UpdateMessageStatus - updating DB");
                saver.updateMessageStatus(response);

                // ✅ UI обновление
                if (chatsListFragment != null) {
                    MessageChat msg = chatsListFragment.getMessageChat(response.chatId);
                    if (msg != null) {
                        chatsListFragment.updateMessageStatus(msg);
                        Log.d(TAG, "UI updated: updateMessageStatus");
                    }
                }
            }
        };
    }

    // ==================== 7. ADD USER TO CHAT ====================
    public EventHandler<AddUserToChatResponse> onAddUserToChat() {
        return command -> {
            AddUserToChatResponse response = command.getPayload();
            if (response != null && saver != null) {
                Log.d(TAG, "DatabaseOperation: AddUserToChat - adding user to chat");
                saver.addUserToChat(response);

                // ✅ UI обновление
                if (chatsListFragment != null) {
                    chatsListFragment.reloadChatsFromDatabaseSafe();
                    Log.d(TAG, "UI updated: addUserToChat + reload");
                }
            }
        };
    }

    // ==================== 8. REMOVE USER FROM CHAT ====================
    public EventHandler<RemoveUserFromChatResponse> onRemoveUserFromChat() {
        return command -> {
            RemoveUserFromChatResponse response = command.getPayload();
            if (response != null && saver != null) {
                Log.d(TAG, "DatabaseOperation: RemoveUserFromChat - removing from DB");
                saver.removeUserFromChat(response);

                // ✅ UI обновление
                if (chatsListFragment != null) {
                    chatsListFragment.reloadChatsFromDatabaseSafe();
                    Log.d(TAG, "UI updated: removeUserFromChat + reload");
                }
            }
        };
    }

    // ==================== 9. UPDATE CHAT NAME ====================
    public EventHandler<ChatNameUpdatedResponse> onChatNameUpdated() {
        return command -> {
            ChatNameUpdatedResponse response = command.getPayload();
            if (response != null && saver != null) {
                Log.d(TAG, "DatabaseOperation: UpdateChatName - updating DB");
                saver.updateChatName(response.chatId, response.name);

                // ✅ UI обновление
                if (chatsListFragment != null) {
                    chatsListFragment.updateChatName(response.chatId, response.name);
                    chatsListFragment.reloadChatsFromDatabaseSafe();
                    Log.d(TAG, "UI updated: updateChatName + reload");
                }
            }
        };
    }

    // ==================== 10. SYNC DB ====================
    public EventHandler<com.example.aichat.dto.response.SyncResponse> onSyncDB() {
        return command -> {
            com.example.aichat.dto.response.SyncResponse response = command.getPayload();
            if (response != null && saver != null) {
                Log.d(TAG, "DatabaseOperation: SyncDB - syncing database");
                saver.syncDatabase(response);

                // ✅ UI обновление - НЕ ВЫЗЫВАЕМ reload, чтобы сохранить свежие имена!
                if (chatsListFragment != null) {
                    // Только обновляем данные в контроллере
                    chatsListFragment.loadChatsFromDatabase(userId);
                    Log.d(TAG, "UI updated: loadChatsFromDatabase (no reload to preserve names)");
                }
            }
        };
    }

    // ==================== 11. CHAT SEARCHING STATUS ====================
    public EventHandler<ChatSearchingStatusResponse> onChatSearchingStatusUpdated() {
        return command -> {
            ChatSearchingStatusResponse response = command.getPayload();
            if (response != null && controllerCallback != null) {
                Log.d(TAG, "Updating chat searching status: " + response.isSearching);
                controllerCallback.setIsChatSearching(response.isSearching);
                controllerCallback.setChatSearchingStatus();
            }
        };
    }

    // ==================== 12. GROUP SEARCHING STATUS ====================
    public EventHandler<GroupSearchingStatusResponse> onGroupSearchingStatusUpdated() {
        return command -> {
            GroupSearchingStatusResponse response = command.getPayload();
            if (response != null && controllerCallback != null) {
                Log.d(TAG, "Updating group searching status: " + response.isSearching);
                controllerCallback.setIsGroupSearching(response.isSearching);
                controllerCallback.setGroupSearchingStatus();
            }
        };
    }
}