package com.causal.product.service;
import org.springframework.stereotype.Service;
import com.causal.product.dto.response.ProductListingResponse;

@Service
public class ProductService {
  public ProductListingResponse[] getTrendingProducts() {
    return new ProductListingResponse[] {};
  }
}
