package com.example.aichat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.aichat.dto.request.NotificationRequest;
import com.example.aichat.dto.response.ApiError;
import com.example.aichat.dto.response.ConnectionChangeResponse;
import com.example.aichat.dto.response.IntegrationResponse;
import com.example.aichat.dto.response.NotificationResponse;
import com.example.aichat.dto.response.PreferenceResponse;
import com.example.aichat.dto.response.UserDataResponse;
import com.example.aichat.dto.response.UserResponse;
import com.example.aichat.model.LocaleManager;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.EventHandler;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.LogoutHelper;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.SignalRCommand;
import com.example.aichat.model.entities.ConnectionInfo;
import com.example.aichat.model.entities.IntegrationTypes;
import com.example.aichat.model.entities.Preference;
import com.example.aichat.model.entities.UserData;
import com.example.aichat.model.notifications.NotificationSettingsManager;
import com.example.aichat.model.utils.FileDownloadProgressManager;
import com.example.aichat.model.utils.FileManager;
import com.example.aichat.model.utils.FileManagerHolder;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends BaseActivity {

    private MaterialSwitch showEmailNotificationsSwitch;
    private MaterialSwitch showNotificationsSwitch;
    private MaterialSwitch backgroundNotificationsSwitch;
    private MaterialSwitch inAppNotificationsSwitch;
    private MaterialSwitch vibrationSwitch;
    private MaterialSwitch fullscreenSwitch;
    private boolean isProgrammaticChange;
    private TextView currentLanguageText;
    private TextView currentThemeText;
    private ScrollView scrollView;
    private int savedScrollY = 0;
    private SharedPreferences prefs;
    private final ConnectionDispatcher dispatcher;
    private UserData userData;
    private Preference preference;
    private final MapperResponse<Preference, PreferenceResponse> preferenceMapper = new PreferenceMapper();
    private final MapperResponse<UserData, UserDataResponse> userDataMapper = new UserDataMapper();
    private ArrayList<ConnectionInfo> devices;

    private FileManager fileManager;
    private TextView storageSummary;
    private TextView emailText, devicesText, userDataText, preferenceText;

    private ProgressBar storageProgress;
    private int emailClickCount = 0;

    // Интеграции
    private ImageView googleIntegration;
    private ImageView instIntegration;
    private ImageView facebookIntegration;
    private ImageView telegramIntegration;

    public SettingsActivity(){
        dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE);
        savedScrollY = prefs.getInt("settings_scroll_y", 0);

        applyTheme(prefs.getString("app_theme", "system"));

        setContentView(R.layout.activity_settings);

        initializeViews();

        loadUserData();

        dispatcher.sendHttpRequestAsync(
                "/api/user/devices",
                HttpClient.HTTPMethod.GET,
                null,
                false
        ).thenAccept(cmd -> {

            if (cmd.isSuccess()) {

                devices = new ArrayList<>(
                        Arrays.asList(
                                cmd.getData(ConnectionInfo[].class)
                        )
                );

                runOnUiThread(this::updateDeviceStatus);

            } else {

                Log.e(
                        "Device Request",
                        cmd.getData(ApiError.class).toString()
                );
            }
        });

        dispatcher.sendHttpRequestAsync(
                "/api/notification/settings",
                HttpClient.HTTPMethod.GET,
                null,
                false
        ).thenAccept(cmd -> {

            if (cmd.isSuccess()) {

                NotificationResponse response =
                        cmd.getData(NotificationResponse.class);

                runOnUiThread(() ->
                        showEmailNotificationsSwitch.setChecked(
                                response.emailNotificationsEnabled
                        )
                );

            } else {

                Log.e(
                        "Settings Request",
                        cmd.getData(ApiError.class).toString()
                );
            }
        });

        loadIntegrations();

        dispatcher.addEventListener(
                "ConnectionChanged",
                ConnectionChangeResponse.class,
                command -> {

                    ConnectionChangeResponse response =
                            command.getPayload();

                    if (response != null) {

                        devices =
                                new ArrayList<>(response.connections);

                        runOnUiThread(this::updateDeviceStatus);
                    }
                }
        );

        restoreScrollPosition();

        setupClickListeners();

        setupNotificationSection();

        loadNotificationSettings();

        loadFullscreenSetting();

        updateCurrentLanguageText();

        updateCurrentThemeText();

        NotificationSettingsManager
                .requestNotificationPermissionIfNeeded(this);

        fileManager = FileManagerHolder.get(
                getApplicationContext(),
                new FileDownloadProgressManager()
        );

        updateStorageInfo();
    }

    // Загрузка интеграций
    private void loadIntegrations() {
        dispatcher.sendHttpRequestAsync("/api/user/integration", HttpClient.HTTPMethod.GET, null, false)
                .thenAccept(cmd -> {
                    if (cmd.isSuccess()) {
                        IntegrationTypes[] response = cmd.getData(IntegrationTypes[].class);
                        runOnUiThread(() -> {
                            applyIntegrations(response);
                            //animateIntegrations(); // Анимация появления
                        });
                    } else {
                        Log.e("Integration Request", "Error: " + cmd.getData(ApiError.class).toString());
                        // В случае ошибки можно установить значения по умолчанию
                        runOnUiThread(() -> {
                            setIntegrationState(googleIntegration, false);
                            setIntegrationState(instIntegration, false);
                            setIntegrationState(facebookIntegration, false);
                            setIntegrationState(telegramIntegration, false);
                        });
                    }
                })
                .exceptionally(throwable -> {
                    Log.e("Integration Request", "Exception: " + throwable.getMessage());
                    return null;
                });
    }

    // Применение состояния интеграций
    private void applyIntegrations(IntegrationTypes[] response) {

        var list = Arrays.asList(response);
        //setIntegrationState(googleIntegration, list.contains(IntegrationTypes.GOOGLE));
        ///setIntegrationState(instIntegration, list.contains(IntegrationTypes.INSTAGRAM));
        //setIntegrationState(facebookIntegration, list.contains(IntegrationTypes.FACEBOOK));
        //setIntegrationState(telegramIntegration, list.contains(IntegrationTypes.TELEGRAM));
    }

    // Установка визуального состояния иконки
    private void setIntegrationState(ImageView view, boolean enabled) {
        if (view != null) {
            view.setAlpha(enabled ? 1f : 0.3f);
            // Можно добавить ContentDescription для доступности
            view.setContentDescription(enabled ?
                    getString(R.string.integration_connected) :
                    getString(R.string.integration_disconnected));
        }
    }

    // Анимация появления иконок интеграций
    private void animateIntegrations() {
        ImageView[] integrations = {
                googleIntegration, instIntegration,
                facebookIntegration, telegramIntegration
        };

        for (int i = 0; i < integrations.length; i++) {
            if (integrations[i] != null) {
                integrations[i].setScaleX(0f);
                integrations[i].setScaleY(0f);
                integrations[i].setAlpha(0f);

                integrations[i].animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(integrations[i].getAlpha()) // Сохраняем установленную прозрачность
                        .setDuration(300)
                        .setStartDelay(i * 80)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
        }
    }

    private void toggleIntegration(String service, ImageView view, boolean currentState) {
        // Использование строк из ресурсов
        String serviceName = "";
        switch (service) {
            case "Google":
                serviceName = getString(R.string.service_google);
                break;
            case "Instagram":
                serviceName = getString(R.string.service_instagram);
                break;
            case "Facebook":
                serviceName = getString(R.string.service_facebook);
                break;
            case "Telegram":
                toggleTelegram();
                break;
        }

        Toast.makeText(this,
                getString(!currentState ? R.string.integration_connected_toast :
                        R.string.integration_disconnected_toast, serviceName),
                Toast.LENGTH_SHORT).show();
    }

    private void toggleTelegram(){
        dispatcher.sendHttpRequestAsync("/integration/telegram/generate", HttpClient.HTTPMethod.POST, null, false).thenAccept(cmd->{
            openUrl(this, "https://t.me/aichatapp_bot?start="+cmd.getData(String.class));
        });
    }

    public static void openUrl(Context context, String url) {
        if (url == null || url.isEmpty()) return;

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }

    private void updateStorageInfo() {
        fileManager.getCacheSizeAsync(size -> {

            long max = fileManager.getCacheLimit();

            int percent = max > 0 ? (int) ((size * 100) / max) : 0;

            long mb = size / (1024 * 1024);
            long maxMb = max / (1024 * 1024);

            storageSummary.setText("Использовано: " + mb + " MB / " + maxMb + " MB");
            storageProgress.setProgress(Math.min(percent, 100));
        });
    }

    private void updateDeviceStatus(){
        int[] devicesCount = getDeviceStatus(devices);
        devicesText.setText(getString(R.string.device_status, devicesCount[0], devicesCount[1]));
    }

    private int[] getDeviceStatus(List<ConnectionInfo> connections){
        int[] conn = new int[]{connections.size(),0};
        for (ConnectionInfo cr: connections) {
            if(cr.getLastOnlineFormat()==null)
                conn[1]++;
        }
        return conn;
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateStorageInfo();

        loadIntegrations();

        loadUserData();
    }

    private void initializeViews() {
        scrollView = findViewById(R.id.settings_scroll);

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        emailText = findViewById(R.id.email_text);
        devicesText = findViewById(R.id.devices_text);
        userDataText = findViewById(R.id.userData_text);
        preferenceText = findViewById(R.id.preference_text);

        storageSummary = findViewById(R.id.storage_summary);
        storageProgress = findViewById(R.id.storage_progress);

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

        // Инициализация интеграций
        googleIntegration = findViewById(R.id.integration_google);
        instIntegration = findViewById(R.id.integration_inst);
        facebookIntegration = findViewById(R.id.integration_facebook);
        telegramIntegration = findViewById(R.id.integration_telegram);

        // Настройка кликов для интеграций (опционально)
        setupIntegrationClickListeners();

        findViewById(R.id.language_item).setOnClickListener(v -> showLanguageSelectionDialog());
        findViewById(R.id.theme_item).setOnClickListener(v -> showThemeSelectionDialog());

        fullscreenSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("fullscreen_mode", isChecked).apply();
            if (isChecked) FullScreenHelper.enableFullScreen(getWindow());
            else restoreSystemUI();
        });
    }

    // Настройка кликов по иконкам интеграций (опционально)
    private void setupIntegrationClickListeners() {
        googleIntegration.setOnClickListener(v -> {
            boolean currentState = googleIntegration.getAlpha() >= 0.8f;
            toggleIntegration("Google", googleIntegration, currentState);
        });

        instIntegration.setOnClickListener(v -> {
            boolean currentState = instIntegration.getAlpha() >= 0.8f;
            toggleIntegration("Instagram", instIntegration, currentState);
        });

        facebookIntegration.setOnClickListener(v -> {
            boolean currentState = facebookIntegration.getAlpha() >= 0.8f;
            toggleIntegration("Facebook", facebookIntegration, currentState);
        });

        telegramIntegration.setOnClickListener(v -> {
            boolean currentState = telegramIntegration.getAlpha() >= 0.8f;
            toggleIntegration("Telegram", telegramIntegration, currentState);
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
        showNotificationsSwitch.setChecked(NotificationSettingsManager.areNotificationsEnabled(this));
        backgroundNotificationsSwitch.setChecked(NotificationSettingsManager.areBackgroundNotificationsEnabled(this));
        inAppNotificationsSwitch.setChecked(NotificationSettingsManager.areInAppNotificationsEnabled(this));
        vibrationSwitch.setChecked(NotificationSettingsManager.isVibrationEnabled(this));
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
                        cmd.getData(UserResponse.class);

                Preference newPreference =
                        preferenceMapper.ToModel(userResponse.preference);

                UserData newUserData =
                        userDataMapper.ToModel(userResponse.userData);

                runOnUiThread(() -> {

                    preference = newPreference;
                    userData = newUserData;

                    emailText.setText(userResponse.email);

                    if (preference != null) {
                        preferenceText.setText(preference.toString());
                    }

                    if (userData != null) {
                        userDataText.setText(userData.toString());
                    }
                });

            } else {

                Log.e(
                        "User Request",
                        cmd.getData(ApiError.class).toString()
                );
            }
        });
    }

    private void setupClickListeners() {
        findViewById(R.id.profile_email_item).setOnClickListener(v -> handleEmailClicks());

        findViewById(R.id.change_password_item).setOnClickListener(v -> {
            if (userData != null) startActivity(new Intent(this, ChangePasswordActivity.class));
        });

        findViewById(R.id.devices_item).setOnClickListener(v -> {
            Intent intent = new Intent(this, DevicesActivity.class);
            intent.putExtra("devices", JsonHelper.Serialize(devices));
            startActivity(intent);
        });

        findViewById(R.id.userData_item).setOnClickListener(v -> {
            if (userData != null) startActivity(new Intent(this, UserDataActivity.class)
                    .putExtra("userData", JsonHelper.Serialize(userData)));
        });

        findViewById(R.id.preference_item).setOnClickListener(v -> {
            if (preference != null) startActivity(new Intent(this, PreferenceActivity.class)
                    .putExtra("preference", JsonHelper.Serialize(preference)));
        });

        findViewById(R.id.logout_item).setOnClickListener(v ->
                dispatcher.sendHttpRequestAsync("/api/session", HttpClient.HTTPMethod.DELETE, null, false)
                        .thenAccept(cmd -> {
                            if (cmd.isSuccess()) {
                                LogoutHelper.logout(this);
                            } else {
                                Log.e("DeleteConnection Request", (ApiError.class).toString());
                            }
                        }));

        findViewById(R.id.storage_item).setOnClickListener(v -> {

            new AlertDialog.Builder(this)
                    .setTitle("Хранилище")
                    .setItems(new String[]{
                            "Очистить кэш",
                            "Лимит: 100 MB",
                            "Лимит: 500 MB",
                            "Лимит: 1 GB",
                            "Лимит: 2 GB"
                    }, (dialog, which) -> {

                        if (which == 0) {

                            fileManager.clearAllDownloadedFiles(() -> {
                                updateStorageInfo();
                                Toast.makeText(this, "Кэш очищен", Toast.LENGTH_SHORT).show();
                            });

                        } else {

                            long[] limits = {
                                    100L * 1024 * 1024,
                                    500L * 1024 * 1024,
                                    1024L * 1024 * 1024,
                                    2L * 1024 * 1024 * 1024
                            };

                            fileManager.setCacheLimit(limits[which - 1]);
                            updateStorageInfo();
                        }
                    })
                    .show();
        });
    }


    private void setupNotificationSection() {
        showEmailNotificationsSwitch.setOnCheckedChangeListener((emailButtonView, emailIsChecked) -> {
            if (isProgrammaticChange) return;
            NotificationRequest request = new NotificationRequest(emailIsChecked);
            dispatcher.sendHttpRequestAsync("/api/notification/settings", HttpClient.HTTPMethod.PUT, request, false).thenAccept((cmd) -> {
                if (!cmd.isSuccess()) {
                    isProgrammaticChange = true;
                    showEmailNotificationsSwitch.setChecked(!emailIsChecked);
                    isProgrammaticChange = false;
                    Log.e("setupNotificationSection: ", (ApiError.class).toString());
                }
            });
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
        super.onDestroy();
    }
}