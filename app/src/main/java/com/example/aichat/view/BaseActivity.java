package com.example.aichat.view;

import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aichat.model.LocaleManager;
import com.example.aichat.model.connection.NetworkService;
import com.example.aichat.view.main.MainActivity;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(
                newBase,
                LocaleManager.getLocale(newBase).getLanguage()
        ));
    }

    public void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

    }
}
