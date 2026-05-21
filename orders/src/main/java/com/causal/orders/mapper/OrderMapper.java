package com.causal.orders.mapper;

import com.causal.orders.dto.response.OrderItemShowResponse;
import com.causal.orders.dto.response.OrderShowResponse;
import com.causal.orders.model.Order;
import com.causal.orders.model.OrderItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderShowResponse from(Order order);
    OrderItemShowResponse from(OrderItem item);
}
