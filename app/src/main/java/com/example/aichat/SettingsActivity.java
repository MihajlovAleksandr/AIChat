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
import androidx.appcompat.app.AppCompatActivity;

import com.example.aichat.model.LocaleManager;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.entities.Preference;
import com.example.aichat.model.entities.UserData;
import com.example.aichat.view.BaseActivity;
import com.example.aichat.view.PreferenceActivity;
import com.example.aichat.view.UserDataActivity;
import com.example.aichat.view.main.MainActivity;

public class SettingsActivity extends BaseActivity {
    private static final String WEBSITE_URL = "https://mihajlovaleksandr.github.io/AIChatSite/";
    private static final String SUPPORT_EMAIL = "aichatcorp@gmail.com";

    private ConnectionManager connectionManager;
    private TextView emailText;
    private TextView devicesText;
    private TextView userDataText;
    private TextView preferenceText;
    private OnConnectionEvents events;
    private UserData userData;
    private Preference preference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings); // Assuming you'll use the same layout

        connectionManager = ConnectionSingleton.getInstance().getConnectionManager(); // Or get it from intent

        initializeViews();
        setupConnectionEvents();
        setupClickListeners();

        connectionManager.SendCommand(new Command("GetSettingsInfo"));
    }

    private void initializeViews() {
        // Back button
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // Profile section
        emailText = findViewById(R.id.email_text);
        devicesText = findViewById(R.id.devices_text);
        userDataText = findViewById(R.id.userData_text);
        preferenceText = findViewById(R.id.preference_text);

        // Version
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
            public void OnConnectionFailed() {
                // Handle connection failure
            }

            @Override
            public void OnOpen() {
                // Handle connection open
            }
        };
        connectionManager.addConnectionEvent(events);
    }

    private void setupClickListeners() {
        // Profile section
        findViewById(R.id.change_password_item).setOnClickListener(v -> {
            if (userData != null) {
                Intent intent = new Intent(this, ChangePasswordActivity.class);
                intent.putExtra("userData", userData);
                startActivity(intent);
            }
        });

        findViewById(R.id.devices_item).setOnClickListener(v -> {
            startActivity(new Intent(this, DevicesActivity.class));
        });

        findViewById(R.id.userData_item).setOnClickListener(v -> {
            if (userData != null) {
                Intent intent = new Intent(this, UserDataActivity.class);
                intent.putExtra("userData", userData);
                startActivity(intent);
            }
        });

        findViewById(R.id.preference_item).setOnClickListener(v -> {
            if (preference != null) {
                Intent intent = new Intent(this, PreferenceActivity.class);
                intent.putExtra("preference", preference);
                startActivity(intent);
            }
        });

        findViewById(R.id.logout_item).setOnClickListener(v ->
                connectionManager.SendCommand(new Command("DeleteConnection")));

        // Notifications section
        setupNotificationSwitches();

        // Language section
        setupLanguageSection();

        // Reference section
        setupReferenceSection();
    }

    private void setupNotificationSwitches() {
        Switch emailNotificationsSwitch = findViewById(R.id.email_notifications_switch);
        Switch showNotificationsSwitch = findViewById(R.id.show_notifications_switch);
        Switch backgroundNotificationsSwitch = findViewById(R.id.background_notifications_switch);
        Switch inAppNotificationsSwitch = findViewById(R.id.in_app_notifications_switch);
        Switch vibrationSwitch = findViewById(R.id.vibration_switch);

        // Set actual values from settings
        emailNotificationsSwitch.setChecked(true);
        showNotificationsSwitch.setChecked(true);
        backgroundNotificationsSwitch.setChecked(true);
        inAppNotificationsSwitch.setChecked(true);
        vibrationSwitch.setChecked(true);
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_language)
                .setSingleChoiceItems(languageNames, checkedItem, null)
                .setPositiveButton(R.string.send_button, (dialog, which) -> {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (selectedPosition >= 0) {
                        String selectedLanguage = languageCodes[selectedPosition];
                        changeLanguage(selectedLanguage);
                    }
                })
                .setNegativeButton(R.string.close_button_description, null);

        builder.create().show();
    }

    private void changeLanguage(String language) {
        LocaleManager.setLocale(this, language);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
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
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        try {
            startActivity(Intent.createChooser(intent, "Open link with"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No browser app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmail(String emailAddress, String subject) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + emailAddress));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);

        try {
            startActivity(Intent.createChooser(intent, "Choose email app"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
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
        connectionManager.removeConnectionEvent(events);
        super.onDestroy();
    }
}