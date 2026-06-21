package com.example.aichat.controller;

import com.example.aichat.dto.response.LeaderboardItemResponse;
import com.example.aichat.dto.response.PaginationItem;
import com.example.aichat.LeaderboardActivity;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.concurrent.CompletableFuture;

public class LeaderboardController {

    private final ConnectionDispatcher dispatcher;
    private final LeaderboardActivity activity;

    public LeaderboardController(LeaderboardActivity activity) {
        this.activity=activity;
        this.dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
    }

    public void getThemes(int limit, int page) {
        dispatcher.sendHttpRequestAsync(
                "/api/chat/game/leaderboard?limit="+limit+"&page="+page,
                HttpClient.HTTPMethod.GET,
                null,
                false
        ).thenAccept(cmd -> {
            PaginationItem<LeaderboardItemResponse> items = cmd.getData(
                    new TypeReference<PaginationItem<LeaderboardItemResponse>>() {}
            );activity.runOnUiThread(()->{
                activity.setLeaderboard(items);
            });
        });
    }


}
