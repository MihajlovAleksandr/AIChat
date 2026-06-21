package com.example.aichat.view.payment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.aichat.controller.payment.PaymentsController;
import com.example.aichat.dto.response.PaymentInfoResponse;
import com.example.aichat.model.payment.PaymentStatuses;
import com.example.aichat.R;
import com.example.aichat.view.main.BaseActivity;
import com.example.aichat.view.theme.binders.PaymentScreensThemeBinder;
import java.util.List;

public class PaymentsActivity extends BaseActivity {

    private PaymentsController controller;

    private LinearLayout paymentsContainer;

    private LinearLayout emptyContainer;

    private LinearLayout loadingContainer;

    private TextView loadingText;

    private ImageButton btnBack;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_payments
        );

        setupInsets();

        initViews();

        PaymentScreensThemeBinder.applyPaymentsHistory(
                this
        );

        setupListeners();

        setupBackPressedHandler();

        controller =
                new PaymentsController(this);

        showLoading();

        controller.getPayments();
    }

    @Override
    protected void onResume() {

        super.onResume();

        PaymentScreensThemeBinder.applyPaymentsHistory(
                this
        );
    }

    private void setupInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (view, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat
                                            .Type
                                            .systemBars()
                            );

                    view.setPadding(
                            0,
                            systemBars.top,
                            0,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }

    private void initViews() {

        paymentsContainer =
                findViewById(
                        R.id.paymentsContainer
                );

        emptyContainer =
                findViewById(
                        R.id.emptyContainer
                );

        loadingContainer =
                findViewById(
                        R.id.loadingContainer
                );

        loadingText =
                findViewById(
                        R.id.loadingText
                );

        btnBack =
                findViewById(
                        R.id.btn_back
                );
    }

    private void setupListeners() {

        btnBack.setOnClickListener(
                v -> finish()
        );
    }

    private void setupBackPressedHandler() {

        OnBackPressedCallback callback =
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {

                        finish();
                    }
                };

        getOnBackPressedDispatcher()
                .addCallback(this, callback);
    }

    public void renderPayments(
            List<PaymentInfoResponse> payments
    ) {

        hideLoading();

        paymentsContainer.removeAllViews();

        if (payments == null
                || payments.isEmpty()) {

            showEmptyState();

            return;
        }

        showPayments(payments);
    }

    private void showLoading() {

        loadingContainer.setVisibility(
                View.VISIBLE
        );

        paymentsContainer.setVisibility(
                View.GONE
        );

        emptyContainer.setVisibility(
                View.GONE
        );
    }

    private void hideLoading() {

        loadingContainer.setVisibility(
                View.GONE
        );
    }

    private void showEmptyState() {

        emptyContainer.setVisibility(
                View.VISIBLE
        );

        paymentsContainer.setVisibility(
                View.GONE
        );

        Button productsButton =
                findViewById(
                        R.id.productsButton
                );

        PaymentScreensThemeBinder.applyPaymentsHistory(
                this
        );

        productsButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            PaymentsActivity.this,
                            ProductsActivity.class
                    );

            startActivity(intent);

            finish();
        });
    }

    private void showPayments(
            List<PaymentInfoResponse> payments
    ) {

        emptyContainer.setVisibility(
                View.GONE
        );

        paymentsContainer.setVisibility(
                View.VISIBLE
        );

        for (PaymentInfoResponse payment
                : payments) {

            View paymentView =
                    getLayoutInflater()
                            .inflate(
                                    R.layout.item_payment_info,
                                    paymentsContainer,
                                    false
                            );

            TextView amountText =
                    paymentView.findViewById(
                            R.id.amountText
                    );

            TextView statusText =
                    paymentView.findViewById(
                            R.id.statusText
                    );

            TextView createdAtText =
                    paymentView.findViewById(
                            R.id.createdAtText
                    );

            Button detailsButton =
                    paymentView.findViewById(
                            R.id.detailsButton
                    );

            View statusContainer =
                    paymentView.findViewById(
                            R.id.statusContainer
                    );

            amountText.setText(
                    getString(
                            R.string.payment_amount_value,
                            payment.amount,
                            payment.currency
                    )
            );

            createdAtText.setText(
                    getString(
                            R.string.payment_created_value,
                            String.valueOf(
                                    payment.createdAt
                            )
                    )
            );

            if (payment.status == null) {

                statusText.setText(
                        R.string.payment_status_pending
                );

                statusContainer.setBackgroundResource(
                        R.drawable.bg_payment_status_pending
                );

            } else {
                if (payment.status == PaymentStatuses.Confirmed) {

                    statusText.setText(
                            R.string.payment_status_confirmed
                    );

                    statusContainer.setBackgroundResource(
                            R.drawable.bg_payment_status_confirmed
                    );

                } else if (payment.status == PaymentStatuses.Pending) {

                    statusText.setText(
                            R.string.payment_status_pending
                    );

                    statusContainer.setBackgroundResource(
                            R.drawable.bg_payment_status_pending
                    );

                } else if (payment.status == PaymentStatuses.Failed) {

                    statusText.setText(
                            R.string.payment_status_failed
                    );

                    statusContainer.setBackgroundResource(
                            R.drawable.bg_payment_status_failed
                    );
                } else {

                    statusText.setText(
                            R.string.payment_status_expired
                    );

                    statusContainer.setBackgroundResource(
                            R.drawable.bg_payment_status_failed
                    );
                }
            }

            statusText.setTextColor(
                    Color.WHITE
            );

            PaymentScreensThemeBinder.applyPaymentItem(
                    paymentView
            );

            detailsButton.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                PaymentsActivity.this,
                                PaymentDetailsActivity.class
                        );

                intent.putExtra(
                        "payment_id",
                        payment.id.toString()
                );

                startActivity(intent);
            });

            paymentsContainer.addView(
                    paymentView
            );
        }

        PaymentScreensThemeBinder.applyPaymentsHistory(
                this
        );
    }
}
