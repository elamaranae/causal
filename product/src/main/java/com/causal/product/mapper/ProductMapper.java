package com.causal.product.mapper;

import org.mapstruct.Mapper;

import com.causal.product.dto.response.ProductListingResponse;
import com.causal.product.model.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {
  ProductListingResponse from(Product product);
}
