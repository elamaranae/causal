package com.causal.product.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String name;
  private String description;
  private String primaryThumbnailUrl;
  private long categoryId;

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getPrimaryThumbnailUrl() {
    return primaryThumbnailUrl;
  }

  public Long getCategoryId() {
    return categoryId;
  }
}
