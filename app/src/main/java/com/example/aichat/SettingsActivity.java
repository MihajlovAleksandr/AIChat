package com.example.aichat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.example.aichat.controller.ThemeController;
import com.example.aichat.dto.request.NotificationRequest;
import com.example.aichat.dto.response.ApiError;
import com.example.aichat.dto.response.ConnectionChangeResponse;
import com.example.aichat.dto.response.NotificationResponse;
import com.example.aichat.dto.response.PreferenceResponse;
import com.example.aichat.dto.response.UserDataResponse;
import com.example.aichat.dto.response.UserResponse;
import com.example.aichat.model.ai.AIModel;
import com.example.aichat.model.ai.AISettingsStore;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.LogoutHelper;
import com.example.aichat.model.entities.ConnectionInfo;
import com.example.aichat.model.entities.Preference;
import com.example.aichat.model.entities.UserData;
import com.example.aichat.model.LocaleManager;
import com.example.aichat.model.notifications.NotificationSettingsManager;
import com.example.aichat.model.utils.files.FileDownloadProgressManager;
import com.example.aichat.model.utils.files.FileManager;
import com.example.aichat.model.utils.files.FileManagerHolder;
import com.example.aichat.model.utils.JsonHelper;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.model.utils.mappers.PreferenceMapper;
import com.example.aichat.model.utils.mappers.UserDataMapper;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.model.utils.theme.ThemeModel;
import com.example.aichat.model.utils.theme.ThemeSelectionCoordinator;
import com.example.aichat.model.utils.theme.ThemeStorage;
import com.example.aichat.view.helpers.HoneycombRevealView;
import com.example.aichat.view.main.BaseActivity;
import com.example.aichat.view.helpers.FullScreenHelper;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.view.PreferenceActivity;
import com.example.aichat.view.theme.binders.SettingsActivityThemeBinder;
import com.example.aichat.view.theme.ThemesActivity;
import com.example.aichat.view.UserDataActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@androidx.media3.common.util.UnstableApi
public class SettingsActivity extends BaseActivity {

    private MaterialSwitch showEmailNotificationsSwitch;
    private MaterialSwitch showNotificationsSwitch;
    private MaterialSwitch backgroundNotificationsSwitch;
    private MaterialSwitch inAppNotificationsSwitch;
    private MaterialSwitch vibrationSwitch;
    private MaterialSwitch fullscreenSwitch;

    private boolean isProgrammaticChange;
    private boolean isRestartingWithReveal = false;

    private TextView currentLanguageText;
    private TextView currentThemeText;
    private TextView currentAiModelText;

    private ScrollView scrollView;

    private int savedScrollY = 0;

    private SharedPreferences prefs;

    private final ConnectionDispatcher dispatcher;

    private UserData userData;
    private Preference preference;

    private final MapperResponse<Preference, PreferenceResponse> preferenceMapper =
            new PreferenceMapper();

    private final MapperResponse<UserData, UserDataResponse> userDataMapper =
            new UserDataMapper();

    private ArrayList<ConnectionInfo> devices;

    private FileManager fileManager;

    private TextView storageSummary;
    private TextView emailText;
    private TextView devicesText;
    private TextView userDataText;
    private TextView preferenceText;

    private ProgressBar storageProgress;

    private int emailClickCount = 0;


    public SettingsActivity() {
        dispatcher =
                ConnectionSingleton
                        .getInstance()
                        .getConnectionDispatcher();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs =
                getSharedPreferences(
                        ThemeSelectionCoordinator.SETTINGS_PREFS,
                        MODE_PRIVATE
                );

        savedScrollY =
                prefs.getInt(
                        "settings_scroll_y",
                        0
                );

        applyTheme(
                prefs.getString(
                        ThemeSelectionCoordinator.KEY_APP_THEME,
                        ThemeSelectionCoordinator.THEME_SYSTEM
                )
        );

        setContentView(
                R.layout.activity_settings
        );

        initializeViews();

        SettingsActivityThemeBinder.apply(
                this
        );

        loadUserData();
        loadDevices();
        loadEmailNotificationState();
        registerConnectionChangedListener();
        restoreScrollPosition();
        setupClickListeners();
        setupReferenceAccordion();
        setupNotificationSection();
        loadNotificationSettings();
        loadFullscreenSetting();
        updateCurrentLanguageText();
        updateCurrentThemeText();
        updateCurrentAiModelText();

        SettingsActivityThemeBinder.apply(
                this
        );

        NotificationSettingsManager
                .requestNotificationPermissionIfNeeded(
                        this
                );

        fileManager =
                FileManagerHolder.get(
                        getApplicationContext(),
                        new FileDownloadProgressManager()
                );

        updateStorageInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isRestartingWithReveal) {
            return;
        }

        applyTheme(
                prefs.getString(
                        ThemeSelectionCoordinator.KEY_APP_THEME,
                        ThemeSelectionCoordinator.THEME_SYSTEM
                )
        );

        SettingsActivityThemeBinder.apply(
                this
        );

        updateCurrentThemeText();
        updateCurrentAiModelText();
        SettingsActivityThemeBinder.apply(
                this
        );
        updateStorageInfo();
        loadUserData();
    }

    private void loadDevices() {
        dispatcher.sendHttpRequestAsync(
                "/api/user/devices",
                HttpClient.HTTPMethod.GET,
                null,
                false
        ).thenAccept(cmd -> {
            if (cmd.isSuccess()) {
                devices =
                        new ArrayList<>(
                                Arrays.asList(
                                        cmd.getData(
                                                ConnectionInfo[].class
                                        )
                                )
                        );

                runOnUiThread(
                        this::updateDeviceStatus
                );

            } else {
                Log.e(
                        "Device Request",
                        cmd.getData(
                                ApiError.class
                        ).toString()
                );
            }
        });
    }

    private void loadEmailNotificationState() {
        dispatcher.sendHttpRequestAsync(
                "/api/notification/settings",
                HttpClient.HTTPMethod.GET,
                null,
                false
        ).thenAccept(cmd -> {
            if (cmd.isSuccess()) {
                NotificationResponse response =
                        cmd.getData(
                                NotificationResponse.class
                        );

                runOnUiThread(() -> {
                    showEmailNotificationsSwitch.setChecked(
                            response.emailNotificationsEnabled
                    );

                    SettingsActivityThemeBinder.apply(
                            this
                    );
                });

            } else {
                Log.e(
                        "Settings Request",
                        cmd.getData(
                                ApiError.class
                        ).toString()
                );
            }
        });
    }

    private void registerConnectionChangedListener() {
        dispatcher.addEventListener(
                "ConnectionChanged",
                ConnectionChangeResponse.class,
                command -> {
                    ConnectionChangeResponse response =
                            command.getPayload();

                    if (response != null) {
                        devices =
                                new ArrayList<>(
                                        response.connections
                                );

                        runOnUiThread(
                                this::updateDeviceStatus
                        );
                    }
                }
        );
    }

    private void updateStorageInfo() {
        if (fileManager == null
                || storageSummary == null
                || storageProgress == null) {
            return;
        }

        fileManager.getCacheSizeAsync(size -> {
            long max =
                    fileManager.getCacheLimit();

            int percent =
                    max > 0
                            ? (int) ((size * 100) / max)
                            : 0;

            long mb =
                    size / (1024 * 1024);

            long maxMb =
                    max / (1024 * 1024);

            storageSummary.setText(
                    "Использовано: "
                            + mb
                            + " MB / "
                            + maxMb
                            + " MB"
            );

            storageProgress.setProgress(
                    Math.min(
                            percent,
                            100
                    )
            );
        });
    }

    private void updateDeviceStatus() {
        if (devices == null || devicesText == null) {
            return;
        }

        int[] devicesCount =
                getDeviceStatus(
                        devices
                );

        devicesText.setText(
                getString(
                        R.string.device_status,
                        devicesCount[0],
                        devicesCount[1]
                )
        );
    }

    private int[] getDeviceStatus(List<ConnectionInfo> connections) {
        if (connections == null) {
            return new int[]{
                    0,
                    0
            };
        }

        int[] conn =
                new int[]{
                        connections.size(),
                        0
                };

        for (ConnectionInfo cr : connections) {
            if (cr.getLastOnlineFormat() == null) {
                conn[1]++;
            }
        }

        return conn;
    }

    private void initializeViews() {
        scrollView =
                findViewById(
                        R.id.settings_scroll
                );

        ImageButton backButton =
                findViewById(
                        R.id.back_button
                );

        backButton.setOnClickListener(
                v -> finish()
        );

        emailText =
                findViewById(
                        R.id.email_text
                );

        devicesText =
                findViewById(
                        R.id.devices_text
                );

        userDataText =
                findViewById(
                        R.id.userData_text
                );

        preferenceText =
                findViewById(
                        R.id.preference_text
                );

        storageSummary =
                findViewById(
                        R.id.storage_summary
                );

        storageProgress =
                findViewById(
                        R.id.storage_progress
                );

        TextView versionText =
                findViewById(
                        R.id.version_text
                );

        versionText.setText(
                getString(
                        R.string.version_format,
                        "1.0.0"
                )
        );

        showNotificationsSwitch =
                findViewById(
                        R.id.show_notifications_switch
                );

        showEmailNotificationsSwitch =
                findViewById(
                        R.id.email_notifications_switch
                );

        backgroundNotificationsSwitch =
                findViewById(
                        R.id.background_notifications_switch
                );

        inAppNotificationsSwitch =
                findViewById(
                        R.id.in_app_notifications_switch
                );

        vibrationSwitch =
                findViewById(
                        R.id.vibration_switch
                );

        fullscreenSwitch =
                findViewById(
                        R.id.fullscreen_switch
                );

        currentLanguageText =
                findViewById(
                        R.id.current_language_text
                );

        currentThemeText =
                findViewById(
                        R.id.current_theme_text
                );

        currentAiModelText =
                findViewById(
                        R.id.current_ai_model_text
                );


        findViewById(
                R.id.language_item
        ).setOnClickListener(
                v -> showLanguageSelectionDialog()
        );

        findViewById(
                R.id.theme_item
        ).setOnClickListener(
                v -> showThemeSelectionDialog()
        );

        findViewById(
                R.id.ai_model_item
        ).setOnClickListener(
                v -> showAiModelSelectionDialog()
        );

        fullscreenSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit()
                    .putBoolean(
                            "fullscreen_mode",
                            isChecked
                    )
                    .apply();

            if (isChecked) {
                FullScreenHelper.enableFullScreen(
                        getWindow()
                );

            } else {
                restoreSystemUI();
            }
        });
    }

    private void applyTheme(String mode) {
        ThemeModel selectedCustomTheme =
                getSelectedCustomTheme();

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
                mode
        );
    }

    private ThemeModel getSelectedCustomTheme() {
        ThemeStorage storage =
                new ThemeStorage(
                        this
                );

        String selectedCustomThemeId =
                ThemeSelectionCoordinator.getSelectedCustomThemeId(
                        this
                );

        ThemeModel selectedTheme = getThemeByExistingId(
                storage,
                selectedCustomThemeId
        );

        if (selectedTheme != null) {
            return selectedTheme;
        }

        String currentSetting = prefs != null
                ? prefs.getString(
                ThemeSelectionCoordinator.KEY_APP_THEME,
                ThemeSelectionCoordinator.THEME_SYSTEM
        )
                : ThemeSelectionCoordinator.THEME_SYSTEM;

        if (currentSetting != null
                && !currentSetting.trim().isEmpty()
                && !ThemeSelectionCoordinator.isDefaultThemeMode(currentSetting)) {

            selectedTheme = getThemeByExistingId(
                    storage,
                    currentSetting
            );

            if (selectedTheme != null) {
                return selectedTheme;
            }
        }

        return null;
    }

    private ThemeModel getThemeByExistingId(
            ThemeStorage storage,
            String themeId
    ) {
        if (storage == null || themeId == null || themeId.trim().isEmpty()) {
            return null;
        }

        return storage.getThemeById(
                themeId.trim()
        );
    }

    private void restoreSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow()
                    .setDecorFitsSystemWindows(
                            true
                    );

            WindowInsetsController controller =
                    getWindow()
                            .getInsetsController();

            if (controller != null) {
                controller.show(
                        WindowInsets.Type.statusBars()
                                | WindowInsets.Type.navigationBars()
                );
            }

        } else {
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_VISIBLE
                    );
        }
    }

    private void restoreScrollPosition() {
        scrollView.post(() ->
                scrollView.scrollTo(
                        0,
                        savedScrollY
                )
        );
    }

    private void saveScrollPosition() {
        if (scrollView == null || prefs == null) {
            return;
        }

        prefs.edit()
                .putInt(
                        "settings_scroll_y",
                        scrollView.getScrollY()
                )
                .apply();
    }

    private void loadFullscreenSetting() {
        boolean isFullscreen =
                prefs.getBoolean(
                        "fullscreen_mode",
                        true
                );

        fullscreenSwitch.setChecked(
                isFullscreen
        );

        if (isFullscreen) {
            FullScreenHelper.enableFullScreen(
                    getWindow()
            );
        }
    }

    private void updateCurrentLanguageText() {
        String[] languages =
                getResources()
                        .getStringArray(
                                R.array.languages
                        );

        String[] codes =
                getResources()
                        .getStringArray(
                                R.array.language_codes
                        );

        String currentLang =
                LocaleManager.getLocale(
                        this
                ).getLanguage();

        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentLang)) {
                currentLanguageText.setText(
                        languages[i]
                );

                return;
            }
        }

        currentLanguageText.setText(
                languages[0]
        );
    }

    private void updateCurrentThemeText() {
        ThemeModel selectedCustomTheme =
                getSelectedCustomTheme();

        if (selectedCustomTheme != null) {
            currentThemeText.setText(
                    selectedCustomTheme.getName()
            );

            return;
        }

        String mode =
                prefs.getString(
                        ThemeSelectionCoordinator.KEY_APP_THEME,
                        ThemeSelectionCoordinator.THEME_SYSTEM
                );

        currentThemeText.setText(
                getDefaultThemeTitle(
                        mode
                )
        );
    }


    private void updateCurrentAiModelText() {
        if (currentAiModelText == null) {
            return;
        }

        currentAiModelText.setText(
                AISettingsStore
                        .getSelectedChatModel(
                                this
                        )
                        .getDisplayName()
        );
    }

    private void showAiModelSelectionDialog() {
        AIModel[] models =
                AIModel.chatSelectableModels();

        String[] titles =
                new String[
                        models.length
                ];

        AIModel selectedModel =
                AISettingsStore.getSelectedChatModel(
                        this
                );

        int selectedIndex =
                0;

        for (int i = 0; i < models.length; i++) {
            titles[i] =
                    models[i].getDisplayName();

            if (models[i] == selectedModel) {
                selectedIndex =
                        i;
            }
        }

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        R.string.ai_model_title
                )
                .setSingleChoiceItems(
                        titles,
                        selectedIndex,
                        (dialog, which) -> {
                            if (which < 0 || which >= models.length) {
                                dialog.dismiss();
                                return;
                            }

                            AIModel selected =
                                    models[which];

                            dialog.dismiss();

                            if (selected == selectedModel) {
                                return;
                            }

                            AISettingsStore.saveSelectedChatModel(
                                    this,
                                    selected
                            );

                            updateCurrentAiModelText();
                            restartAppWithReveal();
                        }
                )
                .show();
    }

    private String getDefaultThemeTitle(String mode) {
        if (ThemeSelectionCoordinator.THEME_LIGHT.equals(mode)) {
            return getString(
                    R.string.theme_light
            );
        }

        if (ThemeSelectionCoordinator.THEME_DARK.equals(mode)) {
            return getString(
                    R.string.theme_dark
            );
        }

        return getString(
                R.string.theme_system
        );
    }

    private void applyCustomTheme(String themeId) {
        ThemeStorage storage =
                new ThemeStorage(
                        this
                );

        ThemeModel theme =
                storage.getThemeById(
                        themeId
                );

        if (theme == null || theme.getId() == null) {
            return;
        }

        currentThemeText.setText(
                theme.getName()
        );

        storage.setSelectedThemeId(themeId);

        new ThemeController(
                ConnectionSingleton.getInstance().getConnectionDispatcher()
        ).selectTheme(
                theme.getId()
        );

        restartAppWithReveal(() ->
                ThemeSelectionCoordinator.saveCustomThemeOnly(
                        this,
                        themeId
                )
        );
    }

    private void selectCustomTheme(String themeId) {
        applyCustomTheme(
                themeId
        );
    }

    private void selectDefaultTheme(String mode) {
        currentThemeText.setText(
                getDefaultThemeTitle(
                        mode
                )
        );

        restartAppWithReveal(() -> {
            prefs.edit()
                    .remove("selected_custom_theme")
                    .apply();

            ThemeSelectionCoordinator.saveDefaultThemeOnly(
                    this,
                    mode
            );
        });
    }

    private void showLanguageSelectionDialog() {
        String[] languages =
                getResources()
                        .getStringArray(
                                R.array.languages
                        );

        String[] codes =
                getResources()
                        .getStringArray(
                                R.array.language_codes
                        );

        String currentLang =
                LocaleManager.getLocale(
                        this
                ).getLanguage();

        int currentIndex =
                0;

        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentLang)) {
                currentIndex =
                        i;
            }
        }

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        R.string.select_language
                )
                .setSingleChoiceItems(
                        languages,
                        currentIndex,
                        (dialog, which) -> {
                            String selectedLang =
                                    codes[which];

                            if (!selectedLang.equals(currentLang)) {
                                prefs.edit()
                                        .putString(
                                                "app_language",
                                                selectedLang
                                        )
                                        .apply();

                                LocaleManager.setLocale(
                                        this,
                                        selectedLang
                                );

                                dialog.dismiss();

                                restartAppWithReveal();

                            } else {
                                dialog.dismiss();
                            }
                        }
                )
                .show();
    }

    private List<ThemeModel> deduplicateThemes(List<ThemeModel> source) {
        java.util.LinkedHashMap<String, ThemeModel> result =
                new java.util.LinkedHashMap<>();

        if (source == null) {
            return new ArrayList<>();
        }

        for (ThemeModel model : source) {
            if (model == null || model.getId() == null) {
                continue;
            }

            String themeId = model.getId().toString();

            if (ThemeSelectionCoordinator.isLegacyStandardThemeId(themeId)) {
                continue;
            }

            result.put(
                    themeId,
                    model
            );
        }

        return new ArrayList<>(
                result.values()
        );
    }

    private void showThemeSelectionDialog() {
        ThemeStorage storage =
                new ThemeStorage(
                        this
                );

        List<ThemeModel> customThemes =
                deduplicateThemes(
                        storage.getThemes()
                );

        List<String> titles =
                new ArrayList<>();

        List<String> values =
                new ArrayList<>();

        titles.add(
                getString(
                        R.string.theme_light
                )
        );

        values.add(
                ThemeSelectionCoordinator.THEME_LIGHT
        );

        titles.add(
                getString(
                        R.string.theme_dark
                )
        );

        values.add(
                ThemeSelectionCoordinator.THEME_DARK
        );

        titles.add(
                getString(
                        R.string.theme_system
                )
        );

        values.add(
                ThemeSelectionCoordinator.THEME_SYSTEM
        );

        for (ThemeModel model : customThemes) {
            titles.add(
                    model.getName()
            );

            values.add(
                    model.getId().toString()
            );
        }

        ThemeModel selectedCustomTheme =
                getSelectedCustomTheme();

        String currentTheme =
                selectedCustomTheme != null
                        ? selectedCustomTheme.getId().toString()
                        : prefs.getString(
                        ThemeSelectionCoordinator.KEY_APP_THEME,
                        ThemeSelectionCoordinator.THEME_SYSTEM
                );

        int currentIndex =
                0;

        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equals(currentTheme)) {
                currentIndex =
                        i;

                break;
            }
        }

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        R.string.select_theme
                )
                .setSingleChoiceItems(
                        titles.toArray(
                                new String[0]
                        ),
                        currentIndex,
                        (dialog, which) -> {
                            String selected =
                                    values.get(
                                            which
                                    );

                            if (selected.equals(currentTheme)) {
                                dialog.dismiss();
                                return;
                            }

                            dialog.dismiss();

                            if (ThemeSelectionCoordinator.isDefaultThemeMode(
                                    selected
                            )) {
                                selectDefaultTheme(
                                        selected
                                );

                            } else {
                                selectCustomTheme(
                                        selected
                                );
                            }
                        }
                )
                .setNeutralButton(
                        R.string.manage_themes,
                        (dialog, which) -> {
                            dialog.dismiss();

                            startActivity(
                                    new Intent(
                                            this,
                                            ThemesActivity.class
                                    )
                            );
                        }
                )
                .show();
    }

    private void restartAppWithReveal() {
        restartAppWithReveal(
                null
        );
    }

    private void restartAppWithReveal(@Nullable Runnable beforeRestart) {
        if (isRestartingWithReveal) {
            return;
        }

        isRestartingWithReveal =
                true;

        saveScrollPosition();

        HoneycombRevealView honey =
                new HoneycombRevealView(
                        this
                );

        addContentView(
                honey,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

        honey.bringToFront();

        honey.start(
                false,
                () -> {
                    if (beforeRestart != null) {
                        beforeRestart.run();
                    }

                    ThemeSelectionCoordinator.restartApp(
                            this,
                            MainActivity.class
                    );
                }
        );
    }

    private void loadNotificationSettings() {
        showNotificationsSwitch.setChecked(
                NotificationSettingsManager.areNotificationsEnabled(
                        this
                )
        );

        backgroundNotificationsSwitch.setChecked(
                NotificationSettingsManager.areBackgroundNotificationsEnabled(
                        this
                )
        );

        inAppNotificationsSwitch.setChecked(
                NotificationSettingsManager.areInAppNotificationsEnabled(
                        this
                )
        );

        vibrationSwitch.setChecked(
                NotificationSettingsManager.isVibrationEnabled(
                        this
                )
        );

        SettingsActivityThemeBinder.apply(
                this
        );
    }

    private void loadUserData() {
        dispatcher.sendHttpRequestAsync(
                "/api/user",
                HttpClient.HTTPMethod.GET,
                null,
                false
        ).thenAccept(cmd -> {
            if (cmd.isSuccess()) {
                UserResponse userResponse =
                        cmd.getData(
                                UserResponse.class
                        );

                Preference newPreference =
                        preferenceMapper.ToModel(
                                userResponse.preference
                        );

                UserData newUserData =
                        userDataMapper.ToModel(
                                userResponse.userData
                        );

                runOnUiThread(() -> {
                    preference =
                            newPreference;

                    userData =
                            newUserData;

                    emailText.setText(
                            userResponse.email
                    );

                    if (preference != null) {
                        preferenceText.setText(
                                preference.toString()
                        );
                    }

                    if (userData != null) {
                        userDataText.setText(
                                userData.toString()
                        );
                    }
                });

            } else {
                Log.e(
                        "User Request",
                        cmd.getData(
                                ApiError.class
                        ).toString()
                );
            }
        });
    }

    private void setupClickListeners() {
        findViewById(
                R.id.profile_email_item
        ).setOnClickListener(
                v -> handleEmailClicks()
        );


        findViewById(
                R.id.devices_item
        ).setOnClickListener(v -> {
            Intent intent =
                    new Intent(
                            this,
                            DevicesActivity.class
                    );

            intent.putExtra(
                    "devices",
                    JsonHelper.Serialize(
                            devices
                    )
            );

            startActivity(
                    intent
            );
        });

        findViewById(
                R.id.userData_item
        ).setOnClickListener(v -> {
            if (userData != null) {
                startActivity(
                        new Intent(
                                this,
                                UserDataActivity.class
                        )
                                .putExtra(
                                        "userData",
                                        JsonHelper.Serialize(
                                                userData
                                        )
                                )
                );
            }
        });

        findViewById(
                R.id.preference_item
        ).setOnClickListener(v -> {
            if (preference != null) {
                startActivity(
                        new Intent(
                                this,
                                PreferenceActivity.class
                        )
                                .putExtra(
                                        "preference",
                                        JsonHelper.Serialize(
                                                preference
                                        )
                                )
                );
            }
        });

        findViewById(
                R.id.logout_item
        ).setOnClickListener(v ->
                dispatcher.sendHttpRequestAsync(
                        "/api/session",
                        HttpClient.HTTPMethod.DELETE,
                        null,
                        false
                ).thenAccept(cmd -> {
                    if (cmd.isSuccess()) {
                        LogoutHelper.logout(
                                this
                        );

                    } else {
                        Log.e(
                                "DeleteConnection Request",
                                ApiError.class.toString()
                        );
                    }
                })
        );

        findViewById(
                R.id.storage_item
        ).setOnClickListener(v ->
                startActivity(
                        new Intent(
                                this,
                                com.example.aichat.view.cache.CacheUsageActivity.class
                        )
                )
        );
    }


    private void setupReferenceAccordion() {
        bindReferenceAccordionItem(
                R.id.reference_faq_header,
                R.id.faq_content_text,
                R.id.faq_chevron
        );

        bindReferenceAccordionItem(
                R.id.reference_policy_header,
                R.id.policy_content_text,
                R.id.policy_chevron
        );

        bindReferenceAccordionItem(
                R.id.reference_support_header,
                R.id.support_content_text,
                R.id.support_chevron
        );
    }

    private void bindReferenceAccordionItem(int headerId, int contentId, int chevronId) {
        View header =
                findViewById(
                        headerId
                );

        View content =
                findViewById(
                        contentId
                );

        View chevron =
                findViewById(
                        chevronId
                );

        if (header == null || content == null) {
            return;
        }

        content.setVisibility(
                View.GONE
        );

        content.setAlpha(
                1f
        );

        if (chevron != null) {
            chevron.setRotation(
                    0f
            );
        }

        header.setOnClickListener(v ->
                toggleReferenceContent(
                        content,
                        chevron
                )
        );
    }

    private void toggleReferenceContent(View content, @Nullable View chevron) {
        boolean shouldExpand =
                content.getVisibility() != View.VISIBLE;

        if (chevron != null) {
            chevron.animate()
                    .rotation(
                            shouldExpand ? 180f : 0f
                    )
                    .setDuration(
                            180L
                    )
                    .setInterpolator(
                            new AccelerateDecelerateInterpolator()
                    )
                    .start();
        }

        if (shouldExpand) {
            content.animate()
                    .cancel();

            content.setAlpha(
                    0f
            );

            content.setVisibility(
                    View.VISIBLE
            );

            content.animate()
                    .alpha(
                            1f
                    )
                    .setDuration(
                            180L
                    )
                    .setInterpolator(
                            new AccelerateDecelerateInterpolator()
                    )
                    .start();

            return;
        }

        content.animate()
                .cancel();

        content.animate()
                .alpha(
                        0f
                )
                .setDuration(
                        150L
                )
                .setInterpolator(
                        new AccelerateDecelerateInterpolator()
                )
                .withEndAction(() -> {
                    content.setVisibility(
                            View.GONE
                    );

                    content.setAlpha(
                            1f
                    );
                })
                .start();
    }


    private void setupNotificationSection() {
        showEmailNotificationsSwitch.setOnCheckedChangeListener((emailButtonView, emailIsChecked) -> {
            if (isProgrammaticChange) {
                return;
            }

            NotificationRequest request =
                    new NotificationRequest(
                            emailIsChecked
                    );

            dispatcher.sendHttpRequestAsync(
                    "/api/notification/settings",
                    HttpClient.HTTPMethod.PUT,
                    request,
                    false
            ).thenAccept(cmd -> {
                if (!cmd.isSuccess()) {
                    isProgrammaticChange =
                            true;

                    showEmailNotificationsSwitch.setChecked(
                            !emailIsChecked
                    );

                    isProgrammaticChange =
                            false;

                    Log.e(
                            "setupNotificationSection: ",
                            ApiError.class.toString()
                    );
                }
            });
        });

        showNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) {
                return;
            }

            NotificationSettingsManager.setNotificationsEnabled(
                    this,
                    isChecked
            );

            isProgrammaticChange =
                    true;

            if (!isChecked) {
                showEmailNotificationsSwitch.setChecked(
                        false
                );

                backgroundNotificationsSwitch.setChecked(
                        false
                );

                inAppNotificationsSwitch.setChecked(
                        false
                );

                vibrationSwitch.setChecked(
                        false
                );

                NotificationSettingsManager.setBackgroundNotificationsEnabled(
                        this,
                        false
                );

                NotificationSettingsManager.setInAppNotificationsEnabled(
                        this,
                        false
                );

                NotificationSettingsManager.setVibrationEnabled(
                        this,
                        false
                );

            } else {
                NotificationSettingsManager.requestNotificationPermissionIfNeeded(
                        this
                );
            }

            isProgrammaticChange =
                    false;
        });

        backgroundNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) {
                return;
            }

            if (isChecked && !showNotificationsSwitch.isChecked()) {
                isProgrammaticChange =
                        true;

                backgroundNotificationsSwitch.setChecked(
                        false
                );

                isProgrammaticChange =
                        false;

                Toast.makeText(
                        this,
                        R.string.enable_notifications_first,
                        Toast.LENGTH_SHORT
                ).show();

            } else {
                NotificationSettingsManager.setBackgroundNotificationsEnabled(
                        this,
                        isChecked
                );
            }
        });

        inAppNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) {
                return;
            }

            if (isChecked && !showNotificationsSwitch.isChecked()) {
                isProgrammaticChange =
                        true;

                inAppNotificationsSwitch.setChecked(
                        false
                );

                isProgrammaticChange =
                        false;

                Toast.makeText(
                        this,
                        R.string.enable_notifications_first,
                        Toast.LENGTH_SHORT
                ).show();

            } else {
                NotificationSettingsManager.setInAppNotificationsEnabled(
                        this,
                        isChecked
                );
            }
        });

        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) {
                return;
            }

            if (isChecked && !showNotificationsSwitch.isChecked()) {
                isProgrammaticChange =
                        true;

                vibrationSwitch.setChecked(
                        false
                );

                isProgrammaticChange =
                        false;

                Toast.makeText(
                        this,
                        R.string.enable_notifications_first,
                        Toast.LENGTH_SHORT
                ).show();

            } else {
                NotificationSettingsManager.setVibrationEnabled(
                        this,
                        isChecked
                );
            }
        });
    }

    private void handleEmailClicks() {
        emailClickCount++;

        prefs.edit()
                .putInt(
                        "email_click_count",
                        emailClickCount
                )
                .apply();

        if (emailClickCount == 10) {
            showEasterEggToast(
                    "Ну чего ты щёлкаешь как дятел?!"
            );

        } else if (emailClickCount == 20) {
            showEasterEggToast(
                    "Тебе настолько нечего делать что-ли...?"
            );

        } else if (emailClickCount == 30) {
            showEasterEggToast(
                    "Только попробуй ещё раз... :)"
            );

        } else if (emailClickCount >= 31) {
            prefs.edit()
                    .putInt(
                            "email_click_count",
                            0
                    )
                    .apply();

            finishAffinity();

            System.exit(
                    0
            );
        }
    }

    private void showEasterEggToast(String text) {
        Toast.makeText(
                this,
                text,
                Toast.LENGTH_LONG
        ).show();

        NotificationSettingsManager.playNotificationSound(
                this
        );
    }

    @Override
    protected void onDestroy() {
        saveScrollPosition();

        super.onDestroy();
    }
}
