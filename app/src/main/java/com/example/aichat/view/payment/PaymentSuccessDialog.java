package com.example.aichat.view.payment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.aichat.R;
import com.example.aichat.view.theme.binders.PaymentScreensThemeBinder;

public class PaymentSuccessDialog
        extends DialogFragment {

    private final String title;

    public PaymentSuccessDialog(
            String title
    ) {
        this.title = title;
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
                                R.layout.dialog_payment_success,
                                null,
                                false
                        );

        dialog.setContentView(
                view
        );

        View okButton =
                view.findViewById(
                        R.id.okButton
                );

        if (okButton != null) {
            okButton.setOnClickListener(v ->
                    dismiss()
            );
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
