package com.causal.product.service;
import java.util.List;

import org.springframework.stereotype.Service;
import com.causal.product.dto.response.ProductListingResponse;
import com.causal.product.mapper.ProductMapper;
import com.causal.product.repository.ProductRepository;
import com.causal.product.model.Product;

@Service
public class ProductService {
  private final ProductRepository repository;
  private final ProductMapper mapper;

  public ProductService(ProductRepository repository, ProductMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  public List<ProductListingResponse> getTrendingProducts() {
    List<Product> products = repository.findTop5By();
    return products.stream().map(mapper::from).toList();
  }
}
