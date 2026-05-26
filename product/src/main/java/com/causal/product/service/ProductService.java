package com.causal.product.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.causal.product.client.inventory.InventoryGateway;
import com.causal.product.client.inventory.dto.response.ProductStockShowResponse;
import com.causal.product.dto.response.ProductListingResponse;
import com.causal.product.dto.response.ProductShowResponse;
import com.causal.product.mapper.ProductMapper;
import com.causal.product.model.Product;
import com.causal.product.repository.ProductRepository;

@Service
public class ProductService {
  private final ProductRepository productRepository;
  private final ProductMapper mapper;
  private final InventoryGateway inventoryGateway;
  private final SkuService skuService;

  public ProductService(ProductRepository productRepository, ProductMapper mapper, InventoryGateway inventoryGateway, SkuService skuService) {
    this.productRepository = productRepository;
    this.mapper = mapper;
    this.inventoryGateway = inventoryGateway;
    this.skuService = skuService;
  }

  public ProductShowResponse getProduct(Long id) {
    Product product = productRepository.findWithSkusById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    skuService.attachPricesToSkus(product.getSkus());
    skuService.addStockDetailsToSkus(product.getSkus());
    return mapper.productShowResponseFrom(product);
  }

  public List<ProductListingResponse> getTrendingProducts() {
    List<Product> products = productRepository.findFirst5ByOrderByIdAsc();
    skuService.attachPricesToSkus(products.stream().map(Product::getDefaultSku).toList());
    addStockDetailsToProducts(products);
    return products.stream().map(mapper::productListingResponseFrom).toList();
  }

  public Page<ProductListingResponse> filterProducts(Long categoryId, Pageable pageable) {
    Page<Product> products = productRepository.findByCategoryIdOrderByIdAsc(categoryId, pageable);
    skuService.attachPricesToSkus(products.stream().map(Product::getDefaultSku).toList());
    addStockDetailsToProducts(products.getContent());
    return products.map(mapper::productListingResponseFrom);
  }

  private void addStockDetailsToProducts(List<Product> products) {
    List<ProductStockShowResponse> stocks = inventoryGateway.getStocksByProductIds(products.stream().map(Product::getId).toList());
    Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));
    stocks.forEach(stock -> productMap.get(stock.productId()).setInStock(stock.available()));
  }
}
