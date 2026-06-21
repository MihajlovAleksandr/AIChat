package com.example.aichat.controller.payment;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import com.example.aichat.dto.response.OneTimePaymentData;
import com.example.aichat.dto.response.PaymentCredentialsResponse;
import com.example.aichat.dto.response.SubscriptionPaymentData;
import com.example.aichat.view.payment.PaymentErrorDialog;
import com.example.aichat.view.payment.PaymentSuccessDialog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;

public class PaymentResultHandler {

    private static final String MERCHANT_NAME =
            "AI Chat";

    private final Activity activity;

    private final PaymentSheet paymentSheet;

    private final ObjectMapper mapper;

    public PaymentResultHandler(
            @NonNull Activity activity,
            @NonNull PaymentSheet paymentSheet
    ) {

        this.activity = activity;

        this.paymentSheet = paymentSheet;

        this.mapper = new ObjectMapper();
    }

    public void handle(
            PaymentCredentialsResponse response
    ) {

        if (response == null
                || response.getType() == null) {

            showError(
                    "Некорректный ответ сервера"
            );

            return;
        }

        switch (response.getType()) {

            case Subscription:

                handleSubscription(response);

                break;

            case SingleItem:
            case Payment:

                handleOneTimePayment(response);

                break;

            default:

                showError(
                        "Неизвестный тип оплаты"
                );

                break;
        }
    }

    public void showSuccess(
            @NonNull String name
    ) {

        if (!(activity instanceof FragmentActivity)) {
            return;
        }

        activity.runOnUiThread(() -> {

            PaymentSuccessDialog dialog =
                    new PaymentSuccessDialog(
                            name
                    );

            dialog.show(
                    ((FragmentActivity) activity)
                            .getSupportFragmentManager(),
                    "payment_success"
            );
        });
    }

    public void showError(
            @NonNull String reason
    ) {

        if (!(activity instanceof FragmentActivity)) {
            return;
        }

        activity.runOnUiThread(() -> {

            PaymentErrorDialog dialog =
                    new PaymentErrorDialog(
                            reason
                    );

            dialog.show(
                    ((FragmentActivity) activity)
                            .getSupportFragmentManager(),
                    "payment_error"
            );
        });
    }

    private void handleSubscription(
            PaymentCredentialsResponse response
    ) {

        SubscriptionPaymentData data =
                mapper.convertValue(
                        response.getData(),
                        SubscriptionPaymentData.class
                );

        if (data == null) {

            showError(
                    "Не удалось получить данные подписки"
            );

            return;
        }

        if (data.getPublishableKey() == null
                || data.getClientSecret() == null
                || data.getCustomerId() == null
                || data.getEphemeralKey() == null) {

            showError(
                    "Отсутствуют данные для оплаты"
            );

            return;
        }

        PaymentConfiguration.init(
                activity,
                data.getPublishableKey()
        );

        PaymentSheet.CustomerConfiguration
                customerConfiguration =
                new PaymentSheet.CustomerConfiguration(
                        data.getCustomerId(),
                        data.getEphemeralKey()
                );

        PaymentSheet.Configuration configuration =
                new PaymentSheet.Configuration.Builder(
                        MERCHANT_NAME
                )
                        .customer(
                                customerConfiguration
                        )
                        .allowsDelayedPaymentMethods(
                                false
                        )
                        .build();

        paymentSheet.presentWithPaymentIntent(
                data.getClientSecret(),
                configuration
        );
    }

    private void handleOneTimePayment(
            PaymentCredentialsResponse response
    ) {

        OneTimePaymentData data =
                mapper.convertValue(
                        response.getData(),
                        OneTimePaymentData.class
                );

        if (data == null
                || data.getClientSecret() == null) {

            showError(
                    "Не удалось создать платеж"
            );

            return;
        }

        paymentSheet.presentWithPaymentIntent(
                data.getClientSecret(),
                new PaymentSheet.Configuration(
                        MERCHANT_NAME
                )
        );
    }
}
