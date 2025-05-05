package com.example.aichat;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import com.example.aichat.model.LocaleManager;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.entities.Preference;
import com.example.aichat.model.entities.UserData;
import com.example.aichat.model.notifications.NotificationSettingsManager;
import com.example.aichat.model.notifications.NotificationSettingsManager.NotificationCallback;
import com.example.aichat.view.BaseActivity;
import com.example.aichat.view.PreferenceActivity;
import com.example.aichat.view.UserDataActivity;

public class SettingsActivity extends BaseActivity {
    private static final String WEBSITE_URL = "https://mihajlovaleksandr.github.io/AIChatSite/";
    private static final String SUPPORT_EMAIL = "aichatcorp@gmail.com";

    // UI элементы
    private Switch showEmailNotificationsSwitch;
    private Switch showNotificationsSwitch;
    private Switch backgroundWorkSwitch;
    private Switch backgroundNotificationsSwitch;
    private Switch inAppNotificationsSwitch;
    private Switch vibrationSwitch;

    // Флаг для определения программного изменения Switch
    private boolean isProgrammaticChange = false;

    private ConnectionManager connectionManager;
    private TextView emailText, devicesText, userDataText, preferenceText;
    private OnConnectionEvents events;
    private UserData userData;
    private Preference preference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();

        initializeViews();
        setupConnectionEvents();
        setupClickListeners();
        loadNotificationSettings();

        connectionManager.SendCommand(new Command("GetSettingsInfo"));
        NotificationSettingsManager.requestNotificationPermissionIfNeeded(this);
    }

    private void initializeViews() {
        // Кнопка назад
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // Текстовые поля
        emailText = findViewById(R.id.email_text);
        devicesText = findViewById(R.id.devices_text);
        userDataText = findViewById(R.id.userData_text);
        preferenceText = findViewById(R.id.preference_text);

        // Версия приложения
        TextView versionText = findViewById(R.id.version_text);
        versionText.setText(getString(R.string.version_format, "1.0.0"));

        // Переключатели уведомлений
        showNotificationsSwitch = findViewById(R.id.show_notifications_switch);
        showEmailNotificationsSwitch = findViewById(R.id.email_notifications_switch);
        backgroundWorkSwitch = findViewById(R.id.background_work_switch);
        backgroundNotificationsSwitch = findViewById(R.id.background_notifications_switch);
        inAppNotificationsSwitch = findViewById(R.id.in_app_notifications_switch);
        vibrationSwitch = findViewById(R.id.vibration_switch);
    }

    private void loadNotificationSettings() {
        isProgrammaticChange = true;

        // Загружаем сохраненные настройки уведомлений
        showNotificationsSwitch.setChecked(NotificationSettingsManager.areNotificationsEnabled(this));
        backgroundWorkSwitch.setChecked(NotificationSettingsManager.isBackgroundWorkAllowed(this));
        backgroundNotificationsSwitch.setChecked(NotificationSettingsManager.areBackgroundNotificationsEnabled(this));
        inAppNotificationsSwitch.setChecked(NotificationSettingsManager.areInAppNotificationsEnabled(this));
        vibrationSwitch.setChecked(NotificationSettingsManager.isVibrationEnabled(this));

        isProgrammaticChange = false;
    }

    private void setupConnectionEvents() {
        events = new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                runOnUiThread(() -> {
                    switch (command.getOperation()) {
                        case "GetSettingsInfo":
                            isProgrammaticChange = true;

                            // Получаем данные пользователя
                            emailText.setText(command.getData("email", String.class));
                            preference = command.getData("preference", Preference.class);
                            userData = command.getData("userData", UserData.class);
                            preferenceText.setText(preference.toString());
                            userDataText.setText(userData.toString());
                            int[] devicesCount = command.getData("devices", int[].class);
                            devicesText.setText(getString(R.string.device_status, devicesCount[0], devicesCount[1]));
                            showEmailNotificationsSwitch.setChecked(command.getData("emailNotifications", boolean.class));

                            isProgrammaticChange = false;
                            break;
                        case "PreferenceUpdated":
                            preference = command.getData("preference", Preference.class);
                            preferenceText.setText(preference.toString());
                            break;
                        case "UserDataUpdated":
                            userData = command.getData("userData", UserData.class);
                            userDataText.setText(userData.toString());
                            break;
                        case "DeleteConnection":
                        case "ConnectionsChange":
                            int[] devices = command.getData("count", int[].class);
                            devicesText.setText(getString(R.string.device_status, devices[0], devices[1]));
                            break;
                        case "EmailNotifications":
                            isProgrammaticChange = true;
                            showEmailNotificationsSwitch.setChecked(command.getData("enabled", boolean.class));
                            isProgrammaticChange = false;
                            break;
                    }
                });
            }

            @Override
            public void OnConnectionFailed() {}

            @Override
            public void OnOpen() {}
        };
        connectionManager.addConnectionEvent(events);
    }

    private void setupClickListeners() {
        setupProfileSection();
        setupNotificationSection();
        setupLanguageSection();
        setupReferenceSection();
    }

    private void setupProfileSection() {
        // Изменение пароля
        findViewById(R.id.change_password_item).setOnClickListener(v -> {
            if (userData != null) {
                startActivity(new Intent(this, ChangePasswordActivity.class)
                        .putExtra("userData", userData));
            }
        });

        // Устройства
        findViewById(R.id.devices_item).setOnClickListener(v ->
                startActivity(new Intent(this, DevicesActivity.class)));

        // Данные пользователя
        findViewById(R.id.userData_item).setOnClickListener(v -> {
            if (userData != null) {
                startActivity(new Intent(this, UserDataActivity.class)
                        .putExtra("userData", userData));
            }
        });

        // Настройки
        findViewById(R.id.preference_item).setOnClickListener(v -> {
            if (preference != null) {
                startActivity(new Intent(this, PreferenceActivity.class)
                        .putExtra("preference", preference));
            }
        });

        // Выход
        findViewById(R.id.logout_item).setOnClickListener(v ->
                connectionManager.SendCommand(new Command("DeleteConnection")));
    }

    private void setupNotificationSection() {
        // Email уведомления
        showEmailNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;

            Command command = new Command("EmailNotifications");
            command.addData("enabled", isChecked);
            connectionManager.SendCommand(command);
        });

        // Основные уведомления
        showNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;

            NotificationSettingsManager.setNotificationsEnabled(this, isChecked);

            isProgrammaticChange = true;
            if (!isChecked) {
                // Отключаем все связанные уведомления
                showEmailNotificationsSwitch.setChecked(false);
                backgroundNotificationsSwitch.setChecked(false);
                inAppNotificationsSwitch.setChecked(false);
                vibrationSwitch.setChecked(false);

                // Сохраняем изменения
                NotificationSettingsManager.setBackgroundNotificationsEnabled(this, false);
                NotificationSettingsManager.setInAppNotificationsEnabled(this, false);
                NotificationSettingsManager.setVibrationEnabled(this, false);

                // Отправляем команду для email уведомлений
                Command command = new Command("EmailNotifications");
                command.addData("enabled", false);
                connectionManager.SendCommand(command);
            } else {
                NotificationSettingsManager.requestNotificationPermissionIfNeeded(this);
            }
            isProgrammaticChange = false;
        });

        // Фоновая работа
        backgroundWorkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;

            NotificationSettingsManager.setBackgroundWorkAllowed(this, isChecked);

            isProgrammaticChange = true;
            if (!isChecked) {
                backgroundNotificationsSwitch.setChecked(false);
                NotificationSettingsManager.setBackgroundNotificationsEnabled(this, false);
            }
            isProgrammaticChange = false;
        });

        // Фоновые уведомления
        backgroundNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;

            // Проверяем, включены ли основные уведомления
            if (isChecked && !showNotificationsSwitch.isChecked()) {
                isProgrammaticChange = true;
                backgroundNotificationsSwitch.setChecked(false);
                isProgrammaticChange = false;
                Toast.makeText(this, R.string.enable_notifications_first, Toast.LENGTH_SHORT).show();
            } else {
                NotificationSettingsManager.setBackgroundNotificationsEnabled(this, isChecked);
            }
        });

        // Внутриприложенные уведомления
        inAppNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;

            // Проверяем, включены ли основные уведомления
            if (isChecked && !showNotificationsSwitch.isChecked()) {
                isProgrammaticChange = true;
                inAppNotificationsSwitch.setChecked(false);
                isProgrammaticChange = false;
                Toast.makeText(this, R.string.enable_notifications_first, Toast.LENGTH_SHORT).show();
            } else {
                NotificationSettingsManager.setInAppNotificationsEnabled(this, isChecked);
            }
        });

        // Вибрация
        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;

            // Проверяем, включены ли основные уведомления
            if (isChecked && !showNotificationsSwitch.isChecked()) {
                isProgrammaticChange = true;
                vibrationSwitch.setChecked(false);
                isProgrammaticChange = false;
                Toast.makeText(this, R.string.enable_notifications_first, Toast.LENGTH_SHORT).show();
            } else {
                NotificationSettingsManager.setVibrationEnabled(this, isChecked);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        NotificationSettingsManager.handlePermissionResult(
                this,
                requestCode,
                grantResults,
                new NotificationCallback() {
                    @Override
                    public void onPermissionResult(boolean granted) {
                        if (!granted) {
                            isProgrammaticChange = true;
                            showNotificationsSwitch.setChecked(false);
                            isProgrammaticChange = false;
                            Toast.makeText(SettingsActivity.this,
                                    R.string.notifications_permission_denied,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setupLanguageSection() {
        TextView currentLanguageText = findViewById(R.id.current_language_text);
        currentLanguageText.setText(getCurrentLanguageName());

        findViewById(R.id.language_item).setOnClickListener(v ->
                showLanguageSelectionDialog());
    }

    private void showLanguageSelectionDialog() {
        String currentLanguage = LocaleManager.getLocale(this).getLanguage();
        String[] languageCodes = getResources().getStringArray(R.array.language_codes);
        String[] languageNames = getResources().getStringArray(R.array.languages);

        int checkedItem = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.select_language)
                .setSingleChoiceItems(languageNames, checkedItem, null)
                .setPositiveButton(R.string.send_button, (dialog, which) -> {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (selectedPosition >= 0) {
                        changeLanguage(languageCodes[selectedPosition]);
                    }
                })
                .setNegativeButton(R.string.close_button_description, null)
                .show();
    }

    private void changeLanguage(String language) {
        LocaleManager.setLocale(this, language);
        restartApp();
    }

    private void setupReferenceSection() {
        // FAQ
        findViewById(R.id.faq_item).setOnClickListener(v ->
                openWebPage(WEBSITE_URL + "faq.html"));

        // Политика конфиденциальности
        findViewById(R.id.policy_item).setOnClickListener(v ->
                openWebPage(WEBSITE_URL + "privacy.html"));

        // Поддержка
        findViewById(R.id.support_item).setOnClickListener(v ->
                sendEmail(SUPPORT_EMAIL, "Support request"));
    }

    private void openWebPage(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.browser_not_found), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmail(String email, String subject) {
        try {
            startActivity(new Intent(Intent.ACTION_SENDTO)
                    .setData(Uri.parse("mailto:" + email))
                    .putExtra(Intent.EXTRA_SUBJECT, subject));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.email_app_not_found), Toast.LENGTH_SHORT).show();
        }
    }

    private String getCurrentLanguageName() {
        String currentLanguage = LocaleManager.getLocale(this).getLanguage();
        String[] languageCodes = getResources().getStringArray(R.array.language_codes);
        String[] languageNames = getResources().getStringArray(R.array.languages);

        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                return languageNames[i];
            }
        }
        return languageNames[0];
    }

    @Override
    protected void onDestroy() {
        if (connectionManager != null) {
            connectionManager.removeConnectionEvent(events);
        }
        super.onDestroy();
    }
}