package com.causal.product.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.causal.product.dto.response.SkuShowResponse;
import com.causal.product.mapper.ProductMapper;
import com.causal.product.model.Price;
import com.causal.product.model.Sku;
import com.causal.product.repository.PriceRepository;
import com.causal.product.repository.SkuRepository;

@Service
public class SkuService {
  private final SkuRepository skuRepository;
  private final PriceRepository priceRepository;
  private final ProductMapper mapper;

  public SkuService(SkuRepository skuRepository, PriceRepository priceRepository, ProductMapper mapper) {
    this.skuRepository = skuRepository;
    this.priceRepository = priceRepository;
    this.mapper = mapper;
  }

  public List<SkuShowResponse> getSkus(List<Long> ids) {
    List<Sku> skus = skuRepository.findByIdIn(ids);
    List<Price> prices = priceRepository.findBySkuIdInAndPriceCurrency(ids, "USD");
    Map<Long, Price> priceMap = prices.stream().collect(Collectors.toMap(Price::getSkuId, Function.identity()));
    skus.forEach(sku -> sku.setPrice(priceMap.get(sku.getId())));
    return skus.stream().map(mapper::skuShowResponseFrom).toList();
  }
}
