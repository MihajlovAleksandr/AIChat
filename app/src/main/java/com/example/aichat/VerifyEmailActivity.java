package com.example.aichat;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.example.aichat.databinding.ActivityVerifyEmailBinding;

public class VerifyEmailActivity extends AppCompatActivity {

    private EditText[] codeFields;
    private ConnectionManager connectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectionManager = Singleton.getInstance().getConnectionManager();
        if (connectionManager == null) {
            connectionManager = new ConnectionManager(TokenManager.getToken(this));
        }

        connectionManager.SetCommandGot(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                if (command.getOperation().equals("VerificationCodeAnswer")) {
                    if (command.getData("answer", int.class) == 1) {
                        Singleton.getInstance().setConnectionManager(connectionManager);
                        Intent intent = new Intent(VerifyEmailActivity.this, UserDataActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        new Thread(() -> changeColorForEditText(50, Color.RED)).start();
                    }
                }
            }

            @Override
            public void OnConnectionFailed() {
                Log.e("VerifyEmailActivity", "Connection failed");
            }

            @Override
            public void OnOpen() {
                Log.d("VerifyEmailActivity", "Connection opened");
            }
        });
        ActivityVerifyEmailBinding binding = ActivityVerifyEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        codeFields = new EditText[]{
                findViewById(R.id.code1),
                findViewById(R.id.code2),
                findViewById(R.id.code3),
                findViewById(R.id.code4),
                findViewById(R.id.code5),
                findViewById(R.id.code6)
        };

        setupCodeFields();
    }

    private void changeColorForEditText(int delay, int color) {
        for (EditText editText : codeFields) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                GradientDrawable drawable = (GradientDrawable) editText.getBackground();
                drawable.setStroke(5, color);
            });
        }
    }

    private void setupCodeFields() {
        for (int i = 0; i < codeFields.length; i++) {
            final int currentIndex = i;
            codeFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    changeColorForEditText(0, Color.BLACK);
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && currentIndex < codeFields.length - 1) {
                        codeFields[currentIndex + 1].requestFocus();
                    } else if (s.length() == 0 && currentIndex > 0) {
                        codeFields[currentIndex - 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && currentIndex == codeFields.length - 1) {
                        String fullCode = getFullCode();
                        Log.d("VerifyEmailActivity", "Full code: " + fullCode);
                        Command command = new Command("VerificationCode");
                        command.addData("code", fullCode);
                        connectionManager.SendCommand(command);
                    }
                }
            });

            codeFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == 67 && codeFields[currentIndex].getText().length() == 0 && currentIndex > 0) {
                    codeFields[currentIndex - 1].requestFocus();
                }
                return false;
            });

            codeFields[i].setOnLongClickListener(v -> {
                handlePaste();
                return true;
            });
        }
    }

    private void handlePaste() {
        String clipboardText = getClipboardText();
        if (clipboardText != null && clipboardText.length() == 6) {
            for (int i = 0; i < 6; i++) {
                codeFields[i].setText(String.valueOf(clipboardText.charAt(i)));
            }
        } else if (clipboardText != null && clipboardText.length() == 7 && clipboardText.indexOf(' ') == 3) {
            int curIndex = 0;
            for (int i = 0; i < 7; i++) {
                if (i == 3) continue;
                codeFields[curIndex].setText(String.valueOf(clipboardText.charAt(i)));
                curIndex++;
            }
        }
    }

    private String getFullCode() {
        StringBuilder code = new StringBuilder();
        for (EditText field : codeFields) {
            code.append(field.getText().toString());
        }
        return code.toString();
    }

    private String getClipboardText() {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            android.content.ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            return item.getText().toString();
        }
        return null;
    }
}