package com.causal.inventory.service;

import com.causal.inventory.dto.request.StockCreateRequest;
import com.causal.inventory.dto.response.ProductStockShowResponse;
import com.causal.inventory.dto.response.StockShowResponse;
import com.causal.inventory.mapper.StockMapper;
import com.causal.inventory.model.Stock;
import com.causal.inventory.repository.StockRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final StockMapper stockMapper;

    public StockService(StockRepository stockRepository, StockMapper stockMapper) {
        this.stockRepository = stockRepository;
        this.stockMapper = stockMapper;
    }

    public StockShowResponse getStock(Long skuId) {
        return stockRepository.findBySkuId(skuId)
                .map(stockMapper::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found for sku " + skuId));
    }

    public List<StockShowResponse> getStocksBySkuIds(List<Long> skuIds) {
        return stockRepository.findBySkuIdIn(skuIds).stream()
                .map(stockMapper::from)
                .toList();
    }

    public StockShowResponse createStock(StockCreateRequest request) {
        Stock stock = new Stock();
        stock.setSkuId(request.skuId());
        stock.setProductId(request.productId());
        stock.setQuantity(request.quantity());
        stock.setAvailableCount(request.quantity());
        stock = stockRepository.save(stock);
        return stockMapper.from(stock);
    }

    public List<ProductStockShowResponse> getStocksByProductIds(List<Long> productIds) {
        return stockRepository.findTopStockByProductIds(productIds).stream()
                .map(stock -> new ProductStockShowResponse(stock.getProductId(), stock.getQuantity() > 0))
                .toList();
    }
}
