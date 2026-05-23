package com.causal.product.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "skus")
@Getter
public class Sku {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "media_id")
  private Media media;

  @OneToMany(mappedBy = "sku", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Price> prices = new ArrayList<>();

  @Transient @Getter @Setter
  private Price price;

  @Transient @Setter
  private int stockQuantity;

  @Column(name = "product_id", insertable = false, updatable = false)
  private Long productId;

  @Column(name = "media_id", insertable = false, updatable = false)
  private Long mediaId;

  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> attributes = new HashMap<>();

  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> variantAttributes = new HashMap<>();

  @CreationTimestamp
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;
}
