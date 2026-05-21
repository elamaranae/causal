package com.causal.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.causal.product.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
  @EntityGraph(attributePaths = {"skus", "skus.media"})
  public Optional<Product> findWithSkusById(Long id);
  @EntityGraph(attributePaths = {"defaultSku"})
  public List<Product> findFirst5ByOrderByIdAsc();
  @EntityGraph(attributePaths = {"defaultSku"})
  public Page<Product> findByCategoryIdOrderByIdAsc(Long categoryId, Pageable pageable);
}
