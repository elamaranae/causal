package com.causal.inventory.service;

import com.causal.inventory.dto.request.ReservationExtendRequest;
import com.causal.inventory.dto.request.StockReservationItemRequest;
import com.causal.inventory.dto.request.StockReserveRequest;
import com.causal.inventory.dto.response.ReservationResponse;
import com.causal.inventory.mapper.ReservationMapper;
import com.causal.inventory.model.OutboxEvent;
import com.causal.inventory.model.ProcessedEvent;
import com.causal.inventory.model.Reservation;
import com.causal.inventory.model.Stock;
import com.causal.inventory.repository.OutboxRepository;
import com.causal.inventory.repository.ProcessedEventRepository;
import com.causal.inventory.repository.ReservationRepository;
import com.causal.inventory.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReservationService {

    private static final int RESERVATION_EXPIRY_MINUTES = 10;
    private static final int EXTEND_MINUTES = 5;
    private static final int MAX_LIFETIME_MINUTES = 25;

    private final ReservationRepository reservationRepository;
    private final StockRepository stockRepository;
    private final OutboxRepository outboxRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ReservationMapper reservationMapper;
    private final TransactionTemplate tx;

    public ReservationService(ReservationRepository reservationRepository,
                              StockRepository stockRepository,
                              OutboxRepository outboxRepository,
                              ProcessedEventRepository processedEventRepository,
                              ReservationMapper reservationMapper,
                              PlatformTransactionManager txManager) {
        this.reservationRepository = reservationRepository;
        this.stockRepository = stockRepository;
        this.outboxRepository = outboxRepository;
        this.processedEventRepository = processedEventRepository;
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
                reservation.setUserId(request.userId());
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

    @Transactional
    public ReservationResponse extend(ReservationExtendRequest request) {
        List<Reservation> existing = reservationRepository.findByUserIdAndOrderId(request.userId(), request.orderId());

        if (existing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No reservations found for user: " + request.userId() + " and order: " + request.orderId());
        }

        // Verify all requested items match existing reservations
        Map<Long, Integer> bySkuId = existing.stream()
                .collect(Collectors.toMap(Reservation::getSkuId, Reservation::getQuantity));

        boolean allMatch = request.items().size() == existing.size()
                && request.items().stream().allMatch(item ->
                        item.quantity().equals(bySkuId.get(item.skuId())));

        if (!allMatch) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation mismatch for order: " + request.orderId());
        }

        Instant now = Instant.now();
        for (Reservation reservation : existing) {
            Instant extendedExpiry = now.plus(EXTEND_MINUTES, ChronoUnit.MINUTES);
            Instant maxExpiry = reservation.getCreatedAt().plus(MAX_LIFETIME_MINUTES, ChronoUnit.MINUTES);
            reservation.setExpiresAt(extendedExpiry.isBefore(maxExpiry) ? extendedExpiry : maxExpiry);
        }

        reservationRepository.saveAll(existing);
        return toResponse(request.orderId(), existing);
    }

    @Transactional
    public void confirmReservation(String eventId, Long orderId, List<Map<String, Object>> expectedItems) {
        if (alreadyProcessed(eventId, "confirmReservation")) {
            log.info("Event {} already processed for order {}, skipping", eventId, orderId);
            return;
        }

        List<Reservation> reservations = reservationRepository.findWithLockByOrderId(orderId);

        if (!reservationsMatch(reservations, expectedItems)) {
            log.error("Reservation mismatch for order {}: expected {} items, found {}",
                    orderId, expectedItems.size(), reservations.size());
            markProcessed(eventId, "confirmReservation");
            publishCompleteOrder(orderId, "order_failed", "Reservation mismatch or expired");
            return;
        }

        // Stock was already decremented during reserve — just remove the reservations
        reservationRepository.deleteAll(reservations);
        markProcessed(eventId, "confirmReservation");
        publishCompleteOrder(orderId, "order_success", null);
    }

    @Transactional
    public void releaseReservation(String eventId, Long orderId, List<Map<String, Object>> expectedItems) {
        if (alreadyProcessed(eventId, "releaseReservation")) {
            log.info("Event {} already processed for order {}, skipping", eventId, orderId);
            return;
        }

        List<Reservation> reservations = reservationRepository.findByOrderId(orderId);

        if (reservations.isEmpty()) {
            markProcessed(eventId, "releaseReservation");
            publishCompleteOrder(orderId, "order_failed", "No reservations found");
            return;
        }

        if (!reservationsMatch(reservations, expectedItems)) {
            log.error("Reservation mismatch for order {} during release", orderId);
            markProcessed(eventId, "releaseReservation");
            publishCompleteOrder(orderId, "order_failed", "Reservation mismatch");
            return;
        }

        // Restore available stock for each SKU
        for (Reservation reservation : reservations) {
            Stock stock = stockRepository.findWithLockBySkuId(reservation.getSkuId()).orElse(null);
            if (stock != null) {
                stock.setAvailableCount(stock.getAvailableCount() + reservation.getQuantity());
            }
        }
        reservationRepository.deleteAll(reservations);
        markProcessed(eventId, "releaseReservation");
        publishCompleteOrder(orderId, "order_failed", "Payment failed");
    }

    private boolean alreadyProcessed(String eventId, String consumerId) {
        return processedEventRepository.existsByEventIdAndConsumerId(eventId, consumerId);
    }

    private void markProcessed(String eventId, String consumerId) {
        processedEventRepository.save(new ProcessedEvent(eventId, consumerId));
    }

    private boolean reservationsMatch(List<Reservation> reservations, List<Map<String, Object>> expectedItems) {
        if (reservations.isEmpty() || reservations.size() != expectedItems.size()) {
            return false;
        }

        Map<Long, Integer> bySkuId = reservations.stream()
                .collect(Collectors.toMap(Reservation::getSkuId, Reservation::getQuantity));

        return expectedItems.stream().allMatch(item -> {
            Long skuId = ((Number) item.get("skuId")).longValue();
            int quantity = ((Number) item.get("quantity")).intValue();
            return Integer.valueOf(quantity).equals(bySkuId.get(skuId));
        });
    }

    private void publishCompleteOrder(Long orderId, String status, String reason) {
        Map<String, Object> payload = new java.util.HashMap<>(Map.of(
                "orderId", orderId,
                "status", status
        ));
        if (reason != null) {
            payload.put("reason", reason);
        }

        OutboxEvent event = new OutboxEvent(
                "job.complete_order",
                orderId.toString(),
                "complete_order",
                payload
        );
        outboxRepository.save(event);
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
