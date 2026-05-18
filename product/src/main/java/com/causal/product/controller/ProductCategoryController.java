package com.causal.product.controller;

import org.springframework.web.bind.annotation.RestController;

import com.causal.product.dto.response.ProductCategoryListingResponse;
import com.causal.product.service.ProductCategoryService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class ProductCategoryController {
  private final ProductCategoryService service;

  public ProductCategoryController(ProductCategoryService service) {
    this.service = service;
  }

  @GetMapping("/products/categories")
  public List<ProductCategoryListingResponse> getAllProductCategory() {
    return service.getAll();
  }
}
