package com.causal.inventory.repository;

import com.causal.inventory.model.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findBySkuId(Long skuId);

    List<Stock> findBySkuIdIn(List<Long> skuIds);

    @Query(value = """
            SELECT DISTINCT ON (product_id) *
            FROM stocks
            WHERE product_id IN :productIds
            ORDER BY product_id, quantity DESC
            """, nativeQuery = true)
    List<Stock> findTopStockByProductIds(List<Long> productIds);

    @Modifying
    @Query(value = """
            UPDATE stocks SET available_count = available_count - :quantity, updated_at = NOW()
            WHERE sku_id = :skuId AND available_count - :quantity >= 0
            """, nativeQuery = true)
    int decrementAvailable(Long skuId, int quantity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Stock> findWithLockBySkuId(Long skuId);
}
