package com.example.aichat.controller.payment;

import android.content.Intent;
import androidx.annotation.Nullable;
import com.example.aichat.dto.response.PremiumInfoResponse;
import com.example.aichat.dto.response.ProductResponse;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.payment.PaymentType;
import com.example.aichat.view.payment.CheckoutActivity;
import com.example.aichat.view.payment.ProductsActivity;
import java.util.UUID;

public class ProductController {

    private final ProductsActivity activity;

    private final ConnectionDispatcher dispatcher;

    public ProductController(
            ProductsActivity activity
    ) {

        this.activity = activity;

        dispatcher =
                ConnectionSingleton
                        .getInstance()
                        .getConnectionDispatcher();
    }

    public void loadProducts(
            @Nullable String productType
    ) {

        String url =
                productType == null
                        ? "/api/payments/products"
                        : "/api/payments/products?type="
                        + productType;

        dispatcher.sendHttpRequestAsync(
                url,
                HttpClient.HTTPMethod.GET,
                null,
                false
        ).thenAccept(cmd -> {

            if (!cmd.isSuccess()) {
                return;
            }

            ProductResponse[] products =
                    cmd.getData(
                            ProductResponse[].class
                    );

            activity.runOnUiThread(() ->
                    activity.showProducts(
                            products
                    )
            );
        });
    }

    public void loadPremiumInfo() {

        dispatcher.sendHttpRequestAsync(
                "/api/user/premium",
                HttpClient.HTTPMethod.GET,
                null,
                true
        ).thenAccept(cmd -> {

            if (cmd.getCode() == 204) {

                activity.runOnUiThread(() ->
                        activity.showPremiumInfo(
                                null
                        )
                );

                return;
            }

            if (!cmd.isSuccess()) {

                activity.runOnUiThread(() ->
                        activity.showPremiumInfo(
                                null
                        )
                );

                return;
            }

            PremiumInfoResponse response =
                    cmd.getData(
                            PremiumInfoResponse.class
                    );

            activity.runOnUiThread(() ->
                    activity.showPremiumInfo(
                            response
                    )
            );
        }).exceptionally(throwable -> {

            activity.runOnUiThread(() ->
                    activity.showPremiumInfo(
                            null
                    )
            );

            return null;
        });
    }

    public void chooseProduct(
            UUID productId
    ) {

        Intent intent =
                new Intent(
                        activity,
                        CheckoutActivity.class
                );

        intent.putExtra(
                "product_id",
                productId.toString()
        );

        activity.startActivity(
                intent
        );
    }
}
