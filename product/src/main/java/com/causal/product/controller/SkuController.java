package com.causal.product.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.causal.product.dto.request.SkuBulkGetRequest;
import com.causal.product.dto.response.SkuShowResponse;
import com.causal.product.service.SkuService;

@RestController
public class SkuController {
  private final SkuService service;

  public SkuController(SkuService service) {
    this.service = service;
  }

  @PostMapping("products/skus/bulk")
  public List<SkuShowResponse> getSkus(@Validated @RequestBody SkuBulkGetRequest request) {
    return service.getSkus(request.ids());
  }
}
