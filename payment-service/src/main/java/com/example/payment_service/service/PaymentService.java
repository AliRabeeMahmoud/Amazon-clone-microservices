package com.example.payment_service.service;


import com.example.payment_service.payload.PaymentRequest;
import com.example.payment_service.payload.PaymentResponse;

public interface PaymentService {
    long doPayment(PaymentRequest paymentRequest);

    PaymentResponse getPaymentDetailsByOrderId(long orderId);
}
