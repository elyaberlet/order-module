package com.innowise.orderservice.dto.response;

import com.innowise.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        OrderStatus status,
        BigDecimal totalPrice,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemDto> items,
        UserDto user,
        boolean userInfoAvailable) {
}
