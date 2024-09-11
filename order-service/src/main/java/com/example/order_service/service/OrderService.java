package com.example.order_service.service;


import com.example.order_service.payload.request.OrderRequest;
import com.example.order_service.payload.response.OrderResponse;

public interface OrderService {
    long placeOrder(OrderRequest orderRequest);

    OrderResponse getOrderDetails(long orderId, String bearerToken);
}
