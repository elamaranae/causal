package com.causal.product.service;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.causal.product.dto.response.ProductListingResponse;
import com.causal.product.mapper.ProductMapper;
import com.causal.product.repository.ProductRepository;

@Service
public class ProductService {
  private final ProductRepository repository;
  private final ProductMapper mapper;

  public ProductService(ProductRepository repository, ProductMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  public List<ProductListingResponse> getTrendingProducts() {
    return repository.findTop5By().stream().map(mapper::from).toList();
  }

  public Page<ProductListingResponse> filterProducts(Long categoryId, Pageable pageable) {
    return repository.findByCategoryId(categoryId, pageable).map(mapper::from);
  }
}
