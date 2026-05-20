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
import com.causal.product.repository.PriceRepository;
import com.causal.product.repository.ProductRepository;
import com.causal.product.model.Product;
import com.causal.product.model.Price;

@Service
public class ProductService {
  private final ProductRepository productRepository;
  private final PriceRepository priceRepository;
  private final ProductMapper mapper;

  public ProductService(ProductRepository productRepository, ProductMapper mapper, PriceRepository priceRepository) {
    this.productRepository = productRepository;
    this.priceRepository = priceRepository;
    this.mapper = mapper;
  }

  public ProductShowResponse getProduct(Long id) {
    Product product = productRepository.findWithSkusById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

    List<Price> prices = priceRepository.findBySkuIdInAndPriceCurrency(product.getSkus().stream().map(s -> s.getId()).toList(), "USD");

    return mapper.productShowResponseFrom(product);
  }

  public List<ProductListingResponse> getTrendingProducts() {
    return productRepository.findTop5By().stream().map(mapper::productListingResponseFrom).toList();
  }

  public Page<ProductListingResponse> filterProducts(Long categoryId, Pageable pageable) {
    return productRepository.findByCategoryId(categoryId, pageable).map(mapper::productListingResponseFrom);
  }
}
