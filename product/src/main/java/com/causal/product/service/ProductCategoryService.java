package com.causal.product.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.causal.product.dto.response.ProductCategoryListingResponse;
import com.causal.product.mapper.ProductCategoryMapper;
import com.causal.product.model.ProductCategory;
import com.causal.product.repository.ProductCategoryRepository;

@Service
public class ProductCategoryService {
  private final ProductCategoryRepository repository;
  private final ProductCategoryMapper mapper;

  public ProductCategoryService(ProductCategoryRepository repository, ProductCategoryMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  // @Cacheable(value = "categories")
  public List<ProductCategoryListingResponse> getAll() {
    List<ProductCategory> categories = repository.findAll();
    return categories.stream().map(mapper::from).toList();
  }
}
