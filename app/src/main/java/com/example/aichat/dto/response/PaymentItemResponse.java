package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class PaymentItemResponse {
    public final ProductResponse product;
    public final int quantity;

    @JsonCreator
    public PaymentItemResponse(
            @JsonProperty("product") ProductResponse product,
            @JsonProperty("quantity") int quantity) {
        this.product = product;
        this.quantity = quantity;
    }
}
