package com.example.aichat.view.payment;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.aichat.R;
import com.example.aichat.view.theme.binders.PaymentScreensThemeBinder;

public class PaymentErrorDialog
        extends DialogFragment {

    private final String message;

    public PaymentErrorDialog(
            String message
    ) {
        this.message = message;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(
            @Nullable Bundle savedInstanceState
    ) {

        Dialog dialog =
                new Dialog(
                        requireContext()
                );

        dialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );

        View view =
                LayoutInflater
                        .from(
                                requireContext()
                        )
                        .inflate(
                                R.layout.dialog_payment_error,
                                null,
                                false
                        );

        dialog.setContentView(
                view
        );

        TextView messageText =
                view.findViewById(
                        R.id.messageText
                );

        Button okButton =
                view.findViewById(
                        R.id.okButton
                );

        LinearLayout supportButton =
                view.findViewById(
                        R.id.supportButton
                );

        if (messageText != null) {
            messageText.setText(
                    message
            );
        }

        if (okButton != null) {
            okButton.setOnClickListener(v ->
                    dismiss()
            );
        }

        if (supportButton != null) {
            supportButton.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                        "https://t.me/your_support"
                                )
                        );

                startActivity(
                        intent
                );
            });
        }

        PaymentScreensThemeBinder.applyPaymentResultDialog(
                view
        );

        Window window =
                dialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawableResource(
                    android.R.color.transparent
            );

            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }

        return dialog;
    }
}
