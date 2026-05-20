package com.causal.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.causal.product.model.Price;

public interface PriceRepository extends JpaRepository<Price, Long> {
  List<Price> findBySkuIdInAndPriceCurrency(List<Long> skuIds, String priceCurrency);
}
