package com.example.order_service.external.client;

import com.example.order_service.config.FeignConfig;
import com.example.order_service.exception.CustomException;
import com.example.order_service.payload.request.PaymentRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@CircuitBreaker(name = "external", fallbackMethod = "fallback")
@FeignClient(name = "PAYMENT-SERVICE/payment", configuration = FeignConfig.class)
public interface PaymentService {

    @PostMapping
    public ResponseEntity<Long> doPayment(
            @RequestHeader("Authorization") String bearerToken ,
            @RequestBody PaymentRequest paymentRequest);

    default ResponseEntity<Long> fallback(Exception e) {  // fallback is used when request fails or timeouts
                                                            // i.e. when there is no response
        throw new CustomException("Payment Service is not available",
                "UNAVAILABLE",
                500);
    }
}
