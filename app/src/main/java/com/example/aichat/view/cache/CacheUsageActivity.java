package com.example.aichat.view.cache;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.view.main.BaseActivity;
import com.example.aichat.model.utils.files.CacheCategory;
import com.example.aichat.model.utils.files.FileCacheStats;
import com.example.aichat.model.utils.files.FileDownloadProgressManager;
import com.example.aichat.model.utils.files.FileManager;
import com.example.aichat.model.utils.files.FileManagerHolder;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.model.utils.theme.ThemeModel;
import com.example.aichat.model.utils.theme.ThemeSelectionCoordinator;
import com.example.aichat.model.utils.theme.ThemeStorage;
import com.example.aichat.R;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@androidx.media3.common.util.UnstableApi
public class CacheUsageActivity extends BaseActivity {

    private FileManager fileManager;
    private ScrollView scrollView;
    private LinearLayout root;
    private LinearLayout categoryCard;
    private LinearLayout categoryContainer;
    private LinearLayout limitCard;
    private CacheDonutChartView chartView;
    private TextView subtitleView;
    private TextView clearButton;
    private TextView autoDownloadItem;
    private TextView limitValue;
    private SeekBar limitSeekBar;

    private final Set<CacheCategory> selectedCategories = EnumSet.noneOf(CacheCategory.class);
    private FileCacheStats currentStats;
    private boolean restoringLimitSlider;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyCurrentTheme();
        setContentView(R.layout.activity_cache_usage);
        CacheUi.applyWindowBackground(this);

        fileManager = FileManagerHolder.get(
                getApplicationContext(),
                new FileDownloadProgressManager()
        );

        bindViews();
        applyColors();
        setupListeners();
        loadStats();
        bindCacheLimit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        applyColors();
        loadStats();
        bindCacheLimit();
    }

    private void bindViews() {
        scrollView = findViewById(R.id.cache_usage_scroll);
        root = findViewById(R.id.cache_usage_root);
        categoryCard = findViewById(R.id.cache_category_card);
        categoryContainer = findViewById(R.id.cache_category_container);
        chartView = findViewById(R.id.cache_donut_chart);
        subtitleView = findViewById(R.id.cache_usage_subtitle);
        clearButton = findViewById(R.id.cache_clear_button);
        autoDownloadItem = findViewById(R.id.cache_autodownload_item);
        limitCard = findViewById(R.id.cache_limit_card);
        limitValue = findViewById(R.id.cache_limit_value);
        limitSeekBar = findViewById(R.id.cache_limit_seekbar);

        TextView title = findViewById(R.id.cache_usage_title);
        TextView screenTitle = findViewById(R.id.cache_screen_title);
        TextView hint = findViewById(R.id.cache_usage_hint);
        TextView limitTitle = findViewById(R.id.cache_limit_title);
        TextView limitMin = findViewById(R.id.cache_limit_min);
        TextView limitMax = findViewById(R.id.cache_limit_max);
        ImageButton back = findViewById(R.id.cache_back_button);

        title.setTextColor(CacheUi.colorOnSurface(this));
        screenTitle.setTextColor(CacheUi.colorOnSurface(this));
        subtitleView.setTextColor(CacheUi.colorOnSurfaceVariant(this));
        hint.setTextColor(CacheUi.colorOnSurfaceVariant(this));
        limitTitle.setTextColor(CacheUi.colorPrimary(this));
        limitValue.setTextColor(CacheUi.colorPrimary(this));
        limitMin.setTextColor(CacheUi.colorOnSurfaceVariant(this));
        limitMax.setTextColor(CacheUi.colorOnSurfaceVariant(this));
        back.setColorFilter(CacheUi.colorPrimary(this));
    }

    private void applyColors() {
        scrollView.setBackgroundColor(CacheUi.colorBackground(this));
        root.setBackgroundColor(CacheUi.colorBackground(this));
        categoryCard.setBackground(CacheUi.rounded(CacheUi.cardColor(this), 22, this));
        limitCard.setBackground(CacheUi.rounded(CacheUi.cardColor(this), 22, this));
        clearButton.setBackground(CacheUi.rounded(CacheUi.colorPrimary(this), 28, this));
        clearButton.setTextColor(CacheUi.colorOnPrimary(this));
        autoDownloadItem.setTextColor(CacheUi.colorPrimary(this));

        limitSeekBar.setProgressTintList(
                ColorStateList.valueOf(
                        CacheUi.colorPrimary(this)
                )
        );

        limitSeekBar.setThumbTintList(
                ColorStateList.valueOf(
                        CacheUi.colorPrimary(this)
                )
        );

        limitSeekBar.setProgressBackgroundTintList(
                ColorStateList.valueOf(
                        CacheUi.colorOnSurfaceVariant(this)
                )
        );

        chartView.setThemeColors(
                CacheUi.colorBackground(this),
                CacheUi.colorOnSurface(this),
                CacheUi.colorOnSurfaceVariant(this)
        );
    }

    private void setupListeners() {
        findViewById(R.id.cache_back_button).setOnClickListener(v -> finish());
        autoDownloadItem.setOnClickListener(v -> startActivity(new Intent(this, MediaAutoDownloadActivity.class)));
        clearButton.setOnClickListener(v -> clearSelectedCategories());

        limitSeekBar.setMax(CacheUi.maxLimitSeekProgress());
        limitSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long limit = CacheUi.seekProgressToLimit(progress);
                limitValue.setText(CacheUi.formatCacheLimit(limit));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (fileManager == null || restoringLimitSlider) {
                    return;
                }

                long limit = CacheUi.seekProgressToLimit(seekBar.getProgress());
                fileManager.setCacheLimit(limit);
                Toast.makeText(
                        CacheUsageActivity.this,
                        getString(R.string.cache_limit_saved, CacheUi.formatCacheLimit(limit)),
                        Toast.LENGTH_SHORT
                ).show();
                loadStats();
            }
        });
    }

    private void bindCacheLimit() {
        if (fileManager == null || limitSeekBar == null || limitValue == null) {
            return;
        }

        restoringLimitSlider = true;
        long limit = fileManager.getCacheLimit();
        limitSeekBar.setProgress(CacheUi.limitToSeekProgress(limit));
        limitValue.setText(CacheUi.formatCacheLimit(limit));
        restoringLimitSlider = false;
    }

    private void loadStats() {
        if (fileManager == null) {
            return;
        }

        fileManager.getCacheStatsAsync(stats -> {
            currentStats = stats;
            selectedCategories.clear();

            for (FileCacheStats.CategoryStat stat : stats.getCategoryStats()) {
                if (stat.bytes > 0 || stat.filesCount > 0) {
                    selectedCategories.add(stat.category);
                }
            }

            renderStats(stats);
        });
    }

    private void renderStats(@NonNull FileCacheStats stats) {
        categoryContainer.removeAllViews();

        subtitleView.setText(getString(
                R.string.cache_usage_subtitle,
                FileCacheStats.formatBytes(stats.getTotalBytes())
        ));

        List<CacheDonutChartView.Slice> slices = new ArrayList<>();
        List<FileCacheStats.CategoryStat> categoryStats = stats.getCategoryStats();

        for (int i = 0; i < categoryStats.size(); i++) {
            FileCacheStats.CategoryStat stat = categoryStats.get(i);
            if (stat.bytes <= 0) {
                continue;
            }

            slices.add(new CacheDonutChartView.Slice(
                    stat.bytes,
                    CacheUi.CHART_COLORS[i % CacheUi.CHART_COLORS.length],
                    CacheUi.categoryTitle(this, stat.category)
            ));
        }

        chartView.setSlices(slices);

        int added = 0;
        for (int i = 0; i < categoryStats.size(); i++) {
            FileCacheStats.CategoryStat stat = categoryStats.get(i);
            if (stat.bytes <= 0 && stat.filesCount <= 0) {
                continue;
            }

            if (added > 0) {
                CacheUi.addDivider(this, categoryContainer);
            }

            categoryContainer.addView(buildCategoryRow(stat, CacheUi.CHART_COLORS[i % CacheUi.CHART_COLORS.length]));
            added++;
        }

        updateClearButtonText();
    }

    private View buildCategoryRow(@NonNull FileCacheStats.CategoryStat stat, int color) {
        LinearLayout row = CacheUi.horizontal(this);
        row.setPadding(0, CacheUi.dp(this, 8), 0, CacheUi.dp(this, 8));
        row.setMinimumHeight(CacheUi.dp(this, 62));
        row.setClickable(true);
        row.setFocusable(true);

        TextView check = CacheUi.text(this, "✓", 16, 0xFFFFFFFF, Typeface.BOLD);
        check.setGravity(Gravity.CENTER);
        check.setBackground(CacheUi.rounded(color, 18, this));
        check.setAlpha(selectedCategories.contains(stat.category) ? 1f : 0.28f);
        row.addView(check, new LinearLayout.LayoutParams(CacheUi.dp(this, 34), CacheUi.dp(this, 34)));

        TextView name = CacheUi.text(
                this,
                CacheUi.categoryTitle(this, stat.category) + "  " + (stat.percent > 0 ? stat.percent + "%" : "<1%"),
                17,
                CacheUi.colorOnSurface(this),
                Typeface.BOLD
        );
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameParams.setMargins(CacheUi.dp(this, 16), 0, CacheUi.dp(this, 10), 0);
        row.addView(name, nameParams);

        TextView size = CacheUi.text(this, FileCacheStats.formatBytes(stat.bytes), 16, CacheUi.colorPrimary(this), Typeface.NORMAL);
        row.addView(size);

        row.setOnClickListener(v -> {
            if (selectedCategories.contains(stat.category)) {
                selectedCategories.remove(stat.category);
                check.setAlpha(0.28f);
            } else {
                selectedCategories.add(stat.category);
                check.setAlpha(1f);
            }

            updateClearButtonText();
        });

        return row;
    }

    private void updateClearButtonText() {
        if (currentStats == null) {
            clearButton.setText(R.string.cache_clear_selected);
            return;
        }

        long bytes = 0L;
        for (CacheCategory category : selectedCategories) {
            bytes += currentStats.getBytes(category);
        }

        if (selectedCategories.isEmpty()) {
            clearButton.setText(R.string.cache_select_categories);
            clearButton.setAlpha(0.55f);
            return;
        }

        clearButton.setAlpha(1f);
        clearButton.setText(getString(R.string.cache_clear_button, FileCacheStats.formatBytes(bytes)));
    }

    private void clearSelectedCategories() {
        if (fileManager == null || selectedCategories.isEmpty()) {
            Toast.makeText(this, R.string.cache_select_categories_to_clear, Toast.LENGTH_SHORT).show();
            return;
        }

        EnumSet<CacheCategory> categoriesToClear = EnumSet.copyOf(selectedCategories);
        fileManager.clearCacheCategories(categoriesToClear, () -> {
            Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
            loadStats();
        });
    }

    private void applyCurrentTheme() {
        SharedPreferences prefs = getSharedPreferences(ThemeSelectionCoordinator.SETTINGS_PREFS, MODE_PRIVATE);
        ThemeModel selectedCustomTheme = new ThemeStorage(this).getSelectedTheme();

        if (selectedCustomTheme != null) {
            ThemeSelectionCoordinator.applyNightMode(ThemeSelectionCoordinator.THEME_LIGHT);
            ThemeAttrResolver.applyTheme(selectedCustomTheme);
            return;
        }

        ThemeAttrResolver.clear();
        ThemeSelectionCoordinator.applyNightMode(
                prefs.getString(ThemeSelectionCoordinator.KEY_APP_THEME, ThemeSelectionCoordinator.THEME_SYSTEM)
        );
    }
}
