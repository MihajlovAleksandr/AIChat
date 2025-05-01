package com.example.aichat;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

        connectionManager.SendCommand(new Command("GetSettingsInfo"));
        NotificationSettingsManager.requestNotificationPermissionIfNeeded(this);
    }

    private void initializeViews() {
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        emailText = findViewById(R.id.email_text);
        devicesText = findViewById(R.id.devices_text);
        userDataText = findViewById(R.id.userData_text);
        preferenceText = findViewById(R.id.preference_text);

        TextView versionText = findViewById(R.id.version_text);
        versionText.setText(getString(R.string.version_format, "1.0.0"));
    }

    private void setupConnectionEvents() {
        events = new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                runOnUiThread(() -> {
                    switch (command.getOperation()) {
                        case "GetSettingsInfo":
                            emailText.setText(command.getData("email", String.class));
                            preference = command.getData("preference", Preference.class);
                            userData = command.getData("userData", UserData.class);
                            preferenceText.setText(preference.toString());
                            userDataText.setText(userData.toString());
                            int[] devicesCount = command.getData("devices", int[].class);
                            devicesText.setText(getString(R.string.device_status, devicesCount[0], devicesCount[1]));
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
        findViewById(R.id.change_password_item).setOnClickListener(v -> {
            if (userData != null) {
                startActivity(new Intent(this, ChangePasswordActivity.class)
                        .putExtra("userData", userData));
            }
        });

        findViewById(R.id.devices_item).setOnClickListener(v ->
                startActivity(new Intent(this, DevicesActivity.class)));

        findViewById(R.id.userData_item).setOnClickListener(v -> {
            if (userData != null) {
                startActivity(new Intent(this, UserDataActivity.class)
                        .putExtra("userData", userData));
            }
        });

        findViewById(R.id.preference_item).setOnClickListener(v -> {
            if (preference != null) {
                startActivity(new Intent(this, PreferenceActivity.class)
                        .putExtra("preference", preference));
            }
        });

        findViewById(R.id.logout_item).setOnClickListener(v ->
                connectionManager.SendCommand(new Command("DeleteConnection")));
    }

    private void setupNotificationSection() {
        Switch showNotificationsSwitch = findViewById(R.id.show_notifications_switch);
        showNotificationsSwitch.setChecked(NotificationSettingsManager.areNotificationsEnabled(this));

        showNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            NotificationSettingsManager.setNotificationsEnabled(this, isChecked);
            if (isChecked) {
                NotificationSettingsManager.requestNotificationPermissionIfNeeded(this);
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
                        Switch notificationsSwitch = findViewById(R.id.show_notifications_switch);
                        if (!granted) {
                            notificationsSwitch.setChecked(false);
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
        findViewById(R.id.faq_item).setOnClickListener(v ->
                openWebPage(WEBSITE_URL + "faq.html"));
        findViewById(R.id.policy_item).setOnClickListener(v ->
                openWebPage(WEBSITE_URL + "privacy.html"));
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