package com.example.product_service.payload.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductRequest {
    private String name;
    private long price;
    private long quantity;
}
