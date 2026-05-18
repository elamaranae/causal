package com.causal.product.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.causal.product.dto.response.ProductListingResponse;
import com.causal.product.service.ProductService;

@RestController
public class ProductController {
  private final ProductService service;

  public ProductController(ProductService service) {
    this.service = service;
  }

  @GetMapping("products/trending")
  public List<ProductListingResponse> trendingProduct() {
    return service.getTrendingProducts();
  }
}
