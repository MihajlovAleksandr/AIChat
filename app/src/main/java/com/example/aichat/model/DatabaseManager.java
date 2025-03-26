package com.example.aichat.model;

import android.content.Context;

public class DatabaseManager {
    private static AppDatabase database;

    public static synchronized void init(Context context) {
        if (database == null) {
            database = DatabaseClient.getDatabase(context);
        }
    }

    public static AppDatabase getDatabase() {
        if (database == null) {
            throw new IllegalStateException("DatabaseManager is not initialized!");
        }
        return database;
    }
}
