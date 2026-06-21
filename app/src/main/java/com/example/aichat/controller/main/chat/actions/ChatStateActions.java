package com.example.aichat.controller.main.chat.actions;

import android.util.Log;
import androidx.annotation.Nullable;
import com.example.aichat.dto.request.AISettingsRequest;
import com.example.aichat.dto.request.MakeGuessRequest;
import com.example.aichat.dto.request.MatchmakingRequest;
import com.example.aichat.dto.response.AISettingsResponse;
import com.example.aichat.dto.response.ChatUserActionResponse;
import com.example.aichat.dto.response.DeleteChatResponse;
import com.example.aichat.dto.response.EndChatResponse;
import com.example.aichat.dto.response.UpdateChatNameResponse;
import com.example.aichat.model.ai.AIModel;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.database.AppDatabase;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.database.DatabaseSaver;
import com.example.aichat.model.entities.AiRole;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.ChatGameState;
import com.example.aichat.model.utils.time.TimeConverter;
import com.example.aichat.view.main.chat.ChatFragment;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.view.main.MainActivityAdapter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.UUID;

@androidx.media3.common.util.UnstableApi
public class ChatStateActions {

    private static final String TAG = "ChatStateActions";

    private final ChatFragment fragment;
    private final UUID chatId;
    private final ConnectionDispatcher dispatcher;
    private final DatabaseSaver saver;

    public ChatStateActions(ChatFragment fragment, UUID chatId, UUID userId) {
        this.fragment = fragment;
        this.chatId = chatId;
        dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
        AppDatabase db = DatabaseManager.getDatabase();
        this.saver = new DatabaseSaver(db, userId);
        Log.d(TAG, "ChatStateActions initialized for chatId=" + chatId + ", userId=" + userId);
    }

    public void endChat() {
        Log.d(TAG, "endChat called for chatId=" + chatId);

        dispatcher.sendHttpRequestAsync("/api/chat/"+chatId+"/end", HttpClient.HTTPMethod.POST, null, false)
                .thenAccept(cmd -> {
                    Log.d(TAG, "HTTP response: success=" + cmd.isSuccess() + ", code=" + cmd.getCode());

                    if (cmd.isSuccess()) {
                        EndChatResponse response = cmd.getData(EndChatResponse.class);
                        String endTime = response != null ? response.endedTime : null;
                        Log.d(TAG, "EndTime from server: " + endTime);

                        if (saver != null) {
                            Chat endedChat = new Chat();
                            endedChat.setId(chatId);
                            if (endTime != null) {
                                endedChat.setEndTime(endTime);
                            } else {
                                endedChat.end();
                                endTime = endedChat.getEndTime();
                            }
                            Log.d(TAG, "Calling saver.endChat with endTime=" + endTime);
                            saver.endChat(endedChat);
                            Log.d(TAG, "saver.endChat completed");
                        } else {
                            Log.e(TAG, "saver is NULL!");
                        }

                        updateChatEndedUI(endTime != null ? endTime : TimeConverter.getString(LocalDateTime.now()));
                    } else {
                        Log.e(TAG, "HTTP request failed with code: " + cmd.getCode());
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "HTTP request error", throwable);
                    return null;
                });

        Log.d(TAG, "WebSocket EndChat command sent");
    }

    public CompletableFuture<ChatGameState> getChatGameResult(UUID chatId) {
        return dispatcher.sendHttpRequestAsync("/api/chat/game/" + chatId, HttpClient.HTTPMethod.GET, null, false).thenApply(cmd -> {
            if (cmd.isSuccess()) {
                return cmd.getData(ChatGameState.class);
            } else {
                return ChatGameState.NotAvailable;
            }
        });
    }

    public CompletableFuture<AISettingsResponse> getAISettings(UUID chatId) {
        return dispatcher.sendHttpRequestAsync("/api/chat/" + chatId+"/ai", HttpClient.HTTPMethod.GET, null, false).thenApply(cmd -> {
            if (cmd.isSuccess()) {
                return cmd.getData(AISettingsResponse.class);
            } else {
                return null;
            }
        });
    }

    public CompletableFuture<List<AIModel>> getAvailableModels(UUID chatId) {
        return dispatcher.sendHttpRequestAsync("/api/ai/models", HttpClient.HTTPMethod.GET, null, false).thenApply(cmd -> {
            if (cmd.isSuccess()) {
                var modelsString = cmd.getData(String[].class);
                List<AIModel> models = new ArrayList<>();
                for (var string : modelsString) {
                    models.add(AIModel.fromServerValue(string));
                }
                return models;
            } else {
                return null;
            }
        });
    }

    public CompletableFuture<AISettingsResponse> setAISettings(UUID chatId, AIModel model, @Nullable String prompt) {
        return dispatcher.sendHttpRequestAsync("/api/chat/" + chatId+"/ai", HttpClient.HTTPMethod.PUT, new AISettingsRequest(model, prompt), false).thenApply(cmd -> {
            if (cmd.isSuccess()) {
                AISettingsResponse response = cmd.getData(AISettingsResponse.class);
                return response != null ? response : AISettingsResponse.local(model, prompt);
            }

            throw new RuntimeException("AI settings update failed with code: " + cmd.getCode());
        });
    }

    public CompletableFuture<ChatGameState> setChatGameResult(UUID chatId, AiRole role) {
        return dispatcher.sendHttpRequestAsync("/api/chat/game/guess", HttpClient.HTTPMethod.POST, new MakeGuessRequest(chatId, role), false)
                .thenCompose(cmd -> {
                    if (cmd.isSuccess()) {
                        return getChatGameResult(chatId);
                    } else {
                        return CompletableFuture.completedFuture(ChatGameState.NotAvailable);
                    }
                });
    }

    private void updateChatEndedUI(String endTime){
        Log.d(TAG, "updateChatEndedUI called with endTime=" + endTime);
        new Thread(() -> {
            try {
                Chat chat = DatabaseManager.getDatabase().chatDao().getChatById(chatId);
                if (chat != null && chat.getEndTime() == null) {
                    chat.setEndTime(endTime);
                    DatabaseManager.getDatabase().chatDao().updateChat(chat);
                    Log.d(TAG, "Local chat object updated and saved with endTime=" + endTime);
                } else if (chat != null) {
                    Log.d(TAG, "Chat already has endTime=" + chat.getEndTime());
                } else {
                    Log.e(TAG, "Chat not found in database: " + chatId);
                }

                if (fragment.isAdded() && fragment.getActivity() != null) {
                    fragment.requireActivity().runOnUiThread(() -> {
                        if (fragment.getActivity() instanceof MainActivity) {
                            MainActivity mainActivity = (MainActivity) fragment.getActivity();
                            MainActivityAdapter adapter = mainActivity.getPagerAdapter();
                            if (adapter != null && adapter.getChatsListFragment() != null) {
                                adapter.getChatsListFragment().endChat(chatId);
                                adapter.getChatsListFragment().reloadChatsFromDatabaseSafe();
                                Log.d(TAG, "Chats list updated after chat ended");
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating chat in database", e);
            }

            if (fragment.isAdded() && fragment.getActivity() != null) {
                fragment.requireActivity().runOnUiThread(() -> {
                    fragment.showChatEnded();
                    fragment.getUi().disableMessageInput();
                    Log.d(TAG, "UI updated via fragment.showChatEnded()");
                });
            } else {
                Log.w(TAG, "Fragment not added or activity null, cannot update UI");
            }
        }).start();
    }

    public void onChatEnded() {
        if (!fragment.isAdded()) return;
        fragment.requireActivity().runOnUiThread(() -> {
            fragment.showChatEnded();
            fragment.getUi().disableMessageInput();
            Log.d(TAG, "onChatEnded: UI updated");
        });
    }

    public void onChatDeleted(DeleteChatResponse response) {
        if (!chatId.equals(response.chatId)) return;

        new Thread(() -> {
            if (saver != null) {
                saver.removeChat(response.chatId);
                Log.d(TAG, "Chat removed from DB: " + response.chatId);
            }
        }).start();

        if (!fragment.isAdded()) return;
        fragment.requireActivity().runOnUiThread(fragment::navigateBack);
    }

    public void onChatRenamed(UpdateChatNameResponse response) {
        if (!response.chatId.equals(chatId)) return;

        new Thread(() -> {
            if (saver != null) {
                saver.updateChatName(response.chatId, response.name);
                Log.d(TAG, "Chat name updated in DB: " + response.chatId + " -> " + response.name);
            }
        }).start();

        if (!fragment.isAdded()) return;
        fragment.requireActivity().runOnUiThread(() ->
                fragment.setChatTitle(response.name)
        );
    }

    public void onChatRemoved(ChatUserActionResponse response) {
        Log.d(TAG, "onChatRemoved called for chatId=" + response.chatId);

        if (!chatId.equals(response.chatId)) return;

        new Thread(() -> {
            if (saver != null) {
                saver.removeChat(response.chatId);
                Log.d(TAG, "Chat removed from DB: " + response.chatId);
            }
        }).start();

        if (!fragment.isAdded()) return;

        fragment.requireActivity().runOnUiThread(() -> {
            android.widget.Toast.makeText(fragment.getContext(),
                    "Чат был удалён", android.widget.Toast.LENGTH_SHORT).show();
            fragment.navigateBack();
        });
    }
}
