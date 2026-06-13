package com.causal.product.controller;

import com.causal.product.dto.request.CreateProductRequest;
import com.causal.product.dto.response.ProductShowResponse;
import com.causal.product.service.BackofficeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("products/backoffice")
public class BackofficeController {

    private final BackofficeService backofficeService;

    public BackofficeController(BackofficeService backofficeService) {
        this.backofficeService = backofficeService;
    }

    @PostMapping("/products")
    public ProductShowResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        return backofficeService.createProduct(request);
    }
}
