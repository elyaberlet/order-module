package com.innowise.orderservice.dto.response;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String message,
        Instant timestamp
) {
}