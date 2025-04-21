package com.example.aichat;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.aichat.model.LocaleManager;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Command;
import com.example.aichat.view.LoginActivity;
import com.example.aichat.view.main.MainActivity;

public class SettingsFragment extends Fragment {

    private boolean userInteracted = false;
    private static final String WEBSITE_URL = "https://mihajlovaleksandr.github.io/AIChatSite/";
    private static final String SUPPORT_EMAIL = "aichatcorp@gmail.com";
    ConnectionManager connectionManager;
     public SettingsFragment(ConnectionManager connectionManager){
         this.connectionManager = connectionManager;
         this.connectionManager.addConnectionEvent(new OnConnectionEvents() {
             @Override
             public void OnCommandGot(Command command) {
                 switch (command.getOperation()) {
                     case "Logout":
                         logout();
                         break;
                 }
             }

             @Override
             public void OnConnectionFailed() {

             }

             @Override
             public void OnOpen() {

             }
         });
     }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Back button
        ImageButton backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        // Profile section
        TextView emailText = view.findViewById(R.id.email_text);
        emailText.setText("user@example.com");

        view.findViewById(R.id.change_password_item).setOnClickListener(v -> {
            // Password change logic
        });

        view.findViewById(R.id.devices_item).setOnClickListener(v -> {
            // Devices logic
        });
        view.findViewById(R.id.logout_item).setOnClickListener(v -> connectionManager.SendCommand(new Command("Logout")));

        // Notifications section
        setupNotificationSwitches(view);

        // Language section
        setupLanguageSection(view);

        // Reference section
        setupReferenceSection(view);

        // Version
        TextView versionText = view.findViewById(R.id.version_text);
        versionText.setText(getString(R.string.version_format, "1.0.0"));

        return view;
    }

    private void setupNotificationSwitches(View view) {
        Switch emailNotificationsSwitch = view.findViewById(R.id.email_notifications_switch);
        Switch showNotificationsSwitch = view.findViewById(R.id.show_notifications_switch);
        Switch backgroundNotificationsSwitch = view.findViewById(R.id.background_notifications_switch);
        Switch inAppNotificationsSwitch = view.findViewById(R.id.in_app_notifications_switch);
        Switch vibrationSwitch = view.findViewById(R.id.vibration_switch);

        // Set actual values from settings
        emailNotificationsSwitch.setChecked(true);
        showNotificationsSwitch.setChecked(true);
        backgroundNotificationsSwitch.setChecked(true);
        inAppNotificationsSwitch.setChecked(true);
        vibrationSwitch.setChecked(true);
    }

    private void setupLanguageSection(View view) {
        TextView currentLanguageText = view.findViewById(R.id.current_language_text);
        currentLanguageText.setText(getCurrentLanguageName());

        view.findViewById(R.id.language_item).setOnClickListener(v -> {
            showLanguageSelectionDialog();
        });
    }

    private void showLanguageSelectionDialog() {
        String currentLanguage = LocaleManager.getLocale(requireContext()).getLanguage();
        String[] languageCodes = getResources().getStringArray(R.array.language_codes);
        String[] languageNames = getResources().getStringArray(R.array.languages);

        int checkedItem = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                checkedItem = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.select_language)
                .setSingleChoiceItems(languageNames, checkedItem, null)
                .setPositiveButton(R.string.send_button, (dialog, which) -> {
                    int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                    if (selectedPosition >= 0) {
                        String selectedLanguage = languageCodes[selectedPosition];
                        changeLanguage(selectedLanguage);
                    }
                })
                .setNegativeButton(R.string.close_button_description, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void changeLanguage(String language) {
        LocaleManager.setLocale(requireContext(), language);
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setupReferenceSection(View view) {
        view.findViewById(R.id.faq_item).setOnClickListener(v -> {
            openWebPage(WEBSITE_URL + "faq.html");
        });

        view.findViewById(R.id.policy_item).setOnClickListener(v -> {
            openWebPage(WEBSITE_URL + "privacy.html");
        });

        view.findViewById(R.id.support_item).setOnClickListener(v -> {
            sendEmail(SUPPORT_EMAIL, "Support request");
        });
    }

    private void openWebPage(String url) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        try {
            startActivity(Intent.createChooser(intent, "Открыть ссылку через"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "Нет приложения для открытия веб-страниц", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmail(String emailAddress, String subject) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + emailAddress));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);

        try {
            startActivity(Intent.createChooser(intent, "Выберите почтовое приложение"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "Нет приложения для отправки email", Toast.LENGTH_SHORT).show();
        }
    }

    private void logout(){
        Activity activity  = getActivity();
        Intent intent = new Intent(activity, LoginActivity.class);
        new Thread(()-> {
            DatabaseManager.getDatabase().chatDao().clearTable();
            DatabaseManager.getDatabase().messageDao().clearTable();
        }).start();
        activity.startActivity(intent);
        activity.finish();
    }

    private String getCurrentLanguageName() {
        String currentLanguage = LocaleManager.getLocale(requireContext()).getLanguage();
        String[] languageCodes = getResources().getStringArray(R.array.language_codes);
        String[] languageNames = getResources().getStringArray(R.array.languages);

        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                return languageNames[i];
            }
        }
        return languageNames[0];
    }
}