package com.example.aichat.model.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentType {

    Subscription,
    Payment,
    SingleItem
}
