package com.causal.product.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.causal.product.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
  public List<Product> findTop5By();
  public Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
}
