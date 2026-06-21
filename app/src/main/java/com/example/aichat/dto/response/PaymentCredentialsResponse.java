package com.example.aichat.dto.response;

import com.example.aichat.model.payment.PaymentType;
import com.fasterxml.jackson.databind.JsonNode;

public class PaymentCredentialsResponse {

    private PaymentType type;
    private JsonNode data;

    public PaymentCredentialsResponse() {
    }

    public PaymentType getType() {
        return type;
    }

    public void setType(PaymentType type) {
        this.type = type;
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }
}
