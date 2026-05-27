package com.causal.inventory.repository;

import com.causal.inventory.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByOrderId(Long orderId);

    List<Reservation> findByUserIdAndOrderId(Long userId, Long orderId);

    List<Reservation> findBySkuIdAndExpiresAtLessThanEqual(Long skuId, Instant now);
}
