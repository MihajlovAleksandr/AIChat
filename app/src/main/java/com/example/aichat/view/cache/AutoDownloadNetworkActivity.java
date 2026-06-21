
package com.example.aichat.view.cache;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
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

public class AutoDownloadNetworkActivity extends BaseActivity {

    public static final String EXTRA_NETWORK_MODE =
            "network_mode";

    private MediaAutoDownloadSettingsManager manager;
    private MediaAutoDownloadSettingsManager.NetworkMode mode;

    private ScrollView scrollView;
    private LinearLayout root;
    private LinearLayout masterRow;
    private LinearLayout trafficCard;
    private LinearLayout mediaCard;
    private LinearLayout mediaRows;
    private MaterialSwitch masterSwitch;
    private SeekBar trafficSeekBar;
    private TextView titleView;
    private TextView noteView;

    private boolean refreshing;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applyCurrentTheme();

        setContentView(
                R.layout.activity_auto_download_network
        );

        manager =
                new MediaAutoDownloadSettingsManager(
                        getApplicationContext()
                );

        mode =
                MediaAutoDownloadSettingsManager.NetworkMode.fromKey(
                        getIntent().getStringExtra(
                                EXTRA_NETWORK_MODE
                        )
                );

        bindViews();
        setupListeners();
        renderContent();

        AutoDownloadThemeBinder.applyNetwork(
                this
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        renderContent();

        AutoDownloadThemeBinder.applyNetwork(
                this
        );
    }

    private void bindViews() {
        scrollView =
                findViewById(
                        R.id.network_auto_download_scroll
                );

        root =
                findViewById(
                        R.id.network_auto_download_root
                );

        masterRow =
                findViewById(
                        R.id.network_auto_download_master_row
                );

        trafficCard =
                findViewById(
                        R.id.network_traffic_card
                );

        mediaCard =
                findViewById(
                        R.id.network_media_card
                );

        mediaRows =
                findViewById(
                        R.id.network_media_rows
                );

        masterSwitch =
                findViewById(
                        R.id.network_auto_download_master_switch
                );

        trafficSeekBar =
                findViewById(
                        R.id.network_traffic_seekbar
                );

        titleView =
                findViewById(
                        R.id.network_auto_download_title
                );

        noteView =
                findViewById(
                        R.id.network_note
                );
    }

    private void setupListeners() {
        findViewById(
                R.id.network_auto_download_back
        ).setOnClickListener(v ->
                finish()
        );

        masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (refreshing) {
                return;
            }

            manager.setEnabled(
                    mode,
                    isChecked
            );

            renderContent();

            AutoDownloadThemeBinder.applyNetwork(
                    this
            );
        });

        trafficSeekBar.setMax(
                2
        );

        trafficSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        if (fromUser) {
                            manager.setTrafficPreset(
                                    mode,
                                    progress
                            );

                            renderContent();

                            AutoDownloadThemeBinder.applyNetwork(
                                    AutoDownloadNetworkActivity.this
                            );
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                }
        );
    }

    private void renderContent() {
        if (manager == null || mode == null) {
            return;
        }

        refreshing =
                true;

        MediaAutoDownloadSettingsManager.NetworkPolicy policy =
                manager.getPolicy(
                        mode
                );

        titleView.setText(
                networkModeTitle(
                        mode
                )
        );

        masterSwitch.setChecked(
                policy.enabled
        );

        trafficSeekBar.setProgress(
                policy.trafficPreset
        );

        mediaRows.removeAllViews();

        addToggleRow(
                getString(
                        R.string.cache_media_photo
                ),
                getString(
                        R.string.cache_enabled_for_all_chats
                ),
                policy.photoEnabled,
                checked -> manager.setPhotoEnabled(
                        mode,
                        checked
                )
        );

        CacheUi.addDivider(
                this,
                mediaRows
        );

        addToggleRow(
                getString(
                        R.string.cache_media_video
                ),
                getString(
                        R.string.cache_up_to_for_all_chats,
                        FileCacheStats.formatBytes(
                                policy.maxVideoBytes
                        )
                ),
                policy.videoEnabled,
                checked -> manager.setVideoEnabled(
                        mode,
                        checked
                )
        );

        CacheUi.addDivider(
                this,
                mediaRows
        );

        addToggleRow(
                getString(
                        R.string.cache_media_files
                ),
                getString(
                        R.string.cache_up_to_for_all_chats,
                        FileCacheStats.formatBytes(
                                policy.maxFileBytes
                        )
                ),
                policy.filesEnabled,
                checked -> manager.setFilesEnabled(
                        mode,
                        checked
                )
        );

        CacheUi.addDivider(
                this,
                mediaRows
        );

        addToggleRow(
                getString(
                        R.string.cache_media_stories
                ),
                policy.storiesEnabled
                        ? getString(
                        R.string.cache_on_short
                )
                        : getString(
                        R.string.cache_off_short
                ),
                policy.storiesEnabled,
                checked -> manager.setStoriesEnabled(
                        mode,
                        checked
                )
        );

        refreshing =
                false;

        AutoDownloadThemeBinder.applyNetwork(
                this
        );
    }

    private interface ToggleSetter {
        void set(boolean checked);
    }

    private void addToggleRow(
            @NonNull String title,
            @NonNull String subtitle,
            boolean checked,
            @NonNull ToggleSetter setter
    ) {
        LinearLayout row =
                CacheUi.horizontal(
                        this
                );

        row.setMinimumHeight(
                CacheUi.dp(
                        this,
                        72
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

        LinearLayout texts =
                CacheUi.vertical(
                        this
                );

        texts.addView(
                CacheUi.text(
                        this,
                        title,
                        17,
                        CacheUi.colorOnSurface(
                                this
                        ),
                        Typeface.NORMAL
                )
        );

        texts.addView(
                CacheUi.text(
                        this,
                        subtitle,
                        14,
                        CacheUi.colorOnSurfaceVariant(
                                this
                        ),
                        Typeface.NORMAL
                )
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
                checked
        );

        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setter.set(
                    isChecked
            );

            renderContent();

            AutoDownloadThemeBinder.applyNetwork(
                    this
            );
        });

        row.addView(
                switchView
        );

        mediaRows.addView(
                row
        );

        AutoDownloadThemeBinder.tintRowsRecursively(
                row
        );
    }

    private String networkModeTitle(@NonNull MediaAutoDownloadSettingsManager.NetworkMode mode) {
        switch (mode) {
            case WIFI:
                return getString(
                        R.string.cache_network_wifi_short
                );

            case ROAMING:
                return getString(
                        R.string.cache_network_roaming_short
                );

            case MOBILE:
            default:
                return getString(
                        R.string.cache_network_mobile_short
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
