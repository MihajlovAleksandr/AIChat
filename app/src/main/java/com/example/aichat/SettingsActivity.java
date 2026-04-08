package com.example.aichat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.aichat.dto.request.DeleteConnectionRequest;
import com.example.aichat.dto.request.SetNotificationRequest;
import com.example.aichat.dto.response.ConnectionChangeResponse;
import com.example.aichat.dto.response.NotificationResponse;
import com.example.aichat.dto.response.PreferenceResponse;
import com.example.aichat.dto.response.SettingsInfoResponse;
import com.example.aichat.dto.response.UserDataResponse;
import com.example.aichat.model.LocaleManager;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Preference;
import com.example.aichat.model.entities.UserData;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.model.notifications.NotificationSettingsManager;
import com.example.aichat.model.utils.JsonHelper;
import com.example.aichat.model.utils.mappers.MapperResponse;
import com.example.aichat.model.utils.mappers.PreferenceMapper;
import com.example.aichat.model.utils.mappers.UserDataMapper;
import com.example.aichat.util.HoneycombRevealView;
import com.example.aichat.view.BaseActivity;
import com.example.aichat.view.FullScreenHelper;
import com.example.aichat.view.PreferenceActivity;
import com.example.aichat.view.UserDataActivity;
import com.example.aichat.view.main.MainActivity;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends BaseActivity {

    private MaterialSwitch showEmailNotificationsSwitch;
    private MaterialSwitch showNotificationsSwitch;
    private MaterialSwitch backgroundNotificationsSwitch;
    private MaterialSwitch inAppNotificationsSwitch;
    private MaterialSwitch vibrationSwitch;
    private MaterialSwitch fullscreenSwitch;

    private TextView currentLanguageText;
    private TextView currentThemeText;
    private ScrollView scrollView;
    private int savedScrollY = 0;

    private boolean isProgrammaticChange = false;
    private SharedPreferences prefs;

    private ConnectionManager connectionManager;
    private OnConnectionEvents events;
    private UserData userData;
    private Preference preference;
    private final MapperResponse<Preference, PreferenceResponse> preferenceMapper = new PreferenceMapper();
    private final MapperResponse<UserData, UserDataResponse> userDataMapper = new UserDataMapper();

    private TextView emailText, devicesText, userDataText, preferenceText;
    private int emailClickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE);
        savedScrollY = prefs.getInt("settings_scroll_y", 0);
        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();

        applyTheme(prefs.getString("app_theme", "system"));

        setContentView(R.layout.activity_settings);

        initializeViews();
        restoreScrollPosition();

        setupConnectionEvents();
        setupClickListeners();
        setupNotificationSection();
        loadNotificationSettings();
        loadFullscreenSetting();
        updateCurrentLanguageText();
        updateCurrentThemeText();

        if (connectionManager != null) {
            connectionManager.SendCommand(new WSSCommand("GetSettingsInfo"));
        }

        NotificationSettingsManager.requestNotificationPermissionIfNeeded(this);
    }

    private void initializeViews() {
        scrollView = findViewById(R.id.settings_scroll);

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        emailText = findViewById(R.id.email_text);
        devicesText = findViewById(R.id.devices_text);
        userDataText = findViewById(R.id.userData_text);
        preferenceText = findViewById(R.id.preference_text);

        TextView versionText = findViewById(R.id.version_text);
        versionText.setText(getString(R.string.version_format, "1.0.0"));

        showNotificationsSwitch = findViewById(R.id.show_notifications_switch);
        showEmailNotificationsSwitch = findViewById(R.id.email_notifications_switch);
        backgroundNotificationsSwitch = findViewById(R.id.background_notifications_switch);
        inAppNotificationsSwitch = findViewById(R.id.in_app_notifications_switch);
        vibrationSwitch = findViewById(R.id.vibration_switch);
        fullscreenSwitch = findViewById(R.id.fullscreen_switch);
        currentLanguageText = findViewById(R.id.current_language_text);
        currentThemeText = findViewById(R.id.current_theme_text);

        findViewById(R.id.language_item).setOnClickListener(v -> showLanguageSelectionDialog());
        findViewById(R.id.theme_item).setOnClickListener(v -> showThemeSelectionDialog());

        fullscreenSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("fullscreen_mode", isChecked).apply();
            if (isChecked) FullScreenHelper.enableFullScreen(getWindow());
            else restoreSystemUI();
        });
    }

    private void applyTheme(String mode) {
        switch (mode) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void restoreSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void restoreScrollPosition() {
        scrollView.post(() -> scrollView.scrollTo(0, savedScrollY));
    }

    private void saveScrollPosition() {
        prefs.edit().putInt("settings_scroll_y", scrollView.getScrollY()).apply();
    }

    private void loadFullscreenSetting() {
        boolean isFullscreen = prefs.getBoolean("fullscreen_mode", true);
        fullscreenSwitch.setChecked(isFullscreen);
        if (isFullscreen) FullScreenHelper.enableFullScreen(getWindow());
    }

    private void updateCurrentLanguageText() {
        String[] languages = getResources().getStringArray(R.array.languages);
        String[] codes = getResources().getStringArray(R.array.language_codes);
        String currentLang = LocaleManager.getLocale(this).getLanguage();
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentLang)) {
                currentLanguageText.setText(languages[i]);
                return;
            }
        }
        currentLanguageText.setText(languages[0]);
    }

    private void updateCurrentThemeText() {
        String mode = prefs.getString("app_theme", "system");
        switch (mode) {
            case "light":
                currentThemeText.setText(getString(R.string.theme_light));
                break;
            case "dark":
                currentThemeText.setText(getString(R.string.theme_dark));
                break;
            default:
                currentThemeText.setText(getString(R.string.theme_system));
                break;
        }
    }

    private void showLanguageSelectionDialog() {
        String[] languages = getResources().getStringArray(R.array.languages);
        String[] codes = getResources().getStringArray(R.array.language_codes);
        String currentLang = LocaleManager.getLocale(this).getLanguage();

        int currentIndex = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentLang)) currentIndex = i;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.select_language)
                .setSingleChoiceItems(languages, currentIndex, (dialog, which) -> {
                    String selectedLang = codes[which];
                    if (!selectedLang.equals(currentLang)) {
                        prefs.edit().putString("app_language", selectedLang).apply();
                        LocaleManager.setLocale(this, selectedLang);

                        if (connectionManager != null && events != null) {
                            connectionManager.removeConnectionEvent(events);
                            events = null;
                        }

                        dialog.dismiss();

                        HoneycombRevealView honey = new HoneycombRevealView(this);
                        addContentView(honey, new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                        ));
                        honey.start(false, () -> restartAppTo(MainActivity.class));
                    } else dialog.dismiss();
                }).show();
    }

    private void showThemeSelectionDialog() {
        String[] themes = getResources().getStringArray(R.array.theme_modes);
        String[] values = getResources().getStringArray(R.array.theme_mode_values);

        String current = prefs.getString("app_theme", "system");
        int currentIndex = 0;

        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                currentIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.select_theme)
                .setSingleChoiceItems(themes, currentIndex, (dialog, which) -> {
                    String selected = values[which];
                    if (!selected.equals(current)) {
                        prefs.edit().putString("app_theme", selected).apply();
                        applyTheme(selected);

                        if (connectionManager != null && events != null) {
                            connectionManager.removeConnectionEvent(events);
                            events = null;
                        }

                        dialog.dismiss();

                        HoneycombRevealView honey = new HoneycombRevealView(this);
                        addContentView(honey, new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                        ));
                        honey.start(false, () -> restartAppTo(MainActivity.class));
                    } else dialog.dismiss();
                })
                .show();
    }

    private void loadNotificationSettings() {
        isProgrammaticChange = true;
        showNotificationsSwitch.setChecked(NotificationSettingsManager.areNotificationsEnabled(this));
        backgroundNotificationsSwitch.setChecked(NotificationSettingsManager.areBackgroundNotificationsEnabled(this));
        inAppNotificationsSwitch.setChecked(NotificationSettingsManager.areInAppNotificationsEnabled(this));
        vibrationSwitch.setChecked(NotificationSettingsManager.isVibrationEnabled(this));
        isProgrammaticChange = false;
    }

    private void setupConnectionEvents() {
        if (connectionManager == null) return;
        if (events != null) connectionManager.removeConnectionEvent(events);

        events = new OnConnectionEvents() {
            @Override
            public void OnCommandGot(WSSCommand cmd) {
                runOnUiThread(() -> {
                    switch (cmd.getOperation()) {
                        case "GetSettingsInfo":
                            isProgrammaticChange = true;
                            SettingsInfoResponse info = cmd.getData(SettingsInfoResponse.class);
                            emailText.setText(info.email);
                            preference = preferenceMapper.ToModel(info.preference);
                            userData = userDataMapper.ToModel(info.userData);
                            preferenceText.setText(preference.toString());
                            userDataText.setText(userData.toString());
                            int[] devicesCount = info.connectionCount;
                            devicesText.setText(getString(R.string.device_status, devicesCount[0], devicesCount[1]));
                            showEmailNotificationsSwitch.setChecked(info.notifications.emailNotificationsEnabled);
                            isProgrammaticChange = false;
                            break;
                        case "PreferenceUpdated":
                            preference = preferenceMapper.ToModel(cmd.getData(PreferenceResponse.class));
                            preferenceText.setText(preference.toString());
                            break;
                        case "UserDataUpdated":
                            userData = userDataMapper.ToModel(cmd.getData(UserDataResponse.class));
                            userDataText.setText(userData.toString());
                            break;
                        case "ConnectionsChange":
                        case "DeleteConnection":
                            int[] devices = cmd.getData(ConnectionChangeResponse.class).count;
                            devicesText.setText(getString(R.string.device_status, devices[0], devices[1]));
                            break;
                        case "UpdateNotifications":
                            isProgrammaticChange = true;
                            showEmailNotificationsSwitch.setChecked(
                                    cmd.getData(NotificationResponse.class).emailNotificationsEnabled
                            );
                            isProgrammaticChange = false;
                            break;
                    }
                });
            }
            @Override public void OnConnectionFailed() {}
            @Override public void OnOpen() {}
        };
        connectionManager.addConnectionEvent(events);
    }

    private void setupClickListeners() {
        findViewById(R.id.profile_email_item).setOnClickListener(v -> handleEmailClicks());
        findViewById(R.id.change_password_item).setOnClickListener(v -> {
            if (userData != null) startActivity(new Intent(this, ChangePasswordActivity.class));
        });
        findViewById(R.id.devices_item).setOnClickListener(v ->
                startActivity(new Intent(this, DevicesActivity.class)));
        findViewById(R.id.userData_item).setOnClickListener(v -> {
            if (userData != null) startActivity(new Intent(this, UserDataActivity.class)
                    .putExtra("userData", JsonHelper.Serialize(userData)));
        });
        findViewById(R.id.preference_item).setOnClickListener(v -> {
            if (preference != null) startActivity(new Intent(this, PreferenceActivity.class)
                    .putExtra("preference", JsonHelper.Serialize(preference)));
        });
        findViewById(R.id.logout_item).setOnClickListener(v ->
                connectionManager.SendCommand(new WSSCommand("DeleteConnection", new DeleteConnectionRequest(null))));
    }

    private void setupNotificationSection() {
        showEmailNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;
            connectionManager.SendCommand(new WSSCommand("UpdateNotifications", new SetNotificationRequest(isChecked)));
        });

        showNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;

            NotificationSettingsManager.setNotificationsEnabled(this, isChecked);
            isProgrammaticChange = true;

            if (!isChecked) {
                showEmailNotificationsSwitch.setChecked(false);
                backgroundNotificationsSwitch.setChecked(false);
                inAppNotificationsSwitch.setChecked(false);
                vibrationSwitch.setChecked(false);

                NotificationSettingsManager.setBackgroundNotificationsEnabled(this, false);
                NotificationSettingsManager.setInAppNotificationsEnabled(this, false);
                NotificationSettingsManager.setVibrationEnabled(this, false);

                connectionManager.SendCommand(new WSSCommand("UpdateNotifications", new SetNotificationRequest(false)));
            } else NotificationSettingsManager.requestNotificationPermissionIfNeeded(this);

            isProgrammaticChange = false;
        });

        backgroundNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;
            if (isChecked && !showNotificationsSwitch.isChecked()) {
                isProgrammaticChange = true;
                backgroundNotificationsSwitch.setChecked(false);
                isProgrammaticChange = false;
                Toast.makeText(this, R.string.enable_notifications_first, Toast.LENGTH_SHORT).show();
            } else NotificationSettingsManager.setBackgroundNotificationsEnabled(this, isChecked);
        });

        inAppNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;
            if (isChecked && !showNotificationsSwitch.isChecked()) {
                isProgrammaticChange = true;
                inAppNotificationsSwitch.setChecked(false);
                isProgrammaticChange = false;
                Toast.makeText(this, R.string.enable_notifications_first, Toast.LENGTH_SHORT).show();
            } else NotificationSettingsManager.setInAppNotificationsEnabled(this, isChecked);
        });

        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;
            if (isChecked && !showNotificationsSwitch.isChecked()) {
                isProgrammaticChange = true;
                vibrationSwitch.setChecked(false);
                isProgrammaticChange = false;
                Toast.makeText(this, R.string.enable_notifications_first, Toast.LENGTH_SHORT).show();
            } else NotificationSettingsManager.setVibrationEnabled(this, isChecked);
        });
    }

    private void handleEmailClicks() {
        emailClickCount++;
        prefs.edit().putInt("email_click_count", emailClickCount).apply();

        if (emailClickCount == 10) showEasterEggToast("Ну чего ты щёлкаешь как дятел?!");
        else if (emailClickCount == 20) showEasterEggToast("Тебе настолько нечего делать что-ли...?");
        else if (emailClickCount == 30) showEasterEggToast("Только попробуй ещё раз... :)");
        else if (emailClickCount >= 31) {
            prefs.edit().putInt("email_click_count", 0).apply();
            finishAffinity();
            System.exit(0);
        }
    }

    private void showEasterEggToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        NotificationSettingsManager.playNotificationSound(this);
    }

    @Override
    protected void onDestroy() {
        saveScrollPosition();
        if (connectionManager != null && events != null) {
            connectionManager.removeConnectionEvent(events);
            events = null;
        }
        super.onDestroy();
    }
}
