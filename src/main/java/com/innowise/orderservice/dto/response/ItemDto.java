package com.innowise.orderservice.dto.response;

import java.math.BigDecimal;

public record ItemDto(Long id, String name, BigDecimal price) {
}
