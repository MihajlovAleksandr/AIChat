package com.example.aichat.dto.response;

import com.example.aichat.model.payment.PaymentStatuses;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class PaymentResponse {
    public final UUID id;
    public final String stripeInvoiceUrl;
    public final BigDecimal amount;
    public final String currency;
    public final PaymentStatuses status;
    public final String createdAt;
    public final List<PaymentItemResponse> items;

    @JsonCreator
    public PaymentResponse(
            @JsonProperty("id") UUID id,
            @JsonProperty("stripeInvoiceUrl") String stripeInvoiceUrl,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("currency") String currency,
            @JsonProperty("status") PaymentStatuses status,
            @JsonProperty("createdAt") String createdAt,
            @JsonProperty("items") List<PaymentItemResponse> items) {
        this.id = id;
        this.stripeInvoiceUrl = stripeInvoiceUrl;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.items = items;
    }
}
