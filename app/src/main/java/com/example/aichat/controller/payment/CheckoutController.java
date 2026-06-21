package com.example.aichat.controller.payment;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import com.example.aichat.dto.request.CreatePaymentRequest;
import com.example.aichat.dto.request.PaymentItemRequest;
import com.example.aichat.dto.response.ApiError;
import com.example.aichat.dto.response.PaymentCredentialsResponse;
import com.example.aichat.dto.response.ProductResponse;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.payment.PaymentType;
import com.example.aichat.R;
import com.example.aichat.view.payment.CheckoutActivity;
import com.stripe.android.paymentsheet.PaymentSheet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CheckoutController {

    private final CheckoutActivity activity;

    private final ConnectionDispatcher dispatcher;

    private final PaymentResultHandler paymentResultHandler;

    public CheckoutController(
            @NonNull CheckoutActivity activity,
            @NonNull PaymentSheet paymentSheet
    ) {

        this.activity = activity;

        this.dispatcher =
                ConnectionSingleton.getInstance()
                        .getConnectionDispatcher();

        this.paymentResultHandler =
                new PaymentResultHandler(
                        activity,
                        paymentSheet
                );
    }

    public void loadProduct(
            UUID productId
    ) {

        dispatcher.sendHttpRequestAsync(
                "/api/payments/products/" + productId,
                HttpClient.HTTPMethod.GET,
                null,
                false
        ).thenAccept(cmd -> {

            if (!cmd.isSuccess()) {
                return;
            }

            ProductResponse product =
                    cmd.getData(
                            ProductResponse.class
                    );

            activity.showProduct(
                    product
            );
        });
    }

    public void createPayment(
            Pair<UUID, Integer>[] items,
            PaymentType paymentType
    ) {

        List<PaymentItemRequest> itemRequests =
                new ArrayList<>();

        for (Pair<UUID, Integer> item : items) {

            itemRequests.add(
                    new PaymentItemRequest(
                            item.first,
                            item.second
                    )
            );
        }

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        itemRequests,
                        paymentType
                );

        dispatcher.sendHttpRequestAsync(
                "/api/payments",
                HttpClient.HTTPMethod.POST,
                request,
                false
        ).thenAccept(cmd -> {

            if (!cmd.isSuccess()) {

                try {

                    ApiError error =
                            cmd.getData(
                                    ApiError.class
                            );

                    if (error != null
                            && error.getCode() != null) {

                        switch (error.getCode()) {

                            case "USER_ALREADY_HAS_AUTO_RENEW_SUBSCRIPTION":
                            case "USER_ALREADY_HAS_ACTIVE_SUBSCRIPTION":

                                activity.runOnUiThread(() ->
                                        paymentResultHandler.showError(
                                                activity.getString(
                                                        R.string.user_already_has_subscription
                                                )
                                        )
                                );

                                return;
                        }
                    }

                } catch (Exception ignored) {
                }

                activity.runOnUiThread(() ->
                        paymentResultHandler.showError(
                                activity.getString(
                                        R.string.connection_error
                                )
                        )
                );

                return;
            }

            PaymentCredentialsResponse response =
                    cmd.getData(
                            PaymentCredentialsResponse.class
                    );

            paymentResultHandler.handle(
                    response
            );

        }).exceptionally(ex -> {

            activity.runOnUiThread(() ->
                    paymentResultHandler.showError(
                            activity.getString(
                                    R.string.connection_error
                            )
                    )
            );

            return null;
        });
    }
}
