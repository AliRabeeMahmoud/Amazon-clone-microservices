package com.example.product_service.service;


import com.example.product_service.payload.request.ProductRequest;
import com.example.product_service.payload.response.ProductResponse;

public interface ProductService {

    long addProduct(ProductRequest productRequest);

    ProductResponse getProductById(long productId);

    void reduceQuantity(long productId, long quantity);

    public void deleteProductById(long productId);
}
