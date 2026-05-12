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
import com.example.aichat.model.entities.File;

@Database(
        entities = {
                Chat.class,
                Message.class,
                PendingCommand.class,
                File.class
        },
        version = 11,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ChatDao chatDao();
    public abstract MessageDao messageDao();
    public abstract PendingCommandDao pendingCommandDao();
    public abstract FileDao fileDao();

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

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE Chats ADD COLUMN chatTypeHint TEXT");
            db.execSQL("UPDATE Chats SET chatTypeHint = 'Ч' WHERE chatTypeHint IS NULL");
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE Messages ADD COLUMN fileMimeTypes TEXT");
        }
    };

    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE Messages ADD COLUMN fileTypes TEXT");
        }
    };

    private static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS files (" +
                            "fileId TEXT NOT NULL PRIMARY KEY," +
                            "localPath TEXT NOT NULL," +
                            "mimeType TEXT," +
                            "fileType TEXT," +
                            "size INTEGER NOT NULL," +
                            "downloadedAt INTEGER NOT NULL" +
                            ")"
            );
        }
    };

    private static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE files ADD COLUMN lastOpenedAt INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE files ADD COLUMN fileName TEXT");
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
                            .addMigrations(
                                    MIGRATION_3_4,
                                    MIGRATION_4_5,
                                    MIGRATION_5_6,
                                    MIGRATION_6_7,
                                    MIGRATION_7_8,
                                    MIGRATION_8_9,
                                    MIGRATION_9_10,
                                    MIGRATION_10_11
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}