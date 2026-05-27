package com.causal.inventory.mapper;

import com.causal.inventory.dto.response.ReservationItemResponse;
import com.causal.inventory.model.Reservation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    ReservationItemResponse from(Reservation reservation);
}
