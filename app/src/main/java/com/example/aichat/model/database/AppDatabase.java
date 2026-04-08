package com.example.aichat.model.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.PendingCommand;

@Database(
        entities = {Chat.class, Message.class, PendingCommand.class},
        version = 5,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ChatDao chatDao();
    public abstract MessageDao messageDao();
    public abstract PendingCommandDao pendingCommandDao();

    private static volatile AppDatabase INSTANCE;

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE Messages ADD COLUMN statuses TEXT");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE Chats ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE Chats ADD COLUMN `group` INTEGER NOT NULL DEFAULT 0");
        }
    };


    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "aichat.db"
                            )
                            .fallbackToDestructiveMigration()
                            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)

                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
