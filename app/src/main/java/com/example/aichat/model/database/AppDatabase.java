package com.example.aichat.model.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.PendingCommand;

@Database(entities = {Chat.class, Message.class, PendingCommand.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ChatDao chatDao();
    public abstract MessageDao messageDao();
    public abstract PendingCommandDao pendingCommandDao();
}
