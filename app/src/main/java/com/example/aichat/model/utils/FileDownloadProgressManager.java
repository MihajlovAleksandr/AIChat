package com.example.aichat.model.utils;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class FileDownloadProgressManager {

    public interface Listener {
        void onProgress(UUID fileId, int progress);
        void onCompleted(UUID fileId);
        void onError(UUID fileId);
        void onCleared();
    }

    private final Map<UUID, Integer> progressMap = new ConcurrentHashMap<>();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public void addListener(Listener listener) {
        if (listener == null) return;
        listeners.remove(listener);
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        if (listener == null) return;
        listeners.remove(listener);
    }

    public void updateProgress(UUID fileId, int progress) {
        if (fileId == null) return;

        int safeProgress = Math.max(0, Math.min(100, progress));
        Integer prev = progressMap.get(fileId);

        if (prev != null && prev == safeProgress) return;

        progressMap.put(fileId, safeProgress);

        for (Listener listener : listeners) {
            listener.onProgress(fileId, safeProgress);
        }
    }

    public void complete(UUID fileId) {
        if (fileId == null) return;

        Integer prev = progressMap.get(fileId);
        if (prev != null && prev == 100) return;

        progressMap.put(fileId, 100);

        for (Listener listener : listeners) {
            listener.onCompleted(fileId);
        }
    }

    public void error(UUID fileId) {
        if (fileId == null) return;

        if (progressMap.remove(fileId) == null) return;

        for (Listener listener : listeners) {
            listener.onError(fileId);
        }
    }

    public Integer getProgress(UUID fileId) {
        if (fileId == null) return null;
        return progressMap.get(fileId);
    }

    public boolean isDownloading(UUID fileId) {
        if (fileId == null) return false;
        Integer progress = progressMap.get(fileId);
        return progress != null && progress < 100;
    }

    public void clear(UUID fileId) {
        if (fileId == null) return;

        if (progressMap.remove(fileId) == null) return;

        for (Listener listener : listeners) {
            listener.onCleared();
        }
    }

    public void clearAll() {
        if (progressMap.isEmpty()) return;

        progressMap.clear();

        for (Listener listener : listeners) {
            listener.onCleared();
        }
    }
}