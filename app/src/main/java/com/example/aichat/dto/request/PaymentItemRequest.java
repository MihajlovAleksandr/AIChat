package com.example.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class PaymentItemRequest {

    public final UUID productId;
    public final int quantity;

    @JsonCreator
    public PaymentItemRequest(
            @JsonProperty("productId") UUID productId,
            @JsonProperty("quantity") int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
}
