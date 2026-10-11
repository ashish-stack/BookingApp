package com.booking.app.controller;

import com.booking.app.dto.ReservationResponse;
import com.booking.app.dto.ReserveRequest;
import com.booking.app.service.AuthService;
import com.booking.app.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping
public class BookingController {

    private final BookingService bookingService;
    private final AuthService authService;

    public BookingController(
            BookingService bookingService,
            AuthService authService
    ) {
        this.bookingService = bookingService;
        this.authService = authService;
    }

    @PostMapping("/shows/{showId}/reserve")
    public ResponseEntity<ReservationResponse> reserve(
            @PathVariable UUID showId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            )
            String idempotencyHeader,
            @Valid @RequestBody ReserveRequest request
    ) {

        String userId =
                authService.requireUser(authorization);

        String idempotencyKey =
                idempotencyHeader != null
                        ? idempotencyHeader
                        : request.idempotencyKey();

        BookingService.ReservationResult result =
                bookingService.reserve(
                        showId,
                        userId,
                        idempotencyKey,
                        request
                );

        if (result.replay()) {
            return ResponseEntity.ok(result.response());
        }

        return ResponseEntity
                .status(201)
                .body(result.response());
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID reservationId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {

        String userId =
                authService.requireUser(authorization);

        bookingService.cancel(
                reservationId,
                userId
        );

        return ResponseEntity.noContent().build();
    }
}
