package com.booking.app.dto;

import java.util.List;
import java.util.UUID;

public record ShowResponse(
        UUID id,
        String name,
        long price_paise,
        int per_user_limit,
        int total_seats,
        int available_seats,
        int held_seats,
        int confirmed_seats,
        List<SeatResponse> seats
) {

    public record SeatResponse(
            String seat,
            String status
    ) {
    }
}