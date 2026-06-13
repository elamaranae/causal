package com.causal.inventory.repository;

import com.causal.inventory.model.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Reservation> findWithLockByOrderId(Long orderId);

    List<Reservation> findByUserIdAndOrderId(Long userId, Long orderId);

    List<Reservation> findBySkuIdAndExpiresAtLessThanEqual(Long skuId, Instant now);
}
