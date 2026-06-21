package com.example.aichat.model.utils.media.video;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import androidx.annotation.NonNull;

public final class VideoPlayerReturnGuard {

    private static final String PREFS_NAME = "video_player_return_guard";
    private static final String KEY_LAST_STARTED_AT_WALL = "last_started_at_wall";
    private static final String KEY_LAST_FINISHED_AT_WALL = "last_finished_at_wall";
    private static final String KEY_LAST_FINISHED_AT_UPTIME = "last_finished_at_uptime";

    private VideoPlayerReturnGuard() {
    }

    public static void markStarted(@NonNull Context context) {
        prefs(context)
                .edit()
                .putLong(KEY_LAST_STARTED_AT_WALL, System.currentTimeMillis())
                .apply();
    }

    public static void markFinished(@NonNull Context context) {
        prefs(context)
                .edit()
                .putLong(KEY_LAST_FINISHED_AT_WALL, System.currentTimeMillis())
                .putLong(KEY_LAST_FINISHED_AT_UPTIME, SystemClock.uptimeMillis())
                .apply();
    }

    public static boolean consumeRecentFinish(@NonNull Context context, long maxAgeMs) {
        SharedPreferences preferences = prefs(context);

        long finishedWall = preferences.getLong(KEY_LAST_FINISHED_AT_WALL, 0L);

        if (finishedWall <= 0L) {
            return false;
        }

        long age = Math.abs(System.currentTimeMillis() - finishedWall);
        boolean recent = age <= maxAgeMs;

        preferences
                .edit()
                .remove(KEY_LAST_FINISHED_AT_WALL)
                .remove(KEY_LAST_FINISHED_AT_UPTIME)
                .apply();

        return recent;
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
