package com.example.aichat.controller.payment;

import com.example.aichat.dto.response.PaymentResponse;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.view.payment.PaymentDetailsActivity;

public final class PaymentDetailsController {

    private final ConnectionDispatcher dispatcher;
    private final PaymentDetailsActivity activity;

    public PaymentDetailsController(PaymentDetailsActivity activity) {
        this.activity = activity;
        this.dispatcher = ConnectionSingleton
                .getInstance()
                .getConnectionDispatcher();
    }

    public void getPaymentById(String paymentId) {

        dispatcher.sendHttpRequestAsync(
                "/api/payments/" + paymentId,
                HttpClient.HTTPMethod.GET,
                null,
                false
        ).thenAccept(cmd -> {

            activity.runOnUiThread(() -> {

                if (!cmd.isSuccess()) {
                    activity.showError("Не удалось загрузить платеж");
                    return;
                }

                PaymentResponse payment = cmd.getData(PaymentResponse.class);

                if (payment == null) {
                    activity.showError("Платеж не найден");
                    return;
                }

                activity.renderPayment(payment);
            });
        });
    }

    public void getPaymentByPremiumId(String premiumId) {

        dispatcher.sendHttpRequestAsync(
                "/api/payments/premium/" + premiumId,
                HttpClient.HTTPMethod.GET,
                null,
                false
        ).thenAccept(cmd -> {

            activity.runOnUiThread(() -> {

                if (!cmd.isSuccess()) {
                    activity.showError("Не удалось загрузить платеж");
                    return;
                }

                PaymentResponse payment = cmd.getData(PaymentResponse.class);

                if (payment == null) {
                    activity.showError("Платеж не найден");
                    return;
                }

                activity.renderPayment(payment);
            });
        });
    }
}
