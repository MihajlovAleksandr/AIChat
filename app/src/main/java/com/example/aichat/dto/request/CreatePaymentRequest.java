package com.example.aichat.dto.request;

import com.example.aichat.model.payment.PaymentType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CreatePaymentRequest {

    public final List<PaymentItemRequest> items;
    public final PaymentType type;

    @JsonCreator
    public CreatePaymentRequest(
            @JsonProperty("items") List<PaymentItemRequest> items,
            @JsonProperty("type") PaymentType type) {
        this.items = items;
        this.type = type;
    }
}
