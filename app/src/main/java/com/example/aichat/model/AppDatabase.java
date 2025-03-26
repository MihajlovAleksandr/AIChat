package com.example.aichat.model;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Chat.class, Message.class, PendingCommand.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ChatDao chatDao();
    public abstract MessageDao messageDao();
    public abstract PendingCommandDao pendingCommandDao();
}
