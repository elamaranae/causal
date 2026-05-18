package com.causal.product.mapper;

import org.mapstruct.Mapper;

import com.causal.product.dto.response.ProductCategoryListingResponse;
import com.causal.product.model.ProductCategory;

@Mapper(componentModel = "spring" )
public interface ProductCategoryMapper {
  ProductCategoryListingResponse from(ProductCategory category);
}
