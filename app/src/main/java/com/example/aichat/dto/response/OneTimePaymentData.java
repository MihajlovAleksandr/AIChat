package com.example.aichat.dto.response;

public class OneTimePaymentData {

    private String clientSecret;

    private String paymentId;

    public OneTimePaymentData() {
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
}
