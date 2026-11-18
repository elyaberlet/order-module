package com.innowise.orderservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemRequest(
        @NotBlank(message = "Name must not be blank")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @NotNull(message = "Price must not be null")
        @DecimalMin(value = "0.00", message = "Price must not be negative")
        @Digits(integer = 17, fraction = 2, message = "Price must have at most 2 decimal places")
        BigDecimal price
) {
}
