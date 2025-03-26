package com.example.aichat.view;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aichat.R;
import com.example.aichat.controller.VerifyEmailController;
import com.example.aichat.databinding.ActivityVerifyEmailBinding;

public class VerifyEmailActivity extends AppCompatActivity {

    private EditText[] codeFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        VerifyEmailController controller = new VerifyEmailController(this, codeFields);
        setupCodeFields(controller);
    }

    private void setupCodeFields(VerifyEmailController controller) {
        for (int i = 0; i < codeFields.length; i++) {
            final int currentIndex = i;
            codeFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    controller.changeColorForEditText(0, Color.BLACK);
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    controller.handleTextChanged(s, currentIndex);
                }

                @Override
                public void afterTextChanged(Editable s) {
                    controller.handleAfterTextChanged(s, currentIndex);
                }
            });

            codeFields[i].setOnKeyListener((v, keyCode, event) -> controller.handleKeyEvent(keyCode, currentIndex));
            codeFields[i].setOnLongClickListener(v -> {
                controller.handlePaste();
                return true;
            });
        }
    }
}
