package com.example.aichat.dto.response;

import com.example.aichat.model.payment.PaymentType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;

public final class ProductResponse {
    public final UUID id;
    public final String name;
    public final PaymentType paymentType;
    public final BigDecimal price;
    public final String currency;
    public final String description;

    @JsonCreator
    public ProductResponse(
            @JsonProperty("id") UUID id,
            @JsonProperty("name") String name,
            @JsonProperty("paymentType") PaymentType paymentType,
            @JsonProperty("price") BigDecimal price,
            @JsonProperty("currency") String currency,
            @JsonProperty("description") String description) {
        this.id = id;
        this.name = name;
        this.paymentType = paymentType;
        this.price = price;
        this.currency = currency;
        this.description = description;
    }
}
