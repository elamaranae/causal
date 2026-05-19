package com.causal.product.service;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.causal.product.dto.response.ProductListingResponse;
import com.causal.product.dto.response.ProductShowResponse;
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

  public ProductShowResponse getProduct(Long id) {
    Product product = repository.findWithSkusById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    return mapper.productShowResponseFrom(product);
  }

  public List<ProductListingResponse> getTrendingProducts() {
    return repository.findTop5By().stream().map(mapper::productListingResponseFrom).toList();
  }

  public Page<ProductListingResponse> filterProducts(Long categoryId, Pageable pageable) {
    return repository.findByCategoryId(categoryId, pageable).map(mapper::productListingResponseFrom);
  }
}
