package com.causal.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.causal.product.dto.response.ProductListingResponse;
import com.causal.product.dto.response.ProductShowResponse;
import com.causal.product.dto.response.SkuResponse;
import com.causal.product.model.Product;
import com.causal.product.model.Sku;

@Mapper(componentModel = "spring")
public interface ProductMapper {
  ProductListingResponse productListingResponseFrom(Product product);
  ProductShowResponse productShowResponseFrom(Product product);
  @Mapping(source = "default", target = "isDefault")
  SkuResponse skuResponseFrom(Sku sku);
}
