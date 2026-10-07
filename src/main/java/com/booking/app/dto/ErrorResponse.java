package com.booking.app.dto;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String reason,
        String request_id
) {
}
