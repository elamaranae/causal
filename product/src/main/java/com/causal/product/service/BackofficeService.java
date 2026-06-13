package com.causal.product.service;

import com.causal.product.client.inventory.InventoryGateway;
import com.causal.product.dto.request.CreateProductRequest;
import com.causal.product.dto.response.ProductShowResponse;
import com.causal.product.mapper.ProductMapper;
import com.causal.product.model.Price;
import com.causal.product.model.Product;
import com.causal.product.model.ProductCategory;
import com.causal.product.model.Sku;
import com.causal.product.model.Vendor;
import com.causal.product.repository.ProductCategoryRepository;
import com.causal.product.repository.ProductRepository;
import com.causal.product.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BackofficeService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final VendorRepository vendorRepository;
    private final InventoryGateway inventoryGateway;
    private final ProductMapper mapper;

    public BackofficeService(
            ProductRepository productRepository,
            ProductCategoryRepository categoryRepository,
            VendorRepository vendorRepository,
            InventoryGateway inventoryGateway,
            ProductMapper mapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.vendorRepository = vendorRepository;
        this.inventoryGateway = inventoryGateway;
        this.mapper = mapper;
    }

    @Transactional
    public ProductShowResponse createProduct(CreateProductRequest request) {
        ProductCategory category = categoryRepository.findByName(request.categoryName())
                .orElseGet(() -> {
                    ProductCategory c = new ProductCategory();
                    c.setName(request.categoryName());
                    return categoryRepository.save(c);
                });

        Vendor vendor = vendorRepository.findFirstByName("Backoffice")
                .orElseGet(() -> {
                    Vendor v = new Vendor();
                    v.setName("Backoffice");
                    return vendorRepository.save(v);
                });

        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategoryId(category.getId());
        product.setVendorId(vendor.getId());
        product.setPrimaryThumbnailUrl(request.primaryThumbnailUrl());
        product.setPrimaryVariantKey(request.primaryVariantKey());

        for (CreateProductRequest.CreateSkuRequest skuReq : request.skus()) {
            Sku sku = new Sku();
            sku.setProduct(product);
            sku.setVariantAttributes(skuReq.variantAttributes() != null ? skuReq.variantAttributes() : new HashMap<>());

            Price price = new Price();
            price.setSku(sku);
            price.setPriceAmount(skuReq.price());
            price.setPriceCurrency(skuReq.currency());
            price.setEffectiveFrom(Instant.now());
            sku.getPrices().add(price);

            product.getSkus().add(sku);
        }

        product = productRepository.save(product);

        // Set first SKU as default
        product.setDefaultSku(product.getSkus().getFirst());
        product = productRepository.save(product);

        // Create stock in inventory service
        for (int i = 0; i < product.getSkus().size(); i++) {
            Sku sku = product.getSkus().get(i);
            int stock = request.skus().get(i).stock();
            inventoryGateway.createStock(sku.getId(), product.getId(), stock);
            sku.setStockQuantity(stock);
        }

        // Set price on transient field for response
        for (int i = 0; i < product.getSkus().size(); i++) {
            Sku sku = product.getSkus().get(i);
            if (!sku.getPrices().isEmpty()) {
                sku.setPrice(sku.getPrices().getFirst());
            }
        }

        product.setInStock(true);
        return mapper.productShowResponseFrom(product);
    }
}
