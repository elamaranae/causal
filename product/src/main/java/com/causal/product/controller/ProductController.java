package com.causal.product.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @GetMapping("products/filter")
  public Page<ProductListingResponse> filterProducts(
      @RequestParam Long categoryId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable)
  {
    return service.filterProducts(categoryId, pageable);
  }
}
