package com.workshop.logproxy.service;

/** Intentional failure of the payment gateway, used to populate the error metrics. */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }
}
