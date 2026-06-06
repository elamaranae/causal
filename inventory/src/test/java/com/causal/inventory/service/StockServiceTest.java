package com.causal.inventory.service;

import com.causal.inventory.dto.response.ProductStockShowResponse;
import com.causal.inventory.dto.response.StockShowResponse;
import com.causal.inventory.mapper.StockMapper;
import com.causal.inventory.model.Stock;
import com.causal.inventory.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock private StockRepository stockRepository;
    @Mock private StockMapper stockMapper;

    @InjectMocks
    private StockService stockService;

    @Test
    void getStock_notFound_throws() {
        when(stockRepository.findBySkuId(999L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> stockService.getStock(999L));
    }

    @Test
    void getStock_found_returnsResponse() {
        Stock stock = new Stock();
        stock.setSkuId(100L);
        stock.setQuantity(50);
        when(stockRepository.findBySkuId(100L)).thenReturn(Optional.of(stock));
        StockShowResponse expected = new StockShowResponse(1L, 100L, 1L, 50);
        when(stockMapper.from(stock)).thenReturn(expected);

        StockShowResponse result = stockService.getStock(100L);
        assertEquals(expected, result);
    }

    @Test
    void getStocksBySkuIds_returnsMatchingStocks() {
        Stock s1 = new Stock();
        s1.setSkuId(100L);
        Stock s2 = new Stock();
        s2.setSkuId(200L);
        when(stockRepository.findBySkuIdIn(List.of(100L, 200L))).thenReturn(List.of(s1, s2));
        when(stockMapper.from(s1)).thenReturn(new StockShowResponse(1L, 100L, 1L, 10));
        when(stockMapper.from(s2)).thenReturn(new StockShowResponse(2L, 200L, 2L, 20));

        List<StockShowResponse> result = stockService.getStocksBySkuIds(List.of(100L, 200L));
        assertEquals(2, result.size());
    }

    @Test
    void getStocksByProductIds_mapsInStockCorrectly() {
        Stock inStock = new Stock();
        inStock.setProductId(1L);
        inStock.setQuantity(10);
        Stock outOfStock = new Stock();
        outOfStock.setProductId(2L);
        outOfStock.setQuantity(0);

        when(stockRepository.findTopStockByProductIds(List.of(1L, 2L)))
                .thenReturn(List.of(inStock, outOfStock));

        List<ProductStockShowResponse> result = stockService.getStocksByProductIds(List.of(1L, 2L));
        assertEquals(2, result.size());
        assertTrue(result.get(0).available());
        assertFalse(result.get(1).available());
    }
}
