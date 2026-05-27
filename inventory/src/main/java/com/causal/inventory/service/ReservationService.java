package com.causal.inventory.service;

import com.causal.inventory.dto.request.StockReservationItemRequest;
import com.causal.inventory.dto.request.StockReserveRequest;
import com.causal.inventory.dto.response.ReservationResponse;
import com.causal.inventory.mapper.ReservationMapper;
import com.causal.inventory.model.Reservation;
import com.causal.inventory.model.Stock;
import com.causal.inventory.repository.ReservationRepository;
import com.causal.inventory.repository.StockRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final int RESERVATION_EXPIRY_MINUTES = 15;

    private final ReservationRepository reservationRepository;
    private final StockRepository stockRepository;
    private final ReservationMapper reservationMapper;
    private final TransactionTemplate tx;

    public ReservationService(ReservationRepository reservationRepository,
                              StockRepository stockRepository,
                              ReservationMapper reservationMapper,
                              PlatformTransactionManager txManager) {
        this.reservationRepository = reservationRepository;
        this.stockRepository = stockRepository;
        this.reservationMapper = reservationMapper;
        this.tx = new TransactionTemplate(txManager);
    }

    public ReservationResponse reserve(StockReserveRequest request) {
        List<Reservation> existing = reservationRepository.findByOrderId(request.orderId());
        if (matchesRequest(existing, request)) {
            return toResponse(request.orderId(), existing);
        }

        // First attempt
        List<StockReservationItemRequest> failedSkus = tryReserve(request);
        if (!failedSkus.isEmpty()) {
            // Reclaim expired reservations for failed SKUs, then retry
            failedSkus.forEach(item -> reclaimExpired(item.skuId()));
            failedSkus = tryReserve(request);
        }

        if (!failedSkus.isEmpty()) {
            List<Long> ids = failedSkus.stream().map(StockReservationItemRequest::skuId).toList();
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for SKUs: " + ids);
        }

        return toResponse(request.orderId(), reservationRepository.findByOrderId(request.orderId()));
    }

    private boolean matchesRequest(List<Reservation> existing, StockReserveRequest request) {
        if (existing.size() != request.items().size()) return false;

        Map<Long, Integer> bySkuId = existing.stream()
                .collect(Collectors.toMap(Reservation::getSkuId, Reservation::getQuantity));

        return request.items().stream().allMatch(item ->
                item.quantity().equals(bySkuId.get(item.skuId())));
    }

    private List<StockReservationItemRequest> tryReserve(StockReserveRequest request) {
        List<StockReservationItemRequest> failed = new ArrayList<>();

        tx.executeWithoutResult(status -> {
            for (StockReservationItemRequest item : request.items()) {
                if (stockRepository.decrementAvailable(item.skuId(), item.quantity()) == 0) {
                    failed.add(item);
                }
            }

            if (!failed.isEmpty()) {
                status.setRollbackOnly();
                return;
            }

            Instant expiresAt = Instant.now().plus(RESERVATION_EXPIRY_MINUTES, ChronoUnit.MINUTES);
            List<Reservation> reservations = request.items().stream().map(item -> {
                Reservation reservation = new Reservation();
                reservation.setOrderId(request.orderId());
                reservation.setSkuId(item.skuId());
                reservation.setQuantity(item.quantity());
                reservation.setExpiresAt(expiresAt);
                return reservation;
            }).toList();
            reservationRepository.saveAll(reservations);
        });

        return failed;
    }

    private void reclaimExpired(Long skuId) {
        tx.executeWithoutResult(status -> {
            Stock stock = stockRepository.findWithLockBySkuId(skuId).orElse(null);
            if (stock == null) return;

            List<Reservation> expired = reservationRepository.findBySkuIdAndExpiresAtLessThanEqual(skuId, Instant.now());
            if (expired.isEmpty()) return;

            int reclaimed = expired.stream().mapToInt(Reservation::getQuantity).sum();
            reservationRepository.deleteAll(expired);
            stock.setAvailableCount(stock.getAvailableCount() + reclaimed);
        });
    }

    private ReservationResponse toResponse(Long orderId, List<Reservation> reservations) {
        return new ReservationResponse(orderId, reservations.stream().map(reservationMapper::from).toList());
    }
}
