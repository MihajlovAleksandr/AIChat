package com.example.aichat.view.helpers;

import android.view.View;
import android.widget.PopupMenu;
import com.example.aichat.R;

public class LanguageMenuHelper {

    private final LanguageHandler languageHandler;

    public LanguageMenuHelper(LanguageHandler handler) {
        this.languageHandler = handler;
    }

    public void attachToButton(View languageButton) {
        languageButton.setOnClickListener(anchor -> {
            PopupMenu popupMenu = new PopupMenu(languageButton.getContext(), anchor);

            String[] languages = languageButton.getContext().getResources().getStringArray(R.array.languages);
            String[] languageCodes = languageButton.getContext().getResources().getStringArray(R.array.language_codes);

            for (int i = 0; i < languages.length; i++) {
                popupMenu.getMenu().add(0, i, i, languages[i]);
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                int index = item.getItemId();
                if (index >= 0 && index < languageCodes.length) {
                    languageHandler.setLocale(languageCodes[index]);
                }
                return true;
            });

            popupMenu.show();
        });
    }
}
