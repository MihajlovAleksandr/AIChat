package com.example.aichat.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Delete;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PendingCommandDao {
    @Insert
    void insertCommand(PendingCommand command);

    @Query("SELECT * FROM PendingCommands")
    List<PendingCommand> getAllCommands();

    @Delete
    void deleteCommand(PendingCommand command);
}
