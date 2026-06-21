package com.example.aichat.dto.response;

import com.example.aichat.model.payment.PaymentStatuses;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior;
import java.math.BigDecimal;
import java.util.UUID;

public final class PaymentInfoResponse {
    public final UUID id;
    public final BigDecimal amount;
    public final String currency;
    public final PaymentStatuses status;
    public final String createdAt;

    @JsonCreator
    public PaymentInfoResponse(
            @JsonProperty("id") UUID id,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("currency") String currency,
            @JsonProperty("status") PaymentStatuses status,
            @JsonProperty("createdAt") String createdAt) {
        this.id = id;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
    }
}
