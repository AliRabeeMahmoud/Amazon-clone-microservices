package com.example.order_service.service;

import com.example.order_service.exception.CustomException;
import com.example.order_service.external.client.PaymentService;
import com.example.order_service.external.client.ProductService;
import com.example.order_service.model.Order;
import com.example.order_service.payload.request.OrderRequest;
import com.example.order_service.payload.request.PaymentRequest;
import com.example.order_service.payload.response.OrderResponse;
import com.example.order_service.payload.response.PaymentResponse;
import com.example.order_service.payload.response.ProductResponse;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.service.impl.OrderServiceImpl;
import com.example.order_service.utils.PaymentMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
public class OrderServiceImplTest {

    private OrderRepository orderRepository;

    private ProductService productService;

    private PaymentService paymentService;

    private RestTemplate restTemplate;

    OrderService orderService;

    @BeforeEach
    void setup(){
        orderRepository = mock(OrderRepository.class);
        productService = mock(ProductService.class);
        paymentService = mock(PaymentService.class);
        restTemplate = mock(RestTemplate.class);
        orderService = new OrderServiceImpl(orderRepository, productService, paymentService, restTemplate);
    }

    @DisplayName("Get Order - Success Scenario")
    @Test
    void test_When_Order_Success() {

        String bearerToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJVc2VyIiwiaXNzIjoiUk9MRV9VU0VSICIsImlhdCI6MTY3MTU2MTQ3NywiZXhwIjoxNjcxNTYxNTk3fQ.DlxEnvkRKlWxEc_Qm70K5WZw41xYGH6uBc3s_oBe2rYsDiAfQSj9QRfy8fL9cGT5vJvIZkMhRdOqGm0guTh93A";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer "+ bearerToken);

        HttpEntity request = new HttpEntity<>(headers);

        //Mocking
        Order order = getMockOrder();
        when(orderRepository.findById(anyLong()))
                .thenReturn(Optional.of(order));

        when(restTemplate.exchange(
                "http://PRODUCT-SERVICE/product/" + order.getProductId(),
                HttpMethod.GET, request, ProductResponse.class).getBody()).thenReturn(getMockProductResponse());

        when(restTemplate.exchange(
                "http://PAYMENT-SERVICE/payment/order/" + order.getId(),
                HttpMethod.GET, request, PaymentResponse.class).getBody()).thenReturn(getMockPaymentResponse());

        //Actual
        OrderResponse orderResponse = orderService.getOrderDetails(1,bearerToken);

        //Verification
        verify(orderRepository, times(1)).findById(anyLong());
        verify(restTemplate, times(1)).getForObject(
                "http://PRODUCT-SERVICE/product/" + order.getProductId(),
                ProductResponse.class);
        verify(restTemplate, times(1)).getForObject(
                "http://PAYMENT-SERVICE/payment/order/" + order.getId(),
                PaymentResponse.class);

        //Assert
        assertNotNull(orderResponse);
        assertEquals(order.getId(), orderResponse.getOrderId());
    }

    @DisplayName("Get Orders - Failure Scenario")
    @Test
    void test_When_Get_Order_NOT_FOUND_then_Not_Found() {

        String bearerToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJVc2VyIiwiaXNzIjoiUk9MRV9VU0VSICIsImlhdCI6MTY3MTM5NTQ0MCwiZXhwIjoxNjcxMzk1NTYwfQ.fBhI_flxuuXZfwhd8hEVdfkZkMNobVsi4hAdSXdl5qqWRedJWQWZXwYVdfSof6ezH7myZNQgn-kRNBXIDDHGDQ";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + bearerToken);

        when(orderRepository.findById(anyLong()))
                .thenReturn(Optional.ofNullable(null));

        CustomException exception =
                assertThrows(CustomException.class,
                        () -> orderService.getOrderDetails(1,bearerToken));
        assertEquals("NOT_FOUND", exception.getErrorCode());
        assertEquals(404, exception.getStatus());

        verify(orderRepository, times(1))
                .findById(anyLong());
    }

    @DisplayName("Place Order - Success Scenario")
    @Test
    void test_When_Place_Order_Success() {
        String bearerToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJVc2VyIiwiaXNzIjoiUk9MRV9VU0VSICIsImlhdCI6MTY3MTM5NTQ0MCwiZXhwIjoxNjcxMzk1NTYwfQ.fBhI_flxuuXZfwhd8hEVdfkZkMNobVsi4hAdSXdl5qqWRedJWQWZXwYVdfSof6ezH7myZNQgn-kRNBXIDDHGDQ";

        Order order = getMockOrder();
        OrderRequest orderRequest = getMockOrderRequest();

        when(orderRepository.save(any(Order.class)))
                .thenReturn(order);
        when(productService.reduceQuantity(anyString(), anyLong(),anyLong()))
                .thenReturn(new ResponseEntity<Void>(HttpStatus.OK));
        when(paymentService.doPayment(anyString(), any(PaymentRequest.class)))
                .thenReturn(new ResponseEntity<Long>(1L,HttpStatus.OK));

        long orderId = orderService.placeOrder(orderRequest, bearerToken);

        verify(orderRepository, times(2))
                .save(any());
        verify(productService, times(1))
                .reduceQuantity(anyString(), anyLong(),anyLong());
        verify(paymentService, times(1))
                .doPayment(anyString(), any(PaymentRequest.class));

        assertEquals(order.getId(), orderId);
    }

    @DisplayName("Place Order - Payment Failed Scenario")
    @Test
    void test_when_Place_Order_Payment_Fails_then_Order_Placed() {

        String bearerToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJVc2VyIiwiaXNzIjoiUk9MRV9VU0VSICIsImlhdCI6MTY3MTM5NTQ0MCwiZXhwIjoxNjcxMzk1NTYwfQ.fBhI_flxuuXZfwhd8hEVdfkZkMNobVsi4hAdSXdl5qqWRedJWQWZXwYVdfSof6ezH7myZNQgn-kRNBXIDDHGDQ";

        Order order = getMockOrder();
        OrderRequest orderRequest = getMockOrderRequest();

        when(orderRepository.save(any(Order.class)))
                .thenReturn(order);
        when(productService.reduceQuantity(anyString(), anyLong(),anyLong()))
                .thenReturn(new ResponseEntity<Void>(HttpStatus.OK));
        when(paymentService.doPayment(anyString(), any(PaymentRequest.class)))
                .thenThrow(new RuntimeException());

        long orderId = orderService.placeOrder(orderRequest, bearerToken);

        verify(orderRepository, times(2))
                .save(any());
        verify(productService, times(1))
                .reduceQuantity(anyString(), anyLong(),anyLong());
        verify(paymentService, times(1))
                .doPayment(anyString(), any(PaymentRequest.class));

        assertEquals(order.getId(), orderId);
    }

    private OrderRequest getMockOrderRequest() {
        return OrderRequest.builder()
                .productId(1)
                .quantity(10)
                .paymentMode(PaymentMode.CASH)
                .totalAmount(100)
                .build();
    }

    private PaymentResponse getMockPaymentResponse() {
        return PaymentResponse.builder()
                .paymentId(1)
                .paymentDate(Instant.now())
                .paymentMode(PaymentMode.CASH)
                .amount(200)
                .orderId(1)
                .status("ACCEPTED")
                .build();
    }

    private ProductResponse getMockProductResponse() {
        return ProductResponse.builder()
                .productId(2)
                .productName("iPhone")
                .price(100)
                .quantity(200)
                .build();
    }

    private Order getMockOrder() {
        return Order.builder()
                .orderStatus("PLACED")
                .orderDate(Instant.now())
                .id(1)
                .amount(100)
                .quantity(200)
                .productId(2)
                .build();
    }
}
