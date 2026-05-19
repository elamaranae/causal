package com.causal.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.causal.product.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
  @EntityGraph(attributePaths = {"skus"})
  public Optional<Product> findWithSkusById(Long id);
  public List<Product> findTop5By();
  public Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
}
