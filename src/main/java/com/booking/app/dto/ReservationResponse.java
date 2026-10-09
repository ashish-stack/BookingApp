package com.booking.app.dto;

import java.util.List;
import java.util.UUID;

public record ReservationResponse(
        UUID reservationId,
        UUID show_id,
        String user_id,
        List<String> seats,
        long amount_paise,
        String status
) {
}
