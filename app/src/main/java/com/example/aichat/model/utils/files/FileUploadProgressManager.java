package com.example.aichat.model.utils.files;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FileUploadProgressManager {

    public interface Listener {
        void onProgress(UUID fileId, int progress);

        void onCompleted(UUID fileId);

        void onError(UUID fileId);

        void onCleared(UUID fileId);
    }

    private static final Map<UUID, Integer> progressMap = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> aliasMap = new ConcurrentHashMap<>();
    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(@Nullable Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(@Nullable Listener listener) {
        listeners.remove(listener);
    }

    public void updateProgress(@Nullable UUID fileId, int progress) {
        if (fileId == null) {
            return;
        }

        int safeProgress = normalizeProgress(progress);
        UUID canonicalId = resolveCanonicalId(fileId);
        Integer previousProgress = progressMap.get(canonicalId);

        if (previousProgress != null && previousProgress >= safeProgress) {
            safeProgress = previousProgress;
        }

        if (previousProgress != null && previousProgress == safeProgress) {
            return;
        }

        progressMap.put(canonicalId, safeProgress);
        notifyProgress(canonicalId, safeProgress);

        if (!canonicalId.equals(fileId)) {
            notifyProgress(fileId, safeProgress);
        }
    }

    public void complete(@Nullable UUID fileId) {
        if (fileId == null) {
            return;
        }

        UUID canonicalId = resolveCanonicalId(fileId);
        progressMap.put(canonicalId, 100);
        notifyCompleted(canonicalId);

        if (!canonicalId.equals(fileId)) {
            notifyCompleted(fileId);
        }
    }

    public void error(@Nullable UUID fileId) {
        if (fileId == null) {
            return;
        }

        UUID canonicalId = resolveCanonicalId(fileId);
        notifyError(canonicalId);

        if (!canonicalId.equals(fileId)) {
            notifyError(fileId);
        }
    }

    public void clear(@Nullable UUID fileId) {
        if (fileId == null) {
            return;
        }

        UUID canonicalId = resolveCanonicalId(fileId);
        progressMap.remove(canonicalId);
        aliasMap.remove(fileId);

        notifyCleared(canonicalId);

        if (!canonicalId.equals(fileId)) {
            notifyCleared(fileId);
        }
    }

    public void clearAll() {
        for (UUID fileId : progressMap.keySet()) {
            notifyCleared(fileId);
        }

        progressMap.clear();
        aliasMap.clear();
    }

    public void bindAlias(@Nullable UUID oldFileId, @Nullable UUID newFileId) {
        if (oldFileId == null || newFileId == null || oldFileId.equals(newFileId)) {
            return;
        }

        UUID oldCanonicalId = resolveCanonicalId(oldFileId);
        Integer progress = progressMap.get(oldCanonicalId);

        aliasMap.put(oldFileId, newFileId);
        progressMap.remove(oldFileId);
        progressMap.remove(oldCanonicalId);

        if (progress != null) {
            progressMap.put(newFileId, progress);
            notifyProgress(newFileId, progress);
            notifyProgress(oldFileId, progress);
        }
    }

    @Nullable
    public Integer getProgress(@Nullable UUID fileId) {
        if (fileId == null) {
            return null;
        }

        Integer direct = progressMap.get(fileId);

        if (direct != null) {
            return direct;
        }

        UUID canonicalId = aliasMap.get(fileId);
        return canonicalId != null ? progressMap.get(canonicalId) : null;
    }

    public boolean isUploading(@Nullable UUID fileId) {
        Integer progress = getProgress(fileId);
        return progress != null && progress >= 0 && progress < 100;
    }

    public boolean isUploaded(@Nullable UUID fileId) {
        Integer progress = getProgress(fileId);
        return progress != null && progress >= 100;
    }

    @NonNull
    private UUID resolveCanonicalId(@NonNull UUID fileId) {
        UUID current = fileId;

        for (int i = 0; i < 8; i++) {
            UUID next = aliasMap.get(current);

            if (next == null || next.equals(current)) {
                return current;
            }

            current = next;
        }

        return current;
    }

    private int normalizeProgress(int progress) {
        return Math.max(0, Math.min(100, progress));
    }

    private void notifyProgress(@NonNull UUID fileId, int progress) {
        for (Listener listener : listeners) {
            listener.onProgress(fileId, progress);
        }
    }

    private void notifyCompleted(@NonNull UUID fileId) {
        for (Listener listener : listeners) {
            listener.onCompleted(fileId);
        }
    }

    private void notifyError(@NonNull UUID fileId) {
        for (Listener listener : listeners) {
            listener.onError(fileId);
        }
    }

    private void notifyCleared(@NonNull UUID fileId) {
        for (Listener listener : listeners) {
            listener.onCleared(fileId);
        }
    }
}
