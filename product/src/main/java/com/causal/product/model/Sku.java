package com.causal.product.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "skus")
@Getter
public class Sku {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private boolean isDefault;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @Column(name = "product_id", insertable = false, updatable = false)
  private Long productId;

  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> attributes = new HashMap<>();

  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> variantAttributes = new HashMap<>();

  @CreationTimestamp
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;
}
