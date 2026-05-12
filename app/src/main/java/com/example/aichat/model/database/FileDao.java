package com.example.aichat.model.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.aichat.model.entities.File;

import java.util.List;
import java.util.UUID;

@Dao
public interface FileDao {

    @Query("SELECT * FROM files WHERE fileId = :id LIMIT 1")
    File getById(UUID id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(File file);

    @Query("DELETE FROM files WHERE fileId = :id")
    void delete(UUID id);

    @Query("SELECT * FROM files")
    List<File> getAll();

    @Query("SELECT SUM(size) FROM files")
    Long getTotalSize();
    @Query("SELECT * FROM files ORDER BY downloadedAt ASC")
    List<File> getAllOrderByDateAsc();

    @Query("SELECT * FROM files ORDER BY lastOpenedAt ASC")
    List<File> getOldestFirst();

    @Query("UPDATE files SET localPath = :path, mimeType = :mime, size = :size, downloadedAt = :updatedAt, fileName = :fileName WHERE fileId = :fileId")
    void updateFileAfterDownload(UUID fileId,
                                 String path,
                                 String mime,
                                 long size,
                                 long updatedAt,
                                 String fileName);
    @Query("SELECT * FROM files ORDER BY lastOpenedAt ASC")
    List<File> getAllOrderByLastOpenedAsc();

    @Query("UPDATE files SET lastOpenedAt = :time WHERE fileId = :id")
    void updateLastOpened(UUID id, long time);

    @Query("DELETE FROM files")
    void clearAll();
}