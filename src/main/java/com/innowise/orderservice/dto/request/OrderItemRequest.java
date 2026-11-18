package com.innowise.orderservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull(message = "Item id must not be null")
        Long itemId,

        @NotNull(message = "Quantity must not be null")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 10000, message = "Quantity must not exceed 10000")
        Integer quantity) {
}
