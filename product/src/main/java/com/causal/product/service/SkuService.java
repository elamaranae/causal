package com.causal.product.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.causal.product.client.inventory.InventoryGateway;
import com.causal.product.client.inventory.dto.response.StockShowResponse;
import com.causal.product.client.profile.ProfileGateway;
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
  private final InventoryGateway inventoryGateway;
  private final ProfileGateway profileGateway;

  public SkuService(SkuRepository skuRepository, PriceRepository priceRepository, ProductMapper mapper, InventoryGateway inventoryGateway, ProfileGateway profileGateway) {
    this.skuRepository = skuRepository;
    this.priceRepository = priceRepository;
    this.mapper = mapper;
    this.inventoryGateway = inventoryGateway;
    this.profileGateway = profileGateway;
  }

  public List<SkuShowResponse> getSkus(List<Long> ids) {
    List<Sku> skus = skuRepository.findByIdIn(ids);
    attachPricesToSkus(skus);
    addStockDetailsToSkus(skus);
    return skus.stream().map(mapper::skuShowResponseFrom).toList();
  }

  public void attachPricesToSkus(List<Sku> skus) {
    String currency = getUserCurrency();
    attachPricesToSkus(skus, currency);
  }

  public void attachPricesToSkus(List<Sku> skus, String currency) {
    List<Price> prices = priceRepository.findBySkuIdInAndPriceCurrency(skus.stream().map(Sku::getId).toList(), currency);
    Map<Long, Price> priceMap = prices.stream().collect(Collectors.toMap(Price::getSkuId, Function.identity()));
    skus.forEach(sku -> sku.setPrice(priceMap.get(sku.getId())));
  }

  public void addStockDetailsToSkus(List<Sku> skus) {
    List<StockShowResponse> stocks = inventoryGateway.getStocksBySkuIds(skus.stream().map(Sku::getId).toList());
    Map<Long, Sku> skuMap = skus.stream().collect(Collectors.toMap(Sku::getId, Function.identity()));
    stocks.forEach(stock -> skuMap.get(stock.skuId()).setStockQuantity(stock.quantity()));
  }

  String getUserCurrency() {
    try {
      return profileGateway.getCurrentUserProfile().currency();
    } catch (Exception e) {
      return "USD";
    }
  }
}
