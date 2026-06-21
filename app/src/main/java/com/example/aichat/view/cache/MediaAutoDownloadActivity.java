
package com.example.aichat.view.cache;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.view.main.BaseActivity;
import com.example.aichat.model.utils.files.FileCacheStats;
import com.example.aichat.model.utils.files.MediaAutoDownloadSettingsManager;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.model.utils.theme.ThemeModel;
import com.example.aichat.model.utils.theme.ThemeSelectionCoordinator;
import com.example.aichat.model.utils.theme.ThemeStorage;
import com.example.aichat.R;
import com.example.aichat.view.theme.binders.AutoDownloadThemeBinder;
import com.google.android.material.materialswitch.MaterialSwitch;

public class MediaAutoDownloadActivity extends BaseActivity {

    private MediaAutoDownloadSettingsManager manager;
    private ScrollView scrollView;
    private LinearLayout root;
    private LinearLayout card;
    private LinearLayout rowsContainer;
    private TextView resetButton;
    private TextView noteView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applyCurrentTheme();

        setContentView(
                R.layout.activity_media_auto_download
        );

        manager =
                new MediaAutoDownloadSettingsManager(
                        getApplicationContext()
                );

        bindViews();
        setupListeners();
        renderRows();

        AutoDownloadThemeBinder.applyMedia(
                this
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        renderRows();

        AutoDownloadThemeBinder.applyMedia(
                this
        );
    }

    private void bindViews() {
        scrollView =
                findViewById(
                        R.id.media_auto_download_scroll
                );

        root =
                findViewById(
                        R.id.media_auto_download_root
                );

        card =
                findViewById(
                        R.id.media_auto_download_card
                );

        rowsContainer =
                findViewById(
                        R.id.media_auto_download_rows
                );

        resetButton =
                findViewById(
                        R.id.media_auto_download_reset
                );

        noteView =
                findViewById(
                        R.id.media_auto_download_note
                );
    }

    private void setupListeners() {
        findViewById(
                R.id.media_auto_download_back
        ).setOnClickListener(v ->
                finish()
        );

        if (resetButton != null) {
            resetButton.setOnClickListener(v -> {
                manager.resetToDefaults();

                Toast.makeText(
                        this,
                        R.string.cache_auto_download_reset_done,
                        Toast.LENGTH_SHORT
                ).show();

                renderRows();

                AutoDownloadThemeBinder.applyMedia(
                        this
                );
            });
        }
    }

    private void renderRows() {
        if (rowsContainer == null || manager == null) {
            return;
        }

        rowsContainer.removeAllViews();

        addNetworkRow(
                MediaAutoDownloadSettingsManager.NetworkMode.MOBILE
        );

        CacheUi.addDivider(
                this,
                rowsContainer
        );

        addNetworkRow(
                MediaAutoDownloadSettingsManager.NetworkMode.WIFI
        );

        CacheUi.addDivider(
                this,
                rowsContainer
        );

        addNetworkRow(
                MediaAutoDownloadSettingsManager.NetworkMode.ROAMING
        );

        AutoDownloadThemeBinder.tintRowsRecursively(
                rowsContainer
        );
    }

    private void addNetworkRow(@NonNull MediaAutoDownloadSettingsManager.NetworkMode mode) {
        MediaAutoDownloadSettingsManager.NetworkPolicy policy =
                manager.getPolicy(
                        mode
                );

        LinearLayout row =
                CacheUi.horizontal(
                        this
                );

        row.setMinimumHeight(
                CacheUi.dp(
                        this,
                        82
                )
        );

        row.setPadding(
                0,
                CacheUi.dp(
                        this,
                        6
                ),
                0,
                CacheUi.dp(
                        this,
                        6
                )
        );

        row.setClickable(
                true
        );

        row.setFocusable(
                true
        );

        LinearLayout texts =
                CacheUi.vertical(
                        this
                );

        TextView title =
                CacheUi.text(
                        this,
                        networkModeTitle(
                                mode
                        ),
                        17,
                        CacheUi.colorOnSurface(
                                this
                        ),
                        Typeface.BOLD
                );

        TextView summary =
                CacheUi.text(
                        this,
                        buildPolicySummary(
                                policy
                        ),
                        14,
                        CacheUi.colorOnSurfaceVariant(
                                this
                        ),
                        Typeface.NORMAL
                );

        texts.addView(
                title
        );

        texts.addView(
                summary
        );

        row.addView(
                texts,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        MaterialSwitch switchView =
                new MaterialSwitch(
                        this
                );

        switchView.setChecked(
                policy.enabled
        );

        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            manager.setEnabled(
                    mode,
                    isChecked
            );

            summary.setText(
                    buildPolicySummary(
                            manager.getPolicy(
                                    mode
                            )
                    )
            );

            AutoDownloadThemeBinder.tintRowsRecursively(
                    rowsContainer
            );
        });

        row.addView(
                switchView
        );

        row.setOnClickListener(v -> {
            Intent intent =
                    new Intent(
                            this,
                            AutoDownloadNetworkActivity.class
                    );

            intent.putExtra(
                    AutoDownloadNetworkActivity.EXTRA_NETWORK_MODE,
                    mode.key
            );

            startActivity(
                    intent
            );
        });

        rowsContainer.addView(
                row
        );

        AutoDownloadThemeBinder.tintRowsRecursively(
                row
        );
    }

    @NonNull
    private String buildPolicySummary(@NonNull MediaAutoDownloadSettingsManager.NetworkPolicy policy) {
        if (!policy.enabled) {
            return getString(
                    R.string.cache_disabled
            );
        }

        StringBuilder builder =
                new StringBuilder();

        if (policy.photoEnabled) {
            builder.append(
                    getString(
                            R.string.cache_media_photo
                    )
            );
        }

        if (policy.videoEnabled) {
            appendComma(
                    builder
            );

            builder.append(
                    getString(
                            R.string.cache_media_video_with_limit,
                            FileCacheStats.formatBytes(
                                    policy.maxVideoBytes
                            )
                    )
            );
        }

        if (policy.filesEnabled) {
            appendComma(
                    builder
            );

            builder.append(
                    getString(
                            R.string.cache_media_files_with_limit,
                            FileCacheStats.formatBytes(
                                    policy.maxFileBytes
                            )
                    )
            );
        }

        if (policy.storiesEnabled) {
            appendComma(
                    builder
            );

            builder.append(
                    getString(
                            R.string.cache_media_stories
                    )
            );
        }

        return builder.length() > 0
                ? builder.toString()
                : getString(
                R.string.cache_nothing_selected
        );
    }

    private static void appendComma(@NonNull StringBuilder builder) {
        if (builder.length() > 0) {
            builder.append(
                    ", "
            );
        }
    }

    private String networkModeTitle(@NonNull MediaAutoDownloadSettingsManager.NetworkMode mode) {
        switch (mode) {
            case WIFI:
                return getString(
                        R.string.cache_network_wifi
                );

            case ROAMING:
                return getString(
                        R.string.cache_network_roaming
                );

            case MOBILE:
            default:
                return getString(
                        R.string.cache_network_mobile
                );
        }
    }

    private void applyCurrentTheme() {
        SharedPreferences prefs =
                getSharedPreferences(
                        ThemeSelectionCoordinator.SETTINGS_PREFS,
                        MODE_PRIVATE
                );

        ThemeModel selectedCustomTheme =
                new ThemeStorage(
                        this
                ).getSelectedTheme();

        if (selectedCustomTheme != null) {
            ThemeSelectionCoordinator.applyNightMode(
                    ThemeSelectionCoordinator.THEME_LIGHT
            );

            ThemeAttrResolver.applyTheme(
                    selectedCustomTheme
            );

            return;
        }

        ThemeAttrResolver.clear();

        ThemeSelectionCoordinator.applyNightMode(
                prefs.getString(
                        ThemeSelectionCoordinator.KEY_APP_THEME,
                        ThemeSelectionCoordinator.THEME_SYSTEM
                )
        );
    }
}
