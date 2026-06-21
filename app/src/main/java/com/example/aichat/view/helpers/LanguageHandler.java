package com.example.aichat.view.helpers;

import android.app.Activity;
import android.content.Context;
import com.example.aichat.model.LocaleManager;

public class LanguageHandler {

    private final Context context;

    public LanguageHandler(Context context) {
        this.context = context;
    }

    public void setLocale(String langCode) {

        LocaleManager.setLocale(context, langCode);

        if (context instanceof Activity) {

            Activity activity =
                    (Activity) context;

            activity.finish();

            activity.overridePendingTransition(0, 0);

            activity.startActivity(
                    activity.getIntent()
            );

            activity.overridePendingTransition(0, 0);
        }
    }
}
