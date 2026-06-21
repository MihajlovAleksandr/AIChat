package com.example.aichat.model.utils.files;

import android.content.Context;
import androidx.annotation.NonNull;

public final class FileManagerHolder {

    private static volatile FileManager instance;

    public static synchronized void init(@NonNull FileManager fileManager) {
        instance = fileManager;
    }

    public static FileManager get(@NonNull Context context,
                                  @NonNull FileDownloadProgressManager progressManager) {

        FileManager local = instance;

        if (local == null || local.getProgressManager() != progressManager) {
            synchronized (FileManagerHolder.class) {
                local = instance;
                if (local == null || local.getProgressManager() != progressManager) {
                    local = new FileManager(context, progressManager);
                    instance = local;
                }
            }
        }

        return local;
    }
}
