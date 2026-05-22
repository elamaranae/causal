package com.causal.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.causal.product.model.Sku;

public interface SkuRepository extends JpaRepository<Sku, Long> {
  @EntityGraph(attributePaths = {"media", "product"})
  List<Sku> findByIdIn(List<Long> ids);
}
