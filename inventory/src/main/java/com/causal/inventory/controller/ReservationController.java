package com.causal.inventory.controller;

import com.causal.inventory.dto.request.ReservationExtendRequest;
import com.causal.inventory.dto.response.ReservationResponse;
import com.causal.inventory.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("internal/inventory/reservations/extend")
    public ReservationResponse extend(@Valid @RequestBody ReservationExtendRequest request) {
        return reservationService.extend(request);
    }
}
