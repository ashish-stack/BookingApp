package com.booking.app.service;

import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class MetricsService {

    private final Counter confirmed;
    private final Counter seatTaken;
    private final Counter userLimit;
    private final Counter idempotentReplay;
    private final Counter seatNotFound;

    public MetricsService(MeterRegistry registry) {

        confirmed = Counter.builder("reservations.confirmed")
                .description("Confirmed reservations")
                .register(registry);

        seatTaken = Counter.builder("reservations.declined")
                .tag("reason", "seat-taken")
                .description("Reservations declined")
                .register(registry);

        userLimit = Counter.builder("reservations.declined")
                .tag("reason", "per-user-limit")
                .description("Reservations declined")
                .register(registry);

        idempotentReplay = Counter.builder("reservations.declined")
                .tag("reason", "idempotent-replay")
                .description("Idempotent replay requests")
                .register(registry);

        seatNotFound = Counter.builder("reservations.declined")
                .tag("reason", "seat-not-found")
                .description("Reservations declined")
                .register(registry);
    }

    public void recordConfirmed() {
        confirmed.increment();
    }

    public void recordDecline(String reason) {

        switch (reason) {
            case "seat-taken" -> seatTaken.increment();
            case "per-user-limit" -> userLimit.increment();
            case "seat-not-found" -> seatNotFound.increment();
        }
    }

    public void recordIdempotentReplay() {
        idempotentReplay.increment();
    }
}
