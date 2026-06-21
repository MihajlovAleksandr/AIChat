package com.example.aichat.model.utils.files;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FileCacheStats {

    public static final class CategoryStat {
        public final CacheCategory category;
        public final long bytes;
        public final int filesCount;
        public final int percent;

        CategoryStat(
                @NonNull CacheCategory category,
                long bytes,
                int filesCount,
                int percent
        ) {
            this.category = category;
            this.bytes = Math.max(0, bytes);
            this.filesCount = Math.max(0, filesCount);
            this.percent = Math.max(0, percent);
        }
    }

    private final long totalBytes;
    private final int totalFilesCount;
    private final Map<CacheCategory, Long> bytesByCategory;
    private final Map<CacheCategory, Integer> countsByCategory;

    public FileCacheStats(
            long totalBytes,
            int totalFilesCount,
            @Nullable Map<CacheCategory, Long> bytesByCategory,
            @Nullable Map<CacheCategory, Integer> countsByCategory
    ) {
        this.totalBytes = Math.max(0, totalBytes);
        this.totalFilesCount = Math.max(0, totalFilesCount);

        Map<CacheCategory, Long> safeBytes = new EnumMap<>(CacheCategory.class);
        Map<CacheCategory, Integer> safeCounts = new EnumMap<>(CacheCategory.class);

        for (CacheCategory category : CacheCategory.values()) {
            long bytes = bytesByCategory != null && bytesByCategory.get(category) != null
                    ? bytesByCategory.get(category)
                    : 0L;

            int count = countsByCategory != null && countsByCategory.get(category) != null
                    ? countsByCategory.get(category)
                    : 0;

            safeBytes.put(category, Math.max(0L, bytes));
            safeCounts.put(category, Math.max(0, count));
        }

        this.bytesByCategory = Collections.unmodifiableMap(safeBytes);
        this.countsByCategory = Collections.unmodifiableMap(safeCounts);
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getTotalFilesCount() {
        return totalFilesCount;
    }

    public long getBytes(@NonNull CacheCategory category) {
        Long value = bytesByCategory.get(category);
        return value != null ? value : 0L;
    }

    public int getFilesCount(@NonNull CacheCategory category) {
        Integer value = countsByCategory.get(category);
        return value != null ? value : 0;
    }

    @NonNull
    public List<CategoryStat> getCategoryStats() {
        List<CategoryStat> result = new ArrayList<>();

        for (CacheCategory category : CacheCategory.values()) {
            long bytes = getBytes(category);
            int count = getFilesCount(category);
            int percent = totalBytes > 0 ? Math.round((bytes * 100f) / totalBytes) : 0;
            result.add(new CategoryStat(category, bytes, count, percent));
        }

        Collections.sort(result, (left, right) -> Long.compare(right.bytes, left.bytes));
        return result;
    }

    @NonNull
    public static String formatBytes(long bytes) {
        long safeBytes = Math.max(0L, bytes);
        double value = safeBytes;
        String unit = "B";

        if (value >= 1024) {
            value /= 1024d;
            unit = "KB";
        }

        if (value >= 1024) {
            value /= 1024d;
            unit = "MB";
        }

        if (value >= 1024) {
            value /= 1024d;
            unit = "GB";
        }

        if ("B".equals(unit)) {
            return safeBytes + " B";
        }

        if (value >= 100) {
            return String.format(Locale.US, "%.0f %s", value, unit);
        }

        if (value >= 10) {
            return String.format(Locale.US, "%.1f %s", value, unit);
        }

        return String.format(Locale.US, "%.2f %s", value, unit);
    }
}
