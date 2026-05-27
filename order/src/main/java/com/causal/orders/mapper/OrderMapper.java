package com.causal.orders.mapper;

import com.causal.orders.dto.response.AddressResponse;
import com.causal.orders.dto.response.OrderItemShowResponse;
import com.causal.orders.dto.response.OrderShowResponse;
import com.causal.orders.dto.response.PriceResponse;
import com.causal.orders.model.Order;
import com.causal.orders.model.OrderAddress;
import com.causal.orders.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "total", source = ".", qualifiedByName = "orderTotal")
    OrderShowResponse from(Order order);

    @Mapping(target = "price", source = ".", qualifiedByName = "itemPrice")
    OrderItemShowResponse from(OrderItem item);

    AddressResponse from(OrderAddress address);

    @Named("orderTotal")
    default PriceResponse orderTotal(Order order) {
        return new PriceResponse(order.getTotalCurrency(), order.getTotalAmount());
    }

    @Named("itemPrice")
    default PriceResponse itemPrice(OrderItem item) {
        return new PriceResponse(item.getPurchaseCurrency(), item.getPurchaseAmount());
    }
}
