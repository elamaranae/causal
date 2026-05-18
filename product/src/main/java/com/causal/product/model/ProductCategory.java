package com.causal.product.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "product_categories")
@Getter
public class ProductCategory {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;
  private String name;
  private String description;
  private Long parentId;
}
