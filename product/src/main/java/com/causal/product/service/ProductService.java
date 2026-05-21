package com.causal.product.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.causal.product.dto.response.ProductListingResponse;
import com.causal.product.dto.response.ProductShowResponse;
import com.causal.product.mapper.ProductMapper;
import com.causal.product.model.Price;
import com.causal.product.model.Product;
import com.causal.product.model.Sku;
import com.causal.product.repository.PriceRepository;
import com.causal.product.repository.ProductRepository;

@Service
public class ProductService {
  private static final String DEFAULT_CURRENCY = "USD";

  private final ProductRepository productRepository;
  private final PriceRepository priceRepository;
  private final ProductMapper mapper;

  public ProductService(ProductRepository productRepository, PriceRepository priceRepository, ProductMapper mapper) {
    this.productRepository = productRepository;
    this.priceRepository = priceRepository;
    this.mapper = mapper;
  }

  public ProductShowResponse getProduct(Long id) {
    Product product = productRepository.findWithSkusById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    attachPricesToSkus(product.getSkus());
    return mapper.productShowResponseFrom(product);
  }

  public List<ProductListingResponse> getTrendingProducts() {
    List<Product> products = productRepository.findFirst5ByOrderByIdAsc();
    attachPricesToSkus(products.stream().map(Product::getDefaultSku).toList());
    return products.stream().map(mapper::productListingResponseFrom).toList();
  }

  public Page<ProductListingResponse> filterProducts(Long categoryId, Pageable pageable) {
    Page<Product> products = productRepository.findByCategoryIdOrderByIdAsc(categoryId, pageable);
    attachPricesToSkus(products.stream().map(Product::getDefaultSku).toList());
    return products.map(mapper::productListingResponseFrom);
  }

  private void attachPricesToSkus(List<Sku> skus) {
    List<Price> prices = priceRepository.findBySkuIdInAndPriceCurrency(skus.stream().map(sku -> sku.getId()).toList(), "USD");
    Map<Long, Price> map = prices.stream().collect(Collectors.toMap(Price::getSkuId, Function.identity()));
    skus.forEach(sku -> sku.setPrice(map.get(sku.getId())));
  }
}
